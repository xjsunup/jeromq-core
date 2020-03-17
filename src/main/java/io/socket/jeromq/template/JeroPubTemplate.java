package io.socket.jeromq.template;

import com.baidu.bjf.remoting.protobuf.Codec;
import com.baidu.bjf.remoting.protobuf.ProtobufProxy;
import io.socket.jeromq.codec.JeroJsonObjectCodec;
import io.socket.jeromq.config.JeroConfig;
import io.socket.jeromq.core.JeroPub;
import io.socket.jeromq.enums.AsyncTaskEnum;
import io.socket.jeromq.enums.PubThreadType;
import io.socket.jeromq.model.ZHeader;
import io.socket.jeromq.utils.ByteUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author xuejian.sun
 * @date 2019-03-22 19:01
 */
@Slf4j
public class JeroPubTemplate implements Closeable {

    private ZContext zContext;

    private JeroPub jeroPub;

    @Getter
    private String tagName;

    private JeroJsonObjectCodec<Object> jeroJsonObjectCodec;

    public JeroPubTemplate(JeroConfig jeroConfig, boolean bind, PubThreadType pubThreadType
            , AsyncTaskEnum asyncTaskEnum, int maxTaskSize) {
        if(pubThreadType == PubThreadType.MULTI) {
            asyncTaskEnum = asyncTaskEnum == null ? AsyncTaskEnum.DISRUPTOR : asyncTaskEnum;
            maxTaskSize = maxTaskSize == 0 ? 1024 : maxTaskSize;
        }
        zContext = new ZContext(jeroConfig.getIoThread());
        jeroPub = new JeroPub(jeroConfig, pubThreadType, asyncTaskEnum, maxTaskSize);
        this.tagName = "Publisher@" + jeroPub.hashCode() + ":" + jeroConfig.address();
        jeroPub.init(zContext);
        if(bind) {
            jeroPub.bind();
        } else {
            jeroPub.connect();
        }
    }

    public JeroPubTemplate(JeroConfig jeroConfig) {
        this(jeroConfig, true, PubThreadType.MULTI, AsyncTaskEnum.DISRUPTOR, 1024);
    }

    public JeroPubTemplate(JeroConfig jeroConfig, PubThreadType pubThreadType) {
        this(jeroConfig, true, pubThreadType, null, 0);
    }

    public JeroPubTemplate(JeroConfig jeroConfig, boolean bind, PubThreadType pubThreadType) {
        this(jeroConfig, bind, pubThreadType, null, 0);
    }

    public JeroPubTemplate(JeroConfig jeroConfig, boolean bind) {
        this(jeroConfig, bind, PubThreadType.MULTI, AsyncTaskEnum.DISRUPTOR, 1024);
    }

    public void sendMessage(String message) {
        jeroPub.send(message);
    }

    public void sendMore(byte[] more, byte[] message) {
        jeroPub.send(more, message);
    }

    public void sendMessage(byte[] message) {
        jeroPub.send(message);
    }

    /**
     * topic 和消息拼接到一起发送
     *
     * @param topic   主题消息
     * @param message message
     */
    public void sendMessage(String topic, String message) {
        sendMessage(topic + message);
    }

    /**
     * topic 和消息拼接到一起发送
     *
     * @param topic   主题消息
     * @param message 字节消息
     */
    public void sendMessage(String topic, byte[] message) {
        byte[] topicByte = topic.getBytes(ZMQ.CHARSET);
        byte[] sendMsg = byteMerge(topicByte, message);
        sendMessage(sendMsg);
    }

    public void sendMessage(ZHeader header, byte[] message) {
        byte[] topicByte = header.toBytes();
        byte[] sendMsg = byteMerge(topicByte, message);
        sendMessage(sendMsg);
    }

    public void sendMessage(ZHeader header, String message) {
        sendMessage(header, message.getBytes(ZMQ.CHARSET));
    }

    /**
     * 将对象转为json，和topic组合成一条消息一起发送
     *
     * @param topic 主题消息
     * @param obj   数据对象
     * @param <T>   对象类型
     */
    public <T> void sendJsonMessage(String topic, T obj) {
        if(jeroJsonObjectCodec == null) {
            jeroJsonObjectCodec = new JeroJsonObjectCodec<>();
        }
        jeroJsonObjectCodec.encode(obj)
                .ifPresent(bytes -> sendMessage(topic, bytes));
    }


    /**
     * 发送protobuf对象数据，类对象上必须使用百度框架@ProtobufClass标记，不然无法序列化
     * see more:<l>https://github.com/jhunters/jprotobuf</l>
     *
     * @param topic  主题消息
     * @param obj    protobuf 对象 类上需要使用@ProtobufClass标记
     * @param tClass class类型
     * @param <T>    t
     */
    public <T> void sendProtobufObjMessage(String topic, T obj, Class<T> tClass) {
        sendProtobufObjMessage(topic.getBytes(ZMQ.CHARSET), obj, tClass);
    }

    /**
     * 发送PB对象数据，类对象上必须使用百度框架@ProtobufClass标记，不然无法序列化
     *
     * @param header 消息头信息。
     * @param obj    protobuf 对象 类上需要使用@ProtobufClass标记
     * @param tClass class类型
     * @param <T>    t
     */
    public <T> void sendProtobufObjMessage(ZHeader header, T obj, Class<T> tClass) {
        sendProtobufObjMessage(header.toBytes(), obj, tClass);
    }

    public <T> void sendProtobufObjMessage(byte[] topic, T obj, Class<T> tClass) {
        Codec<T> codec = ProtobufProxy.create(tClass);
        byte[] protobufMessage;
        try {
            protobufMessage = codec.encode(obj);
        } catch (IOException e) {
            log.error("protobuf encode failure..", e);
            return;
        }
        byte[] data = byteMerge(topic, protobufMessage);
        sendMessage(data);
    }

    /**
     * Combine two byte arrays into one
     *
     * @param topic message topic
     * @param data  message
     * @return topic+message
     */
    private byte[] byteMerge(byte[] topic, byte[] data) {
        return ByteUtil.merge(topic, data);
    }

    private void destroy() {
        if(jeroPub != null) {
            jeroPub.destroy();
        }
        zContext.destroy();
    }

    @Override
    public void close() {
        log.info("{} is auto closed", tagName);
        destroy();
    }
}
