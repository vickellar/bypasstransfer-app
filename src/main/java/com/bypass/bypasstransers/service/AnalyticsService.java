package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.dto.StaffPerformanceDTO;
import com.bypass.bypasstransers.dto.AccountPerformanceDTO;
import com.bypass.bypasstransers.dto.ExpenditureDTO;
import com.bypass.bypasstransers.enums.Role;
import com.bypass.bypasstransers.model.Expenditure;
import com.bypass.bypasstransers.model.Transaction;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.model.Wallet;
import com.bypass.bypasstransers.repository.ExpenditureRepository;
import com.bypass.bypasstransers.repository.TransactionRepository;
import com.bypass.bypasstransers.repository.UserRepository;
import com.bypass.bypasstransers.repository.WalletRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final ExpenditureRepository expenditureRepository;
    private final SecurityService securityService;

    public AnalyticsService(TransactionRepository transactionRepository, 
                           UserRepository userRepository,
                           WalletRepository walletRepository,
                           ExpenditureRepository expenditureRepository,
                           SecurityService securityService) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.expenditureRepository = expenditureRepository;
        this.securityService = securityService;
    }

    // Staff Performance
    public List<StaffPerformanceDTO> getStaffPerformance() {
        List<User> staff = userRepository.findByRole(Role.STAFF);
        List<StaffPerformanceDTO> performanceList = new ArrayList<>();

        for (User user : staff) {
            StaffPerformanceDTO dto = calculateStaffPerformance(user);
            performanceList.add(dto);
        }

        // Sort by total amount (descending)
        performanceList.sort((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()));
        
        // Assign performance levels
        assignPerformanceLevels(performanceList);
        
        return performanceList;
    }

    public List<StaffPerformanceDTO> getStaffPerformanceDetailed() {
        return getStaffPerformance();
    }

    private StaffPerformanceDTO calculateStaffPerformance(User user) {
        List<Wallet> wallets = walletRepository.findByOwner(user);
        List<Transaction> transactions = new ArrayList<>();
        
        for (Wallet wallet : wallets) {
            transactions.addAll(transactionRepository.findByWallet(wallet));
        }

        int totalTransactions = transactions.size();
        BigDecimal totalAmount = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFees = transactions.stream()
                .map(Transaction::getFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalNet = totalAmount.subtract(totalFees);
        BigDecimal walletBalance = wallets.stream()
                .map(Wallet::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        StaffPerformanceDTO dto = new StaffPerformanceDTO();
        dto.setStaffId(user.getId());
        dto.setStaffName(user.getUsername());
        dto.setTotalTransactions(totalTransactions);
        dto.setTotalAmount(totalAmount);
        dto.setTotalFees(totalFees);
        dto.setTotalNet(totalNet);
        dto.setWalletCount(wallets.size());
        dto.setWalletBalance(walletBalance);

        return dto;
    }

    private void assignPerformanceLevels(List<StaffPerformanceDTO> performanceList) {
        if (performanceList.isEmpty()) return;

        BigDecimal maxAmount = performanceList.get(0).getTotalAmount();
        
        for (StaffPerformanceDTO dto : performanceList) {
            BigDecimal percentage = BigDecimal.ZERO;
            if (maxAmount.compareTo(BigDecimal.ZERO) > 0) {
                percentage = dto.getTotalAmount().multiply(new BigDecimal("100"))
                        .divide(maxAmount, 2, RoundingMode.HALF_UP);
            }
            
            if (percentage.compareTo(new BigDecimal("80")) >= 0) {
                dto.setPerformanceLevel("High");
            } else if (percentage.compareTo(new BigDecimal("50")) >= 0) {
                dto.setPerformanceLevel("Medium");
            } else {
                dto.setPerformanceLevel("Low");
            }
        }
    }

    public List<StaffPerformanceDTO> getTopPerformingStaff(int limit) {
        return getStaffPerformance().stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    // Account Performance
    public List<AccountPerformanceDTO> getAccountPerformance() {
        List<String> accountTypes = Arrays.asList("Mukuru", "Econet", "Innbucks");
        List<AccountPerformanceDTO> performanceList = new ArrayList<>();

        BigDecimal totalCompanyAmount = BigDecimal.ZERO;
        Map<String, AccountPerformanceDTO> dtoMap = new HashMap<>();

        for (String accountType : accountTypes) {
            List<Wallet> wallets = walletRepository.findByAccountType(accountType);
            List<Transaction> transactions = new ArrayList<>();
            
            for (Wallet wallet : wallets) {
                transactions.addAll(transactionRepository.findByWallet(wallet));
            }

            int totalTransactions = transactions.size();
            BigDecimal totalAmount = transactions.stream()
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalFees = transactions.stream()
                    .map(Transaction::getFee)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalNet = totalAmount.subtract(totalFees);
            
            Set<Long> activeUserIds = wallets.stream()
                    .map(w -> w.getOwner().getId())
                    .collect(Collectors.toSet());

            AccountPerformanceDTO dto = new AccountPerformanceDTO();
            dto.setAccountType(accountType);
            dto.setTotalTransactions(totalTransactions);
            dto.setTotalAmount(totalAmount);
            dto.setTotalFees(totalFees);
            dto.setTotalNet(totalNet);
            dto.setActiveUsers(activeUserIds.size());

            dtoMap.put(accountType, dto);
            totalCompanyAmount = totalCompanyAmount.add(totalAmount);
        }

        // Calculate percentages
        for (AccountPerformanceDTO dto : dtoMap.values()) {
            if (totalCompanyAmount.compareTo(BigDecimal.ZERO) > 0) {
                dto.setPercentageOfTotal(dto.getTotalAmount().multiply(new BigDecimal("100"))
                        .divide(totalCompanyAmount, 2, RoundingMode.HALF_UP));
            }
            
            // Assign performance level
            if (dto.getPercentageOfTotal().compareTo(new BigDecimal("40")) >= 0) {
                dto.setPerformanceLevel("High");
            } else if (dto.getPercentageOfTotal().compareTo(new BigDecimal("25")) >= 0) {
                dto.setPerformanceLevel("Medium");
            } else {
                dto.setPerformanceLevel("Low");
            }
            
            performanceList.add(dto);
        }

        // Sort by total amount
        performanceList.sort((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()));
        
        return performanceList;
    }

    public List<AccountPerformanceDTO> getAccountPerformanceDetailed() {
        return getAccountPerformance();
    }

    public List<AccountPerformanceDTO> getTopPerformingAccounts(int limit) {
        return getAccountPerformance().stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    // Expenditure Methods
    public List<Expenditure> getAllExpenditures() {
        return expenditureRepository.findAll();
    }

    public void saveExpenditure(ExpenditureDTO dto) {
        Expenditure expenditure = new Expenditure();
        expenditure.setDescription(dto.getDescription());
        expenditure.setCategory(dto.getCategory());
        expenditure.setAmount(dto.getAmount());
        expenditure.setDate(dto.getDate() != null ? dto.getDate() : LocalDate.now());
        expenditure.setNotes(dto.getNotes());
        expenditure.setRecordedBy(securityService.getCurrentUser());
        
        expenditureRepository.save(expenditure);
    }

    public void deleteExpenditure(Long id) {
        expenditureRepository.deleteById(id);
    }

    public BigDecimal getTotalExpenditureThisMonth() {
        YearMonth currentMonth = YearMonth.now();
        LocalDate startDate = currentMonth.atDay(1);
        LocalDate endDate = currentMonth.atEndOfMonth();
        
        BigDecimal total = expenditureRepository.getTotalExpenditureForPeriod(startDate, endDate);
        return total != null ? total : BigDecimal.ZERO;
    }

    public BigDecimal getTotalExpenditureThisYear() {
        LocalDate startDate = LocalDate.now().withDayOfYear(1);
        LocalDate endDate = LocalDate.now();
        
        BigDecimal total = expenditureRepository.getTotalExpenditureForPeriod(startDate, endDate);
        return total != null ? total : BigDecimal.ZERO;
    }

    public BigDecimal getTotalExpenditureForPeriod(LocalDate startDate, LocalDate endDate) {
        BigDecimal total = expenditureRepository.getTotalExpenditureForPeriod(startDate, endDate);
        return total != null ? total : BigDecimal.ZERO;
    }

    public Map<String, BigDecimal> getMonthlyExpenditureSummary() {
        YearMonth currentMonth = YearMonth.now();
        LocalDate startDate = currentMonth.atDay(1);
        LocalDate endDate = currentMonth.atEndOfMonth();
        
        List<Object[]> results = expenditureRepository.getExpenditureByCategoryForPeriod(startDate, endDate);
        Map<String, BigDecimal> summary = new HashMap<>();
        
        for (Object[] result : results) {
            String category = (String) result[0];
            BigDecimal amount = (BigDecimal) result[1];
            summary.put(category, amount);
        }
        
        return summary;
    }

    public List<Expenditure> getExpenditureReport(LocalDate startDate, LocalDate endDate) {
        return expenditureRepository.findByDateBetweenOrderByDateDesc(startDate, endDate);
    }
}
