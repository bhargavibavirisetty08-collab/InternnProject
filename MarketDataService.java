package com.example.InternProject.Service;

import com.example.InternProject.Model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.*;

@Service
public class MarketDataService {

    @Autowired
    private OrderService orderService;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public Level2Snapshot getLevel2Snapshot(Integer stockId) {

        OrderBook orderBook =
                orderService.getOrderBookForMarketData(stockId);

        if (orderBook == null) {
            return new Level2Snapshot(
                    stockId,
                    new ArrayList<>(),
                    new ArrayList<>()
            );
        }

        List<PriceLevel> buyLevels = new ArrayList<>();

        for (Map.Entry<Double, ArrayDeque<Order>> entry :
                orderBook.getBuyLevels().entrySet()) {

            int totalQuantity = 0;

            for (Order order : entry.getValue()) {
                totalQuantity += order.getQuantity();
            }

            buyLevels.add(
                    new PriceLevel(
                            entry.getKey(),
                            totalQuantity
                    )
            );
        }

        List<PriceLevel> sellLevels = new ArrayList<>();

        for (Map.Entry<Double, ArrayDeque<Order>> entry :
                orderBook.getSellLevels().entrySet()) {

            int totalQuantity = 0;

            for (Order order : entry.getValue()) {
                totalQuantity += order.getQuantity();
            }

            sellLevels.add(
                    new PriceLevel(
                            entry.getKey(),
                            totalQuantity
                    )
            );
        }

        return new Level2Snapshot(
                stockId,
                buyLevels,
                sellLevels
        );
    }
    @Scheduled(fixedRate = 100)
    public void broadcastMarketData() {

        for (Integer stockId : orderService.getStockIds()) {

            Level2Snapshot snapshot =
                    getLevel2Snapshot(stockId);

            messagingTemplate.convertAndSend(
                    "/topic/market/" + stockId,
                    snapshot
            );
        }
    }
}