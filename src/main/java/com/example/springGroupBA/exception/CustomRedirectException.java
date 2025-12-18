package com.example.springGroupBA.exception;

import lombok.Getter;

@Getter
public class CustomRedirectException extends RuntimeException {

    private String redirectUrl;

    public CustomRedirectException(String message, String redirectUrl) {
        super(message);
        this.redirectUrl = redirectUrl;
    }
}
