package com.bypass.bypasstransers.model;

/**
 * Result of sending a transactional email (password reset, verification, etc.).
 */
public final class EmailSendOutcome {

    private final String displayLinkOptional;
    private final boolean smtpSent;

    private EmailSendOutcome(String displayLinkOptional, boolean smtpSent) {
        this.displayLinkOptional = displayLinkOptional;
        this.smtpSent = smtpSent;
    }

    /** Email was accepted by SMTP for delivery to the user's mailbox. */
    public static EmailSendOutcome deliveredToMailbox() {
        return new EmailSendOutcome(null, true);
    }

    /** User had an email on file but SMTP did not accept the message. */
    public static EmailSendOutcome smtpFailed() {
        return new EmailSendOutcome(null, false);
    }

    /** No email address; link may be shown in UI (e.g. admin/debug). */
    public static EmailSendOutcome noRecipientEmail(String displayLink) {
        return new EmailSendOutcome(displayLink, false);
    }

    public String getDisplayLinkOptional() {
        return displayLinkOptional;
    }

    public boolean isSmtpSent() {
        return smtpSent;
    }
}
