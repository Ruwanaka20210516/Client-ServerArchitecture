package com.smartcampus.exception;

public class LinkedResourceNotFoundException extends RuntimeException {
    private final String linkField;
    private final String linkValue;

    public LinkedResourceNotFoundException(String linkField, String linkValue) {
        super("Referenced " + linkField + " '" + linkValue + "' does not exist.");
        this.linkField = linkField;
        this.linkValue = linkValue;
    }

    public String getLinkField() {
        return linkField;
    }

    public String getLinkValue() {
        return linkValue;
    }
}
