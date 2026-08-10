package com.example.InternProject.Service;

import com.example.InternProject.Model.*;
import com.example.InternProject.Repo.OrderRepo;
import com.example.InternProject.Repo.PortfolioRepo;
import com.example.InternProject.Repo.TransactionRepo;
import com.example.InternProject.Repo.UserRepo;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
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
//    private PriorityQueue<Order> sellQueue = new PriorityQueue<>(((o1, o2) -> Double.compare(o1.getPrice() , o2.getPrice()))); //1st implement

//    private PriorityQueue<Order> getBuyQueue (Integer stockId){
//        return buyQueues.computeIfAbsent(stockId , id -> new PriorityQueue<>((o1 , o2) -> Double.compare(o2.getPrice() , o1.getPrice())));
//    }
//
//    private PriorityQueue<Order> getSellQueue (Integer stockId){
//        return sellQueues.computeIfAbsent(stockId , id->new PriorityQueue<>(((o1, o2) -> Double.compare(o1.getPrice() , o2.getPrice()))));
//    }

//    private Map<Integer , PriorityQueue<Order>> buyQueues = new ConcurrentHashMap<>();
//    private  Map<Integer , PriorityQueue<Order>> sellQueues = new ConcurrentHashMap<>(); //2nd implement

//    private PriorityQueue<Order> getBuyQueue(Integer stockId) {
//        return buyQueues.computeIfAbsent(stockId, id ->
//                new PriorityQueue<>((o1, o2) -> {
//                    int priceCompare = Double.compare(o2.getPrice(), o1.getPrice());
//                    if (priceCompare != 0) {
//                        return priceCompare;
//                    }
//                    return o1.getCreatedAt().compareTo(o2.getCreatedAt());
//                })
//        );
//    }                       // delete for store orders in a queue !!!!!!!!!!!!!

//    private PriorityQueue<Order> getSellQueue(Integer stockId) {
//        return sellQueues.computeIfAbsent(stockId, id ->
//                new PriorityQueue<>((o1, o2) -> {
//                    int priceCompare = Double.compare(o1.getPrice(), o2.getPrice());
//                    if (priceCompare != 0) {
//                        return priceCompare;
//                    }
//                    return o1.getCreatedAt().compareTo(o2.getCreatedAt());
//                })
//        );
//    }

   private Map<Integer, OrderBook> orderBooks = new ConcurrentHashMap<>(); // for every company has it's own orderBook (3rd implement)

    private OrderBook getOrderBook(Integer stockId) {
        return orderBooks.computeIfAbsent(
                stockId,
                id -> new OrderBook()
        );
    }

    private ArrayDeque<Order> getBuyQueue(Integer stockId, Double price) {
        OrderBook orderBook = getOrderBook(stockId);

        return orderBook.getBuyLevels()
                .computeIfAbsent(price, p -> new ArrayDeque<>());
    }

    private ArrayDeque<Order> getSellQueue(Integer stockId, Double price) {
        OrderBook orderBook = getOrderBook(stockId);

        return orderBook.getSellLevels()
                .computeIfAbsent(price, p -> new ArrayDeque<>());
    }

    //to check the No two threads are come at same time !
    private Map<Integer,Object> stockLocks = new ConcurrentHashMap<>();
    private Object getStockLock(Integer stockId){

        return stockLocks.computeIfAbsent(
                stockId,
                id -> new Object()
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

        User seller = userRepo.findById(order.getUser().getId())
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        order.setUser(seller);

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
      ArrayDeque<Order> queue = getSellQueue(
                savedOrder.getStock().getId(),
                savedOrder.getPrice()
        );
        queue.offer(savedOrder);

        // 5. Try matching
        matchOrders(savedOrder.getStock().getId() ,  portfolio);
        return savedOrder;
    }

    @Transactional
    public Order buyOrder(Order order) {
        double requiredAmount = order.getPrice() * order.getQuantity();
        order.setLockedAmount(requiredAmount);

//        User buyer = order.getUser();
        User buyer = userRepo.findById(order.getUser().getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        order.setUser(buyer);

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

        ArrayDeque<Order> queue = getBuyQueue(
                savedOrder.getStock().getId(),
                savedOrder.getPrice()
        );
        queue.offer(savedOrder);

        matchOrders(savedOrder.getStock().getId() ,  null);

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

        if(order.getStatus() == OrderStatus.COMPLETED ||
                order.getStatus() == OrderStatus.CANCELLED){
            throw new RuntimeException("Completed order cannot be cancelled");
        }

        OrderBook orderBook = getOrderBook(order.getStock().getId());

        // Remove from queue
        if(order.getType() == Order_type.BUY){

            TreeMap<Double, ArrayDeque<Order>> buyLevels = orderBook.getBuyLevels();
            ArrayDeque<Order> buyQueue = buyLevels.get(order.getPrice());
            if (buyQueue != null) {
                buyQueue.remove(order);
                if (buyQueue.isEmpty()) {
                    buyLevels.remove(order.getPrice());
                }
            }

            // Release locked money
            User buyer = order.getUser();
            buyer.setLockedBalance(
                    buyer.getLockedBalance() - order.getLockedAmount()
            );
            userRepo.save(buyer);
        } else if (order.getType() == Order_type.SELL) {

            TreeMap<Double, ArrayDeque<Order>> sellLevels = orderBook.getSellLevels();
            ArrayDeque<Order> sellQueue = sellLevels.get(order.getPrice());
            if (sellQueue != null) {
                sellQueue.remove(order);
                if (sellQueue.isEmpty()) {
                    sellLevels.remove(order.getPrice());
                }
            }

            // Release locked shares
            Portfolio portfolio =
                    portfolioRepo.findByUserAndStock(
                            order.getUser(),
                            order.getStock()
                    ).orElseThrow(() -> new RuntimeException("Portfolio not found"));

            portfolio.setLockedQuantity(portfolio.getLockedQuantity() - order.getQuantity());
            portfolioRepo.save(portfolio);
        }
        else {
            throw new RuntimeException("Invalid order type");
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
            OrderBook orderBook =
                    getOrderBook(order.getStock().getId());
            if (order.getType() == Order_type.BUY) {
                orderBook.getBuyLevels()
                        .computeIfAbsent(
                                order.getPrice(),
                                price ->   new ArrayDeque<>()
                        )
                        .add(order);

            } else if (order.getType() == Order_type.SELL) {
                orderBook.getSellLevels()
                        .computeIfAbsent(
                                order.getPrice(),
                                price ->  new ArrayDeque<>()
                        )
                        .add(order);
            }
        }
    }

    @Transactional
    public void matchOrders(Integer stockId , Portfolio sellerPortfolio) {

        synchronized (getStockLock(stockId)) {

          //  long startTime = System.nanoTime(); // calculating time before and after optimization

            OrderBook orderBook = getOrderBook(stockId);
            TreeMap<Double, ArrayDeque<Order>> buyLevels =
                    orderBook.getBuyLevels();
            TreeMap<Double, ArrayDeque<Order>> sellLevels =
                    orderBook.getSellLevels();
//            System.out.println("========== ORDER BOOK DEBUG ==========");
//            System.out.println("Stock ID: " + stockId);
//            System.out.println("BUY LEVELS: " + buyLevels.keySet());
//            System.out.println("SELL LEVELS: " + sellLevels.keySet());
//            System.out.println("======================================");

            long totalStart = System.nanoTime();
            long matchingStart = 0;
            long matchingEnd = 0;
            long dbStart = 0;
            long dbEnd = 0;
            while (!buyLevels.isEmpty() && !sellLevels.isEmpty()) {

                matchingStart = System.nanoTime();
                // 1. Get best prices
                Map.Entry<Double, ArrayDeque<Order>> bestBuy =
                        buyLevels.lastEntry();

                Map.Entry<Double, ArrayDeque<Order>> bestSell =
                        sellLevels.firstEntry();

                // 2. Get queues
               ArrayDeque<Order> buyQueue = bestBuy.getValue();
                ArrayDeque<Order> sellQueue = bestSell.getValue();

                // 3. Get first order at each price
                Order buyOrder = buyQueue.peek();
                Order sellOrder = sellQueue.peek();

                // 4. Check price matching
                if (buyOrder.getPrice() < sellOrder.getPrice()) {
                    break;
                }

                if (sellerPortfolio == null) {
                    sellerPortfolio = portfolioRepo
                            .findByUserAndStock(      // because you give buyOrder is null  and sellOrder should not be null so , we checkk sell
                                    sellOrder.getUser(),
                                    sellOrder.getStock()
                            )
                            .orElseThrow(() ->
                                    new RuntimeException("Seller portfolio not found"));
                }

                int tradedQuantity = Math.min(
                        buyOrder.getQuantity(),
                        sellOrder.getQuantity()
                );

                double tradeAmount = sellOrder.getPrice() * tradedQuantity;
                double reservedAmount = buyOrder.getPrice() * tradedQuantity;
                buyOrder.setLockedAmount(buyOrder.getLockedAmount() - reservedAmount);

                matchingEnd = System.nanoTime();
                dbStart = System.nanoTime();

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
                        sellOrder.getPrice(),
                        sellerPortfolio
                );

//                User buyer = buyOrder.getUser(); // by this we get password null exception
//                User seller = sellOrder.getUser();

//                System.out.println("===== USER DEBUG =====");
//
//                System.out.println("BUY USER ID       : " + buyOrder.getUser().getId());
//                System.out.println("BUY USERNAME      : " + buyOrder.getUser().getUserName());
//             //   System.out.println("BUY PASSWORD      : " + buyOrder.getUser().getPassword());
//                System.out.println("BUY BALANCE       : " + buyOrder.getUser().getBalance());
//                System.out.println("BUY LOCKED BALANCE: " + buyOrder.getUser().getLockedBalance());
//
//                System.out.println("SELL USER ID       : " + sellOrder.getUser().getId());
//                System.out.println("SELL USERNAME      : " + sellOrder.getUser().getUserName());
//              //  System.out.println("SELL PASSWORD      : " + sellOrder.getUser().getPassword());
//                System.out.println("SELL BALANCE       : " + sellOrder.getUser().getBalance());
//                System.out.println("SELL LOCKED BALANCE: " + sellOrder.getUser().getLockedBalance());

                System.out.println("=====================");
                 // Update balances
//                User buyer = userRepo.findById(
//                        buyOrder.getUser().getId()
//                ).orElseThrow(() -> new RuntimeException("Buyer not found")); // * it takes a 2 extra db operations *
                User buyer = buyOrder.getUser();

//                User seller = userRepo.findById(
//                        sellOrder.getUser().getId()
//                ).orElseThrow(() -> new RuntimeException("Seller not found")); // to reduce db operations we put these in sell & buy order methods

                User seller = sellOrder.getUser();

                buyer.setLockedBalance(buyer.getLockedBalance() - reservedAmount);
                buyer.setBalance(buyer.getBalance() - tradeAmount);
                seller.setBalance(seller.getBalance() + tradeAmount);

//                userRepo.save(buyer);
//                userRepo.save(seller);


                // Reduce remaining quantities
                buyOrder.setQuantity(buyOrder.getQuantity() - tradedQuantity);

                sellOrder.setQuantity(sellOrder.getQuantity() - tradedQuantity);

                // Update BUY order status
                if (buyOrder.getQuantity() == 0) {
                    buyOrder.setStatus(OrderStatus.COMPLETED);
//                    userRepo.save(buyer);
                    buyQueue.poll();
                    if (buyQueue.isEmpty()) {
                        buyLevels.remove(bestBuy.getKey()); // if no orders left in that price so we delete that entire queue
                    }
                } else {
                    buyOrder.setStatus(OrderStatus.PARTIALLY_FILLED);
                }

                // Update SELL order status
                if (sellOrder.getQuantity() == 0) {
                    sellOrder.setStatus(OrderStatus.COMPLETED);
                    sellQueue.poll();
                    if (sellQueue.isEmpty()) {
                        sellLevels.remove(bestSell.getKey()); // if no orders left in that price so we delete that entire queue
                    }
                } else {
                    sellOrder.setStatus(OrderStatus.PARTIALLY_FILLED);
                }
                // Save updated orders
//                orderRepo.save(buyOrder); // No save needed.
                                             // Hibernate dirty checking automatically persists the changes.
//                orderRepo.save(sellOrder);

                dbEnd = System.nanoTime();
                System.out.println("----- PERFORMANCE -----");
                System.out.println(
                        "Pure Matching Time: " +
                                (matchingEnd - matchingStart) / 1_000_000.0 + " ms"
                );
                System.out.println(
                        "DB/Business Time: " +
                                (dbEnd - dbStart) / 1_000_000.0 + " ms"
                );

                System.out.println("-----------------------");

//                System.out.println("========== STOCK DEBUG ==========");
//                System.out.println("Stock object : " + buyOrder.getStock());
//                System.out.println("Stock ID     : " + buyOrder.getStock().getId());
//                System.out.println("Stock Symbol : " + buyOrder.getStock().getSymbol());
//                System.out.println("Company Name : " + buyOrder.getStock().getCompanyName());
//                System.out.println("=================================");
//                System.out.println("Trade completed");
//                System.out.println("Stock : " + buyOrder.getStock().getSymbol());
//                System.out.println("Quantity : " + tradedQuantity);
//                System.out.println("Price : " + sellOrder.getPrice());
//                System.out.println("Amount : " + tradeAmount);
            }
            long totalEnd = System.nanoTime();

            System.out.println(
                    "Total matchOrders() Time: " +
                            (totalEnd - totalStart) / 1_000_000.0 + " ms"
            );
        }
    }

}
