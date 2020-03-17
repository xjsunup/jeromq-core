package io.socket.jeromq.codec;

import org.zeromq.ZMQ;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/** bytes to String
 *
 * @author xuejian.sun
 * @date 2019-03-21 10:07
 */
public class JeroStringCodec implements JeroCodec<String> {

    @Override
    public Optional<String> decode(byte[] bytes, Class<String> stringClass) {
        return Optional.of(new String(bytes, StandardCharsets.UTF_8));
    }

    @Override
    public Optional<byte[]> encode(String s) {
        return Optional.of(s.getBytes(ZMQ.CHARSET));
    }
}
