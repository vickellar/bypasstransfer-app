package com.bypass.bypasstransers.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.mail.internet.MimeMessage;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired(required = false)
    private EmailTemplateService templateService;

    /** Must match an authorized sender for your SMTP provider (e.g. Gmail). */
    @Value("${spring.mail.username:}")
    private String fromAddress;

    /**
     * Send a plain-text email. Falls back to console log if SMTP is unavailable.
     *
     * @return true if the message was handed to SMTP successfully
     */
    public boolean sendSimpleEmail(String to, String subject, String body) {
        if (mailSender != null && StringUtils.hasText(fromAddress)) {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setFrom(fromAddress);
                msg.setTo(to);
                msg.setSubject(subject);
                msg.setText(body);
                mailSender.send(msg);
                log.info("[EmailService] Plain email sent to: {}", to);
                return true;
            } catch (MailException ex) {
                log.error("[EmailService] SMTP error sending plain email to {}: {}", to, ex.getMessage(), ex);
            }
        } else {
            if (mailSender == null) {
                log.warn("[EmailService] JavaMailSender not available (configure spring.mail.* / MAIL_*). Console fallback for to={}", to);
            } else {
                log.warn("[EmailService] spring.mail.username is empty; set MAIL_USERNAME. Console fallback for to={}", to);
            }
        }

        logFallback("plain", to, subject, body, null, null, null);
        return false;
    }

    /**
     * Send an HTML template email. templateName is a Thymeleaf template
     * under {@code templates/email/} (e.g. {@code "password-reset.html"}).
     * Falls back to console if SMTP is unavailable.
     *
     * @return true if the message was handed to SMTP successfully
     */
    public boolean sendTemplateEmail(String to, String subject, String templateName, Map<String, Object> model) {
        String rendered = null;
        if (templateService != null) {
            try {
                rendered = templateService.render("email/" + templateName, model);
            } catch (Exception e) {
                log.error("[EmailService] Failed to render template '{}': {}", templateName, e.getMessage(), e);
            }
        }

        if (mailSender != null && StringUtils.hasText(fromAddress)) {
            try {
                MimeMessage mime = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
                helper.setFrom(fromAddress);
                helper.setTo(to);
                helper.setSubject(subject);
                if (rendered != null) {
                    helper.setText(rendered, true);
                } else if (model != null && model.containsKey("body")) {
                    helper.setText(String.valueOf(model.get("body")), false);
                } else {
                    helper.setText("(no content)", false);
                }
                mailSender.send(mime);
                log.info("[EmailService] Template '{}' sent to: {}", templateName, to);
                return true;
            } catch (Exception ex) {
                log.error("[EmailService] SMTP error sending template '{}' to {}: {}", templateName, to, ex.getMessage(), ex);
            }
        } else {
            if (mailSender == null) {
                log.warn("[EmailService] JavaMailSender not available (configure spring.mail.* / MAIL_*). Console fallback for template={} to={}", templateName, to);
            } else {
                log.warn("[EmailService] spring.mail.username is empty; set MAIL_USERNAME. Console fallback for template={} to={}", templateName, to);
            }
        }

        logFallback("template", to, subject, null, templateName, rendered, model);
        return false;
    }

    private void logFallback(String kind, String to, String subject, String body, String templateName, String rendered, Map<String, Object> model) {
        log.info("--- Email (console fallback, {}) ---", kind);
        log.info("To: {}", to);
        log.info("Subject: {}", subject);
        if (templateName != null) {
            log.info("Template: {}", templateName);
        }
        if (rendered != null) {
            log.info("{}", rendered);
        } else if (body != null) {
            log.info("{}", body);
        } else if (model != null && model.containsKey("body")) {
            log.info("{}", model.get("body"));
        }
        log.info("--------------------------------");
    }
}