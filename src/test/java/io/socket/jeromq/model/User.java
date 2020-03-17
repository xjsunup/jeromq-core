package io.socket.jeromq.model;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;
import lombok.Data;

/**
 * @author xuejian.sun
 * @date 2019-03-22 15:48
 */
@Data
@ProtobufClass
public class User {

    private String name;
}
