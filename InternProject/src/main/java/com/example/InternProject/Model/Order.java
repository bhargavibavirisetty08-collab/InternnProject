package com.example.InternProject.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.sql.Time;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Integer id;

//    private Integer  userId;
//    private  Integer stockId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "stock_id")
    private Stock stock;

    @Enumerated(EnumType.STRING)
    private Order_type type;

    @NotNull
    @Positive
    private  Double price;
    @NotNull
    @Positive
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;


    private LocalDateTime createdAt;
}
