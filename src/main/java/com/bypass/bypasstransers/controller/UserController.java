package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.enums.Role;
import com.bypass.bypasstransers.enums.Currency;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.model.Wallet;
import com.bypass.bypasstransers.repository.WalletRepository;
import com.bypass.bypasstransers.repository.ExpenditureRepository;
import com.bypass.bypasstransers.repository.TransactionRepository;
import com.bypass.bypasstransers.repository.UserRepository;
import com.bypass.bypasstransers.service.UserProvisioningService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import com.bypass.bypasstransers.repository.BranchRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
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

/**
 * Controller for managing users, their details, and wallets.
 */
@Controller
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserProvisioningService userProvisioningService;
    private final WalletRepository walletRepository;
    private final ExpenditureRepository expenditureRepository;
    private final TransactionRepository transactionRepository;
    private final BranchRepository branchRepository;

    public UserController(UserRepository userRepository, 
                         PasswordEncoder passwordEncoder, 
                         UserProvisioningService userProvisioningService,
                         WalletRepository walletRepository,
                         ExpenditureRepository expenditureRepository,
                         TransactionRepository transactionRepository,
                         BranchRepository branchRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userProvisioningService = userProvisioningService;
        this.walletRepository = walletRepository;
        this.expenditureRepository = expenditureRepository;
        this.transactionRepository = transactionRepository;
        this.branchRepository = branchRepository;
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", userRepository.findAllByOrderByIsActiveDescCreatedAtDesc());
        model.addAttribute("branches", branchRepository.findAll());
        return "users";
    }
    
    @GetMapping("/users/{id}/details")
    public String viewUserDetails(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Objects.requireNonNull(id, "User ID must not be null");
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            ra.addFlashAttribute("error", "User not found");
            return "redirect:/users";
        }
        User user = userOpt.get();
        model.addAttribute("user", user);
        model.addAttribute("accounts", walletRepository.findByOwnerId(id));
        model.addAttribute("transactions", transactionRepository.findByWalletOwnerId(id));
        model.addAttribute("expenditures", expenditureRepository.findByRecordedBy(user));
        return "user-details";
    }

    @GetMapping("/users/new")
    public String newUserForm(Model model, @RequestParam(value = "role", required = false) Role preRole) {
        User u = new User();
        if (preRole != null) u.setRole(preRole);
        model.addAttribute("user", u);
        model.addAttribute("roles", Role.values());
        return "user-form";
    }

    @GetMapping("/users/edit/{id}")
    public String editUserForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Objects.requireNonNull(id, "User ID must not be null");
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
        Objects.requireNonNull(id, "User ID must not be null");
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            ra.addFlashAttribute("error", "User not found");
            return "redirect:/users";
        }
        model.addAttribute("user", userOpt.get());
        model.addAttribute("accounts", walletRepository.findByOwnerId(id));
        return "user-accounts";
    }
    
    @PostMapping("/users/{id}/accounts/save")
    public String saveUserAccounts(@PathVariable Long id, @RequestParam Map<String, String> accountData, RedirectAttributes ra) {
        try {
            Objects.requireNonNull(id, "User ID must not be null");
            Optional<User> userOpt = userRepository.findById(id);
            if (userOpt.isEmpty()) return "redirect:/users";
            
            for (Map.Entry<String, String> entry : accountData.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key.startsWith("account_") && key.endsWith("_balance")) {
                    String accountIdStr = key.replace("account_", "").replace("_balance", "");
                    try {
                        Long accountId = Long.parseLong(accountIdStr);
                        walletRepository.findById(accountId).ifPresent(w -> {
                            if (w.getOwner().getId().equals(id)) {
                                w.setBalance(new BigDecimal(value));
                                walletRepository.save(w);
                            }
                        });
                    } catch (Exception e) { continue; }
                }
                if (key.startsWith("account_") && key.endsWith("_currency")) {
                    String accountIdStr = key.replace("account_", "").replace("_currency", "");
                    try {
                        Long accountId = Long.parseLong(accountIdStr);
                        walletRepository.findById(accountId).ifPresent(w -> {
                            if (w.getOwner().getId().equals(id)) {
                                w.setCurrency(Currency.valueOf(value));
                                walletRepository.save(w);
                            }
                        });
                    } catch (Exception e) { continue; }
                }
            }
            ra.addFlashAttribute("success", "User accounts updated");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Failed: " + ex.getMessage());
        }
        return "redirect:/users/" + id + "/accounts";
    }
    
    @PostMapping("/users/{id}/accounts/create-default")
    public String createDefaultAccounts(@PathVariable Long id, RedirectAttributes ra) {
        try {
            Objects.requireNonNull(id, "User ID must not be null");
            userRepository.findById(id).ifPresent(userProvisioningService::createDefaultWalletsForUser);
            ra.addFlashAttribute("success", "Default accounts created");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Failed: " + ex.getMessage());
        }
        return "redirect:/users/" + id + "/accounts";
    }

    @PostMapping("/users/{id}/accounts/add")
    public String addCustomAccount(@PathVariable Long id, 
                                   @RequestParam String newAccountType, 
                                   @RequestParam String newAccountCurrency, 
                                   @RequestParam(required = false) BigDecimal newAccountBalance, 
                                   RedirectAttributes ra) {
        try {
            Objects.requireNonNull(id, "User ID must not be null");
            Optional<User> userOpt = userRepository.findById(id);
            if (userOpt.isEmpty()) return "redirect:/users";
            
            Wallet wallet = new Wallet();
            wallet.setOwner(userOpt.get());
            wallet.setAccountType(newAccountType);
            wallet.setCurrency(Currency.valueOf(newAccountCurrency));
            wallet.setBalance(newAccountBalance != null ? newAccountBalance : BigDecimal.ZERO);
            wallet.setLocked(false);
            walletRepository.save(wallet);
            ra.addFlashAttribute("success", "Custom account added");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Failed: " + ex.getMessage());
        }
        return "redirect:/users/" + id + "/accounts";
    }

    @PostMapping("/users/{userId}/accounts/{accountId}/toggle-status")
    public String toggleAccountStatus(@PathVariable Long userId, @PathVariable Long accountId, RedirectAttributes ra) {
        Objects.requireNonNull(userId, "User ID must not be null");
        Objects.requireNonNull(accountId, "Account ID must not be null");
        walletRepository.findById(accountId).ifPresent(w -> {
            w.setLocked(!w.isLocked());
            walletRepository.save(w);
            ra.addFlashAttribute("success", "Account status updated");
        });
        return "redirect:/users/" + userId + "/accounts";
    }

    @PostMapping("/users/{userId}/accounts/{accountId}/delete")
    public String deleteAccount(@PathVariable Long userId, @PathVariable Long accountId, RedirectAttributes ra) {
        try {
            Objects.requireNonNull(userId, "User ID must not be null");
            Objects.requireNonNull(accountId, "Account ID must not be null");
            walletRepository.deleteById(accountId);
            ra.addFlashAttribute("success", "Account deleted");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Deletion failed (associated records exist)");
        }
        return "redirect:/users/" + userId + "/accounts";
    }

    @PostMapping("/users/save")
    public String saveUser(@ModelAttribute User user, @RequestParam(required = false) String rawPassword, @RequestParam(required = false) Long branchId, RedirectAttributes ra) {
        boolean isNew = (user.getId() == null);
        try {
            if (isNew) {
                if (!userRepository.findByUsername(user.getUsername()).isEmpty()) {
                    ra.addFlashAttribute("error", "Username already taken");
                    return "redirect:/users/new";
                }
            }
            if (rawPassword != null && !rawPassword.isBlank()) {
                user.setPassword(passwordEncoder.encode(rawPassword));
            } else if (!isNew) {
                Long id = Objects.requireNonNull(user.getId(), "User ID must not be null for update");
                userRepository.findById(id).ifPresent(existing -> user.setPassword(existing.getPassword()));
            } else {
                user.setPassword(passwordEncoder.encode("changeme123"));
            }
            if (branchId != null) branchRepository.findById(branchId).ifPresent(user::setBranch);
            User saved = userRepository.save(user);
            if (isNew) userProvisioningService.createDefaultWalletsForUser(saved);
            ra.addFlashAttribute("success", "User saved successfully");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Failed: " + ex.getMessage());
        }
        return "redirect:/users";
    }

    @PostMapping("/users/assign-branch")
    public String assignBranch(@RequestParam Long id, @RequestParam(required = false) Long branchId, RedirectAttributes ra) {
        Objects.requireNonNull(id, "User ID must not be null");
        userRepository.findById(id).ifPresent(u -> {
            if (branchId != null) branchRepository.findById(branchId).ifPresent(u::setBranch);
            else u.setBranch(null);
            userRepository.save(u);
        });
        return "redirect:/users";
    }

    @PostMapping("/users/delete")
    @Transactional
    public String deleteUser(@RequestParam Long id, RedirectAttributes ra) {
        Objects.requireNonNull(id, "User ID must not be null");
        userRepository.findById(id).ifPresent(u -> {
            u.setIsActive(false);
            u.setDeletedAt(LocalDateTime.now());
            userRepository.save(u);
            ra.addFlashAttribute("success", "User deactivated");
        });
        return "redirect:/users";
    }
    
    @PostMapping("/users/restore")
    @Transactional
    public String restoreUser(@RequestParam Long id, RedirectAttributes ra) {
        Objects.requireNonNull(id, "User ID must not be null");
        userRepository.findById(id).ifPresent(u -> {
            u.setIsActive(true);
            u.setDeletedAt(null);
            userRepository.save(u);
            ra.addFlashAttribute("success", "User restored");
        });
        return "redirect:/users";
    }

    @ModelAttribute("availableRoles")
    public List<Role> availableRoles() {
        return Arrays.asList(Role.values());
    }
    
    @GetMapping("/users/{id}/export/excel")
    public void exportUserExcel(@PathVariable Long id, HttpServletResponse response) throws Exception {
        Objects.requireNonNull(id, "User ID must not be null");
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) return;
        User user = userOpt.get();
        List<Wallet> accounts = walletRepository.findByOwnerId(id);
        
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("User Data");
        Row headerRow = sheet.createRow(0);
        String[] accountHeaders = {"ID", "Type", "Balance", "Currency", "Status"};
        for (int i = 0; i < accountHeaders.length; i++) headerRow.createCell(i).setCellValue(accountHeaders[i]);
        
        int rowNum = 1;
        for (Wallet account : accounts) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(account.getId() != null ? account.getId() : 0L);
            row.createCell(1).setCellValue(account.getAccountType() != null ? account.getAccountType() : "");
            row.createCell(2).setCellValue(account.getBalance() != null ? account.getBalance().doubleValue() : 0.0);
            row.createCell(3).setCellValue(account.getCurrency() != null ? account.getCurrency().toString() : "");
            row.createCell(4).setCellValue(account.isLocked() ? "Locked" : "Active");
        }
        
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=user_" + user.getUsername() + ".xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
    }
    
    @GetMapping("/users/{id}/export/pdf")
    public void exportUserPdf(@PathVariable Long id, HttpServletResponse response) throws Exception {
        Objects.requireNonNull(id, "User ID must not be null");
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) return;
        User user = userOpt.get();
        List<Wallet> accounts = walletRepository.findByOwnerId(id);
        
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=user_" + user.getUsername() + ".pdf");
        
        com.itextpdf.kernel.pdf.PdfDocument pdfDoc = new com.itextpdf.kernel.pdf.PdfDocument(new com.itextpdf.kernel.pdf.PdfWriter(response.getOutputStream()));
        com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdfDoc);
        document.add(new com.itextpdf.layout.element.Paragraph("User Data Export - " + user.getUsername()));
        
        for (Wallet account : accounts) {
            document.add(new com.itextpdf.layout.element.Paragraph("Account: " + account.getAccountType() + " | Balance: " + (account.getBalance() != null ? account.getBalance() : "0") + " " + account.getCurrency()));
        }
        document.close();
    }

    @GetMapping("/users/api/all")
    @ResponseBody
    public ResponseEntity<?> getAllUsersApi() {
        return ResponseEntity.ok(userRepository.findAll().stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("username", u.getUsername());
            map.put("branch", u.getBranch() != null ? u.getBranch().getName() : "None");
            map.put("role", u.getRole());
            return map;
        }).collect(Collectors.toList()));
    }

    @PutMapping("/admin/users/{id}")
    @ResponseBody
    public ResponseEntity<?> updateUserRest(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        Objects.requireNonNull(id, "User ID must not be null");
        return userRepository.findById(id).map(user -> {
            if (updates.containsKey("role")) {
                Object roleVal = updates.get("role");
                if (roleVal != null) {
                    user.setRole(Role.valueOf(roleVal.toString()));
                }
            }
            userRepository.save(user);
            return ResponseEntity.ok(user);
        }).orElse(ResponseEntity.notFound().build());
    }
}