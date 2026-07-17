package com.example.InternProject.Controller;

import com.example.InternProject.Model.User;
import com.example.InternProject.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @Autowired
    UserService service;

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

}
