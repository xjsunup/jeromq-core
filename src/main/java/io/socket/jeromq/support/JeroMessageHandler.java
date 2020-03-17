package io.socket.jeromq.support;

import io.socket.jeromq.annonation.JeroHandler;
import io.socket.jeromq.enums.StringTopic;
import io.socket.jeromq.enums.ZTopic;
import io.socket.jeromq.model.ZHeader;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author xuejian.sun
 * @date 2019-03-20 18:49
 */
public class JeroMessageHandler {

    private Map<String, List<Invoker>> jeroTopicInvokerMethods;

    public JeroMessageHandler() {
        initialize(this);
    }

    @Slf4j
    private static class Invoker {

        private Method method;

        private Object target;

        private Class<?> parameterType;

        private Invoker(Method method, Object target) {
            this.method = method;
            this.parameterType = method.getParameterTypes()[0];
            this.target = target;
        }

        private void invoker(Object message) {
            if(!message.getClass().isAssignableFrom(parameterType)) {
                log.warn("当前参数[{}] 与 {}.{}({})中的参数不一致,必须为自身类或子类。currentMessage -> {}"
                        , message.getClass().getName(), target.getClass().getName(),
                        method.getName(), parameterType.getName(), message);
                return;
            }
            try {
                method.setAccessible(true);
                method.invoke(target, message);
            } catch (IllegalAccessException | InvocationTargetException e) {
                log.error("", e);
            }
        }
    }

    private void initialize(JeroMessageHandler messageHandler) {
        Class<? extends JeroMessageHandler> handlerClass = messageHandler.getClass();
        Method[] methods = handlerClass.getMethods();
        jeroTopicInvokerMethods = Arrays.stream(methods)
                .filter(method -> !Modifier.isPrivate(method.getModifiers())
                        && (method.isAnnotationPresent(JeroHandler.class))
                        && method.getParameters().length == 1)
                .collect(Collectors.groupingBy(
                        method -> method.getAnnotation(JeroHandler.class).topic(),
                        Collectors.mapping(
                                method -> new Invoker(method, messageHandler)
                                , Collectors.toList())));

    }

    /**
     * 数据分发
     *
     * @param header message header
     * @param object message content
     */
    public void forward(ZHeader header, Object object) {
        ZTopic zTopic = header.getZTopic();
        if(zTopic instanceof StringTopic) {
            forward((StringTopic) zTopic, object);
        }
    }

    private void forward(StringTopic stringTopic, Object obj) {
        List<Invoker> invokers = jeroTopicInvokerMethods.get(stringTopic.getTopicString());
        if(Objects.nonNull(invokers) && invokers.size() > 0) {
            for(Invoker invoker : invokers) {
                invoker.invoker(obj);
            }
        }
    }
}
