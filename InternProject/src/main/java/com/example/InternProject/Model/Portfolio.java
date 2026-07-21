package com.example.InternProject.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Entity
@Data
@Table(name = "portfolio")
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

//    private Integer userId;
//    private Integer stockId;

    @ManyToOne
    @JoinColumn(name="user_id" , nullable = false)
    @NotNull
    private User user;

    @ManyToOne
    @JoinColumn(name="stock_id" , nullable = false)
    @NotNull
    private Stock stock;

    @NotNull
    @Positive
    private Integer quantity;

    @NotNull
    @Positive
    private Double averagePrice;
}
