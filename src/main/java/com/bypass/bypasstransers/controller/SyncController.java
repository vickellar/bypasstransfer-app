package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.model.Transaction;
import com.bypass.bypasstransers.repository.TransactionRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Vickeller.01
 */
@RestController
@RequestMapping("/api/sync")
public class SyncController {

    @Autowired
    private TransactionRepository repo;

    @PostMapping
    public List<Transaction> sync(@RequestBody List<Transaction> localTx) {
        List<Transaction> synced = new ArrayList<>();
        for (Transaction tx : localTx) {
            tx.setSyncStatus("SYNCED");
            synced.add(repo.save(tx));
        }
        return synced;
    }
}
