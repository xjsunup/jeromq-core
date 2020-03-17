package io.socket.jeromq.enums;

import lombok.ToString;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author xuejian.sun
 * @date 2019-07-11 15:29
 */
@ToString
public class StringTopic implements ZTopic {

    private String topic = "";

    private StringTopic(String topic) {
        if(topic != null) {
            this.topic = topic;
        }
    }

    private StringTopic() {
    }

    public static StringTopic subscribe(String topic) {
        return new StringTopic(topic);
    }

    public static StringTopic subscribeAll() {
        return new StringTopic();
    }

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
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        StringTopic that = (StringTopic) o;
        return Objects.equals(topic, that.topic);
    }

    @Override
    public int hashCode() {
        int result = topic != null ? topic.hashCode() : 0;
        return 31 * result;
    }
}
