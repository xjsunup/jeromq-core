package io.socket.jeromq;

import io.socket.jeromq.config.JeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * @author xuejian.sun
 * @date 2019-07-16 15:05
 */
@Slf4j
@Ignore
@RunWith(MockitoJUnitRunner.class)
public class JeroPushPollTest extends JeroTest{

    @Test
    public void testPushPool() throws InterruptedException {
        JeroConfig jeroConfig = jeroConfig();
        ZContext zContext = new ZContext(1);
        ZMQ.Socket push = zContext.createSocket(SocketType.PUSH);
        push.bind(jeroConfig.address());
        int i = 20;
        while(i-- > 0){
            push.send("hello"+i,0);
            TimeUnit.SECONDS.sleep(2);
        }
    }

    @Test
    public void testPull() throws InterruptedException {
        pull("pull-0");
        pull("pull-1");
        pull("pull-2");
        TimeUnit.SECONDS.sleep(10);
    }

    private void pull(String name){
        JeroConfig jeroConfig = jeroConfig();
        ZContext zContext = new ZContext(1);
        ZMQ.Socket pull = zContext.createSocket(SocketType.PULL);
        pull.subscribe("".getBytes(StandardCharsets.UTF_8));
        pull.connect(jeroConfig.address());
        new Thread(() -> {
            while(true){
                byte[] recv = pull.recv(0);
                log.info("{}",new String(recv, StandardCharsets.UTF_8));
            }
        },name).start();
    }
}
