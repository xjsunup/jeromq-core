package io.socket.jeromq.support;

import io.socket.jeromq.config.NamedThreadFactory;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author xuejian.sun
 * @date 2019/8/30 14:08
 */
@Slf4j
public class DisruptorAsyncTaskExecutor<T> implements AsyncTaskExecutor<T> {

    private AtomicBoolean shutdown;

    private Disruptor<TaskEvent<T>> disruptor;

    private AsyncTaskCallback<T> callback;

    public DisruptorAsyncTaskExecutor(int ringBufferSize, ThreadFactory threadFactory, ProducerType producerType, WaitStrategy waitStrategy, AsyncTaskCallback<T> callback) {
        this.shutdown = new AtomicBoolean(false);
        this.callback = callback;
        EventFactory<TaskEvent<T>> eventEventFactory = new TaskEventFactory<>();
        this.disruptor = new Disruptor<>(eventEventFactory, ringBufferSize,
                threadFactory, producerType, waitStrategy);
        disruptor.handleEventsWith(this::onEvent);
        disruptor.start();
    }

    public DisruptorAsyncTaskExecutor(String consumerThreadName, ProducerType producerType, WaitStrategy waitStrategy, AsyncTaskCallback<T> callback) {
        this(1024 * 1024, new NamedThreadFactory(consumerThreadName, true), producerType, waitStrategy, callback);
    }

    public DisruptorAsyncTaskExecutor(ProducerType producerType, WaitStrategy waitStrategy, AsyncTaskCallback<T> callback) {
        this("DisruptorTaskThread", producerType, waitStrategy, callback);
    }

    public DisruptorAsyncTaskExecutor(AsyncTaskCallback<T> callback) {
        this(ProducerType.SINGLE, new YieldingWaitStrategy(), callback);
    }

    @ToString
    private static class TaskEvent<T> {
        @Setter
        private T value;
    }

    private static class TaskEventFactory<T> implements EventFactory<TaskEvent<T>> {
        @Override
        public TaskEvent<T> newInstance() {
            return new TaskEvent<>();
        }
    }

    private void onEvent(TaskEvent<T> event, long sequence, boolean endOfBatch) {
        try {
            callback.call(event.value);
        } catch (Exception e) {
            log.error("process failure", e);
        }
    }

    private void translateTo(TaskEvent<T> event, long sequence, T arg0) {
        event.setValue(arg0);
    }

    /**
     * 发布事件
     *
     * @param t event data
     */
    @Override
    public void addTask(T t) {
        if(isShutdown()) {
            throw new RejectedExecutionException("DisruptorAsyncTaskExecutor is shutdown!");
        }
        RingBuffer<TaskEvent<T>> ringBuffer = disruptor.getRingBuffer();
        ringBuffer.publishEvent(this::translateTo, t);
    }

    @Override
    public boolean isShutdown() {
        return disruptor == null || shutdown.get();
    }

    @Override
    public boolean isTerminated() {
        return isShutdown();
    }

    @Override
    public void shutdown() {
        if(disruptor != null) {
            shutdown.set(true);
            disruptor.shutdown();
        }
    }

    @Override
    public void shutdownNow() {
        shutdown();
    }
}
