package com.example.InternProject.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.sql.Time;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "transaction")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
//    private Integer buyerId;
//    private Integer sellerId;
//    private Integer stockId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="buyer_id" , nullable = false)
    @NotNull
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="seller_id" , nullable = false)
    @NotNull
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="stock_id" , nullable = false)
    @NotNull
    private Stock stock;

    @Positive
    @NotNull
    private Double  price;

    @Positive
    @NotNull
    private Integer quantity;

    @NotNull
    private LocalDateTime transactionTime;
}
