package org.example.simplemailclient.exception;

public class EmailSendingException extends RuntimeException {

    public EmailSendingException(String message) {
        super(message);
    }
}