package com.example.InternProject.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name="userdata")
public class User {
    @Id
    private Integer id;
    private String userName;
    private String password;
}
