package io.socket.jeromq.template;

import com.baidu.bjf.remoting.protobuf.code.TemplateCodeGenerator;
import com.baidu.bjf.remoting.protobuf.utils.ClassHelper;

/**
 * @author xuejian.sun
 * @date 2019-03-25 13:41
 */
public class KFProtoCodeGenerator extends TemplateCodeGenerator {
    /**
     * Instantiates a new template code generator.
     *
     * @param cls the cls
     */
    public KFProtoCodeGenerator(Class<?> cls) {
        super(cls);
    }

    @Override
    public String getClassName() {
        return ClassHelper.getClassName(cls) + "$$KF";
    }
}
