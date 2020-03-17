package io.socket.jeromq.support;

import io.socket.jeromq.core.JeroMsgSend;

/**
 * zmq中，XPUB收到的消息第一个字节为标识位
 * 1： 表示订阅请求
 * 0： 表示取消订阅
 *
 * @author xuejian.sun
 * @date 2019-07-25 14:34
 */
public interface JeroXPubMsgCallback<T> {
    /**
     * 订阅
     * @param msg 消息
     * @param msgSend xPub
     */
    void onSubscribe(T msg, JeroMsgSend msgSend);
    /**
     * 取消订阅
     * @param msg 消息
     * @param msgSend xPub
     */
    void onUnSubscribe(T msg, JeroMsgSend msgSend);

    /**
     * 其他指令
     * @param command 指令
     * @param msg 消息
     * @param msgSend xPub
     */
    void onOtherRequest(byte command, T msg, JeroMsgSend msgSend);

    /**
     * 异常回调
     * @param e exception
     */
    void onError(Exception e);
}
