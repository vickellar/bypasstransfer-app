/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bypass.bypasstransers.util;

import com.bypass.bypasstransers.model.Account;

/**
 * Small utility to calculate transfer/withdrawal fees.
 */
public class ChargeCalculator {

    // Provider defaults (percentages expressed as decimals)
    public static final double ECONET_DEFAULT = 0.033; // 3.3%
    public static final double InnBucks_DEFAULT = 0.02;   // 2%
    public static final double MUKURU_DEFAULT = 0.015; // 1.5% (placeholder, adjust if needed)

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
        if (name.contains("innBucks")) return amount * InnBucks_DEFAULT;
        if (name.contains("mukuru")) return amount * MUKURU_DEFAULT;

        // default fallback
        return amount * 0.02;
    }
}