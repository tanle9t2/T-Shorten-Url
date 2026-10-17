package com.tanle.t_shorten_url.controller;

import com.tanle.t_shorten_url.response.AnalyticsResponse;
import com.tanle.t_shorten_url.response.ApiResponse;
import com.tanle.t_shorten_url.response.ShortCodeAnalyticsResponse;
import org.springframework.web.bind.annotation.PathVariable;
import com.tanle.t_shorten_url.service.AnalyticsService;
import com.tanle.t_shorten_url.util.AppConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@CrossOrigin(value = "http://localhost:5173")
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse> getAnalytics(
            @RequestParam(defaultValue = AppConstant.ANONYMOUS_USER_ID) String userId,
            @RequestParam(required = false) Integer period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "tz", required = false) String timeZone) {
        AnalyticsResponse response = analyticsService.getAnalytics(userId, period, from, to, timeZone);
        return ResponseEntity.ok(new ApiResponse<>().success(response));
    }

    @GetMapping("/short-url/{code}/analytics")
    public ResponseEntity<ApiResponse> getShortCodeAnalytics(
            @PathVariable String code,
            @RequestParam(defaultValue = AppConstant.ANONYMOUS_USER_ID) String userId,
            @RequestParam(required = false) Integer period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "tz", required = false) String timeZone) {
        ShortCodeAnalyticsResponse response =
                analyticsService.getShortCodeAnalytics(userId, code, period, from, to, timeZone);
        return ResponseEntity.ok(new ApiResponse<>().success(response));
    }
}
