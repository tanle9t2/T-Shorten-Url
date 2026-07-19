package com.tanle.t_shorten_url.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class ApiResponse<T> {
    private T data;
    private String message;
    private HttpStatus status;


    public ApiResponse success() {
        return this.success(null);
    }
    public ApiResponse success(T data) {
        return ApiResponse.builder()
                .message("Success")
                .data(data)
                .status(HttpStatus.OK)
                .build();
    }
}
