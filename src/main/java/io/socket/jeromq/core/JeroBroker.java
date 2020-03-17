package io.socket.jeromq.core;

import io.socket.jeromq.config.JeroConfig;
import io.socket.jeromq.support.JeroBrokerCallback;
import io.socket.jeromq.utils.ByteUtil;
import io.socket.jeromq.utils.JeroUtil;
import lombok.extern.slf4j.Slf4j;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.Closeable;

/**
 * @author xuejian.sun
 * @date 2019-07-18 14:45
 */
@Slf4j
public class JeroBroker implements Closeable {

    private static final byte[] SUBSCRIBE_FLAG = new byte[]{1};
    /**
     * xSub 配置
     */
    private JeroConfig xSubConfig;
    /**
     * xPub 配置
     */
    private JeroConfig xPubConfig;

    private ZContext zContext;

    private ZMQ.Socket xPub;

    private ZMQ.Socket xSub;

    private JeroBrokerCallback brokerCallback;

    public JeroBroker(JeroConfig xSubConfig, JeroConfig xPubConfig, JeroBrokerCallback brokerCallback) {
        this.xPubConfig = xPubConfig;
        this.xSubConfig = xSubConfig;
        this.brokerCallback = brokerCallback;
        log.info("X-PUB: {}", xPubConfig.address());
        log.info("X-SUB: {}", xSubConfig.address());
    }

    public void init() {
        this.zContext = new ZContext(1);
        JeroUtil.setParams(xPubConfig,zContext);
        this.xPub = zContext.createSocket(SocketType.XPUB);
        JeroUtil.setParams(xPubConfig,xPub);
        xPub.bind(xPubConfig.address());
        xPub.setXpubVerbose(true);
        this.xSub = zContext.createSocket(SocketType.XSUB);
        JeroUtil.setParams(xPubConfig,xSub);
        xSub.bind(xSubConfig.address());
        xSub.send(SUBSCRIBE_FLAG);
        ZMQ.Poller poller = zContext.createPoller(2);
        poller.register(xPub, ZMQ.Poller.POLLIN);
        poller.register(xSub, ZMQ.Poller.POLLIN);
        while(!Thread.interrupted()) {
            try {
                int msgSize = poller.poll(200);
                if(msgSize <= 0){
                    continue;
                }
                // 处理订阅和取消订阅请求
                if(poller.pollin(0)) {
                    byte[] more = null;
                    if(xPub.hasReceiveMore()) {
                        more = xPub.recv();
                    }
                    byte[] recv = xPub.recv();

                    byte subTag = recv[0];
                    byte[] msg = ByteUtil.subBytes(recv, 1);
                    if(subTag == 1) {
                        brokerCallback.onSubscribe(msg, more);
                    } else {
                        brokerCallback.onUnSubscribe(msg, more);
                    }
                }

                //转发PUB消息
                if(poller.pollin(1)) {
                    if(xSub.hasReceiveMore()) {
                        xPub.sendMore(xSub.recv());
                    }
                    byte[] msg = xSub.recv();
                    // Broker it
                    brokerCallback.forwardMessage(msg, xPub);
//                    log.debug("[Broker] Forwarded message: {}", msg);
                }
            } catch (Exception e) {
                brokerCallback.onError(e);
            }
        }
    }

    @Override
    public void close() {
        if(zContext != null && !zContext.isClosed()) {
            if(xPub != null && xPub.unbind(xPubConfig.address())) {
                log.info("X-PUB[{}] unbind successful", xPubConfig.address());
            }
            if(xSub != null && xSub.unbind(xSubConfig.address())) {
                log.info("X-SUB[{}] unbind successful", xSubConfig.address());
            }
            zContext.destroySocket(xPub);
            zContext.destroySocket(xSub);
        }
        log.info("JeroBroker is closed!");
    }
}
