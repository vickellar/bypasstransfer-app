package com.bypass.bypasstransers.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class MailConfig {

    // The JavaMailSender bean is automatically configured by Spring Boot
    // when mail properties are set in application.properties
    // This configuration ensures proper setup
}