package io.socket.jeromq.support;

/**
 * @author xuejian.sun
 * @date 2019-03-21 09:43
 */
@FunctionalInterface
public interface JeroMessageCallback<T> {
    /**
     * zmq message callback
     * @param t message instances
     */
    void callback(T t);
}
