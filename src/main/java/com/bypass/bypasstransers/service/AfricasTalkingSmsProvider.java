package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.model.SmsProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@Profile("africas-talking")
public class AfricasTalkingSmsProvider implements SmsProvider {

    private final String username;
    private final String apiKey;

    public AfricasTalkingSmsProvider() {
        this.username = System.getenv("AT_USERNAME");
        this.apiKey = System.getenv("AT_API_KEY");
    }

    @Override
    public void send(String phoneNumber, String message) {
        if (username == null || apiKey == null) {
            System.out.println("Africa's Talking credentials not set. SMS not sent. Phone=" + phoneNumber);
            return;
        }

        try {
            String endpoint = "https://api.africastalking.com/version1/messaging";
            String payload = "username=" + URLEncoder.encode(username, "UTF-8")
                    + "&to=" + URLEncoder.encode(phoneNumber, "UTF-8")
                    + "&message=" + URLEncoder.encode(message, "UTF-8");

            byte[] postData = payload.getBytes(StandardCharsets.UTF_8);

            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            conn.setRequestProperty("apiKey", apiKey);
            conn.setRequestProperty("Accept", "application/json");
            conn.setFixedLengthStreamingMode(postData.length);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(postData);
            }

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                System.out.println("[AFRICAS_TALKING] SMS sent to " + phoneNumber + " (HTTP " + code + ")");
            } else {
                System.err.println("[AFRICAS_TALKING] SMS failed HTTP=" + code + " for " + phoneNumber);
            }

        } catch (Exception ex) {
            System.err.println("[AFRICAS_TALKING] Error sending SMS to " + phoneNumber + " -> " + ex.getMessage());
        }
    }
}