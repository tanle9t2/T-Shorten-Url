package com.tanle.t_shorten_url.controller;

import com.tanle.t_shorten_url.request.ShortUrlCreatedRequest;
import com.tanle.t_shorten_url.response.ApiResponse;
import com.tanle.t_shorten_url.response.TotalViewUrlResponse;
import com.tanle.t_shorten_url.service.ShortUrlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class ShortUrlController {
    private final ShortUrlService shortUrlService;

    @PostMapping("/short-url")
    public ResponseEntity<ApiResponse> createShortUrl(@RequestBody ShortUrlCreatedRequest request) {
        String code = shortUrlService.save(request);
        return ResponseEntity.ok(new ApiResponse<>().success(code));
    }

    @GetMapping("/short-url/{code}/views")
    public ResponseEntity<ApiResponse> getTotalViews(@PathVariable String code) {
        TotalViewUrlResponse response = this.shortUrlService.getTotalView(code);
        return ResponseEntity.ok(new ApiResponse<>().success(response));
    }
}