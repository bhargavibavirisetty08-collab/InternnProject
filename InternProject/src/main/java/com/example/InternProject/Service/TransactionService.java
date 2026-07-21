package com.example.InternProject.Service;

import com.example.InternProject.Model.Transaction;
import com.example.InternProject.Repo.TransactionRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.List;

@Service
public class TransactionService {

    @Autowired
    TransactionRepo transactionRepo;

    public List<Transaction> getAllTransaction(){
        return transactionRepo.findAll();
    }
}
