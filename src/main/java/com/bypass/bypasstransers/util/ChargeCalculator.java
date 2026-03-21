
package com.bypass.bypasstransers.util;

import com.bypass.bypasstransers.model.Account;

/**
 * Small utility to calculate transfer/withdrawal fees.
 */
public class ChargeCalculator {

    // Company profit percentage (always taken on each chargeable transaction)
    public static final double BASE_PROFIT_DEFAULT = 0.05; // 5%

    // Provider defaults (percentages expressed as decimals)
    public static final double ECONET_DEFAULT = 0.033; // 3.3%
    public static final double InnBucks_DEFAULT = 0.02;   // 2%
    public static final double MUKURU_DEFAULT = 0.04; // 4%

    public static double calculateFee(Account account, double amount) {
        if (account == null) return 0.0;

        double configured = account.getTransferFee();
        if (configured > 0) {
            return amount * configured;
        }

        // fallback based on account name
        String name = account.getName();
        if (name == null) return 0.0;
        name = name.toLowerCase();
        if (name.contains("econet")) return amount * ECONET_DEFAULT;
        if (name.contains("inn") && name.contains("buck")) return amount * InnBucks_DEFAULT;
        if (name.contains("innbucks")) return amount * InnBucks_DEFAULT;
        if (name.contains("mukuru")) return amount * MUKURU_DEFAULT;
        if (name.contains("cash")) return 0.0;

        // default fallback
        return 0.0;
    }

    /**
     * Provider charge percentage based on account type.
     * Returns provider fee ONLY (company profit is added separately).
     */
    public static double getProviderFeePercent(String accountType) {
        if (accountType == null) return 0.0;
        String name = accountType.toLowerCase();
        if (name.contains("econet")) return ECONET_DEFAULT;
        if (name.contains("inn") && name.contains("buck")) return InnBucks_DEFAULT;
        if (name.contains("innbucks")) return InnBucks_DEFAULT;
        if (name.contains("mukuru")) return MUKURU_DEFAULT;
        if (name.contains("cash")) return 0.0;
        return 0.0;
    }

    /**
     * Total fee charged on a transaction:
     * - company profit: 5% of amount
     * - provider fee: based on the chosen account type
     */
    public static double calculateTotalCharge(String accountType, double amount) {
        double providerFee = amount * getProviderFeePercent(accountType);
        double profitFee = amount * BASE_PROFIT_DEFAULT;
        return providerFee + profitFee;
    }

    public static double calculateCompanyProfit(double amount) {
        return amount * BASE_PROFIT_DEFAULT;
    }
}