package com.example.reservation;

public class ConfirmationEmail {

    private final String recipientEmail;
    private final String subject;
    private final String body;

    public ConfirmationEmail(String recipientEmail, String subject, String body) {
        this.recipientEmail = recipientEmail;
        this.subject = subject;
        this.body = body;
    }

    public String getRecipientEmail() { return recipientEmail; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
}