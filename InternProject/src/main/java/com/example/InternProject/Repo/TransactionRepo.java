package com.example.InternProject.Repo;

import com.example.InternProject.Model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepo  extends JpaRepository<Transaction , Integer> {

}
