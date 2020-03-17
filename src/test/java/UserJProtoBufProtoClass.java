import com.baidu.bjf.remoting.protobuf.FieldType;
import com.baidu.bjf.remoting.protobuf.EnumReadable;
import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;
public class UserJProtoBufProtoClass {
@Protobuf(fieldType=FieldType.STRING, order=1, required=false)
public String name;
}
