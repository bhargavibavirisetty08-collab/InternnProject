package com.example.InternProject.Service;

import com.example.InternProject.Model.*;
import com.example.InternProject.Repo.OrderRepo;
import com.example.InternProject.Repo.TransactionRepo;
import com.example.InternProject.Repo.UserRepo;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.relational.core.sql.In;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class OrderService {

    @Autowired
    OrderRepo orderRepo;

    @Autowired
    UserRepo userRepo;

    @Autowired
    TransactionRepo transactionRepo; //to add order details in database

//    private PriorityQueue<Order> buyQueue = new PriorityQueue<>(((o1, o2) -> Double.compare(o2.getPrice() , o1.getPrice())));
//    private PriorityQueue<Order> sellQueue = new PriorityQueue<>(((o1, o2) -> Double.compare(o1.getPrice() , o2.getPrice())));

    private Map<Integer , PriorityQueue<Order>> buyQueues = new HashMap<>();
    private  Map<Integer , PriorityQueue<Order>> sellQueues = new HashMap<>();

    private PriorityQueue<Order> getBuyQueue (Integer stockId){
        return buyQueues.computeIfAbsent(stockId , id -> new PriorityQueue<>((o1 , o2) -> Double.compare(o2.getPrice() , o1.getPrice())));
    }

    private PriorityQueue<Order> getSellQueue (Integer stockId){
        return sellQueues.computeIfAbsent(stockId , id->new PriorityQueue<>(((o1, o2) -> Double.compare(o1.getPrice() , o2.getPrice()))));
    }

    public List<Order> getAllOrders(){
        return orderRepo.findAll();
    }

    public Order getById(int id) {
        return orderRepo.findById(id).orElseThrow(() -> new RuntimeException("Order not found"));
    }

    public Order sellOrder(Order order) {

        order.setType(Order_type.SELL);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());

        Order savedOrder =  orderRepo.save(order);

        PriorityQueue<Order> queue = getSellQueue(savedOrder.getStock().getId());
        queue.add(savedOrder);
        matchOrders(savedOrder.getStock().getId());

        return savedOrder;
    }

    public Order buyOrder(Order order) {

        order.setType(Order_type.BUY);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());

        double requiredAmount = order.getPrice() * order.getQuantity();
        User buyer = order.getUser();
        if(requiredAmount > buyer.getBalance()){
            throw new RuntimeException("Insufficient balance");
        }

        Order savedOrder =  orderRepo.save(order);

        PriorityQueue<Order> queue = getBuyQueue(savedOrder.getStock().getId());
        queue.add(savedOrder);
        matchOrders(savedOrder.getStock().getId());

        return savedOrder;
    }

//    public int getBuyQueueSize() {
//        return buyQueue.size();
//    }
//
//    public int getSellQueueSize() {
//        return sellQueue.size();
//    }
//    public Order getTopBuyOrder() {
//        return buyQueue.peek();
//    }
//
//    public Order getTopSellOrder() {
//        return sellQueue.peek();
//    }

//    public Order updateOrder(int id, Order order) {
//        Order getOrder = orderRepo.findById(id).orElse(null);
//        if(getOrder == null){
//            throw new RuntimeException("Order not found");
//        }

//        getOrder.setUser(order.getUser());
//        getOrder.setStock(order.getStock());
//        getOrder.setType(order.getType());
//        getOrder.setPrice(order.getPrice());
//        getOrder.setStatus(order.getStatus());
//        getOrder.setQuantity(order.getQuantity());
//        getOrder.setCreatedAt(order.getCreatedAt());
//
//        return orderRepo.save(getOrder);
//    }


//    public String deleteOrder(int id) {
//
//        Order order = orderRepo.findById(id)
//                .orElseThrow(() -> new RuntimeException("Order not found"));
//
//        if (order.getType() == Order_type.BUY) {
//            buyQueue.remove(order);
//        } else {
//            sellQueue.remove(order);
//        }
//
//        orderRepo.delete(order);
//
//        return "Deleted Successfully";
//    }

    @PostConstruct
    public void loadPendingOrders() {

        List<Order> pendingOrders = orderRepo.findByStatus(OrderStatus.PENDING);

        for (Order order : pendingOrders) {

            if (order.getType() == Order_type.BUY) {
                getBuyQueue(order.getStock().getId()).add(order);
            } else {
               getSellQueue(order.getStock().getId()).add(order);
            }
        }

    }

    @Transactional
    private void matchOrders(Integer stockId) {
        PriorityQueue<Order> buyQueue = getBuyQueue(stockId);
        PriorityQueue<Order> sellQueue = getSellQueue(stockId);

        while (!buyQueue.isEmpty() && !sellQueue.isEmpty()) {
            Order buyOrder = buyQueue.peek();
            Order sellOrder = sellQueue.peek();

            if (buyOrder.getStock().getId().equals(sellOrder.getStock().getId())
                    && buyOrder.getPrice() >= sellOrder.getPrice()) {

                int tradedQuantity = Math.min(
                        buyOrder.getQuantity(),
                        sellOrder.getQuantity()
                );

                double tradeAmount = sellOrder.getPrice() * tradedQuantity;

                Transaction transaction = new Transaction();

                transaction.setBuyer(buyOrder.getUser());
                transaction.setSeller(sellOrder.getUser());
                transaction.setStock(buyOrder.getStock());
                transaction.setQuantity(tradedQuantity);
                transaction.setPrice(sellOrder.getPrice());
                transaction.setTransactionTime(LocalDateTime.now());

                transactionRepo.save(transaction);

                User buyer = buyOrder.getUser();
                User seller = sellOrder.getUser();

                // Update balance
                buyer.setBalance(buyer.getBalance() - tradeAmount);
                seller.setBalance(seller.getBalance() + tradeAmount);

                // Save users
                userRepo.save(buyer);
                userRepo.save(seller);
                // Update remaining quantity
                buyOrder.setQuantity(
                        buyOrder.getQuantity() - tradedQuantity
                );
                sellOrder.setQuantity(
                        sellOrder.getQuantity() - tradedQuantity
                );

                // Update status and remove completed orders
                if (buyOrder.getQuantity() == 0) {
                    buyOrder.setStatus(OrderStatus.COMPLETED);
                    buyQueue.poll();
                }
                if (sellOrder.getQuantity() == 0) {
                    sellOrder.setStatus(OrderStatus.COMPLETED);
                    sellQueue.poll();
                }

                // Save updated orders
                orderRepo.save(buyOrder);
                orderRepo.save(sellOrder);

                System.out.println("Trade completed");
                System.out.println("Quantity : " + tradedQuantity);
                System.out.println("Amount : " + tradeAmount);
            } else {
                break;
            }
        }
    }

}
