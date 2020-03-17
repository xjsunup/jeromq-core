package io.socket.jeromq.codec;

import java.util.Optional;

/**
 * @author xuejian.sun
 * @date 2019-07-12 13:49
 */
public class JeroByteCodec implements JeroCodec<byte[]> {

    @Override
    public Optional<byte[]> decode(byte[] bytes, Class<byte[]> aClass) {
        return Optional.of(bytes);
    }

    @Override
    public Optional<byte[]> encode(byte[] bytes) {
        return Optional.of(bytes);
    }
}
