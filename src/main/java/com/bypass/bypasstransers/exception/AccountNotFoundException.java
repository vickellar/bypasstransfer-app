package com.bypass.bypasstransers.exception;

/**
 * Thrown when an account cannot be found by name.
 */
public class AccountNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

	public AccountNotFoundException(String message) {
        super(message);
    }

    public AccountNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
