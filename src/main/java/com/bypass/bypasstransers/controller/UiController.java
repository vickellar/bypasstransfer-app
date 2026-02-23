package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 *
 * @author Vickeller.01
 */
@Controller
public class UiController {

    @Autowired
    private final TransactionService service;

    public UiController(TransactionService service) {
        this.service = service;
    }

    @GetMapping("/transactions")
    public String transactions(Model model) {
        model.addAttribute("transactions", service.findAll());
        return "redirect:/app";
    }
    
   
}