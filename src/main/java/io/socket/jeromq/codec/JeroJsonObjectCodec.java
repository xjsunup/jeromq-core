package io.socket.jeromq.codec;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/** bytes to JSON object
 *
 * @author xuejian.sun
 * @date 2019-03-21 10:08
 */
@Slf4j
public class JeroJsonObjectCodec<T> implements JeroCodec<T> {

    @Override
    public Optional<T> decode(byte[] bytes, Class<T> tClass) {
        try {
            return Optional.ofNullable(JSON.parseObject(bytes,tClass));
        }catch (Exception e){
            log.error("",e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<byte[]> encode(T t) {
        try {
            return Optional.ofNullable(JSON.toJSONBytes(t));
        }catch (Exception e){
            log.error("",e);
            return Optional.empty();
        }
    }
}
