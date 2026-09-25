package com.matteopaciolla.prbe.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(@RequestParam(value = "logout", required = false) Boolean logout,
                        @RequestParam(value = "error", required = false) Boolean error,
                        Model model) {
        if (error != null && error) {
            model.addAttribute("error", "Invalid username or password");
        } else {
            model.addAttribute("error", null);
        }
        if (logout != null && logout) {
            model.addAttribute("isLoggedIn", false);
            return "redirect:/homepage"; // Redirect to login page with logout message
        } else {
            return "login"; // Return the name of the login view (e.g., login.html)
        }
    }

    @PostMapping("/perform_login")
    public String performLogin() {
        return "homepage";
    }
}