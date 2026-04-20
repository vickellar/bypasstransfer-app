package com.bypass.bypasstransers.util;

import com.bypass.bypasstransers.model.Account;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Small utility to calculate transfer/withdrawal fees using BigDecimal for financial precision.
 */
public class ChargeCalculator {

    // Company profit percentage (always taken on each chargeable transaction)
    public static final BigDecimal BASE_PROFIT_DEFAULT = new BigDecimal("0.05"); // 5%

    // Provider defaults (percentages expressed as decimals)
    public static final BigDecimal ECONET_DEFAULT = new BigDecimal("0.033"); // 3.3%
    public static final BigDecimal InnBucks_DEFAULT = new BigDecimal("0.02");   // 2%
    public static final BigDecimal MUKURU_DEFAULT = new BigDecimal("0.04"); // 4%

    public static BigDecimal calculateFee(Account account, BigDecimal amount) {
        if (account == null || amount == null) return BigDecimal.ZERO;

        BigDecimal configured = account.getTransferFee();
        if (configured != null && configured.compareTo(BigDecimal.ZERO) > 0) {
            return amount.multiply(configured).setScale(4, RoundingMode.HALF_UP);
        }

        // fallback based on account name
        String name = account.getName();
        if (name == null) return BigDecimal.ZERO;
        name = name.toLowerCase();
        
        BigDecimal rate = BigDecimal.ZERO;
        if (name.contains("econet")) rate = ECONET_DEFAULT;
        else if (name.contains("inn") && name.contains("buck")) rate = InnBucks_DEFAULT;
        else if (name.contains("innbucks")) rate = InnBucks_DEFAULT;
        else if (name.contains("mukuru")) rate = MUKURU_DEFAULT;
        else if (name.contains("cash")) return BigDecimal.ZERO;

        return amount.multiply(rate).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Provider charge percentage based on account type.
     * Returns provider fee ONLY (company profit is added separately).
     */
    public static BigDecimal getProviderFeePercent(String accountType) {
        if (accountType == null) return BigDecimal.ZERO;
        String name = accountType.toLowerCase();
        if (name.contains("econet")) return ECONET_DEFAULT;
        if (name.contains("inn") && name.contains("buck")) return InnBucks_DEFAULT;
        if (name.contains("innbucks")) return InnBucks_DEFAULT;
        if (name.contains("mukuru")) return MUKURU_DEFAULT;
        if (name.contains("cash")) return BigDecimal.ZERO;
        return BigDecimal.ZERO;
    }

    /**
     * Total fee charged on a transaction:
     * - company profit: 5% of amount
     * - provider fee: based on the chosen account type
     */
    public static BigDecimal calculateTotalCharge(String accountType, BigDecimal amount) {
        if (amount == null) return BigDecimal.ZERO;
        BigDecimal providerFee = amount.multiply(getProviderFeePercent(accountType));
        BigDecimal profitFee = amount.multiply(BASE_PROFIT_DEFAULT);
        return providerFee.add(profitFee).setScale(4, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateCompanyProfit(BigDecimal amount) {
        if (amount == null) return BigDecimal.ZERO;
        return amount.multiply(BASE_PROFIT_DEFAULT).setScale(4, RoundingMode.HALF_UP);
    }
}