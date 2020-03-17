package io.socket.jeromq.annonation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author xuejian.sun
 * @date 2019-03-21 14:55
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JeroHandler {
    /**
     * 用户自定义topic,默认为空传不订阅,优先级高于JeroTopic,。
     * 如果该元素有值，则优先使用这个元素作为订阅主题。
     * @return ""
     */
    String topic() default "";
}
