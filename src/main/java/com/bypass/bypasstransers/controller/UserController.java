package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.enums.Role;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.model.Wallet;
import com.bypass.bypasstransers.model.Transaction;
import com.bypass.bypasstransers.model.Expenditure;
import com.bypass.bypasstransers.repository.WalletRepository;
import com.bypass.bypasstransers.repository.EmailVerificationTokenRepository;
import com.bypass.bypasstransers.repository.ExpenditureRepository;
import com.bypass.bypasstransers.repository.PasswordResetTokenRepository;
import com.bypass.bypasstransers.repository.TransactionRepository;
import com.bypass.bypasstransers.repository.UserRepository;
import com.bypass.bypasstransers.service.AuditService;
import com.bypass.bypasstransers.service.PasswordResetService;
import com.bypass.bypasstransers.service.UserProvisioningService;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final PasswordResetService passwordResetService;
    private final UserProvisioningService userProvisioningService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final WalletRepository walletRepository;
    private final ExpenditureRepository expenditureRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final TransactionRepository transactionRepository;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder, 
                         AuditService auditService, PasswordResetService passwordResetService,
                         UserProvisioningService userProvisioningService,
                         PasswordResetTokenRepository passwordResetTokenRepository,
                         WalletRepository walletRepository,
                         ExpenditureRepository expenditureRepository,
                         EmailVerificationTokenRepository emailVerificationTokenRepository,
                         TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.passwordResetService = passwordResetService;
        this.userProvisioningService = userProvisioningService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.walletRepository = walletRepository;
        this.expenditureRepository = expenditureRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        // Show all users with inactive ones marked
        model.addAttribute("users", userRepository.findAllByOrderByIsActiveDescCreatedAtDesc());
        return "users";
    }
    
    @GetMapping("/users/{id}/details")
    public String viewUserDetails(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            ra.addFlashAttribute("error", "User not found");
            return "redirect:/users";
        }
        
        User user = userOpt.get();
        model.addAttribute("user", user);
        
        // Get user's accounts/wallets
        model.addAttribute("accounts", walletRepository.findByOwnerId(id));
        
        // Get user's transactions - include all transactions related to user's wallets
        model.addAttribute("transactions", transactionRepository.findByWalletOwnerId(id));
        
        // Get user's expenditures
        model.addAttribute("expenditures", expenditureRepository.findByRecordedBy(user));
        
        return "user-details";
    }

    @GetMapping("/users/new")
    public String newUserForm(Model model, @RequestParam(value = "role", required = false) Role preRole) {
        User u = new User();
        if (preRole != null) {
            u.setRole(preRole);
        }
        model.addAttribute("user", u);
        model.addAttribute("roles", Role.values());
        return "user-form";
    }

    @GetMapping("/users/edit/{id}")
    public String editUserForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Optional<User> u = userRepository.findById(id);
        if (u.isEmpty()) {
            ra.addFlashAttribute("error", "User not found");
            return "redirect:/users";
        }
        model.addAttribute("user", u.get());
        model.addAttribute("roles", Role.values());
        return "user-form";
    }
    
    @GetMapping("/users/{id}/accounts")
    public String editUserAccounts(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            ra.addFlashAttribute("error", "User not found");
            return "redirect:/users";
        }
        
        User user = userOpt.get();
        model.addAttribute("user", user);
        model.addAttribute("accounts", walletRepository.findByOwnerId(id));
        return "user-accounts";
    }
    
    @PostMapping("/users/{id}/accounts/save")
    public String saveUserAccounts(@PathVariable Long id, 
                                  @RequestParam Map<String, String> accountData,
                                  RedirectAttributes ra) {
        try {
            Optional<User> userOpt = userRepository.findById(id);
            if (userOpt.isEmpty()) {
                ra.addFlashAttribute("error", "User not found");
                return "redirect:/users";
            }
            
            User user = userOpt.get();
            
            // Process account updates
            for (Map.Entry<String, String> entry : accountData.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                
                if (key.startsWith("account_") && key.endsWith("_balance")) {
                    // Extract account ID from key: account_123_balance
                    String accountIdStr = key.replace("account_", "").replace("_balance", "");
                    try {
                        Long accountId = Long.parseLong(accountIdStr);
                        Optional<Wallet> walletOpt = walletRepository.findById(accountId);
                        if (walletOpt.isPresent() && walletOpt.get().getOwner().getId().equals(id)) {
                            Wallet wallet = walletOpt.get();
                            double newBalance = Double.parseDouble(value);
                            wallet.setBalance(newBalance);
                            walletRepository.save(wallet);
                        }
                    } catch (NumberFormatException e) {
                        // Skip invalid account IDs
                        continue;
                    }
                }
            }
            
            auditService.logEntity("admin", "accounts", id, "UPDATE_USER_ACCOUNTS", null, user.getUsername());
            ra.addFlashAttribute("success", "User accounts updated successfully");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Failed to update user accounts: " + ex.getMessage());
        }
        return "redirect:/users/" + id + "/accounts";
    }
    
    @PostMapping("/users/{id}/accounts/create-default")
    public String createDefaultAccounts(@PathVariable Long id, RedirectAttributes ra) {
        try {
            Optional<User> userOpt = userRepository.findById(id);
            if (userOpt.isEmpty()) {
                ra.addFlashAttribute("error", "User not found");
                return "redirect:/users";
            }
            
            User user = userOpt.get();
            userProvisioningService.createDefaultWalletsForUser(user);
            
            auditService.logEntity("admin", "accounts", id, "CREATE_DEFAULT_ACCOUNTS", null, user.getUsername());
            ra.addFlashAttribute("success", "Default accounts (Mukuru, Econet, Innbucks) created successfully for user " + user.getUsername());
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Failed to create default accounts: " + ex.getMessage());
        }
        return "redirect:/users/" + id + "/accounts";
    }

    @PostMapping("/users/save")
    public String saveUser(@ModelAttribute User user, @RequestParam(required = false) String rawPassword, RedirectAttributes ra) {
        boolean isNew = (user.getId() == null);
        try {
            // Check for duplicate username
            if (isNew) {
                List<User> existingUsers = userRepository.findByUsername(user.getUsername());
                if (!existingUsers.isEmpty()) {
                    ra.addFlashAttribute("error", "Username '" + user.getUsername() + "' is already taken. Please choose a different username.");
                    String redirectUrl = "/users/new";
                    if (user.getRole() != null) {
                        redirectUrl += "?role=" + user.getRole();
                    }
                    return redirectUrl;
                }
            } else {
                // When editing, check if another user has the same username
                List<User> existingUsers = userRepository.findByUsername(user.getUsername());
                for (User existing : existingUsers) {
                    if (!existing.getId().equals(user.getId())) {
                        ra.addFlashAttribute("error", "Username '" + user.getUsername() + "' is already taken by another user.");
                        return "redirect:/users/edit/" + user.getId();
                    }
                }
            }
            
            if (rawPassword != null && !rawPassword.isBlank()) {
                user.setPassword(passwordEncoder.encode(rawPassword));
            } else {
                // preserve existing password when editing and no new password provided
                if (!isNew) {
                    User existing = userRepository.findById(user.getId()).orElse(null);
                    if (existing != null) {
                        user.setPassword(existing.getPassword());
                    }
                } else {
                    // Set a default password for new users if none provided
                    user.setPassword(passwordEncoder.encode("changeme123"));
                }
            }
            
            // Save the user first to get an ID
            User savedUser = userRepository.save(user);

            // If newly created, create default wallets (Mukuru, Econet, Innbucks)
            if (isNew) {
                try {
                    userProvisioningService.createDefaultWalletsForUser(savedUser);
                    // Log success for debugging
                    System.out.println("[USER CREATION] Default wallets created for user: " + savedUser.getUsername());
                } catch (Exception e) {
                    System.err.println("[USER CREATION] Failed to create default wallets for user: " + savedUser.getUsername() + ", Error: " + e.getMessage());
                    e.printStackTrace();
                }
                
                // Send password reset link if email exists
                if (user.getEmail() != null && !user.getEmail().isBlank()) {
                    try {
                        passwordResetService.createTokenForUser(savedUser);
                    } catch (Exception ex) {
                        // ignore send failures but log audit
                        System.err.println("[USER CREATION] Failed to create password reset token for user: " + savedUser.getUsername());
                    }
                }
            }

            // audit log
            auditService.logEntity("admin", "users", savedUser.getId(), isNew ? "CREATE_USER" : "UPDATE_USER", null, savedUser.getUsername());

            if (isNew) {
                // Check if default wallets were created successfully
                List<Wallet> userWallets = walletRepository.findByOwnerId(savedUser.getId());
                if (userWallets.isEmpty()) {
                    ra.addFlashAttribute("warning", "User created successfully, but default wallets could not be created. Please create accounts manually.");
                } else {
                    String walletNames = userWallets.stream()
                        .map(Wallet::getAccountType)
                        .collect(Collectors.joining(", "));
                    ra.addFlashAttribute("success", "User created successfully with default wallets: " + walletNames);
                }
            } else {
                ra.addFlashAttribute("success", "User updated successfully");
            }
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Failed to save user: " + ex.getMessage());
        }
        return "redirect:/users";
    }

    @PostMapping("/users/delete")
    @Transactional
    public String deleteUser(@RequestParam Long id, RedirectAttributes ra) {
        try {
            Optional<User> opt = userRepository.findById(id);
            if (opt.isEmpty()) {
                ra.addFlashAttribute("error", "User not found");
                return "redirect:/users";
            }
            User u = opt.get();

            // Prevent deleting the last admin-capable account
            long adminCount = userRepository.countByRoleAndIsActiveTrue(Role.ADMIN);
            long superCount = userRepository.countByRoleAndIsActiveTrue(Role.SUPER_ADMIN);
            if (u.getRole() == Role.SUPER_ADMIN) {
                if (superCount <= 1 && adminCount == 0) {
                    ra.addFlashAttribute("error", "Cannot delete the last administrative user. Add another admin first.");
                    return "redirect:/users";
                }
            } else if (u.getRole() == Role.ADMIN) {
                if (adminCount <= 1 && superCount == 0) {
                    ra.addFlashAttribute("error", "Cannot delete the last administrative user. Add another admin first.");
                    return "redirect:/users";
                }
            }

            // SOFT DELETE: Mark user as inactive instead of deleting
            // This preserves all financial records for audit purposes
            System.out.println("[DELETE USER] Soft deleting user ID: " + id);
            u.setIsActive(false);
            u.setDeletedAt(LocalDateTime.now());
            userRepository.save(u);
            System.out.println("[DELETE USER] User soft deleted successfully - all records preserved");
            
            auditService.logEntity("admin", "users", id, "SOFT_DELETE_USER", u.getUsername(), "User deactivated - records preserved");
            ra.addFlashAttribute("success", "User deactivated successfully. All financial records have been preserved for audit purposes.");
        } catch (Exception ex) {
            System.err.println("[DELETE USER] Error deleting user ID " + id + ": " + ex.getMessage());
            ex.printStackTrace();
            ra.addFlashAttribute("error", "Failed to delete user: " + ex.getMessage());
        }
        return "redirect:/users";
    }
    
    @PostMapping("/users/restore")
    @Transactional
    public String restoreUser(@RequestParam Long id, RedirectAttributes ra) {
        try {
            Optional<User> opt = userRepository.findById(id);
            if (opt.isEmpty()) {
                ra.addFlashAttribute("error", "User not found");
                return "redirect:/users";
            }
            User u = opt.get();
            
            if (u.isActive()) {
                ra.addFlashAttribute("info", "User is already active");
                return "redirect:/users";
            }
            
            // Restore user
            u.setIsActive(true);
            u.setDeletedAt(null);
            userRepository.save(u);
            
            auditService.logEntity("admin", "users", id, "RESTORE_USER", u.getUsername(), "User account restored");
            ra.addFlashAttribute("success", "User " + u.getUsername() + " has been restored successfully");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Failed to restore user: " + ex.getMessage());
        }
        return "redirect:/users";
    }

    @ModelAttribute("availableRoles")
    public List<Role> availableRoles() {
        return Arrays.asList(Role.values());
    }
    
    @GetMapping("/users/{id}/export/excel")
    public void exportUserExcel(@PathVariable Long id, HttpServletResponse response) throws Exception {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        User user = userOpt.get();
        
        // Get user data
        List<Wallet> accounts = walletRepository.findByOwnerId(id);
        List<Transaction> transactions = transactionRepository.findByCreatedBy(user.getUsername());
        List<Expenditure> expenditures = expenditureRepository.findByRecordedBy(user);
        
        // Create Excel workbook
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("User Data - " + user.getUsername());
        
        // Create styles
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        
        // Create header row for accounts
        Row headerRow = sheet.createRow(0);
        String[] accountHeaders = {"ID", "Name", "Type", "Balance", "Status", "Created"};
        for (int i = 0; i < accountHeaders.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(accountHeaders[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Add accounts data
        int rowNum = 1;
        for (Wallet account : accounts) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(account.getId());
            row.createCell(1).setCellValue(account.getAccountType());
            row.createCell(2).setCellValue(account.getAccountType());
            row.createCell(3).setCellValue(account.getBalance());
            row.createCell(4).setCellValue(account.isLocked() ? "Locked" : "Active");
            row.createCell(5).setCellValue(account.getCreatedAt() != null ? account.getCreatedAt().toString() : "");
        }
        
        // Add blank row before transactions
        rowNum++;
        Row transHeaderRow = sheet.createRow(rowNum++);
        String[] transHeaders = {"Transactions for " + user.getUsername()};
        transHeaderRow.createCell(0).setCellValue(transHeaders[0]);
        transHeaderRow.getCell(0).setCellStyle(headerStyle);
        
        // Add transaction headers
        Row transHeader = sheet.createRow(rowNum++);
        String[] transactionHeaders = {"ID", "Date", "Type", "From", "To", "Amount", "Fee", "Status"};
        for (int i = 0; i < transactionHeaders.length; i++) {
            Cell cell = transHeader.createCell(i);
            cell.setCellValue(transactionHeaders[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Add transaction data
        for (Transaction transaction : transactions) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(transaction.getId());
            row.createCell(1).setCellValue(transaction.getDate() != null ? transaction.getDate().toString() : "");
            row.createCell(2).setCellValue(transaction.getType() != null ? transaction.getType().toString() : "");
            row.createCell(3).setCellValue(transaction.getFromAccount() != null ? transaction.getFromAccount() : "");
            row.createCell(4).setCellValue(transaction.getToAccount() != null ? transaction.getToAccount() : "");
            row.createCell(5).setCellValue(transaction.getAmount());
            row.createCell(6).setCellValue(transaction.getFee());
            row.createCell(7).setCellValue(transaction.getSyncStatus());
        }
        
        // Add blank row before expenditures
        rowNum++;
        Row expHeaderRow = sheet.createRow(rowNum++);
        String[] expHeaders = {"Expenditures for " + user.getUsername()};
        expHeaderRow.createCell(0).setCellValue(expHeaders[0]);
        expHeaderRow.getCell(0).setCellStyle(headerStyle);
        
        // Add expenditure headers
        Row expHeader = sheet.createRow(rowNum++);
        String[] expenditureHeaders = {"ID", "Date", "Category", "Description", "Amount"};
        for (int i = 0; i < expenditureHeaders.length; i++) {
            Cell cell = expHeader.createCell(i);
            cell.setCellValue(expenditureHeaders[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Add expenditure data
        for (Expenditure expenditure : expenditures) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(expenditure.getId());
            row.createCell(1).setCellValue(expenditure.getDate() != null ? expenditure.getDate().toString() : "");
            row.createCell(2).setCellValue(expenditure.getCategory());
            row.createCell(3).setCellValue(expenditure.getDescription());
            row.createCell(4).setCellValue(expenditure.getAmount());
        }
        
        // Auto-size columns
        for (int i = 0; i < 8; i++) {
            sheet.autoSizeColumn(i);
        }
        
        // Set response headers
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=user_" + user.getUsername() + "_data.xlsx");
        
        // Write to response
        workbook.write(response.getOutputStream());
        workbook.close();
    }
    
    @GetMapping("/users/{id}/export/pdf")
    public void exportUserPdf(@PathVariable Long id, HttpServletResponse response) throws Exception {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        User user = userOpt.get();
        
        // Get user data
        List<Wallet> accounts = walletRepository.findByOwnerId(id);
        List<Transaction> transactions = transactionRepository.findByCreatedBy(user.getUsername());
        List<Expenditure> expenditures = expenditureRepository.findByRecordedBy(user);
        
        // Set response headers
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=user_" + user.getUsername() + "_data.pdf");
        
        // Create PDF
        com.itextpdf.kernel.pdf.PdfDocument pdfDoc = new com.itextpdf.kernel.pdf.PdfDocument(new com.itextpdf.kernel.pdf.PdfWriter(response.getOutputStream()));
        com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdfDoc);
        
        // Add title
        document.add(new com.itextpdf.layout.element.Paragraph("User Data Export - " + user.getUsername())
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setFontSize(20).setMarginBottom(20));
        
        // Add user info
        document.add(new com.itextpdf.layout.element.Paragraph("User Information:")
                .setFontSize(16).setBold().setMarginTop(10));
        document.add(new com.itextpdf.layout.element.Paragraph("Username: " + user.getUsername()));
        document.add(new com.itextpdf.layout.element.Paragraph("Email: " + (user.getEmail() != null ? user.getEmail() : "Not set")));
        document.add(new com.itextpdf.layout.element.Paragraph("Phone: " + (user.getPhoneNumber() != null ? user.getPhoneNumber() : "Not set")));
        document.add(new com.itextpdf.layout.element.Paragraph("Role: " + user.getRole()));
        document.add(new com.itextpdf.layout.element.Paragraph("Created: " + (user.getCreatedAt() != null ? user.getCreatedAt().toString() : "Unknown")));
        document.add(new com.itextpdf.layout.element.Paragraph("Status: " + (user.isActive() ? "Active" : "Inactive")));
        
        // Add accounts section
        document.add(new com.itextpdf.layout.element.Paragraph("\nAccounts/Wallets:")
                .setFontSize(16).setBold().setMarginTop(20));
        
        if (!accounts.isEmpty()) {
            for (Wallet account : accounts) {
                document.add(new com.itextpdf.layout.element.Paragraph(
                    "• " + account.getAccountType() + " (" + account.getAccountType() + ") - Balance: " + 
                    String.format("%.2f", account.getBalance()) + " - " + (account.isLocked() ? "Locked" : "Active")));
            }
        } else {
            document.add(new com.itextpdf.layout.element.Paragraph("No accounts found"));
        }
        
        // Add transactions section
        document.add(new com.itextpdf.layout.element.Paragraph("\nTransactions:")
                .setFontSize(16).setBold().setMarginTop(20));
        
        if (!transactions.isEmpty()) {
            for (Transaction transaction : transactions) {
                document.add(new com.itextpdf.layout.element.Paragraph(
                    "• " + transaction.getType() + " - Amount: " + 
                    String.format("%.2f", transaction.getAmount()) + 
                    " - Fee: " + String.format("%.2f", transaction.getFee()) + 
                    " - Date: " + (transaction.getDate() != null ? transaction.getDate().toString() : "Unknown")));
            }
        } else {
            document.add(new com.itextpdf.layout.element.Paragraph("No transactions found"));
        }
        
        // Add expenditures section
        document.add(new com.itextpdf.layout.element.Paragraph("\nExpenditures:")
                .setFontSize(16).setBold().setMarginTop(20));
        
        if (!expenditures.isEmpty()) {
            for (Expenditure expenditure : expenditures) {
                document.add(new com.itextpdf.layout.element.Paragraph(
                    "• " + expenditure.getCategory() + " - " + expenditure.getDescription() + 
                    " - Amount: " + String.format("%.2f", expenditure.getAmount()) + 
                    " - Date: " + (expenditure.getDate() != null ? expenditure.getDate().toString() : "Unknown")));
            }
        } else {
            document.add(new com.itextpdf.layout.element.Paragraph("No expenditures found"));
        }
        
        document.close();
    }
}