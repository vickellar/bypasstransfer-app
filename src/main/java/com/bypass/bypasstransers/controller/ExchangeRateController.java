package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.model.ExchangeRate;
import com.bypass.bypasstransers.repository.ExchangeRateRepository;
import com.bypass.bypasstransers.service.ExchangeRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/exchange-rates")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class ExchangeRateController {

    @Autowired
    private ExchangeRateService exchangeRateService;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @GetMapping
    public String viewRates(Model model) {
        List<ExchangeRate> rates = exchangeRateRepository.findAll().stream()
                .sorted(Comparator.comparing(ExchangeRate::getToCurrency))
                .collect(Collectors.toList());
        
        model.addAttribute("rates", rates);
        model.addAttribute("allRates", exchangeRateService.getAllRates());
        return "admin-exchange-rates";
    }

    @PostMapping("/update")
    public String updateRate(@RequestParam String currency, 
                             @RequestParam BigDecimal rate, 
                             RedirectAttributes ra) {
        try {
            exchangeRateService.setManualRate(currency, rate);
            ra.addFlashAttribute("success", "Exchange rate for " + currency + " updated to " + rate);
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to update rate: " + e.getMessage());
        }
        return "redirect:/admin/exchange-rates";
    }

    @PostMapping("/fetch-live")
    public String fetchLive(RedirectAttributes ra) {
        try {
            exchangeRateService.fetchLiveRates();
            ra.addFlashAttribute("success", "Live exchange rates fetched successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to fetch live rates: " + e.getMessage());
        }
        return "redirect:/admin/exchange-rates";
    }
}
