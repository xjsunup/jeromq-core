package io.socket.jeromq.core;

/**
 * @author xuejian.sun
 * @date 2019-07-16 14:02
 */
public interface JeroMsgSend {
    /**
     * 发送字符串消息
     *
     * @param message 字符消息
     */
    void send(String message);

    /**
     * send more
     *
     * @param more    先发一帧特殊消息
     * @param message 消息
     */
    void send(byte[] more, String message);

    /**
     * 发送字节消息
     *
     * @param message 字节消息
     */
    void send(byte[] message);

    /**
     * send more
     *
     * @param more    先发一帧特殊消息
     * @param message 消息
     */
    void send(byte[] more, byte[] message);
}
