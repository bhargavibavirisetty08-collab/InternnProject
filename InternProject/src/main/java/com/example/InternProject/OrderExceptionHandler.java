package com.example.InternProject.Event;


import com.lmax.disruptor.ExceptionHandler;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class OrderExceptionHandler implements ExceptionHandler<OrderEvent> {

    private static final Logger log =
            LoggerFactory.getLogger(OrderExceptionHandler.class);

    @Override
    public void handleEventException(Throwable ex, long sequence, OrderEvent event) {
//        System.out.println("========== ORDER PROCESSING FAILED ==========");
//        System.out.println("Sequence : " + sequence);
//        System.out.println("Order Type : " + event.getOrderType());
//        System.out.println("Reason : " + ex.getMessage());
//        ex.printStackTrace();
        log.error("========== ORDER PROCESSING FAILED ==========");
        log.error("Sequence : {}", sequence);
        log.error("Order Type : {}", event.getOrderType());
        log.error("Reason : {}", ex.getMessage(), ex);
    }

    @Override
    public void handleOnStartException(Throwable throwable) {

    }

    @Override
    public void handleOnShutdownException(Throwable throwable) {

    }
}
