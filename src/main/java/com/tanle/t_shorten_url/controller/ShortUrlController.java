package com.tanle.t_shorten_url.controller;

import com.tanle.t_shorten_url.request.ShortUrlCreatedRequest;
import com.tanle.t_shorten_url.response.*;
import com.tanle.t_shorten_url.service.ShortUrlService;
import com.tanle.t_shorten_url.util.AppConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(value = "http://localhost:5173")
public class ShortUrlController {
    private final ShortUrlService shortUrlService;

    @PostMapping("/short-url")
    public ResponseEntity<ApiResponse> createShortUrl(@RequestBody ShortUrlCreatedRequest request) {
        ShortUrlResponse response = shortUrlService.save(request);
        return ResponseEntity.ok(new ApiResponse<>().success(response));
    }

    @GetMapping("/short-url/{code}/views")
    public ResponseEntity<ApiResponse> getTotalViews(@PathVariable String code) {
        TotalViewUrlResponse response = this.shortUrlService.getTotalView(code);
        return ResponseEntity.ok(new ApiResponse<>().success(response));
    }

    @GetMapping("/short-url/{id}")
    public ResponseEntity<ApiResponse> getUrlById(@PathVariable String id) {
        ShortUrlDetailResponse response = this.shortUrlService.findById(id);
        return ResponseEntity.ok(new ApiResponse<>().success(response));
    }

    @DeleteMapping("/short-url/{code}")
    public ResponseEntity<ApiResponse> deleteUrl(@PathVariable String code) {
        this.shortUrlService.deleteShortUrlById(code);
        return ResponseEntity.ok(new ApiResponse<>().success());
    }

    @GetMapping("/short-urls")
    public ResponseEntity<PageResponse> getShortUrlsByUserId(
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = AppConstant.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstant.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = AppConstant.DEFAULT_SORT_BY) String sortBy,
            @RequestParam(defaultValue = AppConstant.DEFAULT_SORT_DIRECTION) String order) {

        PageResponse<ShortUrlResponse> response = shortUrlService.findByUserId(userId, page, size, sortBy, order);
        return ResponseEntity.ok(response);
    }
}