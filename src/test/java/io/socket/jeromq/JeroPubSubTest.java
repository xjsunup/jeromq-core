package io.socket.jeromq;

import com.google.protobuf.InvalidProtocolBufferException;
import io.socket.jeromq.annonation.JeroHandler;
import io.socket.jeromq.codec.JeroJsonObjectCodec;
import io.socket.jeromq.codec.JeroProtobufCodec;
import io.socket.jeromq.codec.JeroStringCodec;
import io.socket.jeromq.config.JeroConfig;
import io.socket.jeromq.enums.ConnectProtocolEnum;
import io.socket.jeromq.enums.PubThreadType;
import io.socket.jeromq.enums.StringTopic;
import io.socket.jeromq.handler.JsonTestObj;
import io.socket.jeromq.handler.MyJeroMessageHandler;
import io.socket.jeromq.model.JeroHeader;
import io.socket.jeromq.model.QuoteS;
import io.socket.jeromq.model.Side;
import io.socket.jeromq.pmodel.QuoteSTest;
import io.socket.jeromq.pmodel.SideTest;
import io.socket.jeromq.support.JeroMessageHandler;
import io.socket.jeromq.template.JeroPubTemplate;
import io.socket.jeromq.template.JeroSubTemplate;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author xuejian.sun
 * @date 2019-03-21 16:38
 */
@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class JeroPubSubTest extends JeroTest {

    @Override
    protected JeroConfig jeroConfig() {
        return super.jeroConfig().setReceiveTimeout(200);
    }

    @Test
    public void testUseCallbackMsg() throws InterruptedException {
        try (JeroPubTemplate jeroPubTemplate = new JeroPubTemplate(jeroConfig(), PubThreadType.SINGLE);
             JeroSubTemplate manager = new JeroSubTemplate(jeroConfig())) {
            JeroHeader header = JeroHeader.builder()
                    .zTopic(StringTopic.subscribe("hello"))
                    .build();
            manager.subscribe(header, new JeroStringCodec(), String.class, str -> log.info("recv[hello] -> {}", str));
            new Thread(() -> {
                int i = 0;
                while(!Thread.currentThread().isInterrupted()) {
                    try {
                        jeroPubTemplate.sendMessage("A:" + "hello" + (++i));
                        jeroPubTemplate.sendMessage("hello:" + "b" + (++i));
                        Thread.sleep(300);
                    } catch (InterruptedException e) {

                    }
                }
            }).start();
            Thread.sleep(5000);
            manager.unsubscribe(header)
                    .subscribe(JeroHeader.builder()
                                    .zTopic(StringTopic.subscribeAll())
                                    .build(),
                            new JeroStringCodec(), String.class, str -> log.info("recv all -> {}", str));
            Thread.sleep(5000);
        }
    }

    @Test
    public void testUseCallbackMsgByte() {
        try (JeroPubTemplate jeroPubTemplate = new JeroPubTemplate(jeroConfig(), PubThreadType.SINGLE);
             JeroSubTemplate manager = new JeroSubTemplate(jeroConfig())) {
            JeroHeader header = JeroHeader.builder()
                    .zTopic(StringTopic.subscribe("hello"))
                    .build();
            manager.subscribe(header, bytes -> log.info("recv[hello] -> {}", new String(bytes, ZMQ.CHARSET)));
            new Thread(() -> {
                int i = 0;
                while(!Thread.currentThread().isInterrupted()) {
                    try {
                        jeroPubTemplate.sendMessage("hello:" + "b" + (++i));
                        Thread.sleep(300);
                    } catch (InterruptedException e) {

                    }
                }
            }).start();
            try {
                Thread.sleep(5000);
                manager.unsubscribe(header);
            } catch (InterruptedException e) {

            }
        }
    }

    @Test
    public void subStringTopicTest() {
        System.out.println("========================== 订阅String消息 ==========================");
        JeroConfig jeroConfig = jeroConfig();
        MyJeroMessageHandler jeroMessageHandler = new MyJeroMessageHandler();
        try (JeroPubTemplate jeroPubTemplate = new JeroPubTemplate(jeroConfig, PubThreadType.SINGLE);
             JeroSubTemplate manager = new JeroSubTemplate(jeroConfig, jeroMessageHandler)) {
            JeroHeader header = JeroHeader.builder()
                    .zTopic(StringTopic.subscribe("hello"))
                    .build();
            manager.subscribe(header, new JeroStringCodec(), String.class);
            new Thread(() -> {
                int i = 0;
                while(!Thread.currentThread().isInterrupted()) {
                    try {
                        jeroPubTemplate.sendMessage("A:" + "hello" + (++i));
                        jeroPubTemplate.sendMessage("hello:" + "b" + (++i));
                        Thread.sleep(300);
                    } catch (InterruptedException e) {

                    }
                }
            }).start();
            try {
                Thread.sleep(5000);
                manager.unsubscribe(header)
                        .subscribe(JeroHeader.builder()
                                        .zTopic(StringTopic.subscribeAll())
                                        .build(),
                                new JeroStringCodec(), String.class);
                Thread.sleep(5000);
            } catch (InterruptedException e) {

            }
        }
    }

    @Test
    public void subJsonTest() {
        System.out.println("========================== 订阅JSON消息 ==========================");
        JeroConfig jeroConfig = jeroConfig();
        MyJeroMessageHandler jeroMessageHandler = new MyJeroMessageHandler();
        try (JeroPubTemplate jeroPubTemplate = new JeroPubTemplate(jeroConfig, PubThreadType.SINGLE);
             JeroSubTemplate manager = new JeroSubTemplate(jeroConfig, jeroMessageHandler)) {
            //订阅
            manager.subscribe(JeroHeader.builder()
                            .zTopic(StringTopic.subscribe("json"))
                            .build(),
                    new JeroJsonObjectCodec<>(), JsonTestObj.class)
                    .subscribe(JeroHeader.builder()
                            .zTopic(StringTopic.subscribeAll())
                            .build(), new JeroStringCodec(), String.class);
            //发送消息
            new Thread(() -> {
                int i = 0;
                while(!Thread.currentThread().isInterrupted()) {
                    try {
                        JsonTestObj jsonTestObj = new JsonTestObj();
                        jsonTestObj.setId(++i);
                        jsonTestObj.setMessage("json hello");
                        jeroPubTemplate.sendJsonMessage("json", jsonTestObj);
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {

                    }
                }
            }).start();
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {

            }
        }
    }

    @Test
    public void subAll() {
        System.out.println("========================== 订阅所有消息 ==========================");
        JeroConfig jeroConfig = jeroConfig();
        MyJeroMessageHandler jeroMessageHandler = new MyJeroMessageHandler();
        try (JeroPubTemplate jeroPubTemplate = new JeroPubTemplate(jeroConfig, PubThreadType.SINGLE);
             JeroSubTemplate manager = new JeroSubTemplate(jeroConfig, jeroMessageHandler)) {
            manager.subscribe(JeroHeader.builder()
                            .zTopic(StringTopic.subscribeAll())
                            .build(),
                    new JeroStringCodec(), String.class);

            new Thread(() -> {
                int i = 0;
                while(!Thread.currentThread().isInterrupted()) {
                    try {
                        JsonTestObj jsonTestObj = new JsonTestObj();
                        ++i;
                        jsonTestObj.setId(i);
                        jsonTestObj.setMessage("json hello");
                        jeroPubTemplate.sendJsonMessage("json", jsonTestObj);

                        jeroPubTemplate.sendMessage("AX,hello" + i);
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {

                    }
                }
            }).start();
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {

            }
        }
    }

    @Test
    public void testSendString() {
        System.out.println("========================== 测试发送带Topic的字符串消息 ==========================");
        JeroConfig jeroConfig = jeroConfig();
        MyJeroMessageHandler jeroMessageHandler = new MyJeroMessageHandler();
        try (JeroPubTemplate jeroPubTemplate = new JeroPubTemplate(jeroConfig, PubThreadType.SINGLE);
             JeroSubTemplate manager = new JeroSubTemplate(jeroConfig, jeroMessageHandler)) {
            manager.subscribe(JeroHeader.builder()
                            .zTopic(StringTopic.subscribe("testTopicStr"))
                            .build(),
                    new JeroStringCodec(), String.class);

            new Thread(() -> {
                int i = 0;
                while(!Thread.currentThread().isInterrupted()) {
                    try {
                        ++i;
                        jeroPubTemplate.sendMessage("testTopicStr", "hello" + i);
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {

                    }
                }
            }).start();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {

            }
        }
    }

    @Test
    public void testUseDefaultCodec() {
        System.out.println("========================== 测试使用是默认的编解码器（byte message） ==========================");
        JeroConfig jeroConfig = jeroConfig();
        MyJeroMessageHandler jeroMessageHandler = new MyJeroMessageHandler();
        try (JeroPubTemplate jeroPubTemplate = new JeroPubTemplate(jeroConfig, PubThreadType.SINGLE);
             JeroSubTemplate manager = new JeroSubTemplate(jeroConfig, jeroMessageHandler)) {
            manager.subscribe(JeroHeader.builder()
                    .zTopic(StringTopic.subscribe("byteCodec"))
                    .build());

            new Thread(() -> {
                int i = 0;
                while(!Thread.currentThread().isInterrupted()) {
                    try {
                        ++i;
                        jeroPubTemplate.sendMessage("byteCodec", "hello" + i);
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {

                    }
                }
            }).start();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {

            }
        }
    }

    @Test
    public void subProtobufTest() throws InterruptedException {
        System.out.println("========================== 订阅PB消息 ==========================");
        JeroConfig jeroConfig = jeroConfig();
        MyJeroMessageHandler jeroMessageHandler = new MyJeroMessageHandler();
        // init testXpubXsub
        try (JeroPubTemplate jeroPubTemplate = new JeroPubTemplate(jeroConfig, PubThreadType.SINGLE);
             JeroSubTemplate manager = new JeroSubTemplate(jeroConfig, jeroMessageHandler)) {
            manager.subscribe(JeroHeader.builder()
                            .zTopic(StringTopic.subscribe("protobuf"))
                            .build(),
                    new JeroProtobufCodec<>(), QuoteS.class);
            new Thread(() -> {
                int i = 0;
                while(!Thread.currentThread().isInterrupted()) {
                    try {
                        ++i;
                        QuoteSTest.QuoteS quotes = gQuoteS(i);
                        jeroPubTemplate.sendMessage("protobuf", quotes.toByteArray());
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            Thread.sleep(10000);
        }
    }
    /**
     * 场景：使用被JProtoBuf的对象转为PB消息传输，topic使用string类型
     * 预期：订阅方按照指定的string topic可以接收到数据，并使用google生成的java对象接收该数据
     */
    @Test
    public void sendJProtobufObjMessageUseStringTopic() throws InterruptedException {
        System.out.println("========================== 订阅JPB消息 ==========================");
        LongAdder longAdder = new LongAdder();
        JeroConfig jeroConfig = jeroConfig();
        // init testXpubXsub
        try (JeroPubTemplate jeroPubTemplate = new JeroPubTemplate(jeroConfig, PubThreadType.SINGLE);
             JeroSubTemplate manager = new JeroSubTemplate(jeroConfig, new JeroMessageHandler() {
                 @JeroHandler(topic = "KFMBP")
                 public void reckfMbp(byte[] bytes) throws InvalidProtocolBufferException {
                     QuoteSTest.QuoteS quoteS = QuoteSTest.QuoteS.parseFrom(bytes);
                     longAdder.increment();
                     log.info("{},{}", quoteS.toString(), longAdder.intValue());
                 }
             })) {
            //使用默认的序列化方式，由调用发自己序列化
            manager.subscribe(JeroHeader.builder().zTopic(StringTopic.subscribe("KFMBP")).build());
            new Thread(() -> {
                int i = 0;
                while(!Thread.currentThread().isInterrupted()) {
                    try {
                        ++i;
                        QuoteS quoteS = jQuoteS(i);
                        jeroPubTemplate.sendProtobufObjMessage("KFMBP", quoteS, QuoteS.class);
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            Awaitility.await()
                    .atMost(10, TimeUnit.SECONDS)
                    .until(() -> longAdder.intValue() > 0);
        }

    }

    @Test
    public void multiThreadPub() throws InterruptedException {
        JeroConfig jeroConfig = jeroConfig().setConnectType(ConnectProtocolEnum.TCP).setIdentity("MultiThreadPub");
        try (JeroPubTemplate jeroPubTemplate = new JeroPubTemplate(jeroConfig, PubThreadType.MULTI);
             JeroSubTemplate subTemplate = new JeroSubTemplate(jeroConfig)) {
            subTemplate.subscribe(JeroHeader.builder().zTopic(StringTopic.subscribeAll()).build(), new JeroStringCodec(), String.class, str -> {
                System.out.println(Thread.currentThread().getName() + " " + str);
            });
            new Thread(() -> {
                while(!Thread.currentThread().isInterrupted()) {
                    try {
                        jeroPubTemplate.sendMessage("333");
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            new Thread(() -> {
                while(!Thread.currentThread().isInterrupted()) {
                    try {
                        jeroPubTemplate.sendMessage("222");
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            new Thread(() -> {
                while(!Thread.currentThread().isInterrupted()) {
                    try {
                        jeroPubTemplate.sendMessage("111");
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            TimeUnit.SECONDS.sleep(3);
        }
    }

    @Test
    @Ignore
    public void subManyTopic() {

        ZContext context = new ZContext(1);
        ZMQ.Socket pub = context.createSocket(SocketType.PUB);
        pub.bind("tcp://0.0.0.0:7771");
        ZMQ.Socket sub = context.createSocket(SocketType.SUB);
        sub.connect("tcp://0.0.0.0:7771");
        new Thread(() -> {
            while(true) {
                pub.send("xxxxx");
                pub.send("yyyyy");
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        sub.subscribe("x");
        int i = 0;
        while(!Thread.interrupted()) {
            ++i;
            if(i == 10) {
                sub.subscribe("y");
            }
            String s = sub.recvStr();
            System.out.println(s);
        }
    }

    private QuoteSTest.QuoteS gQuoteS(int i) {
        return QuoteSTest.QuoteS.newBuilder()
                .setDate(20190402)
                .setAmount(1000)
                .setCode(i)
                .setLastPx(3.42f)
                .addAsk(1)
                .addAsk(2)
                .addAsk(3)
                .addAsk(4)
                .addAsk(5)
                .addBid(10)
                .addBid(11)
                .addBid(12)
                .addBid(13)
                .setLastTrdSide(SideTest.Side.BUY)
                .build();
    }

    private QuoteS jQuoteS(int i) {
        QuoteS quoteS = new QuoteS();
        List<Float> ask = new ArrayList<>();
        ask.add(1f);
        ask.add(2f);
        ask.add(3f);
        ask.add(4f);
        ask.add(5f);
        List<Float> bid = new ArrayList<>();
        bid.add(10f);
        bid.add(11f);
        bid.add(12f);
        bid.add(13f);
        quoteS.setDate(20190402)
                .setAmount(1000L)
                .setCode(i)
                .setLastPx(3.42f)
                .setAsk(ask)
                .setBid(bid)
                .setLastTrdSide(Side.BUY);
        return quoteS;
    }
}
