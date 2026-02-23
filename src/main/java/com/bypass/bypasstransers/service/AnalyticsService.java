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
        performanceList.sort((a, b) -> Double.compare(b.getTotalAmount(), a.getTotalAmount()));
        
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
        double totalAmount = transactions.stream().mapToDouble(Transaction::getAmount).sum();
        double totalFees = transactions.stream().mapToDouble(Transaction::getFee).sum();
        double totalNet = totalAmount - totalFees;
        double walletBalance = wallets.stream().mapToDouble(Wallet::getBalance).sum();

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

        double maxAmount = performanceList.get(0).getTotalAmount();
        
        for (StaffPerformanceDTO dto : performanceList) {
            double percentage = maxAmount > 0 ? (dto.getTotalAmount() / maxAmount) * 100 : 0;
            
            if (percentage >= 80) {
                dto.setPerformanceLevel("High");
            } else if (percentage >= 50) {
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

        double totalCompanyAmount = 0;
        Map<String, AccountPerformanceDTO> dtoMap = new HashMap<>();

        for (String accountType : accountTypes) {
            List<Wallet> wallets = walletRepository.findByAccountType(accountType);
            List<Transaction> transactions = new ArrayList<>();
            
            for (Wallet wallet : wallets) {
                transactions.addAll(transactionRepository.findByWallet(wallet));
            }

            int totalTransactions = transactions.size();
            double totalAmount = transactions.stream().mapToDouble(Transaction::getAmount).sum();
            double totalFees = transactions.stream().mapToDouble(Transaction::getFee).sum();
            double totalNet = totalAmount - totalFees;
            
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
            totalCompanyAmount += totalAmount;
        }

        // Calculate percentages
        for (AccountPerformanceDTO dto : dtoMap.values()) {
            if (totalCompanyAmount > 0) {
                dto.setPercentageOfTotal((dto.getTotalAmount() / totalCompanyAmount) * 100);
            }
            
            // Assign performance level
            if (dto.getPercentageOfTotal() >= 40) {
                dto.setPerformanceLevel("High");
            } else if (dto.getPercentageOfTotal() >= 25) {
                dto.setPerformanceLevel("Medium");
            } else {
                dto.setPerformanceLevel("Low");
            }
            
            performanceList.add(dto);
        }

        // Sort by total amount
        performanceList.sort((a, b) -> Double.compare(b.getTotalAmount(), a.getTotalAmount()));
        
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

    public double getTotalExpenditureThisMonth() {
        YearMonth currentMonth = YearMonth.now();
        LocalDate startDate = currentMonth.atDay(1);
        LocalDate endDate = currentMonth.atEndOfMonth();
        
        Double total = expenditureRepository.getTotalExpenditureForPeriod(startDate, endDate);
        return total != null ? total : 0.0;
    }

    public double getTotalExpenditureThisYear() {
        LocalDate startDate = LocalDate.now().withDayOfYear(1);
        LocalDate endDate = LocalDate.now();
        
        Double total = expenditureRepository.getTotalExpenditureForPeriod(startDate, endDate);
        return total != null ? total : 0.0;
    }

    public double getTotalExpenditureForPeriod(LocalDate startDate, LocalDate endDate) {
        Double total = expenditureRepository.getTotalExpenditureForPeriod(startDate, endDate);
        return total != null ? total : 0.0;
    }

    public Map<String, Double> getMonthlyExpenditureSummary() {
        YearMonth currentMonth = YearMonth.now();
        LocalDate startDate = currentMonth.atDay(1);
        LocalDate endDate = currentMonth.atEndOfMonth();
        
        List<Object[]> results = expenditureRepository.getExpenditureByCategoryForPeriod(startDate, endDate);
        Map<String, Double> summary = new HashMap<>();
        
        for (Object[] result : results) {
            String category = (String) result[0];
            Double amount = (Double) result[1];
            summary.put(category, amount);
        }
        
        return summary;
    }

    public List<Expenditure> getExpenditureReport(LocalDate startDate, LocalDate endDate) {
        return expenditureRepository.findByDateBetweenOrderByDateDesc(startDate, endDate);
    }
}
