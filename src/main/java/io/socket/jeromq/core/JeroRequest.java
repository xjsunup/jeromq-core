package io.socket.jeromq.core;

import io.socket.jeromq.config.JeroConfig;
import io.socket.jeromq.support.JeroMessageCallback;
import lombok.extern.slf4j.Slf4j;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** request reply 请求应答模式
 *
 * @author xuejian.sun
 * @date 2019-07-16 09:41
 */
@Slf4j
public class JeroRequest {

    private String name;

    private ZContext zContext;

    private JeroConfig jeroConfig;

    private ZMQ.Socket request;

    public JeroRequest(JeroConfig jeroConfig, ZContext zContext) {
        this.jeroConfig = jeroConfig;
        this.zContext = zContext;
        initReq();
    }

    public JeroRequest(JeroConfig jeroConfig) {
        this(jeroConfig, new ZContext(1));
    }


    private void initReq(){
        this.request = zContext.createSocket(SocketType.REQ);
        // 在该模式下，超时时间是指，请求服务端后，该时间内服务端没有返回，之后服务端再返回将接收不到。
        this.request.setReceiveTimeOut(jeroConfig.getReceiveTimeout());
        this.request.setTCPKeepAliveCount(jeroConfig.getTcpKeepAlive());
        this.request.setTCPKeepAlive(jeroConfig.getTcpKeepAlive());
        this.request.setTCPKeepAliveIdle(jeroConfig.getTcpKeepAliveIdle());
        this.request.setTCPKeepAliveInterval(jeroConfig.getTcpKeepAliveInterval());
        this.request.setIdentity(jeroConfig.getIdentity().getBytes(ZMQ.CHARSET));
        this.request.connect(jeroConfig.address());
    }

    /**
     * 向服务端发送一个请求
     * @param message 消息
     * @param callback 服务端反馈回调器
     * @return 是否发送成功
     */
    public boolean request(String message, JeroMessageCallback<byte[]> callback){
        try {
            boolean sendResult = request.send(message, 0);
            CompletableFuture.runAsync(() -> {
                byte[] recv = request.recv(0);
                callback.callback(recv);
            });
            return sendResult;
        }catch (ZMQException e){
            log.error("发送失败,需等待前一个请求反馈后,才能进行下次请求. errorCode:{}",e.getErrorCode(),e);
            return false;
        }
    }

    /**
     * 向服务端发送一个请求
     * @param message 消息
     * @param timeout 超时时间MilliSecond
     * @return response message
     */
    public byte[] request(String message,int timeout) {
        try {
            request.send(message);
            return CompletableFuture.supplyAsync(() -> request.recv(0))
                    .get(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            return null;
        }
    }

    public void destroy(){
        if(request != null){
            request.close();
        }
        if(zContext != null){
            zContext.close();
        }
        log.info("JeroRequest_{} Closed: {}", jeroConfig.getIdentity(), jeroConfig.address());
    }
}
