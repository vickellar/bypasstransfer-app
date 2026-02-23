package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.enums.Role;
import com.bypass.bypasstransers.model.Wallet;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.model.Transaction;
import com.bypass.bypasstransers.model.Expenditure;
import com.bypass.bypasstransers.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipInputStream;

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
    
    /**
     * Create a full system backup
     */
    @Transactional(readOnly = true)
    public byte[] createBackup() throws Exception {
        Map<String, Object> backupData = new HashMap<>();
        
        // Add backup metadata
        backupData.put("backupTimestamp", LocalDateTime.now());
        backupData.put("backupVersion", "1.0");
        
        // Backup all entities
        backupData.put("users", userRepository.findAll());
        backupData.put("accounts", walletRepository.findAll());
        backupData.put("transactions", transactionRepository.findAll());
        backupData.put("expenditures", expenditureRepository.findAll());
        backupData.put("passwordResetTokens", passwordResetTokenRepository.findAll());
        backupData.put("emailVerificationTokens", emailVerificationTokenRepository.findAll());
        
        // Convert to JSON
        String jsonData = objectMapper.writeValueAsString(backupData);
        
        // Create zip file with JSON data
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry("backup.json");
            zos.putNextEntry(entry);
            zos.write(jsonData.getBytes());
            zos.closeEntry();
        }
        
        return baos.toByteArray();
    }
    
    /**
     * Restore system from backup
     */
    @Transactional
    public void restoreBackup(byte[] backupData) throws Exception {
        // Clear existing data (in reverse order of foreign key dependencies)
        passwordResetTokenRepository.deleteAll();
        emailVerificationTokenRepository.deleteAll();
        transactionRepository.deleteAll();
        expenditureRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.deleteAll();
        
        // Read zip file
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(backupData))) {
            ZipEntry entry = zis.getNextEntry();
            if (entry != null && "backup.json".equals(entry.getName())) {
                // Read JSON data
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }
                
                String jsonData = baos.toString();
                Map<String, Object> backupMap = objectMapper.readValue(jsonData, Map.class);
                
                // Restore data
                restoreUsers((List<Map<String, Object>>) backupMap.get("users"));
                restoreAccounts((List<Map<String, Object>>) backupMap.get("accounts"));
                restoreTransactions((List<Map<String, Object>>) backupMap.get("transactions"));
                restoreExpenditures((List<Map<String, Object>>) backupMap.get("expenditures"));
                restorePasswordResetTokens((List<Map<String, Object>>) backupMap.get("passwordResetTokens"));
                restoreEmailVerificationTokens((List<Map<String, Object>>) backupMap.get("emailVerificationTokens"));
            }
        }
    }
    
    private void restoreUsers(List<Map<String, Object>> users) {
        for (Map<String, Object> userData : users) {
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
    
    private void restoreAccounts(List<Map<String, Object>> accounts) {
        for (Map<String, Object> accountData : accounts) {
            Wallet account = new Wallet(); // Changed from Account to Wallet
            account.setId(((Number) accountData.get("id")).longValue());
            account.setAccountType((String) accountData.get("name")); // Map the name field to accountType
            account.setBalance(((Number) accountData.get("balance")).doubleValue());
            account.setCreatedAt(LocalDateTime.parse((String) accountData.get("createdAt")));
            account.setLocked((Boolean) accountData.get("locked"));
            
            // Set owner
            Long ownerId = ((Number) accountData.get("ownerId")).longValue();
            User owner = userRepository.findById(ownerId).orElse(null);
            if (owner != null) {
                account.setOwner(owner);
                walletRepository.save(account);
            }
        }
    }
    
    private void restoreTransactions(List<Map<String, Object>> transactions) {
        for (Map<String, Object> txData : transactions) {
            Transaction tx = new Transaction();
            tx.setId(((Number) txData.get("id")).longValue());
            tx.setAmount(((Number) txData.get("amount")).doubleValue());
            tx.setFee(((Number) txData.get("fee")).doubleValue());
            tx.setNetAmount(((Number) txData.get("netAmount")).doubleValue());
            tx.setType(com.bypass.bypasstransers.enums.TransactionType.valueOf((String) txData.get("type")));
            tx.setFromAccount((String) txData.get("fromAccount"));
            tx.setToAccount((String) txData.get("toAccount"));
            tx.setSyncStatus((String) txData.get("syncStatus"));
            tx.setCreatedBy((String) txData.get("createdBy"));
            tx.setDate(LocalDateTime.parse((String) txData.get("date")));
            
            // Set wallet if exists
            if (txData.get("walletId") != null) {
                Long walletId = ((Number) txData.get("walletId")).longValue();
                Wallet wallet = walletRepository.findById(walletId).orElse(null);
                if (wallet != null) {
                    tx.setWallet(wallet);
                    transactionRepository.save(tx);
                }
            } else {
                transactionRepository.save(tx);
            }
        }
    }
    
    private void restoreExpenditures(List<Map<String, Object>> expenditures) {
        for (Map<String, Object> expData : expenditures) {
            Expenditure exp = new Expenditure();
            exp.setId(((Number) expData.get("id")).longValue());
            exp.setAmount(((Number) expData.get("amount")).doubleValue());
            exp.setCategory((String) expData.get("category"));
            exp.setDescription((String) expData.get("description"));
            exp.setDate(java.time.LocalDate.parse((String) expData.get("date")));
            
            // Set recordedBy
            String username = (String) expData.get("recordedBy");
            User user = userRepository.findByUsername(username).stream().findFirst().orElse(null);
            if (user != null) {
                exp.setRecordedBy(user);
                expenditureRepository.save(exp);
            }
        }
    }
    
    private void restorePasswordResetTokens(List<Map<String, Object>> tokens) {
        // Password reset tokens are temporary, so we don't restore them
        // They will be regenerated when needed
    }
    
    private void restoreEmailVerificationTokens(List<Map<String, Object>> tokens) {
        // Email verification tokens are temporary, so we don't restore them
        // They will be regenerated when needed
    }
    
    /**
     * Get backup information without restoring
     */
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
                Map<String, Object> backupMap = objectMapper.readValue(jsonData, Map.class);
                
                info.put("backupTimestamp", backupMap.get("backupTimestamp"));
                info.put("backupVersion", backupMap.get("backupVersion"));
                info.put("userCount", ((List<?>) backupMap.get("users")).size());
                info.put("accountCount", ((List<?>) backupMap.get("accounts")).size());
                info.put("transactionCount", ((List<?>) backupMap.get("transactions")).size());
                info.put("expenditureCount", ((List<?>) backupMap.get("expenditures")).size());
            }
        }
        
        return info;
    }
}
