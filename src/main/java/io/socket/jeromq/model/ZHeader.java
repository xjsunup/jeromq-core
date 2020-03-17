package io.socket.jeromq.model;

import io.socket.jeromq.enums.ZTopic;

/**
 * @author xuejian.sun
 * @date 2019-07-24 14:33
 */
public interface ZHeader {
    /**
     * 实例对象中所有字段转为字节数组。
     *
     * @return bytes
     */
    byte[] toBytes();

    /**
     * 订阅主题, 默认值将不转出byte，否则有可能订阅不到任何数据，
     * a因为zq订阅的特殊性，前缀订阅匹配
     * 例如数据123， 可以使用 1 或 12 或 123 作为topic都可以订阅到该数据。
     *
     * @return bytes
     */
    byte[] subscribeTopic();

    /**
     * zt
     *
     * @return
     */
    ZTopic getZTopic();
}
