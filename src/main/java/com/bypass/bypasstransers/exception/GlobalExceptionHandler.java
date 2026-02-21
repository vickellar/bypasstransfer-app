package com.bypass.bypasstransers.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Global exception handler for controller-layer exceptions.
 * Converts exceptions to user-friendly flash messages and redirects.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InsufficientBalanceException.class)
    public String handleInsufficientBalance(InsufficientBalanceException ex, RedirectAttributes ra) {
        log.warn("Insufficient balance: {}", ex.getMessage());
        ra.addFlashAttribute("error", ex.getMessage());
        return "redirect:/";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException ex, RedirectAttributes ra) {
        log.warn("Validation error: {}", ex.getMessage());
        ra.addFlashAttribute("error", ex.getMessage());
        return "redirect:/";
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public String handleAccountNotFound(AccountNotFoundException ex, RedirectAttributes ra) {
        log.warn("Account not found: {}", ex.getMessage());
        ra.addFlashAttribute("error", ex.getMessage());
        return "redirect:/";
    }
}
