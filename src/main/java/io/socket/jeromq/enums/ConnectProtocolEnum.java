package io.socket.jeromq.enums;

import lombok.Getter;

/**
 * @author xuejian.sun
 * @date 2019-07-15 14:15
 */
public enum ConnectProtocolEnum {
    /**
     * 进程间通信
     */
    IPC("ipc"),
    /**
     * 可靠性传输消息。
     */
    TCP("tcp"),
    /**
     * 进程内通信
     */
    INPROC("inproc"),
    /**
     * 短消息传输，可靠性不如TCP。
     */
    UDP("udp");

    @Getter
    private String type;

    ConnectProtocolEnum(String type) {
        this.type = type;
    }
}
