package io.socket.jeromq.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import io.socket.jeromq.annonation.JeroHandler;
import io.socket.jeromq.enums.JeroTopic;
import io.socket.jeromq.support.JeroMessageHandler;
import io.socket.jeromq.model.QuoteS;
import io.socket.jeromq.pmodel.QuoteSTest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * @author xuejian.sun
 * @date 2019-03-22 15:03
 */
@Slf4j
public class MyJeroMessageHandler extends JeroMessageHandler {
    @Getter
    private int jMPB = 0;
    @Getter
    private int gMPB = 0;

    @JeroHandler(topic = "hello")
    public void onMessage(String message) {
        System.out.println(Thread.currentThread().getName()+";是否是守护："+Thread.currentThread().isDaemon()+";topic_hello -> receiver: " + message);
    }

    @JeroHandler
    public void processALL(String message){
        log.info("topic[{}] -> {}", JeroTopic.ALL,message);
    }

    @JeroHandler(topic = "json")
    public void testJsonObj(JsonTestObj jsonTestObj){
        log.info("{}",jsonTestObj);
    }

    @JeroHandler(topic = "protobuf")
    public void testProtobuf(QuoteS quoteS){
        log.info("{}",quoteS);
    }

    public void testMarketPB(QuoteS quoteS){
        log.info("MPB -> {}",quoteS);
        ++jMPB;
    }

    public void testMarketPB(byte[] message){
        log.info("MPB -> {}",message);
        ++gMPB;
        try {
            QuoteSTest.QuoteS quoteS = QuoteSTest.QuoteS.parseFrom(message);
            log.info("{}",quoteS);
        } catch (InvalidProtocolBufferException e) {
            log.error("parse google protobuf failure");
        }
    }

    @JeroHandler(topic = "testTopicStr")
    public void testTopicStr(String message){
        log.info("topic[testTopicStr] -> {}",message);
    }

    @JeroHandler(topic = "byteCodec")
    public void byteCodec(byte[] message){
        log.info("topic[byteCodec] -> {}",new String(message, StandardCharsets.UTF_8));
    }
}
