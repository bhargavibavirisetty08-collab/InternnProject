package com.example.InternProject.Model;

import java.util.List;

public class Level2Snapshot {

    private Integer stockId;
    private List<PriceLevel> buyLevels;
    private List<PriceLevel> sellLevels;

    public Level2Snapshot(
            Integer stockId,
            List<PriceLevel> buyLevels,
            List<PriceLevel> sellLevels) {

        this.stockId = stockId;
        this.buyLevels = buyLevels;
        this.sellLevels = sellLevels;
    }

    public Integer getStockId() {
        return stockId;
    }

    public List<PriceLevel> getBuyLevels() {
        return buyLevels;
    }

    public List<PriceLevel> getSellLevels() {
        return sellLevels;
    }
}
