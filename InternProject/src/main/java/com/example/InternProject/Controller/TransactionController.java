package com.example.InternProject.Controller;

import com.example.InternProject.Model.Transaction;
import com.example.InternProject.Service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TransactionController {
    @Autowired
    TransactionService transactionService;

    @GetMapping("transactions")
    public List<Transaction> getAllTransaction (){
        return transactionService.getAllTransaction();
    }
}
