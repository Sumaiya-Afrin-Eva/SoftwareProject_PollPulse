package com.pollpulse.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RegisterPageController {

    @GetMapping("/register")
    @PreAuthorize("permitAll()")
    public String register() {
        return "register";
    }
}
