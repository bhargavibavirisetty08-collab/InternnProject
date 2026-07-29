package com.example.InternProject.Event;

import com.lmax.disruptor.EventFactory;

public class orderEventFactory implements EventFactory<OrderEvent> {
    @Override
    public OrderEvent newInstance() {
        return new OrderEvent();
    }
}
