package com.example.InternProject.Repo;

import com.example.InternProject.Model.Portfolio;
import com.example.InternProject.Model.Stock;
import com.example.InternProject.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PortfolioRepo extends JpaRepository<Portfolio , Integer> {
    Optional<Portfolio> findByUserAndStock(User user, Stock stock);
}
