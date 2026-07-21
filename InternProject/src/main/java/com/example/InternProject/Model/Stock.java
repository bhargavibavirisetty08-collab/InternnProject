package com.example.InternProject.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;


@Entity
@Data
@Table (name = "stock")
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Integer id;

    @NotBlank
   private String symbol;

    @NotBlank
   private String companyName;

    @NotNull
    @Positive
   private Double currentPrice;

    @NotNull
    @Positive
   private Integer availableQuantity;
}
