package io.socket.jeromq.enums;

/**
 * @author xuejian.sun
 * @date 2019-03-22 14:18
 */
public interface ZTopic {
    /**
     * 获取topic
     * @return byte topic
     */
    byte[] getTopic();
    /**
     * 获取topic
     * @return String topic
     */
    String getTopicString();

    /**
     * 获取数据类型代码
     * @return int code
     */
    int getDataTypeCode();
}
