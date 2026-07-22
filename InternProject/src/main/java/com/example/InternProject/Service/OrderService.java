package com.example.InternProject.Service;

import com.example.InternProject.Model.*;
import com.example.InternProject.Repo.OrderRepo;
import com.example.InternProject.Repo.PortfolioRepo;
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
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OrderService {

    @Autowired
    OrderRepo orderRepo;

    @Autowired
    UserRepo userRepo;

    @Autowired
    TransactionRepo transactionRepo; //to add order details in database

    @Autowired
    PortfolioService portfolioService;

    @Autowired
    PortfolioRepo portfolioRepo;

//    private PriorityQueue<Order> buyQueue = new PriorityQueue<>(((o1, o2) -> Double.compare(o2.getPrice() , o1.getPrice())));
//    private PriorityQueue<Order> sellQueue = new PriorityQueue<>(((o1, o2) -> Double.compare(o1.getPrice() , o2.getPrice())));

    private Map<Integer , PriorityQueue<Order>> buyQueues = new ConcurrentHashMap<>();
    private  Map<Integer , PriorityQueue<Order>> sellQueues = new ConcurrentHashMap<>();

//    private PriorityQueue<Order> getBuyQueue (Integer stockId){
//        return buyQueues.computeIfAbsent(stockId , id -> new PriorityQueue<>((o1 , o2) -> Double.compare(o2.getPrice() , o1.getPrice())));
//    }
//
//    private PriorityQueue<Order> getSellQueue (Integer stockId){
//        return sellQueues.computeIfAbsent(stockId , id->new PriorityQueue<>(((o1, o2) -> Double.compare(o1.getPrice() , o2.getPrice()))));
//    }


    //to check the No two threads are come at same time !
    private Map<Integer,Object> stockLocks = new ConcurrentHashMap<>();
    private Object getStockLock(Integer stockId){

        return stockLocks.computeIfAbsent(
                stockId,
                id -> new Object()
        );
    }

    private PriorityQueue<Order> getBuyQueue(Integer stockId) {
        return buyQueues.computeIfAbsent(stockId, id ->
                new PriorityQueue<>((o1, o2) -> {
                    int priceCompare = Double.compare(o2.getPrice(), o1.getPrice());
                    if (priceCompare != 0) {
                        return priceCompare;
                    }
                    return o1.getCreatedAt().compareTo(o2.getCreatedAt());
                })
        );
    }

    private PriorityQueue<Order> getSellQueue(Integer stockId) {
        return sellQueues.computeIfAbsent(stockId, id ->
                new PriorityQueue<>((o1, o2) -> {
                    int priceCompare = Double.compare(o1.getPrice(), o2.getPrice());
                    if (priceCompare != 0) {
                        return priceCompare;
                    }
                    return o1.getCreatedAt().compareTo(o2.getCreatedAt());
                })
        );
    }

    public List<Order> getAllOrders(){
        return orderRepo.findAll();
    }

    public Order getById(int id) {
        return orderRepo.findById(id).orElseThrow(() -> new RuntimeException("Order not found"));
    }

    @Transactional
    public Order sellOrder(Order order) {

        // 1. Check seller owns enough shares
        Portfolio portfolio = portfolioRepo
                .findByUserAndStock(order.getUser(), order.getStock())
                .orElseThrow(() -> new RuntimeException("Stock not found in portfolio"));

        int availableShares = portfolio.getQuantity() - portfolio.getLockedQuantity();

        if(order.getQuantity() > availableShares){
            throw new RuntimeException("Not enough available shares");
        }
        // Lock shares
        portfolio.setLockedQuantity(
                portfolio.getLockedQuantity() + order.getQuantity()
        );

        portfolioRepo.save(portfolio);

        // 2. Set order details
        order.setType(Order_type.SELL);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setOriginalQuantity(order.getQuantity());

        // 3. Save order
        Order savedOrder = orderRepo.save(order);

        // 4. Add to sell queue
        PriorityQueue<Order> queue = getSellQueue(savedOrder.getStock().getId());
        queue.add(savedOrder);

        // 5. Try matching
        matchOrders(savedOrder.getStock().getId());
        return savedOrder;
    }

    @Transactional
    public Order buyOrder(Order order) {
        double requiredAmount = order.getPrice() * order.getQuantity();
        order.setLockedAmount(requiredAmount);

        User buyer = order.getUser();

        double availableBalance =
                buyer.getBalance() - buyer.getLockedBalance();

        if(requiredAmount > availableBalance){
            throw new RuntimeException("Insufficient available balance");
        }

        order.setType(Order_type.BUY);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setOriginalQuantity(order.getQuantity());

        buyer.setLockedBalance(
                buyer.getLockedBalance() + requiredAmount
        );

        userRepo.save(buyer);

        Order savedOrder = orderRepo.save(order);

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

    @Transactional
    public String deleteOrder(int id) {
        Order order = orderRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if(order.getStatus() == OrderStatus.COMPLETED){
            throw new RuntimeException("Completed order cannot be cancelled");
        }

        // Remove from queue
        if(order.getType() == Order_type.BUY){
            PriorityQueue<Order> buyQueue = getBuyQueue(order.getStock().getId());
            buyQueue.remove(order);
            // Release locked money
            User buyer = order.getUser();
            buyer.setLockedBalance(
                    buyer.getLockedBalance() - order.getLockedAmount()
            );
            userRepo.save(buyer);
        } else {
            PriorityQueue<Order> sellQueue =
                    getSellQueue(order.getStock().getId());
            sellQueue.remove(order);
            // Release locked shares
            Portfolio portfolio =
                    portfolioRepo.findByUserAndStock(
                            order.getUser(),
                            order.getStock()
                    ).orElseThrow(() -> new RuntimeException("Portfolio not found"));

            portfolio.setLockedQuantity(portfolio.getLockedQuantity() - order.getQuantity());
            portfolioRepo.save(portfolio);
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepo.save(order);
        return "Order Cancelled Successfully";
    }

    @PostConstruct
    public void loadPendingOrders() {
        List<Order> pendingOrders =  orderRepo.findByStatusIn(List.of(
                        OrderStatus.PENDING,
                        OrderStatus.PARTIALLY_FILLED));

        for (Order order : pendingOrders) {
            if(order.getStock() == null){
                continue;
            }
            if (order.getType() == Order_type.BUY) {
                getBuyQueue(order.getStock().getId()).add(order);
            } else {
               getSellQueue(order.getStock().getId()).add(order);
            }
        }

    }

    @Transactional
    private void matchOrders(Integer stockId) {

        synchronized (getStockLock(stockId)) {

            PriorityQueue<Order> buyQueue = getBuyQueue(stockId);
            PriorityQueue<Order> sellQueue = getSellQueue(stockId);

            while (!buyQueue.isEmpty() && !sellQueue.isEmpty()) {
                Order buyOrder = buyQueue.peek();
                Order sellOrder = sellQueue.peek();

                // Check price matching
                if (buyOrder.getPrice() < sellOrder.getPrice()) {
                    break;
                }

                int tradedQuantity = Math.min(
                        buyOrder.getQuantity(),
                        sellOrder.getQuantity()
                );

                double tradeAmount = sellOrder.getPrice() * tradedQuantity;
                buyOrder.setLockedAmount(buyOrder.getLockedAmount() - tradeAmount);


                // Create transaction history
                Transaction transaction = new Transaction();

                transaction.setBuyer(buyOrder.getUser());
                transaction.setSeller(sellOrder.getUser());
                transaction.setStock(buyOrder.getStock());
                transaction.setQuantity(tradedQuantity);
                transaction.setPrice(sellOrder.getPrice());
                transaction.setTransactionTime(LocalDateTime.now());

                transactionRepo.save(transaction);

                // Update portfolios
                portfolioService.updatePortfolio(
                        buyOrder.getUser(),
                        sellOrder.getUser(),
                        buyOrder.getStock(),
                        tradedQuantity,
                        sellOrder.getPrice()
                );

                // Update balances
                User buyer = buyOrder.getUser();
                User seller = sellOrder.getUser();

                buyer.setLockedBalance(
                        buyer.getLockedBalance() - tradeAmount
                );
                buyer.setBalance(
                        buyer.getBalance() - tradeAmount
                );
                seller.setBalance(
                        seller.getBalance() + tradeAmount
                );

                userRepo.save(buyer);
                userRepo.save(seller);

                // Reduce remaining quantities
                buyOrder.setQuantity(
                        buyOrder.getQuantity() - tradedQuantity
                );

                sellOrder.setQuantity(
                        sellOrder.getQuantity() - tradedQuantity
                );

                // Update BUY order status
                if (buyOrder.getQuantity() == 0) {
                    buyOrder.setStatus(OrderStatus.COMPLETED);
//                    buyer.setLockedBalance(
//                            buyer.getLockedBalance() - buyOrder.getLockedAmount()
//                    );
                    userRepo.save(buyer);
                    buyQueue.poll();
                } else {
                    buyOrder.setStatus(OrderStatus.PARTIALLY_FILLED);
                }

                // Update SELL order status
                if (sellOrder.getQuantity() == 0) {
                    sellOrder.setStatus(OrderStatus.COMPLETED);
                    sellQueue.poll();
                } else {
                    sellOrder.setStatus(OrderStatus.PARTIALLY_FILLED);
                }
                // Save updated orders
                orderRepo.save(buyOrder);
                orderRepo.save(sellOrder);

                System.out.println("Trade completed");
                System.out.println("Stock : " + buyOrder.getStock().getSymbol());
                System.out.println("Quantity : " + tradedQuantity);
                System.out.println("Price : " + sellOrder.getPrice());
                System.out.println("Amount : " + tradeAmount);
            }
        }
    }

}
