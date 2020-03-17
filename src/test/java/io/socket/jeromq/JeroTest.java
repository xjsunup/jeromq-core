package io.socket.jeromq;

import io.socket.jeromq.config.JeroConfig;
import io.socket.jeromq.enums.ConnectProtocolEnum;

/**
 * @author xuejian.sun
 * @date 2019-07-16 13:11
 */
public class JeroTest {

    protected JeroConfig jeroConfig() {
        return new JeroConfig()
                .setIp("127.0.0.1")
                .setPort(3322)
                .setIoThread(1)
                .setConnectType(ConnectProtocolEnum.IPC);
    }
}
