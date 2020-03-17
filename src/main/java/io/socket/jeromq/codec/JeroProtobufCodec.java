package io.socket.jeromq.codec;

import com.baidu.bjf.remoting.protobuf.Codec;
import com.baidu.bjf.remoting.protobuf.ProtobufProxy;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Optional;

/** protobuf bytes encode decode
 * 使用百度封装过后的protobuf解码器，使用该解码器，T类型必须标记为@ProtobufClass
 * see more: <l>https://github.com/jhunters/jprotobuf</l>
 *
 * @author xuejian.sun
 * @date 2019-03-21 10:20
 */
@Slf4j
public class JeroProtobufCodec<T> implements JeroCodec<T> {
    @Override
    public Optional<T> decode(byte[] bytes, Class<T> tClass) {
        Codec<T> codec = ProtobufProxy.create(tClass);
        try {
             return Optional.of(codec.decode(bytes));
        } catch (IOException e) {
            log.error("decode failure",e);
            return Optional.empty();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<byte[]> encode(T t) {
        Codec<T> codec = (Codec<T>) ProtobufProxy.create(t.getClass());
        try {
            return Optional.of(codec.encode(t));
        } catch (IOException e) {
            log.error("encode failure",e);
            return Optional.empty();
        }
    }
}
