package com.example.InternProject.Controller;

import com.example.InternProject.Model.User;
import com.example.InternProject.Repo.UserRepo;
import com.example.InternProject.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {

    @Autowired
    UserService service;

    @Autowired
    UserRepo userRepo;

    @GetMapping("/")
    public String greet(){
        return "Hello Guys , lets start the project";
    }
    @PostMapping("/register")
      public User register(@RequestBody User user){
        return service.register(user);
      }

      @PostMapping("/login")
      public String login (@RequestBody User user){
        return service.verify(user);
      }

      @GetMapping("/wallet")
      public Double getWallet(){
        return service.getLoggedInUser().getBalance();
      }

    @PostMapping("/wallet/add")
    public String addMoney(@RequestParam Double amount) {
        User user = service.getLoggedInUser();
        user.setBalance(user.getBalance() + amount);
        userRepo.save(user);
        return "Money added successfully";
    }

}
