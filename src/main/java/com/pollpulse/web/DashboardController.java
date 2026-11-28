package com.pollpulse.web;

import com.pollpulse.exception.ResourceNotFoundException;
import com.pollpulse.user.User;
import com.pollpulse.user.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final UserRepository userRepository;

    public DashboardController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    public String dashboard(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userRepository.findByEmailIgnoreCase(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        model.addAttribute("name", user.getName());
        model.addAttribute("roles", user.getRoles().stream()
                .map(role -> role.getName().name())
                .toList());
        return "dashboard";
    }
}
