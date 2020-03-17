package io.socket.jeromq.core;

import io.socket.jeromq.codec.JeroCodec;
import io.socket.jeromq.config.JeroConfig;
import io.socket.jeromq.support.JeroMessageCallback;
import io.socket.jeromq.model.ZHeader;
import io.socket.jeromq.utils.JeroUtil;
import lombok.extern.slf4j.Slf4j;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * pub - sub 请求订阅模式
 * 订阅模式下请务必将receiveTimeout设置成一个较小的值， 详细可见JeroConfig类
 *
 * @author xuejian.sun
 * @date 2019-03-20 17:32
 */
@Slf4j
public class JeroSub<T> {
    /**
     * sub connect config
     */
    protected JeroConfig jeroConfig;
    /**
     * sub topic name
     */
    protected ZHeader header;
    /**
     * 数据解码器
     */
    protected JeroCodec<T> jeroCodec;
    /**
     * 数据回调
     */
    protected JeroMessageCallback<T> callback;
    /**
     * 数据类型
     */
    protected Class<T> tClass;

    protected ZMQ.Socket subscribe;

    protected ZContext zContext;
    /**
     * 锁的作用， 防止在收的过程中取消订阅， 导致EINTR错误信号产生。
     */
    private ReentrantLock lock;

    private boolean isMeCreatedContext;

    /**
     * is subscribe now
     */
    protected AtomicBoolean running;

    public JeroSub(JeroConfig jeroConfig, ZHeader header, JeroCodec<T> jeroCodec, Class<T> tClass, JeroMessageCallback<T> callback) {
        this.jeroConfig = jeroConfig;
        this.header = header;
        this.jeroCodec = jeroCodec;
        this.callback = callback;
        this.tClass = tClass;
        // 此处需使用公平锁，如不使用公平锁，有可能导致无法取消订阅
        // 消费线程是个死循环，可能会一直持有该锁，造成unsub线程饥饿。
        this.lock = new ReentrantLock(true);
    }

    public void init() {
        init(new ZContext(1));
        isMeCreatedContext = true;
    }

    public void init(ZContext zContext) {
        if(this.zContext != null && this.subscribe != null) {
            return;
        }
        this.zContext = zContext;
        JeroUtil.setParams(jeroConfig, this.zContext);
        subscribeConnect();
    }

    /**
     * connect subscribe
     *
     * @return connect result {true,false}
     */
    private boolean subscribeConnect() {
        this.subscribe = zContext.createSocket(SocketType.SUB);
        JeroUtil.setParams(jeroConfig, this.subscribe);
        return this.subscribe.connect(jeroConfig.address());
    }

    /**
     * sub message
     */
    public JeroSub<T> sub() {
        if(zContext == null) {
            throw new NullPointerException("please use the init() method to initialize first.");
        }
        // 获取订阅的主题
        byte[] topic = header.subscribeTopic();
        this.subscribe.subscribe(topic);
        running = new AtomicBoolean(true);
        log.info("subscribe[ {},topic[{}], header[{}], is initialize and prepare rec message!",
                jeroConfig.address(), header.getZTopic().getTopicString(), header);
        return this;
    }

    /**
     * started receive subscribe msg
     */
    public void recv() {
        // 获取发送方主题的长度, 消息放送方默认是整个header中的默认字节数。
        int msgPrefixByteLength = header.toBytes().length;
        while(running.get() && !Thread.interrupted()) {
            lock.lock();
            try {
                if(running.get()) {
                    pollerMsg(msgPrefixByteLength);
                }
            } catch (ZMQException zmqException) {
                log.error("zmq rec failure .. , running -> {}", running.get(), zmqException);
                if(!running.get()) {
                    reconnect();
                }
            } catch (Exception e) {
                log.error("jero message rec failure .. running:{}", running.get(), e);
            } finally {
                lock.unlock();
            }
        }
        log.info("Sub[{},topic[{}], header[{}], process done ... ",
                jeroConfig.address(), header.getZTopic().getTopicString(), header);
    }

    /**
     * handling messages from ZMQ sockets
     * 子类对象可以重写该方法 来自定义处理从zmq套接字中收到的消息
     *
     * @param msgPrefixByteLength header length
     */
    protected void pollerMsg(int msgPrefixByteLength) {
        // 获取订阅到的消息，去除消息中的topic标识符，防止序列化到结构体中出现错误。
        byte[] recv = this.subscribe.recv();
        if(recv == null) {
            return;
        }
        byte[] body = new byte[recv.length - msgPrefixByteLength];
        System.arraycopy(recv, msgPrefixByteLength, body, 0, body.length);
        jeroCodec.decode(body, tClass)
                .ifPresent(t -> callback.callback(t));
    }

    protected void reconnect() {
        unsubscribe();
        int maxNums = jeroConfig.getReconnectMaxNums();
        boolean reconnect = false;
        while(!reconnect && maxNums > 0) {
            reconnect = subscribeConnect();
            maxNums--;
            try {
                log.info("reconnect subscribe -> {}", jeroConfig.address());
                TimeUnit.MILLISECONDS.sleep(jeroConfig.getReconnectInterval());
            } catch (InterruptedException e) {
                log.error("reconnect failure", e);
            }
        }
        if(!reconnect) {
            log.error("重连失败");
        }
    }

    /**
     * 安全的取消订阅。
     */
    public void unsubscribe() {
        if(subscribe != null && running.get()) {
            lock.lock();
            try {
                unsub();
            } catch (Exception e) {
                log.error("", e);
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 用于子类重写
     */
    protected void unsub() {
        running.compareAndSet(true, false);
        if(subscribe != null && subscribe.unsubscribe(header.toBytes())) {
            log.info("unsubscribed {},topic[{}], header[{}]", jeroConfig.address(), header.getZTopic().getTopicString(), header);
        }
        if(subscribe != null && subscribe.unbind(jeroConfig.address())) {
            log.info("unbind {},topic[{}], header[{}]", jeroConfig.address(), header.getZTopic().getTopicString(), header);
        }
        if(subscribe != null && subscribe.disconnect(jeroConfig.address())) {
            log.info("disconnect {},topic[{}], header[{}]", jeroConfig.address(), header.getZTopic().getTopicString(), header);
        }
        destroy();
    }


    /**
     * stop sub socket
     */
    public void destroy() {
        if(zContext != null && !zContext.isClosed()) {
            zContext.destroySocket(subscribe);
            subscribe = null;
            if(isMeCreatedContext) {
                zContext.close();
            }
        }
    }
}
