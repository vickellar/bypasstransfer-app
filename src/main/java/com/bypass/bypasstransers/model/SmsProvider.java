
package com.bypass.bypasstransers.model;

public interface SmsProvider {
    void send(String phoneNumber, String message);
}

