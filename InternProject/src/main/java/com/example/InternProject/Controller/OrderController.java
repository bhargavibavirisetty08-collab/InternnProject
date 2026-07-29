package com.example.InternProject.Controller;

import com.example.InternProject.Event.OrderEventProducer;
import com.example.InternProject.Model.Order;
import com.example.InternProject.Model.Order_type;
import com.example.InternProject.Service.OrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class OrderController {

    @Autowired
    OrderService orderService;

    @Autowired
    OrderEventProducer orderEventProducer;

    @GetMapping("/orders")
    public List<Order> getAllOrders(){
       return orderService.getAllOrders();
    }

    @GetMapping("/orders/{id}")
    public Order getById(@PathVariable int id ){
        return orderService.getById(id);
    }

    @PostMapping("/orders/sell")
    public String sellOrder(@Valid  @RequestBody Order order){
//        return orderService.sellOrder(order);

        orderEventProducer.publish(order, Order_type.SELL);
        return "Sell order submitted successfully";
    }

    @PostMapping("/orders/buy")

    public String buyOrder(@Valid @RequestBody Order order){
        System.out.println("apiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiii");
//      return orderService.buyOrder(order);

        orderEventProducer.publish(order, Order_type.BUY);
        return "Buy order submitted successfully";
    }

//    @GetMapping("/orders/buyQueue")
//    public int getBuyQueueSize(){
//        return orderService.getBuyQueueSize();
//    }

//    @GetMapping("/orders/sellQueue")
//    public int getSellQueueSize(){
//        return orderService.getSellQueueSize();
//    }

//    @GetMapping("/orders/topBuy")
//    public Order topBuyOrder(){
//        return orderService.getTopBuyOrder();
//    }

//
//    @GetMapping("/orders/topSell")
//    public Order topSellOrder(){
//        return orderService.getTopSellOrder();
//    }

//    @PutMapping("/orders/{id}")
//    public Order updateOrder( @PathVariable int id ,  @Valid @RequestBody Order order){
//        return orderService.updateOrder(id , order);
//    }

    @DeleteMapping("/orders/{id}")
    public String deleteOrder( @PathVariable int id ){
        return orderService.deleteOrder(id);
    }

}
