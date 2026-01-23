package com.bypass.bypasstransers.service;

import org.springframework.stereotype.Service;

@Service
public class EmailService {

    // Lightweight fallback email sender. If you want real SMTP sending,
    // add 'spring-boot-starter-mail' to your pom and inject JavaMailSender.

    public void sendSimpleEmail(String to, String subject, String body) {
        // Console fallback: always print the message so the link is visible in logs
        System.out.println("--- Email (fallback) ---");
        System.out.println("To: " + to);
        System.out.println("Subject: " + subject);
        System.out.println(body);
        System.out.println("------------------------");
    }
}