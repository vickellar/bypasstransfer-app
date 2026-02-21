package com.bypass.bypasstransers.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.util.Map;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired(required = false)
    private EmailTemplateService templateService;

    // If a JavaMailSender bean is available (spring-boot-starter-mail configured), use it.
    // Otherwise fall back to printing the message to the console.
    public void sendSimpleEmail(String to, String subject, String body) {
        if (mailSender != null) {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setTo(to);
                msg.setSubject(subject);
                msg.setText(body);
                mailSender.send(msg);
                return;
            } catch (MailException ex) {
                // fall through to console fallback
                System.err.println("Failed to send email via JavaMailSender: " + ex.getMessage());
            }
        }

        // Console fallback: always print the message so the link is visible in logs
        System.out.println("--- Email (fallback) ---");
        System.out.println("To: " + to);
        System.out.println("Subject: " + subject);
        System.out.println(body);
        System.out.println("------------------------");
    }

    // Send an HTML/template email. templateName refers to a Thymeleaf template under templates/email/
    public void sendTemplateEmail(String to, String subject, String templateName, Map<String, Object> model) {
        String rendered = null;
        if (templateService != null) {
            try {
                rendered = templateService.render("email/" + templateName, model);
            } catch (Exception e) {
                System.err.println("Failed to render email template: " + e.getMessage());
            }
        }

        if (mailSender != null) {
            try {
                MimeMessage mime = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
                helper.setTo(to);
                helper.setSubject(subject);
                if (rendered != null) {
                    helper.setText(rendered, true);
                } else if (model != null && model.containsKey("body")) {
                    helper.setText(String.valueOf(model.get("body")), false);
                } else {
                    helper.setText("", false);
                }
                mailSender.send(mime);
                return;
            } catch (Exception ex) {
                System.err.println("Failed to send template email via JavaMailSender: " + ex.getMessage());
            }
        }

        // Fallback: print rendered template (or plain body) to logs so links are visible
        System.out.println("--- Email (template fallback) ---");
        System.out.println("To: " + to);
        System.out.println("Subject: " + subject);
        if (rendered != null) System.out.println(rendered);
        else if (model != null && model.containsKey("body")) System.out.println(model.get("body"));
        System.out.println("------------------------");
    }
}