package io.socket.jeromq.template;

import io.socket.jeromq.codec.JeroByteCodec;
import io.socket.jeromq.codec.JeroCodec;
import io.socket.jeromq.config.NamedThreadFactory;
import io.socket.jeromq.config.JeroConfig;
import io.socket.jeromq.core.JeroSub;
import io.socket.jeromq.model.ZHeader;
import io.socket.jeromq.support.JeroMessageCallback;
import io.socket.jeromq.support.JeroMessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.zeromq.ZContext;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * ZeroMQ subscriber 统一调度器，共享一个ZContext和地址。
 *
 * @author xuejian.sun
 * @date 2019-03-21 13:46
 */
@Slf4j
public class JeroSubTemplate implements Closeable {

    private ZContext zContext;
    /**
     * sub group 通用配置
     */
    private JeroConfig jeroConfig;

    /**
     * 存放所有和topic绑定的JeroSub
     */
    private Map<ZHeader, JeroSub> jeroSubMaps;
    /**
     * sub收到消息后会回掉其中的方法。
     */
    private JeroMessageHandler jeroMessageHandler;

    /**
     * 线程池，用于调度JeroSub group中的任务。
     */
    private ThreadPoolExecutor threadPoolExecutor;

    public JeroSubTemplate(JeroConfig jeroConfig
            , JeroMessageHandler jeroMessageHandler) {
        this.jeroConfig = jeroConfig;
        this.jeroMessageHandler = jeroMessageHandler;
        zContext = new ZContext(jeroConfig.getIoThread());
        jeroSubMaps = new HashMap<>(16);
        threadPoolExecutor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 30
                , TimeUnit.SECONDS, new SynchronousQueue<>()
                , new NamedThreadFactory("JeroSubThreadPool", false));
    }

    public JeroSubTemplate(JeroConfig jeroConfig) {
        this(jeroConfig, null);
    }

    /**
     * 订阅消息，use JeroMessageHandler callback msg
     *
     * @param header 消息头信息
     * @param codec  编解码器
     * @param tClass 返回数据类型，有解码器将数据序列化到该类型中
     * @param <T>    数据类型
     * @return this
     */
    public <T> JeroSubTemplate subscribe(ZHeader header, JeroCodec<T> codec, Class<T> tClass) {
        if(jeroMessageHandler == null) {
            log.warn("subscribe fail, JeroMessageHandler is Null!");
            return this;
        }
        if(!jeroSubMaps.containsKey(header)) {
            JeroSub<T> jeroSub = new JeroSub<>(jeroConfig, header,
                    codec, tClass, t -> jeroMessageHandler.forward(header, t));
            jeroSub.init(zContext);
            jeroSub.sub();
            threadPoolExecutor.submit(jeroSub::recv);
            jeroSubMaps.put(header, jeroSub);
        }
        return this;
    }

    /**
     * 订阅消息
     *
     * @param header      消息头信息
     * @param codec       编解码器
     * @param tClass      返回数据类型，有解码器将数据序列化到该类型中
     * @param <T>         数据类型
     * @param msgCallback 消息回调
     * @return this
     */
    public <T> JeroSubTemplate subscribe(ZHeader header, JeroCodec<T> codec, Class<T> tClass, JeroMessageCallback<T> msgCallback) {
        if(msgCallback == null) {
            log.warn("subscribe fail, Callback is Null!");
            return this;
        }
        if(!jeroSubMaps.containsKey(header)) {
            JeroSub<T> jeroSub = new JeroSub<>(jeroConfig, header,
                    codec, tClass, msgCallback);
            jeroSub.init(zContext);
            jeroSub.sub();
            threadPoolExecutor.submit(jeroSub::recv);
            jeroSubMaps.put(header, jeroSub);
        }
        return this;
    }

    /**
     * 订阅消息，使用默认的byte消息
     *
     * @param header 消息头信息
     * @return this
     */
    public JeroSubTemplate subscribe(ZHeader header) {
        if(jeroMessageHandler == null) {
            log.warn("subscribe fail, JeroMessageHandler is Null!");
            return this;
        }
        if(!jeroSubMaps.containsKey(header)) {
            JeroSub<byte[]> jeroSub = new JeroSub<>(jeroConfig, header,
                    new JeroByteCodec(), null, t -> jeroMessageHandler.forward(header, t));
            jeroSub.init(zContext);
            jeroSub.sub();
            threadPoolExecutor.submit(jeroSub::recv);
            jeroSubMaps.put(header, jeroSub);
        }
        return this;
    }

    /**
     * 订阅消息，使用默认的byte消息
     *
     * @param header 消息头信息
     * @return this
     */
    public JeroSubTemplate subscribe(ZHeader header, JeroMessageCallback<byte[]> msgCallback) {
        if(msgCallback == null) {
            log.warn("subscribe fail, Callback is Null!");
            return this;
        }
        if(!jeroSubMaps.containsKey(header)) {
            JeroSub<byte[]> jeroSub = new JeroSub<>(jeroConfig, header,
                    new JeroByteCodec(), null, msgCallback);
            jeroSub.init(zContext);
            jeroSub.sub();
            threadPoolExecutor.submit(jeroSub::recv);
            jeroSubMaps.put(header, jeroSub);
        }
        return this;
    }

    /**
     * 取消订阅
     *
     * @param header message header
     * @return this
     */
    public JeroSubTemplate unsubscribe(ZHeader header) {
        if(jeroSubMaps.containsKey(header)) {
            JeroSub jeroSub = jeroSubMaps.get(header);
            jeroSub.unsubscribe();
            jeroSubMaps.remove(header);
        }
        return this;
    }

    @Override
    public void close() {
        if(Objects.nonNull(jeroSubMaps) && !jeroSubMaps.isEmpty()) {
            jeroSubMaps.forEach((k, v) -> v.unsubscribe());
        }
        if(threadPoolExecutor != null && !threadPoolExecutor.isShutdown()) {
            threadPoolExecutor.shutdown();
        }
        if(zContext != null && !zContext.isClosed()) {
            zContext.close();
        }
    }
}
