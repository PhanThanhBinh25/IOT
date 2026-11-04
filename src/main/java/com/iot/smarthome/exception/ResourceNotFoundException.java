package com.iot.smarthome.exception;

/**
 * Ném ra khi không tìm thấy tài nguyên trong DB.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

