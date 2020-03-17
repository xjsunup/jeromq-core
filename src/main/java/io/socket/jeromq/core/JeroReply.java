package io.socket.jeromq.core;

import io.socket.jeromq.config.JeroConfig;
import io.socket.jeromq.support.JeroRequestReplyMsgCallback;
import lombok.extern.slf4j.Slf4j;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** request reply 请求应答模式
 *
 * @author xuejian.sun
 * @date 2019-07-16 09:41
 */
@Slf4j
public class JeroReply implements JeroMsgSend {

    private ZContext zContext;

    private JeroConfig jeroConfig;

    private JeroRequestReplyMsgCallback<byte[]> callback;

    private ZMQ.Socket reply;

    private AtomicBoolean running;

    private ExecutorService singleThreadReceiver;

    public JeroReply(JeroConfig jeroConfig, JeroRequestReplyMsgCallback<byte[]> callback) {
        this(jeroConfig,new ZContext(1), callback);
    }

    public JeroReply(JeroConfig jeroConfig, ZContext zContext, JeroRequestReplyMsgCallback<byte[]> callback) {
        this.jeroConfig = jeroConfig;
        this.callback = callback;
        this.zContext = zContext;
        this.running = new AtomicBoolean(false);
        this.singleThreadReceiver = new ThreadPoolExecutor(1, 1,
                0, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(1),
                runnable -> new Thread(runnable, "Reply_"+jeroConfig.getIdentity()+" -> " + jeroConfig.address()));
        initResp();
    }

    private void initResp() {
        this.reply = zContext.createSocket(SocketType.REP);
        this.reply.setReceiveTimeOut(jeroConfig.getReceiveTimeout());
        this.reply.setTCPKeepAliveCount(jeroConfig.getTcpKeepAlive());
        this.reply.setTCPKeepAlive(jeroConfig.getTcpKeepAlive());
        this.reply.setTCPKeepAliveIdle(jeroConfig.getTcpKeepAliveIdle());
        this.reply.setTCPKeepAliveInterval(jeroConfig.getTcpKeepAliveInterval());
        this.reply.bind(jeroConfig.address());
    }

    public void receive() {
        if(!running.get()) {
            running.compareAndSet(false, true);
            singleThreadReceiver.submit(() -> {
                while(running.get()) {
                    try {
                        byte[] recv = reply.recv(0);
                        if(recv != null) {
                            callback.callback(recv, this);
                        }
                    }catch (ZMQException e){
                        int errorCode = e.getErrorCode();
                        //正常线程中断
                        if(errorCode != 156384765){
                            log.error("",e);
                        }
                    } catch (Exception e){
                        log.error("",e);
                    }
                }
            });
        }
    }

    @Override
    public void send(String message) {
        reply.send(message,0);
    }

    @Override
    public void send(byte[] more, String message) {
        reply.sendMore(message);
        reply.send(message);
    }

    @Override
    public void send(byte[] message) {
        reply.send(message,0);
    }

    @Override
    public void send(byte[] more, byte[] message) {
        reply.sendMore(message);
        reply.send(message);
    }

    public void destroy() {
        running.compareAndSet(true,false);
        if(reply != null){
            reply.close();
        }
        if(zContext != null){
            zContext.close();
        }
        log.info("JeroReply_{} Closed: {}", jeroConfig.getIdentity(), jeroConfig.address());
    }
}
