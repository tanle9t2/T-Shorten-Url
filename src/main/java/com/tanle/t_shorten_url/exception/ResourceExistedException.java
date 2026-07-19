package com.tanle.t_shorten_url.exception;

public class ResourceExistedException  extends RuntimeException {
    public ResourceExistedException(String message) {
        super(message);
    }
}
