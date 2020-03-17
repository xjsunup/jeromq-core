package io.socket.jeromq.support;

import org.zeromq.ZMQ;

/**
 * @author xuejian.sun
 * @date 2019/9/4 9:20
 */
public interface JeroBrokerCallback {

    /**
     * 订阅
     *
     * @param msg 消息
     */
    void onSubscribe(byte[] msg, byte[] sendMore);

    /**
     * 取消订阅
     *
     * @param msg 消息
     */
    void onUnSubscribe(byte[] msg, byte[] sendMore);

    /**
     * xSub收到 pub发送的消息， 将消息通过xPub转发出去
     *
     * @param message    消息
     * @param xPubSocket xPub
     */
    default void forwardMessage(byte[] message, ZMQ.Socket xPubSocket) {
        xPubSocket.send(message, 0);
    }

    /**
     * 异常回调
     *
     * @param e exception
     */
    void onError(Exception e);
}
