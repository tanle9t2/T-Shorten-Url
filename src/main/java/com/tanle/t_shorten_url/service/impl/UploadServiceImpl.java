package com.tanle.t_shorten_url.service.impl;

import com.tanle.t_shorten_url.service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@Slf4j
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucketName}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    @Override
    public String uploadFile(String keyName, byte[] content, String contentType) {
        log.info("Starting upload for file: {} to bucket: {}", keyName, bucketName);
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(keyName)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(content));
            
            String fileUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, keyName);
            log.info("File uploaded successfully. URL: {}", fileUrl);
            
            return fileUrl;
        } catch (Exception e) {
            log.error("Error occurred while uploading file: {}", keyName, e);
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }
}
