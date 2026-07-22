package com.example.InternProject.Repo;

import com.example.InternProject.Model.Order;
import com.example.InternProject.Model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepo extends JpaRepository<Order , Integer> {
    List<Order> findByStatusIn(List<OrderStatus> statuses);
}
