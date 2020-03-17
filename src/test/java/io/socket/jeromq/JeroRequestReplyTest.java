package io.socket.jeromq;

import io.socket.jeromq.config.JeroConfig;
import io.socket.jeromq.core.JeroReply;
import io.socket.jeromq.core.JeroRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * @author xuejian.sun
 * @date 2019-07-16 13:10
 */
@RunWith(MockitoJUnitRunner.class)
@Slf4j
@Ignore
public class JeroRequestReplyTest extends JeroTest {

    @Test
    public void testRequestReply() throws InterruptedException {
        JeroConfig jeroConfig = jeroConfig().setIdentity("xjsun");

        JeroReply reply = new JeroReply(jeroConfig, (data, socket) -> {
            log.info(new String(data, StandardCharsets.UTF_8));
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            socket.send("pong ....");
        });
        reply.receive();

        JeroRequest request = new JeroRequest(jeroConfig);
        boolean ping = request.request("ping", data -> System.out.println("response -> "+new String(data, StandardCharsets.UTF_8)));
        String message = ping ? "发送成功" : "发送失败";
        System.out.println(message);

        TimeUnit.SECONDS.sleep(5);
        reply.destroy();
        request.destroy();
    }

    @Test
    @Ignore
    public void testRoute(){
        ZContext context = new ZContext(1);
        ZMQ.Socket route = context.createSocket(SocketType.ROUTER);
        route.bind("ipc://test");

        ZMQ.Socket xpub = context.createSocket(SocketType.XPUB);
        xpub.connect("ipc://test");

        new Thread(new Worker("Jack")).start();
        int i = 10;
        while(--i > 0){
//            if(i == 6){
//                new Thread(new Worker("Axi")).start();
//            }
            String user = route.recvStr();
            String topic = route.recvStr();
            log.info("user:{} , topic:{}",user,topic);
            route.sendMore(user);
//            route.recv(0); // send more
            byte[] msg = route.recv(0);// message
            System.out.println("receive "+user+" -> "+new String(msg,ZMQ.CHARSET));
            if(i == 1){
                route.send("end");
            }
            route.send(""+i);
        }
    }


    class Worker implements Runnable{

        private String name;

        public Worker(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            ZContext zContext = new ZContext(1);
            ZMQ.Socket dealer = zContext.createSocket(SocketType.XSUB);
            dealer.connect("ipc://test");
            dealer.setIdentity(name.getBytes(ZMQ.CHARSET));
            ByteBuffer buffer = ByteBuffer.allocate(1);
            buffer.put((byte)1);
            dealer.send(buffer.array());
            dealer.sendMore("topicA");
            dealer.send("hello, I'm "+name);
            while(true){
//                dealer.sendMore("");
//                dealer.recv();
                String responseMsg = dealer.recvStr();
                if(responseMsg.equals("end")){
                    break;
                }
                System.out.println(name + " received -> "+responseMsg);
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
