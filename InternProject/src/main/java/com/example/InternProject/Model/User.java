package com.example.InternProject.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Entity
@Data
@Table(name="userdata")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @NotBlank
    private String userName;
    @NotBlank
    private String password;
    @NotNull
    @Positive
    private Double balance = 100000.0;

    @Enumerated(EnumType.STRING)
    private Role role;

    @NotNull
    private Double LockedBalance = 0.0;
}
