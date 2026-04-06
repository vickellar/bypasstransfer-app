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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.bypass.bypasstransers.model.Transaction;
import com.bypass.bypasstransers.repository.TransactionRepository;

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

    // ============================================================
    // TRANSACTION HISTORY
    // ============================================================

    /** All transactions for this staff member across ALL their wallets
     * @param model
     * @return  */
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

        double totalVolume = transactions.stream().mapToDouble(Transaction::getAmount).sum();
        double totalFees   = transactions.stream().mapToDouble(Transaction::getFee).sum();

        List<OfflineTransaction> offlineTxs = offlineTransactionRepository.findByUsernameOrderByOfflineRecordedAtDesc(currentUser.getUsername());

        model.addAttribute("user", currentUser);
        model.addAttribute("transactions", transactions);
        model.addAttribute("offlineTransactions", offlineTxs);
        model.addAttribute("wallets", wallets);
        model.addAttribute("totalVolume", totalVolume);
        model.addAttribute("totalFees", totalFees);
        model.addAttribute("selectedWallet", null);
        return "staff-transaction-history";
    }

    /** Transactions for a SPECIFIC wallet (per-account history)
     * @param walletId
     * @param model
     * @param ra
     * @return  */
    @GetMapping("/transactions/wallet/{walletId}")
    public String walletTransactionHistory(@PathVariable Long walletId, Model model,
                                           org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        User currentUser = securityService.getCurrentUser();

        // Security: make sure the wallet belongs to the current user OR their branch
        Wallet wallet = walletService.getWalletById(walletId).orElse(null);
        if (wallet == null) {
            ra.addFlashAttribute("error", "Wallet not found or access denied");
            return "redirect:/staff/transactions";
        }

        List<Transaction> transactions = transactionRepository.findByWalletId(walletId);
        List<Wallet> wallets = walletService.getCurrentUserWallets();

        double totalVolume = transactions.stream().mapToDouble(Transaction::getAmount).sum();
        double totalFees   = transactions.stream().mapToDouble(Transaction::getFee).sum();

        List<OfflineTransaction> offlineTxs = offlineTransactionRepository.findByUsernameOrderByOfflineRecordedAtDesc(currentUser.getUsername())
                .stream()
                .filter(ot -> (ot.getFromAccount() != null && ot.getFromAccount().equals(wallet.getAccountType())) || 
                              (ot.getToAccount() != null && ot.getToAccount().equals(wallet.getAccountType())))
                .toList();

        model.addAttribute("user", currentUser);
        model.addAttribute("transactions", transactions);
        model.addAttribute("offlineTransactions", offlineTxs);
        model.addAttribute("wallets", wallets);
        model.addAttribute("totalVolume", totalVolume);
        model.addAttribute("totalFees", totalFees);
        model.addAttribute("selectedWallet", wallet);
        return "staff-transaction-history";
    }

    // ============================================================
    // RECONCILIATION
    // ============================================================

    @Autowired
    private com.bypass.bypasstransers.service.ReconsiliationService reconService;

    /** Staff reconciliation page — compare expected vs actual balances
     * @param model
     * @return  */
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

        double expectedBalance = wallets.stream().mapToDouble(Wallet::getBalance).sum();
        // Total cash in = sum of netAmount on RECEIVE-type transactions
        double totalIn  = transactions.stream()
                .filter(t -> t.getType() != null && t.getType().name().equals("RECEIVE"))
                .mapToDouble(Transaction::getNetAmount).sum();
        // Total cash out = sum of amount on SEND-type transactions
        double totalOut = transactions.stream()
                .filter(t -> t.getType() != null && t.getType().name().equals("SEND"))
                .mapToDouble(Transaction::getAmount).sum();

        // Check which wallets are already reconciled this week
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

        // Get this staff's reconciliation history
        List<com.bypass.bypasstransers.model.DailyReconciliation> myHistory = 
            reconService.getHistoryByStaff(currentUser.getUsername());

        model.addAttribute("user", currentUser);
        model.addAttribute("wallets", wallets);
        model.addAttribute("transactions", transactions);
        model.addAttribute("expectedBalance", expectedBalance);
        model.addAttribute("totalIn", totalIn);
        model.addAttribute("totalOut", totalOut);
        model.addAttribute("walletReconciledThisWeek", walletReconciledThisWeek);
        model.addAttribute("walletReconResults", walletReconResults);
        model.addAttribute("myHistory", myHistory);
        return "staff-reconciliation";
    }



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
        model.addAttribute("isEdit", false);
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
            
            // Convert amount to wallet currency if needed
            Double amountInWalletCurrency = amount;
            String conversionNote = "";
            
            // Check if wallet has a currency. If not, default to USD (should be fixed by migration/entity default)
            Currency walletCurrency = wallet.getCurrency() != null ? wallet.getCurrency() : Currency.USD;
            
            if (currency != walletCurrency) {
                amountInWalletCurrency = currencyConversionService.convert(amount, currency.name(), walletCurrency.name());
                conversionNote = String.format(" (Converted from %.2f %s to %.2f %s)", 
                    amount, currency, amountInWalletCurrency, walletCurrency);
            }

            // Check if wallet has sufficient balance
            if (wallet.getBalance() < amountInWalletCurrency) {
                ra.addFlashAttribute("error", "Insufficient wallet balance. Required: " + 
                    String.format("%.2f", amountInWalletCurrency) + " " + wallet.getCurrency() + 
                    ". Available: " + String.format("%.2f", wallet.getBalance()) + " " + wallet.getCurrency());
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
            wallet.setBalance(wallet.getBalance() - amountInWalletCurrency);
            walletRepository.save(wallet);
            
            // Audit log
            auditService.logEntity(currentUser.getUsername(), "expenditures", expense.getId(), "CREATE",
                "0", String.valueOf(expense.getAmount()));
            
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
        expenseDTO.setWalletId(expense.getWallet() != null ? expense.getWallet().getId() : null);
        
        model.addAttribute("expense", expenseDTO);
        model.addAttribute("user", currentUser);
        model.addAttribute("currencies", com.bypass.bypasstransers.enums.Currency.values());
		model.addAttribute("wallets", walletService.getCurrentUserWallets());
		 // Get user's wallets
        
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

}