package com.example.InternProject.Service;

import com.example.InternProject.Model.Order;
import com.example.InternProject.Model.Portfolio;
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
}
