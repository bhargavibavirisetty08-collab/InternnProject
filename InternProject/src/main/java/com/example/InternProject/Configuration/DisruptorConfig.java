package com.example.InternProject.Configuration;

import com.example.InternProject.Event.OrderEvent;
import com.example.InternProject.Event.OrderEventHandler;
import com.example.InternProject.Event.OrderExceptionHandler;
import com.example.InternProject.Event.orderEventFactory;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ThreadFactory;

@Configuration
public class DisruptorConfig {


    private final OrderEventHandler orderEventHandler;
    private final OrderExceptionHandler orderExceptionHandler;
    private Disruptor<OrderEvent> disruptor;
    private static final Logger log =
            LoggerFactory.getLogger(DisruptorConfig.class);

    public DisruptorConfig(
            OrderEventHandler orderEventHandler,
            OrderExceptionHandler orderExceptionHandler) {

        this.orderEventHandler = orderEventHandler;
        this.orderExceptionHandler = orderExceptionHandler;
    }

    @Bean
    public RingBuffer<OrderEvent> orderRingBuffer(){
         int bufferSize = 1024;

        ThreadFactory threadFactory = Thread::new;
        this.disruptor =
                new Disruptor<>(
                        new orderEventFactory(),
                        bufferSize,
                        threadFactory,
                        ProducerType.MULTI,
                        new BlockingWaitStrategy()
                );
        disruptor.handleEventsWith(orderEventHandler);
        disruptor.setDefaultExceptionHandler(orderExceptionHandler);
        disruptor.start();
        return disruptor.getRingBuffer();
    }

    @PreDestroy
    public void shutdown(){
        if(disruptor != null){
            log.info("Shutting down Disruptor...");
            disruptor.shutdown();
            log.info("Disruptor stopped successfully");
        }
    }
}
