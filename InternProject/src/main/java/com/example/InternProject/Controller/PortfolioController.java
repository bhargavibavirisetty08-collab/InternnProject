package com.example.InternProject.Controller;

import com.example.InternProject.Model.Portfolio;
import com.example.InternProject.Service.PortfolioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
public class PortfolioController {

    @Autowired
    PortfolioService portfolioService;

    @GetMapping("/portfolio")
    public List<Portfolio>getAllPortfolio(){
       return   portfolioService.getAllPortfolio();
    }


    @PostMapping("/portfolio/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public Portfolio assignPortfolio(@RequestBody  Portfolio portfolio){
        return portfolioService.assignPortfolio(portfolio);
    }
}
