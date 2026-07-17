package com.example.InternProject.Repo;

import com.example.InternProject.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepo extends JpaRepository<User , Integer> {
   public User findByUserName(String userName);
}
