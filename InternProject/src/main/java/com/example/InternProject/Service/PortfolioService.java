package com.example.InternProject.Service;

import com.example.InternProject.Model.Order;
import com.example.InternProject.Model.Portfolio;
import com.example.InternProject.Model.Stock;
import com.example.InternProject.Model.User;
import com.example.InternProject.Repo.OrderRepo;
import com.example.InternProject.Repo.PortfolioRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PortfolioService {
    @Autowired
    PortfolioRepo portfolioRepo;

    public List<Portfolio> getAllPortfolio(){
        return portfolioRepo.findAll();
    }

    public void updatePortfolio(User buyer , User seller , Stock stock , int Quantity , double price){
        Portfolio sellerPortfolio = portfolioRepo.findByUserAndStock(seller , stock).orElseThrow(()->new RuntimeException("Stock not Found"));
        sellerPortfolio.setQuantity(sellerPortfolio.getQuantity() - Quantity);

        sellerPortfolio.setLockedQuantity(
                sellerPortfolio.getLockedQuantity() - Quantity
        );

        portfolioRepo.save(sellerPortfolio);

        Portfolio buyerPortfolio = portfolioRepo.findByUserAndStock(buyer , stock).orElse(null);

        if(buyerPortfolio == null){
            buyerPortfolio = new Portfolio();
            buyerPortfolio.setUser(buyer);
            buyerPortfolio.setQuantity(Quantity);
            buyerPortfolio.setAveragePrice(price);
            buyerPortfolio.setStock(stock);
        }
        else{
            int oldQuantity = buyerPortfolio.getQuantity();
            double oldAveragePrice = buyerPortfolio.getAveragePrice();

            int totalQuantity = oldQuantity + Quantity;

            double newAveragePrice =
                    ((oldQuantity * oldAveragePrice) + (Quantity * price))
                            / totalQuantity;

            buyerPortfolio.setQuantity(totalQuantity);
            buyerPortfolio.setAveragePrice(newAveragePrice);
        }
        portfolioRepo.save(buyerPortfolio);
    }

    public Portfolio assignPortfolio(Portfolio portfolio) {
        return portfolioRepo.save(portfolio);
    }
}
