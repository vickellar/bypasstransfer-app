package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.dto.ExpenditureDTO;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.model.Expenditure;
import com.bypass.bypasstransers.model.Wallet;
import com.bypass.bypasstransers.repository.ExpenditureRepository;
import com.bypass.bypasstransers.repository.WalletRepository;
import com.bypass.bypasstransers.service.SecurityService;
import com.bypass.bypasstransers.service.AuditService;
import com.bypass.bypasstransers.service.WalletService;
import com.bypass.bypasstransers.enums.Currency;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/staff")
@PreAuthorize("hasAnyRole('STAFF','SUPERVISOR','ADMIN','SUPER_ADMIN')")
public class StaffExpenseController {

    @Autowired
    private SecurityService securityService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private ExpenditureRepository expenditureRepository;

    @Autowired
    private AuditService auditService;

    @GetMapping("/expenses")
    public String listExpenses(Model model) {
        User currentUser = securityService.getCurrentUser();
        List<Expenditure> expenses = expenditureRepository.findByRecordedBy(currentUser);
        
        model.addAttribute("expenses", expenses);
        model.addAttribute("user", currentUser);
        model.addAttribute("totalAmount", expenses.stream().mapToDouble(Expenditure::getAmount).sum());
        
        return "staff-expenses";
    }

    @GetMapping("/expenses/new")
    public String newExpenseForm(Model model) {
        User currentUser = securityService.getCurrentUser();
        
        // Get user's wallets
        List<Wallet> userWallets = walletService.getCurrentUserWallets();
        
        model.addAttribute("expense", new ExpenditureDTO());
        model.addAttribute("user", currentUser);
        model.addAttribute("wallets", userWallets);
        model.addAttribute("currencies", Currency.values());
        
        return "staff-expense-form";
    }

    @PostMapping("/expenses/save")
    public String saveExpense(
            @RequestParam("walletId") Long walletId,
            @RequestParam("amount") Double amount,
            @RequestParam("category") String category,
            @RequestParam("currency") Currency currency,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "description", required = false) String description,
            RedirectAttributes ra) {
        try {
            User currentUser = securityService.getCurrentUser();
            
            // Validate required fields
            if (amount <= 0) {
                ra.addFlashAttribute("error", "Amount must be greater than zero");
                return "redirect:/staff/expenses/new";
            }
            
            if (walletId == null) {
                ra.addFlashAttribute("error", "Please select a wallet");
                return "redirect:/staff/expenses/new";
            }
            
            // Verify wallet ownership
            Wallet wallet = walletService.getWalletById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
            
            if (!wallet.getOwner().getId().equals(currentUser.getId()) && !securityService.isSupervisorOrAbove()) {
                ra.addFlashAttribute("error", "You can only use your own wallets");
                return "redirect:/staff/expenses/new";
            }
            
            // Check if wallet has sufficient balance
            if (wallet.getBalance() < amount) {
                ra.addFlashAttribute("error", "Insufficient wallet balance. Available: " + wallet.getBalance() + " " + wallet.getCurrency());
                return "redirect:/staff/expenses/new";
            }
            
            // Create expenditure
            Expenditure expense = new Expenditure();
            expense.setDescription(description);
            expense.setCategory(category);
            expense.setAmount(amount);
            expense.setDate(date != null ? date : LocalDate.now());
            expense.setRecordedBy(currentUser);
            expense.setCurrency(currency);
            expense.setWallet(wallet); // Link to wallet
            
            expenditureRepository.save(expense);
            
            // Deduct from wallet balance
            wallet.setBalance(wallet.getBalance() - amount);
            walletRepository.save(wallet);
            
            // Audit log
            auditService.logEntity(currentUser.getUsername(), "expenditures", expense.getId(), "CREATE",
                "0", String.valueOf(expense.getAmount()));
            
            ra.addFlashAttribute("success", "Expense recorded successfully. Amount deducted from " + wallet.getAccountType() + " wallet.");
            
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to save expense: " + e.getMessage());
        }
        
        return "redirect:/staff/expenses";
    }

    @GetMapping("/expenses/{id}/edit")
    public String editExpenseForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        User currentUser = securityService.getCurrentUser();
        Expenditure expense = expenditureRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Expense not found"));
        
        // Check ownership
        if (!expense.getRecordedBy().getId().equals(currentUser.getId())) {
            ra.addFlashAttribute("error", "You can only edit your own expenses");
            return "redirect:/staff/expenses";
        }
        
        ExpenditureDTO expenseDTO = new ExpenditureDTO();
        expenseDTO.setId(expense.getId());
        expenseDTO.setAmount(expense.getAmount());
        expenseDTO.setCategory(expense.getCategory());
        expenseDTO.setDescription(expense.getDescription());
        expenseDTO.setDate(expense.getDate());
        expenseDTO.setNotes(expense.getNotes());
        expenseDTO.setCurrency(expense.getCurrency());
        
        model.addAttribute("expense", expenseDTO);
        model.addAttribute("user", currentUser);
        model.addAttribute("isEdit", true);
        
        return "staff-expense-form";
    }

    @PostMapping("/expenses/{id}/update")
    public String updateExpense(@PathVariable Long id, @ModelAttribute ExpenditureDTO expenseDTO, RedirectAttributes ra) {
        try {
            User currentUser = securityService.getCurrentUser();
            Expenditure existingExpense = expenditureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found"));
            
            // Check ownership
            if (!existingExpense.getRecordedBy().getId().equals(currentUser.getId())) {
                ra.addFlashAttribute("error", "You can only edit your own expenses");
                return "redirect:/staff/expenses";
            }
            
            // Validate
            if (expenseDTO.getAmount() <= 0) {
                ra.addFlashAttribute("error", "Amount must be greater than zero");
                return "redirect:/staff/expenses/" + id + "/edit";
            }
            
            // Update expense
            existingExpense.setAmount(expenseDTO.getAmount());
            existingExpense.setCategory(expenseDTO.getCategory());
            existingExpense.setDescription(expenseDTO.getDescription());
            existingExpense.setDate(expenseDTO.getDate());
            existingExpense.setNotes(expenseDTO.getNotes());
            existingExpense.setCurrency(expenseDTO.getCurrency() != null ? expenseDTO.getCurrency() : Currency.USD);
            
            expenditureRepository.save(existingExpense);
            
            auditService.logEntity(currentUser.getUsername(), "expenditures", existingExpense.getId(), "UPDATE",
                String.valueOf(existingExpense.getAmount()), String.valueOf(existingExpense.getAmount()));
            
            ra.addFlashAttribute("success", "Expense updated successfully");
            
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to update expense: " + e.getMessage());
        }
        
        return "redirect:/staff/expenses";
    }

    @PostMapping("/expenses/{id}/delete")
    public String deleteExpense(@PathVariable Long id, RedirectAttributes ra) {
        try {
            User currentUser = securityService.getCurrentUser();
            Expenditure expense = expenditureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found"));
            
            // Check ownership
            if (!expense.getRecordedBy().getId().equals(currentUser.getId())) {
                ra.addFlashAttribute("error", "You can only delete your own expenses");
                return "redirect:/staff/expenses";
            }
            
            expenditureRepository.delete(expense);
            
            auditService.logEntity(currentUser.getUsername(), "expenditures", expense.getId(), "DELETE",
                String.valueOf(expense.getAmount()), "0");
            
            ra.addFlashAttribute("success", "Expense deleted successfully");
            
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete expense: " + e.getMessage());
        }
        
        return "redirect:/staff/expenses";
    }

    @GetMapping("/transactions")
    public String transactionHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {
        
        User currentUser = securityService.getCurrentUser();
        
        if (startDate == null) startDate = LocalDate.now().minusMonths(1);
        if (endDate == null) endDate = LocalDate.now();
        
        final LocalDate finalStartDate = startDate;
        final LocalDate finalEndDate = endDate;
        
        List<Expenditure> expenses = expenditureRepository.findByRecordedBy(currentUser)
            .stream()
            .filter(e -> {
                LocalDate expenseDate = e.getDate();
                return !expenseDate.isBefore(finalStartDate) && !expenseDate.isAfter(finalEndDate);
            })
            .sorted((e1, e2) -> e2.getDate().compareTo(e1.getDate()))
            .toList();
        
        model.addAttribute("expenses", expenses);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("user", currentUser);
        model.addAttribute("totalAmount", expenses.stream().mapToDouble(Expenditure::getAmount).sum());
        
        return "staff-transaction-history";
    }
}