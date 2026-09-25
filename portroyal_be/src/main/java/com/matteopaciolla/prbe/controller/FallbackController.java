package com.matteopaciolla.prbe.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class FallbackController {

    @GetMapping(path={"/", "home", "homepage"}, produces = "text/html")
    public String homepage(Principal principal, Model model) {
        model.addAttribute("isLoggedIn", principal != null);
        return "homepage";
    }
}