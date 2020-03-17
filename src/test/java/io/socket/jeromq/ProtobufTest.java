package io.socket.jeromq;

import com.baidu.bjf.remoting.protobuf.ProtobufIDLGenerator;
import io.socket.jeromq.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author xuejian.sun
 * @date 2019-03-25 10:34
 */
@Ignore
public class ProtobufTest {

    @Test
    public void generateTest() throws IOException {
        String idl = ProtobufIDLGenerator.getIDL(User.class,null,null);

//        ProtobufProxy.create()

        File file = new File("./test.proto");

        file.createNewFile();
        FileOutputStream fileInputStream = new FileOutputStream(new File("test.proto"));
        FileChannel channel = fileInputStream.getChannel();

        ByteBuffer buffer = ByteBuffer.allocate(1024);

        buffer.put(idl.getBytes());

        buffer.flip();

        channel.write(buffer);

        buffer.clear();

        channel.close();
        fileInputStream.close();
    }

    @Test
    public void test(){
        Sou sou1 = new Sou(1,"a");
        Sou sou2 = new Sou(1,"b");
        Sou sou3 = new Sou(2,"c");
        List<Sou> list = new ArrayList<>();
        list.add(sou1);
        list.add(sou2);
        list.add(sou3);

        Map<Integer, List<String>> collect = list.stream()
                .collect(Collectors.groupingBy(Sou::getX, Collectors.mapping(Sou::getTxt, Collectors.toList())));

        System.out.println("");

    }

    @AllArgsConstructor
    @Data
    static class Sou{
        private int x;

        private String txt;
    }
}
