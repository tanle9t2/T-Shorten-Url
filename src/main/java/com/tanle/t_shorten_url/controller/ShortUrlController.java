package com.tanle.t_shorten_url.controller;

import com.tanle.t_shorten_url.request.ShorUrlCreatedRequest;
import com.tanle.t_shorten_url.request.ShortUrlRequest;
import com.tanle.t_shorten_url.response.ApiResponse;
import com.tanle.t_shorten_url.response.ShortUrlResponse;
import com.tanle.t_shorten_url.service.ShortUrlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class ShortUrlController {
    private final ShortUrlService shortUrlService;

    @PostMapping("/short-url")
    public ResponseEntity<ApiResponse> createShortUrl(@RequestBody ShorUrlCreatedRequest request) {
        String code = shortUrlService.save(request);
        return ResponseEntity.ok(new ApiResponse<>().success(code));
    }
}