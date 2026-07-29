package com.example.InternProject.Event;

import com.example.InternProject.Model.Order;
import com.example.InternProject.Model.Order_type;

public class OrderEvent {
    private Order order;

    private Order_type orderType;

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Order_type getOrderType() {
        return orderType;
    }

    public void setOrderType(Order_type orderType) {
        this.orderType = orderType;
    }
}
