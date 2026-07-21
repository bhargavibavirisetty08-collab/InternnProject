package com.example.InternProject.Service;

import com.example.InternProject.Model.Order;
import com.example.InternProject.Model.Stock;
import com.example.InternProject.Repo.OrderRepo;
import com.example.InternProject.Repo.StockRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StockService {
    @Autowired
    StockRepo stockRepo;

    public List<Stock> getAllStocks() {
        return stockRepo.findAll();
    }

    public Stock postAllStocks(Stock stock) {
       return stockRepo.save(stock);
    }

    public Stock getById(int id) {
        return stockRepo.findById(id).orElse(null);
    }

    public Stock updateStock(int id, Stock updateStock) {
        Stock stock = stockRepo.findById(id).orElse(null);
        if (stock == null) {
            throw new RuntimeException("Stock not found");
        }
        stock.setSymbol(updateStock.getSymbol());
        stock.setCompanyName(updateStock.getCompanyName());
        stock.setCurrentPrice(updateStock.getCurrentPrice());
        stock.setAvailableQuantity(updateStock.getAvailableQuantity());

        return stockRepo.save(stock);
    }

    public String deleteStock(int id) {

        if (!stockRepo.existsById(id)) {
            return "Stock not Found";
        } else {
            stockRepo.deleteById(id);
            return "Delete Successfully";
        }
    }
}
