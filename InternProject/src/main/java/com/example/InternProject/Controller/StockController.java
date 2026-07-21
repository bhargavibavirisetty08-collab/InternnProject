package com.example.InternProject.Controller;

import com.example.InternProject.Model.Stock;
import com.example.InternProject.Service.StockService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class StockController {

    @Autowired
    StockService stockService;

    @GetMapping("/stocks")
    public List<Stock> getAllStocks(){
        return stockService.getAllStocks();
    }

    @PostMapping("/stocks")
    @PreAuthorize("hasRole('ADMIN')")
    public Stock postAllStocks(@Valid  @RequestBody Stock stock){
            stockService.postAllStocks(stock);
            return stock;
    }

    @GetMapping("/stocks/{id}")
    public Stock getById( @PathVariable int id){
      return stockService.getById(id);
    }

    @PutMapping("/stocks/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Stock updateStock(@Valid @PathVariable int id , @RequestBody Stock stock){
       return stockService.updateStock(id , stock);
    }

    @DeleteMapping("/stocks/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteStock(@Valid @PathVariable int id){
       return stockService.deleteStock(id);
    }
}
