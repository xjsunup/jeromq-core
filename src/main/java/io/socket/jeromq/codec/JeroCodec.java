package io.socket.jeromq.codec;

import java.util.Optional;

/**
 * @author xuejian.sun
 * @date 2019-03-21 10:05
 */
public interface JeroCodec<T> {
    /**
     * 将二进制数据解析成 T 类型
     * @param bytes 消息
     * @param tClass tClass
     * @return T
     */
    Optional<T> decode(byte[] bytes, Class<T> tClass);

    /**
     * 将参数编码
     * @param t 参数
     * @return bytes
     */
    Optional<byte[]> encode(T t);
}
