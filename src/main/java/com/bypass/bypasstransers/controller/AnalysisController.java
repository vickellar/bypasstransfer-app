package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.service.AnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Vickeller.01
 */
@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    @Autowired
    private AnalysisService service;

    @GetMapping("/profit")
    public double profit() {
        return service.totalProfit();
    }
}
