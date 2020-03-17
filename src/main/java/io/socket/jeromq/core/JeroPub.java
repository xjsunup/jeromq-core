package io.socket.jeromq.core;

import io.socket.jeromq.config.JeroConfig;
import io.socket.jeromq.config.NamedThreadFactory;
import io.socket.jeromq.enums.AsyncTaskEnum;
import io.socket.jeromq.enums.PubThreadType;
import io.socket.jeromq.support.AsyncTaskExecutor;
import io.socket.jeromq.support.DisruptorAsyncTaskExecutor;
import io.socket.jeromq.support.LinkedAsyncTaskExecutor;
import io.socket.jeromq.utils.ByteUtil;
import io.socket.jeromq.utils.JeroUtil;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.dsl.ProducerType;
import lombok.extern.slf4j.Slf4j;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * pub - sub 请求订阅模式
 *
 * @author xuejian.sun
 * @date 2019-03-22 19:12
 */
@Slf4j
public class JeroPub implements JeroMsgSend {

    protected JeroConfig jeroConfig;

    protected ZMQ.Socket publisher;

    protected ZContext context;

    private PubThreadType pubThreadType;

    private boolean bindState;

    private boolean connState;

    private AsyncTaskEnum asyncTaskEnum;

    private int maxTaskSize;

    private AsyncTaskExecutor<byte[]> taskExecutor;

    public JeroPub(JeroConfig jeroConfig, PubThreadType pubThreadType, AsyncTaskEnum asyncTaskEnum, int maxTaskSize) {
        this.jeroConfig = Objects.requireNonNull(jeroConfig);
        this.pubThreadType = Objects.requireNonNull(pubThreadType);
        if(pubThreadType == PubThreadType.MULTI) {
            this.asyncTaskEnum = Objects.requireNonNull(asyncTaskEnum);
            this.maxTaskSize = maxTaskSize == 0 ? 1024 : maxTaskSize;
        }
    }

    public JeroPub(JeroConfig jeroConfig) {
        this(jeroConfig, PubThreadType.MULTI, AsyncTaskEnum.DISRUPTOR, 1024);
    }

    public JeroPub init() {
        return init(new ZContext(1));
    }

    public JeroPub init(ZContext zContext) {
        String identity;
        if(jeroConfig.getIdentity().isEmpty()) {
            identity = "JeroPub" + this.hashCode();
        } else {
            identity = jeroConfig.getIdentity();
        }
        jeroConfig.setIdentity(identity);
        this.context = zContext;
        JeroUtil.setParams(jeroConfig, context);
        this.publisher = zContext.createSocket(SocketType.PUB);
        JeroUtil.setParams(jeroConfig, publisher);
        return this;
    }

    public void bind() {
        if(publisher == null) {
            throw new IllegalStateException("请先初始化JeroPub");
        }
        if(connState) {
            log.error("绑定失败, 当前为连接状态");
            return;
        }
        if(!bindState) {
            this.bindState = publisher.bind(jeroConfig.address());
        }
        initTaskExecutor();
    }

    public void connect() {
        if(publisher == null) {
            throw new IllegalStateException("请先初始化JeroPub");
        }
        if(bindState) {
            log.error("连接失败, 当前为绑定状态");
            return;
        }
        if(!connState) {
            this.connState = publisher.connect(jeroConfig.address());
        }
        initTaskExecutor();
        // 连接状态下建立成功需要sleep1秒防止消息丢失
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            log.error("", e);
        }
    }

    private void initTaskExecutor() {
        if(pubThreadType == PubThreadType.MULTI) {
            if(asyncTaskEnum == AsyncTaskEnum.LINKED_BLOCKING_QUEUE) {
                this.taskExecutor = new LinkedAsyncTaskExecutor<>(maxTaskSize, jeroConfig.getIdentity(), publisher::send);
            } else if(asyncTaskEnum == AsyncTaskEnum.DISRUPTOR) {
                this.taskExecutor = new DisruptorAsyncTaskExecutor<>(maxTaskSize
                        , new NamedThreadFactory(jeroConfig.getIdentity(), true), ProducerType.MULTI
                        , new SleepingWaitStrategy(), publisher::send);
            } else {
                log.warn("unknown AsyncTaskEnum -> {}", asyncTaskEnum);
            }
        }
    }

    /**
     * zmq socket pub message
     *
     * @param message msg
     */
    public void publishMsg(byte[] message) {
        if(Objects.nonNull(publisher)) {
            publisher.send(message);
        }
    }

    @Override
    public void send(String message) {
        byte[] messageBytes = message.getBytes(ZMQ.CHARSET);
        if(pubThreadType == PubThreadType.SINGLE) {
            publishMsg(messageBytes);
        } else {
            taskExecutor.addTask(message.getBytes(ZMQ.CHARSET));
        }
    }

    @Override
    public void send(byte[] more, String message) {
        byte[] messageBytes = ByteUtil.merge(more, message.getBytes(ZMQ.CHARSET));
        if(pubThreadType == PubThreadType.SINGLE) {
            publishMsg(messageBytes);
        } else {
            taskExecutor.addTask(message.getBytes(ZMQ.CHARSET));
        }
    }

    @Override
    public void send(byte[] message) {
        if(pubThreadType == PubThreadType.SINGLE) {
            publishMsg(message);
        } else {
            taskExecutor.addTask(message);
        }
    }

    @Override
    public void send(byte[] more, byte[] message) {
        byte[] messageBytes = ByteUtil.merge(more, message);
        if(pubThreadType == PubThreadType.SINGLE) {
            publishMsg(messageBytes);
        } else {
            taskExecutor.addTask(messageBytes);
        }
    }

    public boolean isClosed() {
        return context.isClosed() || publisher == null;
    }

    public void destroy() {
        if(taskExecutor != null && !taskExecutor.isShutdown()) {
            taskExecutor.shutdown();
        }
        if(!context.isClosed() && publisher != null) {
            publisher.unbind(jeroConfig.address());
            context.destroySocket(publisher);
            publisher = null;
            context.close();
        }
    }
}
