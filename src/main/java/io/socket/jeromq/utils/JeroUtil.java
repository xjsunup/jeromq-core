package io.socket.jeromq.utils;

import io.socket.jeromq.config.JeroConfig;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

/**
 * @author xuejian.sun
 * @date 2019-07-23 15:59
 */
public final class JeroUtil {

    private JeroUtil() {
    }

    public static void setParams(JeroConfig jeroConfig, ZMQ.Socket socket) {
        socket.setReceiveTimeOut(jeroConfig.getReceiveTimeout());
        socket.setTCPKeepAlive(jeroConfig.getTcpKeepAlive());
        socket.setTCPKeepAliveCount(jeroConfig.getTcpKeepAliveCount());
        socket.setTCPKeepAliveIdle(jeroConfig.getTcpKeepAliveIdle());
        socket.setTCPKeepAliveInterval(jeroConfig.getTcpKeepAliveInterval());
    }

    public static void setParams(JeroConfig jeroConfig, ZContext context) {
        context.setLinger(jeroConfig.getLinger());
        context.setRcvHWM(jeroConfig.getRcvHwm());
        context.setSndHWM(jeroConfig.getSedHwm());
    }
}
