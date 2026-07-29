package com.example.InternProject.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
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
    @PositiveOrZero
    private  Double price;
    @NotNull
    @PositiveOrZero
    private Integer quantity;

    @PositiveOrZero
    private Integer originalQuantity = 0;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @PositiveOrZero
    private Double lockedAmount = 0.0;

    private LocalDateTime createdAt;
}
