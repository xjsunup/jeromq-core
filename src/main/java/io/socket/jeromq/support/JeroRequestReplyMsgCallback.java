package io.socket.jeromq.support;

import io.socket.jeromq.core.JeroMsgSend;

/**
 * @author xuejian.sun
 * @date 2019-07-16 10:27
 */
public interface JeroRequestReplyMsgCallback<T> {
    /**
     * 请求应答模式消息回掉器
     * @param t 消息
     * @param jeroMsgSend 当前消息通道
     */
    void callback(T t, JeroMsgSend jeroMsgSend);
}
