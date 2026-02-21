package com.bypass.bypasstransers.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String loginPage(Authentication authentication) {
        // If the user is already authenticated, avoid showing the login page and redirect to the app root.
        if (authentication != null && authentication.isAuthenticated() &&
                !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/";
        }
        return "login";
    }

}