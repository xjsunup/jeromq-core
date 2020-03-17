package io.socket.jeromq.core;

import io.socket.jeromq.codec.JeroCodec;
import io.socket.jeromq.config.JeroConfig;
import io.socket.jeromq.support.JeroXPubMsgCallback;
import io.socket.jeromq.support.LinkedAsyncTaskExecutor;
import io.socket.jeromq.utils.ByteUtil;
import io.socket.jeromq.utils.JeroUtil;
import lombok.extern.slf4j.Slf4j;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author xuejian.sun
 * @date 2019-07-24 10:48
 */
@Slf4j
public class JeroXPub<T> implements JeroMsgSend {

    private JeroConfig jeroConfig;
    /**
     * 数据解码器
     */
    protected JeroCodec<T> jeroCodec;
    /**
     * 数据回调
     */
    protected JeroXPubMsgCallback<T> callback;
    /**
     * 数据类型
     */
    protected Class<T> tClass;

    private ZContext context;

    private ZMQ.Socket publisher;

    private LinkedAsyncTaskExecutor<byte[]> pubThread;

    private ExecutorService recThread;

    private volatile boolean running;

    private boolean isMeCreatedCtx;

    private ReentrantLock lock;

    public JeroXPub(JeroConfig jeroConfig, JeroCodec<T> jeroCodec, Class<T> tClass, JeroXPubMsgCallback<T> messageCallback) {
        this.jeroConfig = jeroConfig;
        this.jeroCodec = jeroCodec;
        this.tClass = tClass;
        this.callback = messageCallback;
        this.lock = new ReentrantLock(true);
        this.recThread = new ThreadPoolExecutor(1, 1,
                0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1), runnable -> new Thread(runnable, "JeroXPub_Rec" + this.hashCode()));
    }

    public JeroXPub init() {
        this.isMeCreatedCtx = true;
        return init(new ZContext(1));
    }

    public JeroXPub init(ZContext zContext) {
        this.context = zContext;
        JeroUtil.setParams(jeroConfig, context);
        this.publisher = context.createSocket(SocketType.XPUB);
        JeroUtil.setParams(jeroConfig, publisher);
        publisher.bind(jeroConfig.address());
        this.pubThread = new LinkedAsyncTaskExecutor<>("JeroXPub_" + jeroConfig.getIdentity(), publisher::send);
        return this;
    }

    public void recvMsg() {
        if(context == null) {
            throw new NullPointerException("please use the init() method to initialize first.");
        }
        if(running) {
            return;
        }
        running = true;
        recThread.execute(() -> {
            while(running && !Thread.interrupted()) {
                lock.lock();
                try {
                    if(running) {
                        pollerMsg();
                    }
                } catch (ZMQException e) {
                    ZMQ.Error error = ZMQ.Error.findByCode(e.getErrorCode());
                    running = false;
                    log.error("{}", error.getMessage());
                } catch (Exception e) {
                    log.info("poll msg failure", e);
                }finally {
                    lock.unlock();
                }
            }
        });
    }

    public void pollerMsg() {
        if(publisher == null) {
            running = false;
            return;
        }
        byte[] recv = publisher.recv();
        if(recv == null) {
            return;
        }
        //zmq中，XPUB收到的消息第一个字节为标识位
        // 1： 表示订阅请求
        // 0： 表示取消订阅
        byte[] data = ByteUtil.subBytes(recv, 1);
        byte flag = recv[0];
        try {
            jeroCodec.decode(data, tClass)
                    .ifPresent(msg -> {
                        if(flag == 1) {
                            callback.onSubscribe(msg, this);
                        } else if(flag == 0) {
                            callback.onUnSubscribe(msg, this);
                        } else {
                            callback.onOtherRequest(flag, msg, this);
                        }
                    });
        } catch (Exception e) {
            callback.onError(e);
        }
    }

    @Override
    public void send(String message) {
        if(Objects.nonNull(message) && Objects.nonNull(publisher)) {
            pubThread.addTask(message.getBytes(ZMQ.CHARSET));
        }
    }

    @Override
    public void send(byte[] more, String message) {
        if(Objects.nonNull(message) && Objects.nonNull(publisher)) {
            pubThread.addTask(ByteUtil.merge(more, message.getBytes(ZMQ.CHARSET)));
        }
    }

    @Override
    public void send(byte[] message) {
        if(Objects.nonNull(message) && Objects.nonNull(publisher)) {
            pubThread.addTask(message);
        }
    }

    @Override
    public void send(byte[] more, byte[] message) {
        if(Objects.nonNull(message) && Objects.nonNull(publisher)) {
            pubThread.addTask(ByteUtil.merge(more, message));
        }
    }

    public void destroy() {
        lock.lock();
        try {
            running = false;
            if(!context.isClosed() && publisher != null) {
                publisher.unbind(jeroConfig.address());
                context.destroySocket(publisher);
                if(isMeCreatedCtx) {
                    context.close();
                }
            }
            if(recThread != null && !recThread.isShutdown()) {
                recThread.shutdown();
            }
            if(pubThread != null && !pubThread.isShutdown()) {
                pubThread.shutdown();
            }
        } finally {
            lock.unlock();
        }
    }
}
