package com.bypass.bypasstransers.exception;

/**
 * Thrown when an account has insufficient balance for a debit operation.
 */
public class InsufficientBalanceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

	public InsufficientBalanceException(String message) {
        super(message);
    }

    public InsufficientBalanceException(String message, Throwable cause) {
        super(message, cause);
    }
}
