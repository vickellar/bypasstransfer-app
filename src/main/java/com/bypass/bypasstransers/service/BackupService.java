package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.enums.Role;
import com.bypass.bypasstransers.model.Wallet;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.model.Transaction;
import com.bypass.bypasstransers.model.Expenditure;
import com.bypass.bypasstransers.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipInputStream;

/**
 * Service for creating and restoring full system backups in JSON format.
 */
@Service
public class BackupService {
    
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final ExpenditureRepository expenditureRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final ObjectMapper objectMapper;
    
    public BackupService(UserRepository userRepository,
                        WalletRepository walletRepository,
                        TransactionRepository transactionRepository,
                        ExpenditureRepository expenditureRepository,
                        PasswordResetTokenRepository passwordResetTokenRepository,
                        EmailVerificationTokenRepository emailVerificationTokenRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.expenditureRepository = expenditureRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    @Transactional(readOnly = true)
    public byte[] createBackup() throws Exception {
        Map<String, Object> backupData = new HashMap<>();
        backupData.put("backupTimestamp", LocalDateTime.now());
        backupData.put("backupVersion", "1.1");
        backupData.put("users", userRepository.findAll());
        backupData.put("accounts", walletRepository.findAll());
        backupData.put("transactions", transactionRepository.findAll());
        backupData.put("expenditures", expenditureRepository.findAll());
        backupData.put("passwordResetTokens", passwordResetTokenRepository.findAll());
        backupData.put("emailVerificationTokens", emailVerificationTokenRepository.findAll());
        
        String jsonData = objectMapper.writeValueAsString(backupData);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry("backup.json");
            zos.putNextEntry(entry);
            zos.write(jsonData.getBytes());
            zos.closeEntry();
        }
        return baos.toByteArray();
    }
    
    @Transactional
    public void restoreBackup(byte[] backupData) throws Exception {
        passwordResetTokenRepository.deleteAll();
        emailVerificationTokenRepository.deleteAll();
        transactionRepository.deleteAll();
        expenditureRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.deleteAll();
        
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(backupData))) {
            ZipEntry entry = zis.getNextEntry();
            if (entry != null && "backup.json".equals(entry.getName())) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }
                
                String jsonData = baos.toString();
                Map<String, Object> backupMap = objectMapper.readValue(jsonData, new TypeReference<Map<String, Object>>() {});
                
                Object usersObj = backupMap.get("users");
                if (usersObj instanceof List) {
                    restoreUsers((List<?>) usersObj);
                }
                
                Object accountsObj = backupMap.get("accounts");
                if (accountsObj instanceof List) {
                    restoreAccounts((List<?>) accountsObj);
                }
                
                Object transactionsObj = backupMap.get("transactions");
                if (transactionsObj instanceof List) {
                    restoreTransactions((List<?>) transactionsObj);
                }
                
                Object expendituresObj = backupMap.get("expenditures");
                if (expendituresObj instanceof List) {
                    restoreExpenditures((List<?>) expendituresObj);
                }
                
                // Tokens are currently placeholder restoration
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private void restoreUsers(List<?> users) {
        for (Object item : users) {
            Map<String, Object> userData = (Map<String, Object>) item;
            User user = new User();
            user.setId(((Number) userData.get("id")).longValue());
            user.setUsername((String) userData.get("username"));
            user.setPassword((String) userData.get("password"));
            user.setEmail((String) userData.get("email"));
            user.setPhoneNumber((String) userData.get("phoneNumber"));
            user.setRole(Role.valueOf((String) userData.get("role")));
            user.setEmailVerified((Boolean) userData.get("emailVerified"));
            user.setCreatedAt(LocalDateTime.parse((String) userData.get("createdAt")));
            user.setIsActive((Boolean) userData.get("isActive"));
            if (userData.get("deletedAt") != null) {
                user.setDeletedAt(LocalDateTime.parse((String) userData.get("deletedAt")));
            }
            userRepository.save(user);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void restoreAccounts(List<?> accounts) {
        for (Object item : accounts) {
            Map<String, Object> accountData = (Map<String, Object>) item;
            Wallet account = new Wallet();
            account.setId(((Number) accountData.get("id")).longValue());
            account.setAccountType((String) accountData.get("accountType"));
            if (account.getAccountType() == null) {
                account.setAccountType((String) accountData.get("name")); // Fallback for older backups
            }
            account.setBalance(new BigDecimal(accountData.get("balance").toString()));
            account.setCreatedAt(LocalDateTime.parse((String) accountData.get("createdAt")));
            account.setLocked((Boolean) accountData.get("locked"));
            
            Long ownerId = ((Number) accountData.get("ownerId")).longValue();
            User owner = userRepository.findById(ownerId).orElse(null);
            if (owner != null) {
                account.setOwner(owner);
                walletRepository.save(account);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private void restoreTransactions(List<?> transactions) {
        for (Object item : transactions) {
            Map<String, Object> txData = (Map<String, Object>) item;
            Transaction tx = new Transaction();
            tx.setId(((Number) txData.get("id")).longValue());
            tx.setAmount(new BigDecimal(txData.get("amount").toString()));
            tx.setFee(new BigDecimal(txData.get("fee").toString()));
            tx.setNetAmount(new BigDecimal(txData.get("netAmount").toString()));
            tx.setType(com.bypass.bypasstransers.enums.TransactionType.valueOf((String) txData.get("type")));
            tx.setFromAccount((String) txData.get("fromAccount"));
            tx.setToAccount((String) txData.get("toAccount"));
            tx.setSyncStatus((String) txData.get("syncStatus"));
            tx.setCreatedBy((String) txData.get("createdBy"));
            tx.setDate(LocalDateTime.parse((String) txData.get("date")));
            
            if (txData.get("walletId") != null) {
                Long walletId = ((Number) txData.get("walletId")).longValue();
                walletRepository.findById(walletId).ifPresent(w -> {
                    tx.setWallet(w);
                    transactionRepository.save(tx);
                });
            } else {
                transactionRepository.save(tx);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private void restoreExpenditures(List<?> expenditures) {
        for (Object item : expenditures) {
            Map<String, Object> expData = (Map<String, Object>) item;
            Expenditure exp = new Expenditure();
            exp.setId(((Number) expData.get("id")).longValue());
            exp.setAmount(new BigDecimal(expData.get("amount").toString()));
            exp.setCategory((String) expData.get("category"));
            exp.setDescription((String) expData.get("description"));
            exp.setDate(java.time.LocalDate.parse((String) expData.get("date")));
            
            String username = (String) expData.get("recordedBy");
            userRepository.findByUsername(username).stream().findFirst().ifPresent(u -> {
                exp.setRecordedBy(u);
                expenditureRepository.save(exp);
            });
        }
    }
    
    public Map<String, Object> getBackupInfo(byte[] backupData) throws Exception {
        Map<String, Object> info = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(backupData))) {
            ZipEntry entry = zis.getNextEntry();
            if (entry != null && "backup.json".equals(entry.getName())) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }
                String jsonData = baos.toString();
                Map<String, Object> backupMap = objectMapper.readValue(jsonData, new TypeReference<Map<String, Object>>() {});
                info.put("backupTimestamp", backupMap.get("backupTimestamp"));
                info.put("backupVersion", backupMap.get("backupVersion"));
                
                Object users = backupMap.get("users");
                info.put("userCount", users instanceof List ? ((List<?>) users).size() : 0);
                
                Object accounts = backupMap.get("accounts");
                info.put("accountCount", accounts instanceof List ? ((List<?>) accounts).size() : 0);
                
                Object transactions = backupMap.get("transactions");
                info.put("transactionCount", transactions instanceof List ? ((List<?>) transactions).size() : 0);
                
                Object expenditures = backupMap.get("expenditures");
                info.put("expenditureCount", expenditures instanceof List ? ((List<?>) expenditures).size() : 0);
            }
        }
        return info;
    }
}
