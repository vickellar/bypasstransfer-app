/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bypass.bypasstransers.repository;

import com.bypass.bypasstransers.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author Vickeller.01
 */
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    // find wallets by owner id or username can be added later if needed
}