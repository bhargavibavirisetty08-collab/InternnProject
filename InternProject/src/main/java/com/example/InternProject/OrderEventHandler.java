package com.example.InternProject.Event;

import com.example.InternProject.Model.Order;
import com.example.InternProject.Model.Order_type;
import com.example.InternProject.Service.OrderService;
import com.lmax.disruptor.EventHandler;
import org.springframework.stereotype.Component;

@Component
public class OrderEventHandler implements EventHandler<OrderEvent> {

    private final OrderService orderService;

    public OrderEventHandler(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) throws Exception {

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println(
                "Processing order from Disruptor: "
                        + event.getOrderType()
        );

        Order order = event.getOrder();

        if(event.getOrderType() == Order_type.BUY){
            orderService.buyOrder(order);
        }else if(event.getOrderType() == Order_type.SELL){
            orderService.sellOrder(order);
        }else{
            throw new RuntimeException("Invalid order type");
        }

    }
}
