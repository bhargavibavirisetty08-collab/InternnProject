package com.example.InternProject.Event;

import com.example.InternProject.Model.Order;
import com.example.InternProject.Model.Order_type;
import com.lmax.disruptor.RingBuffer;
import org.springframework.stereotype.Component;

@Component
public class OrderEventProducer {
    private final RingBuffer<OrderEvent> ringBuffer;

    public OrderEventProducer(RingBuffer<OrderEvent> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    public void publish(Order order, Order_type orderType) {

        long sequence = ringBuffer.next();
        try {
            OrderEvent event = ringBuffer.get(sequence);
            event.setOrder(order);
            event.setOrderType(orderType);
        } finally {
            ringBuffer.publish(sequence);
        }

    }

}
