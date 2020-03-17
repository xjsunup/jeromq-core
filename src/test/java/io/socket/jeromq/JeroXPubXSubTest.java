package io.socket.jeromq;

import io.socket.jeromq.annonation.JeroHandler;
import io.socket.jeromq.codec.JeroByteCodec;
import io.socket.jeromq.codec.JeroStringCodec;
import io.socket.jeromq.config.JeroConfig;
import io.socket.jeromq.core.JeroBroker;
import io.socket.jeromq.core.JeroMsgSend;
import io.socket.jeromq.core.JeroXPub;
import io.socket.jeromq.core.JeroXSub;
import io.socket.jeromq.enums.ConnectProtocolEnum;
import io.socket.jeromq.enums.StringTopic;
import io.socket.jeromq.model.JeroHeader;
import io.socket.jeromq.support.JeroBrokerCallback;
import io.socket.jeromq.support.JeroMessageHandler;
import io.socket.jeromq.support.JeroXPubMsgCallback;
import io.socket.jeromq.template.JeroSubTemplate;
import io.socket.jeromq.utils.ByteUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.zeromq.ZMQ;

import java.util.concurrent.TimeUnit;

/**
 * @author xuejian.sun
 * @date 2019-07-18 14:57
 */
@Slf4j
@Ignore
@RunWith(MockitoJUnitRunner.class)
public class JeroXPubXSubTest extends JeroTest {

    @Test
    public void test() throws InterruptedException {
        // init sub
        class Sub extends JeroMessageHandler {
            @JeroHandler(topic = "A")
            public void all(byte[] message) {
                log.info("A -> {}", new String(message, ZMQ.CHARSET));
            }

            @JeroHandler(topic = "B")
            public void b(byte[] message) {
                log.info("B -> {}", new String(message, ZMQ.CHARSET));
            }
        }

        JeroSubTemplate sub = new JeroSubTemplate(new JeroConfig().setPort(7766), new Sub());
        JeroHeader aHeader = JeroHeader.builder().zTopic(StringTopic.subscribe("A")).build();
        sub.subscribe(aHeader)
                .subscribe(JeroHeader.builder().zTopic(StringTopic.subscribe("B")).build());
        TimeUnit.SECONDS.sleep(5);
        sub.unsubscribe(aHeader);
        TimeUnit.SECONDS.sleep(50000);
    }

    @Test
    @Ignore
    public void pub() {
        // init broker
        JeroBroker broker = new JeroBroker(new JeroConfig().setPort(7777), new JeroConfig().setPort(7766),
                new JeroBrokerCallback() {
                    @Override
                    public void onSubscribe(byte[] msg, byte[] sendMore) {

                    }

                    @Override
                    public void onUnSubscribe(byte[] msg, byte[] sendMore) {

                    }

                    @Override
                    public void onError(Exception e) {

                    }
                });
        broker.init();
    }

    @Test
    public void testXpubXsub() throws InterruptedException {
        JeroConfig config = jeroConfig().setIp("server.idc")
                .setConnectType(ConnectProtocolEnum.IPC).setReceiveTimeout(1000);
        JeroXPub xPub = new JeroXPub<>(config, new JeroByteCodec(), null, new JeroXPubMsgCallback<byte[]>() {
            @Override
            public void onSubscribe(byte[] msg, JeroMsgSend msgSend) {
                log.info("recv sub -> {}", msg);
                byte[] message = "world".getBytes(ZMQ.CHARSET);
                byte[] send = ByteUtil.merge(msg, message);
                msgSend.send(send);
            }

            @Override
            public void onUnSubscribe(byte[] msg, JeroMsgSend msgSend) {
                log.info("recv unsub -> {}", msg);
            }

            @Override
            public void onOtherRequest(byte command, byte[] msg, JeroMsgSend msgSend) {
                log.info("recv other {} -> {}", command, msg);
            }

            @Override
            public void onError(Exception e) {

            }
        }).init();

        xPub.recvMsg();

        new Thread(new XSub("ls")).start();

        new Thread(new XSub("zs")).start();

        TimeUnit.SECONDS.sleep(3);

        xPub.destroy();
    }

    class XSub implements Runnable {
        private String identity;

        public XSub(String identity) {
            this.identity = identity;
        }

        @Override
        public void run() {
            JeroConfig config = jeroConfig().setConnectType(ConnectProtocolEnum.IPC)
                    .setIdentity(identity)
                    .setIp("server.idc").setReceiveTimeout(1000);
            JeroHeader jeroHeader = JeroHeader.builder().zTopic(StringTopic.subscribe(identity)).build();
            JeroXSub xSub = new JeroXSub<>(config, jeroHeader, new JeroStringCodec(), String.class, str -> log.info("sub[{}] rec -> {}", identity, str));
            xSub.init();
            xSub.sub().recv();
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            xSub.unsubscribe();
        }
    }
}
