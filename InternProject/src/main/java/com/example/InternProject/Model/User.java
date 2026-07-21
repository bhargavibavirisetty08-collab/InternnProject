package com.example.InternProject.Model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name="userdata")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String userName;
    private String password;
    private Double balance = 100000.0;

    @Enumerated(EnumType.STRING)
    private Role role;
}
