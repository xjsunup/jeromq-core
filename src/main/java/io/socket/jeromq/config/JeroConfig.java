package io.socket.jeromq.config;

import io.socket.jeromq.enums.ConnectProtocolEnum;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * zmq config
 *
 * @author xuejian.sun
 * @date 2019-03-20 16:51
 */
@Data
@Accessors(chain = true)
public class JeroConfig implements Cloneable {
    private String ip = "127.0.0.1";

    private int port = 9991;

    private int ioThread = 1;

    private int tcpKeepAlive = 1;

    private int tcpKeepAliveCount = 10;

    private int tcpKeepAliveIdle = 15;

    private int tcpKeepAliveInterval = 15;

    private int linger = 0;
    private int ipeHwm = 1000;
    /**
     * 发送水位
     */
    private int sedHwm = 1000;
    /**
     * receive 水位
     */
    private int rcvHwm = 1000;
    /**
     * 身份
     */
    private String identity = "";
    /**
     * 接收消息超时时间，milliseconds
     */
    private int receiveTimeout = 300;
    /**
     * 重连间隔时间，milliseconds
     */
    private int reconnectInterval = 2000;
    /**
     * 重连次数
     */
    private int reconnectMaxNums = 5;
    /**
     * udp
     * tcp
     * ipc：进程间通信
     */
    private ConnectProtocolEnum connectType = ConnectProtocolEnum.TCP;

    public String address() {
        return connectType.getType() +
                "://" +
                ip +
                ":" +
                port;
    }

    @Override
    public JeroConfig clone() {
        JeroConfig jeroConfig = new JeroConfig();
        jeroConfig.setIp(this.ip)
                .setPort(this.port)
                .setIoThread(this.ioThread)
                .setTcpKeepAlive(this.tcpKeepAlive)
                .setTcpKeepAliveCount(this.tcpKeepAliveCount)
                .setTcpKeepAliveIdle(this.tcpKeepAliveIdle)
                .setTcpKeepAliveInterval(this.tcpKeepAliveInterval)
                .setLinger(this.linger)
                .setIpeHwm(this.ipeHwm)
                .setSedHwm(this.sedHwm)
                .setRcvHwm(this.rcvHwm)
                .setIdentity(this.identity + "Clone")
                .setReceiveTimeout(this.receiveTimeout)
                .setReconnectInterval(this.reconnectInterval)
                .setReconnectMaxNums(this.reconnectMaxNums)
                .setConnectType(this.connectType);
        return jeroConfig;
    }
}
