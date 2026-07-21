package com.example.InternProject.Repo;

import com.example.InternProject.Model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockRepo extends JpaRepository<Stock , Integer> {

}
