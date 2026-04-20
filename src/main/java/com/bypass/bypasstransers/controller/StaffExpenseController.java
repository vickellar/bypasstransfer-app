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
import com.bypass.bypasstransers.service.CurrencyConversionService;
import com.bypass.bypasstransers.enums.Currency;
import com.bypass.bypasstransers.model.OfflineTransaction;
import com.bypass.bypasstransers.repository.OfflineTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import com.bypass.bypasstransers.model.Transaction;
import com.bypass.bypasstransers.repository.TransactionRepository;

/**
 * Controller for staff members to manage their expenses and view their transaction history.
 */
@Controller
@RequestMapping("/staff")
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
    private TransactionRepository transactionRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private CurrencyConversionService currencyConversionService;

    @Autowired
    private OfflineTransactionRepository offlineTransactionRepository;

    @Autowired
    private com.bypass.bypasstransers.service.ReconsiliationService reconService;

    /** All transactions for this staff member across ALL their wallets */
    @GetMapping("/transactions")
    public String myTransactionHistory(Model model) {
        User currentUser = securityService.getCurrentUser();
        Long branchId = (currentUser.getBranch() != null) ? currentUser.getBranch().getId() : null;
        
        List<Transaction> transactions;
        List<Wallet> wallets;
        
        if (branchId != null) {
            transactions = transactionRepository.findByWalletOwnerIdOrWalletBranchId(currentUser.getId(), branchId);
            wallets = walletRepository.findByOwnerIdOrBranchId(currentUser.getId(), branchId);
        } else {
            transactions = transactionRepository.findByWalletOwnerId(currentUser.getId());
            wallets = walletRepository.findByOwnerId(currentUser.getId());
        }

        BigDecimal totalVolume = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFees   = transactions.stream()
                .map(Transaction::getFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<OfflineTransaction> offlineTxs = offlineTransactionRepository.findByUsernameOrderByOfflineRecordedAtDesc(currentUser.getUsername());

        model.addAttribute("user", currentUser);
        model.addAttribute("transactions", transactions);
        model.addAttribute("offlineTransactions", offlineTxs);
        model.addAttribute("wallets", wallets);
        model.addAttribute("totalVolume", totalVolume.doubleValue());
        model.addAttribute("totalFees", totalFees.doubleValue());
        model.addAttribute("selectedWallet", null);
        return "staff-transaction-history";
    }

    /** Transactions for a SPECIFIC wallet (per-account history) */
    @GetMapping("/transactions/wallet/{walletId}")
    public String walletTransactionHistory(@PathVariable Long walletId, Model model,
                                           RedirectAttributes ra) {
        User currentUser = securityService.getCurrentUser();

        Objects.requireNonNull(walletId, "Wallet ID must not be null");
        Wallet wallet = walletService.getWalletById(walletId).orElse(null);
        if (wallet == null) {
            ra.addFlashAttribute("error", "Wallet not found or access denied");
            return "redirect:/staff/transactions";
        }

        List<Transaction> transactions = transactionRepository.findByWalletId(walletId);
        List<Wallet> wallets = walletService.getCurrentUserWallets();

        BigDecimal totalVolume = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFees   = transactions.stream()
                .map(Transaction::getFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<OfflineTransaction> offlineTxs = offlineTransactionRepository.findByUsernameOrderByOfflineRecordedAtDesc(currentUser.getUsername())
                .stream()
                .filter(ot -> (ot.getFromAccount() != null && ot.getFromAccount().equals(wallet.getAccountType())) || 
                              (ot.getToAccount() != null && ot.getToAccount().equals(wallet.getAccountType())))
                .toList();

        model.addAttribute("user", currentUser);
        model.addAttribute("transactions", transactions);
        model.addAttribute("offlineTransactions", offlineTxs);
        model.addAttribute("wallets", wallets);
        model.addAttribute("totalVolume", totalVolume.doubleValue());
        model.addAttribute("totalFees", totalFees.doubleValue());
        model.addAttribute("selectedWallet", wallet);
        return "staff-transaction-history";
    }

    /** Staff reconciliation page — compare expected vs actual balances */
    @GetMapping("/reconciliation")
    public String reconciliationPage(Model model) {
        User currentUser = securityService.getCurrentUser();
        List<Wallet> wallets = walletService.getCurrentUserWallets();
        
        Long branchId = (currentUser.getBranch() != null) ? currentUser.getBranch().getId() : null;
        List<Transaction> transactions;
        if (branchId != null) {
            transactions = transactionRepository.findByWalletOwnerIdOrWalletBranchId(currentUser.getId(), branchId);
        } else {
            transactions = transactionRepository.findByWalletOwnerId(currentUser.getId());
        }

        BigDecimal expectedBalance = wallets.stream()
                .map(Wallet::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalIn = transactions.stream()
                .filter(t -> t.getType() != null && t.getType().name().equals("RECEIVE"))
                .map(Transaction::getNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalOut = transactions.stream()
                .filter(t -> t.getType() != null && t.getType().name().equals("SEND"))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        java.util.Map<Long, Boolean> walletReconciledThisWeek = new java.util.HashMap<>();
        java.util.Map<Long, com.bypass.bypasstransers.model.DailyReconciliation> walletReconResults = new java.util.HashMap<>();
        for (Wallet w : wallets) {
            boolean reconciled = reconService.isAlreadyReconciledThisWeek(w.getId());
            walletReconciledThisWeek.put(w.getId(), reconciled);
            if (reconciled) {
                reconService.getThisWeeksReconciliation(w.getId()).ifPresent(r -> 
                    walletReconResults.put(w.getId(), r)
                );
            }
        }

        List<com.bypass.bypasstransers.model.DailyReconciliation> myHistory = 
            reconService.getHistoryByStaff(currentUser.getUsername());

        model.addAttribute("user", currentUser);
        model.addAttribute("wallets", wallets);
        model.addAttribute("transactions", transactions);
        model.addAttribute("expectedBalance", expectedBalance.doubleValue());
        model.addAttribute("totalIn", totalIn.doubleValue());
        model.addAttribute("totalOut", totalOut.doubleValue());
        model.addAttribute("walletReconciledThisWeek", walletReconciledThisWeek);
        model.addAttribute("walletReconResults", walletReconResults);
        model.addAttribute("myHistory", myHistory);
        return "staff-reconciliation";
    }

    @GetMapping("/expenses")
    public String listExpenses(Model model) {
        User currentUser = securityService.getCurrentUser();
        List<Expenditure> expenses = expenditureRepository.findByRecordedBy(currentUser);
        
        BigDecimal totalAmount = expenses.stream()
                .map(Expenditure::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("expenses", expenses);
        model.addAttribute("user", currentUser);
        model.addAttribute("totalAmount", totalAmount.doubleValue());
        
        return "staff-expenses";
    }

    @GetMapping("/expenses/new")
    public String newExpenseForm(Model model) {
        model.addAttribute("isEdit", false);
        User currentUser = securityService.getCurrentUser();
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
            @RequestParam("amount") BigDecimal amount,
            @RequestParam("category") String category,
            @RequestParam("currency") Currency currency,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "description", required = false) String description,
            RedirectAttributes ra) {
        try {
            User currentUser = securityService.getCurrentUser();
            
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                ra.addFlashAttribute("error", "Amount must be greater than zero");
                return "redirect:/staff/expenses/new";
            }
            
            if (walletId == null) {
                ra.addFlashAttribute("error", "Please select a wallet");
                return "redirect:/staff/expenses/new";
            }
            
            Wallet wallet = walletService.getWalletById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
            
            if (!wallet.getOwner().getId().equals(currentUser.getId()) && !securityService.isSupervisorOrAbove()) {
                ra.addFlashAttribute("error", "You can only use your own wallets");
                return "redirect:/staff/expenses/new";
            }
            
            BigDecimal amountInWalletCurrency = amount;
            String conversionNote = "";
            Currency walletCurrency = wallet.getCurrency() != null ? wallet.getCurrency() : Currency.USD;
            
            if (currency != walletCurrency) {
                amountInWalletCurrency = currencyConversionService.convert(amount, currency.name(), walletCurrency.name());
                conversionNote = String.format(" (Converted from %.2f %s to %.2f %s)", 
                    amount.doubleValue(), currency, amountInWalletCurrency.doubleValue(), walletCurrency);
            }

            if (wallet.getBalance().compareTo(amountInWalletCurrency) < 0) {
                ra.addFlashAttribute("error", "Insufficient wallet balance. Required: " + 
                    amountInWalletCurrency.setScale(2, RoundingMode.HALF_UP) + " " + walletCurrency + 
                    ". Available: " + wallet.getBalance().setScale(2, RoundingMode.HALF_UP) + " " + walletCurrency);
                return "redirect:/staff/expenses/new";
            }
            
            Expenditure expense = new Expenditure();
            expense.setDescription(description);
            expense.setCategory(category);
            expense.setAmount(amount);
            expense.setDate(date != null ? date : LocalDate.now());
            expense.setRecordedBy(currentUser);
            expense.setCurrency(currency);
            expense.setWallet(wallet);
            
            expenditureRepository.save(expense);
            
            wallet.setBalance(wallet.getBalance().subtract(amountInWalletCurrency));
            walletRepository.save(wallet);
            
            auditService.logEntity(currentUser.getUsername(), "expenditures", expense.getId(), "CREATE",
                "0", expense.getAmount().toString());
            
            ra.addFlashAttribute("success", "Expense recorded successfully." + conversionNote + " Amount deducted from " + wallet.getAccountType() + " wallet.");
            
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to save expense: " + e.getMessage());
        }
        
        return "redirect:/staff/expenses";
    }

    @GetMapping("/expenses/{id}/edit")
    public String editExpenseForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        model.addAttribute("isEdit", true);
        User currentUser = securityService.getCurrentUser();
        
        Objects.requireNonNull(id, "Expense ID must not be null");
        Expenditure expense = expenditureRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Expense not found"));
        
        if (expense.getRecordedBy() == null || !expense.getRecordedBy().getId().equals(currentUser.getId())) {
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
        expenseDTO.setWalletId(expense.getWallet() != null ? expense.getWallet().getId() : null);
        
        model.addAttribute("expense", expenseDTO);
        model.addAttribute("user", currentUser);
        model.addAttribute("currencies", Currency.values());
        model.addAttribute("wallets", walletService.getCurrentUserWallets());
        
        return "staff-expense-form";
    }

    @PostMapping("/expenses/{id}/update")
    public String updateExpense(@PathVariable Long id, @ModelAttribute ExpenditureDTO expenseDTO, RedirectAttributes ra) {
        try {
            User currentUser = securityService.getCurrentUser();
            
            Objects.requireNonNull(id, "Expense ID must not be null");
            Expenditure existingExpense = expenditureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found"));
            
            if (existingExpense.getRecordedBy() == null || !existingExpense.getRecordedBy().getId().equals(currentUser.getId())) {
                ra.addFlashAttribute("error", "You can only edit your own expenses");
                return "redirect:/staff/expenses";
            }
            
            if (expenseDTO.getAmount() == null || expenseDTO.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                ra.addFlashAttribute("error", "Amount must be greater than zero");
                return "redirect:/staff/expenses/" + id + "/edit";
            }
            
            existingExpense.setAmount(expenseDTO.getAmount());
            existingExpense.setCategory(expenseDTO.getCategory());
            existingExpense.setDescription(expenseDTO.getDescription());
            existingExpense.setDate(expenseDTO.getDate());
            existingExpense.setNotes(expenseDTO.getNotes());
            existingExpense.setCurrency(expenseDTO.getCurrency() != null ? expenseDTO.getCurrency() : Currency.USD);
            
            expenditureRepository.save(existingExpense);
            
            auditService.logEntity(currentUser.getUsername(), "expenditures", existingExpense.getId(), "UPDATE",
                existingExpense.getAmount().toString(), existingExpense.getAmount().toString());
            
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
            
            Objects.requireNonNull(id, "Expense ID must not be null");
            Expenditure expense = expenditureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found"));
            
            if (expense.getRecordedBy() == null || !expense.getRecordedBy().getId().equals(currentUser.getId())) {
                ra.addFlashAttribute("error", "You can only delete your own expenses");
                return "redirect:/staff/expenses";
            }
            
            expenditureRepository.delete(expense);
            
            auditService.logEntity(currentUser.getUsername(), "expenditures", expense.getId(), "DELETE",
                expense.getAmount().toString(), "0");
            
            ra.addFlashAttribute("success", "Expense deleted successfully");
            
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete expense: " + e.getMessage());
        }
        
        return "redirect:/staff/expenses";
    }
}