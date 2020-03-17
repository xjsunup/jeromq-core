package io.socket.jeromq.enums;

import java.nio.charset.StandardCharsets;

/**
 * @author xuejian.sun
 * @date 2019-03-20 18:41
 */
public enum JeroTopic implements ZTopic {
    /**
     * 默认为空，不接受任何类型
     */
    NONE("NONE",100),
    /**
     * 任何消息
     */
    ALL("", 127),

    HELLO("hello", 126),

    PROTOBUF("protobuf", 125),

    JSON_TEST("json", 124);

    JeroTopic(String topic, int dataTypeCode) {
        this.topic = topic;
        this.dataTypeCode = dataTypeCode;
    }

    private String topic;

    private int dataTypeCode;

    @Override
    public byte[] getTopic() {
        return topic.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String getTopicString() {
        return topic;
    }

    @Override
    public int getDataTypeCode() {
        return dataTypeCode;
    }
}
