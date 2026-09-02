package com.tanle.t_shorten_url.service;

public interface UploadService {
    String uploadFile(String keyName, byte[] content, String contentType);
}
