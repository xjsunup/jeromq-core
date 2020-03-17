package io.socket.jeromq.core;

import io.socket.jeromq.codec.JeroCodec;
import io.socket.jeromq.config.JeroConfig;
import io.socket.jeromq.model.ZHeader;
import io.socket.jeromq.support.JeroMessageCallback;
import io.socket.jeromq.utils.ByteUtil;
import io.socket.jeromq.utils.JeroUtil;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.zeromq.SocketType;
import org.zeromq.ZContext;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author xuejian.sun
 * @date 2019-07-23 15:52
 */
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class JeroXSub<T> extends JeroSub<T> implements JeroMsgSend {

    private static final byte[] SUBSCRIBER = {1};

    private static final byte[] UN_SUBSCRIBER = {0};

    private ExecutorService singleThread;

    public JeroXSub(JeroConfig jeroConfig, ZHeader header,
                    JeroCodec<T> jeroCodec, Class<T> tClass, JeroMessageCallback<T> callback) {
        super(jeroConfig, header, jeroCodec, tClass, callback);
        String processName;
        if(!jeroConfig.getIdentity().isEmpty()) {
            processName = jeroConfig.getIdentity() + "_XSub";
        } else {
            processName = "JeroXSub";
        }
        singleThread = new ThreadPoolExecutor(1, 1, 0,
                TimeUnit.SECONDS, new LinkedBlockingQueue<>(1), runnable -> new Thread(runnable, processName));
    }

    @Override
    public void init(ZContext zContext) {
        super.zContext = zContext;
        JeroUtil.setParams(super.jeroConfig, super.zContext);
        super.subscribe = super.zContext.createSocket(SocketType.XSUB);
        JeroUtil.setParams(super.jeroConfig, super.subscribe);
        subscribe.connect(jeroConfig.address());
    }

    @Override
    public void send(String message) {
        if(subscribe != null) {
            subscribe.send(message);
        }
    }

    @Override
    public void send(byte[] more, String message) {
        if(subscribe != null) {
            subscribe.sendMore(more);
            subscribe.send(message);
        }
    }

    @Override
    public void send(byte[] message) {
        if(subscribe != null) {
            subscribe.send(message);
        }
    }

    @Override
    public void send(byte[] more, byte[] message) {
        if(subscribe != null) {
            subscribe.sendMore(more);
            subscribe.send(message);
        }
    }

    @Override
    public JeroXSub<T> sub() {
        if(zContext == null) {
            throw new NullPointerException("please use the init() method to initialize first.");
        }
        // 获取订阅的主题
        byte[] topic = header.subscribeTopic();
        // 获取发送方主题的长度, 消息放送方默认是整个header中的默认字节数。
        int msgHeadLength = header.toBytes().length;
        send(ByteUtil.merge(JeroXSub.SUBSCRIBER, topic));
        running = new AtomicBoolean(true);
        log.info("XSubscribe[ {},topic[{}], header[{}], is initialize and prepare rec message!",
                jeroConfig.address(), header.getZTopic().getTopicString(), header);
        return this;
    }

    @Override
    public void recv() {
        singleThread.execute(() -> {
            super.recv();
            log.info("jeroXSub[{},topic[{}], header[{}], process done ... ",
                    jeroConfig.address(), header.getZTopic().getTopicString(), header);
        });
    }

    @Override
    protected void unsub() {
        running.compareAndSet(true, false);
        if(subscribe != null) {
            subscribe.send(ByteUtil.merge(JeroXSub.UN_SUBSCRIBER, header.subscribeTopic()));
        }
        log.info("XSub unsubscribe {},topic[{}], header[{}]", jeroConfig.address(), header.getZTopic().getTopicString(), header);
        if(subscribe != null && subscribe.unbind(jeroConfig.address())) {
            log.info("XSub unbind {},topic[{}], header[{}]", jeroConfig.address(), header.getZTopic().getTopicString(), header);
        }
        if(subscribe != null && subscribe.disconnect(jeroConfig.address())) {
            log.info("XSub disconnect {},topic[{}], header[{}]", jeroConfig.address(), header.getZTopic().getTopicString(), header);
        }
        destroy();
    }

    @Override
    public void destroy() {
        super.destroy();
        if(singleThread != null && !singleThread.isShutdown()) {
            singleThread.shutdown();
        }
    }
}
