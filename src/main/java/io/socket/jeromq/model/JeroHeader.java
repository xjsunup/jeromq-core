package io.socket.jeromq.model;

import io.socket.jeromq.enums.StringTopic;
import io.socket.jeromq.enums.ZTopic;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.nio.ByteBuffer;

/**
 * @author xuejian.sun
 * @date 2019-07-10 14:23
 */
@ToString
@Getter
@Builder
public class JeroHeader implements ZHeader {
    /**
     * 主题信息
     */
    private ZTopic zTopic;

    @Override
    public byte[] toBytes() {
        if(zTopic == null) {
            throw new NullPointerException("ZTopic must not be null!");
        }
        if(zTopic instanceof StringTopic) {
            return zTopic.getTopic();
        }
        ByteBuffer allocate = ByteBuffer.allocate(3);
        allocate.put((byte) zTopic.getDataTypeCode());
        return allocate.array();
    }

    @Override
    public byte[] subscribeTopic() {
        if(zTopic == null) {
            throw new NullPointerException("ZTopic must not be null!");
        }
        if(zTopic instanceof StringTopic) {
            return zTopic.getTopic();
        }
        ByteBuffer allocate = ByteBuffer.allocate(effectiveBytes());
        allocate.put((byte) zTopic.getDataTypeCode());
        return allocate.array();
    }

    /**
     * 有效字节数
     *
     * @return byte length
     */
    private int effectiveBytes() {
        return zTopic.getTopic().length;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }
        if(o == null || getClass() != o.getClass()) {
            return false;
        }
        JeroHeader header = (JeroHeader) o;
        return zTopic == header.zTopic ;
    }

    @Override
    public int hashCode() {
        return zTopic != null ? zTopic.hashCode() : 0;
    }
}
