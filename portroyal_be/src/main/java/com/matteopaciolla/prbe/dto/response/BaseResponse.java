package com.matteopaciolla.prbe.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class BaseResponse<T> {

    private Integer status;
    private String message;
    private T data;

    public BaseResponse(Integer status) {
        this.status = status;
    }

    public BaseResponse(Integer status, String message) {
        this.status = status;
        this.message = message;
    }

    public BaseResponse(Integer status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public BaseResponse() {
        this.status = HttpStatus.OK.value();
    }

    public BaseResponse(T data) {
        this();
        this.data = data;
    }

    public BaseResponse(String message, T data) {
        this();
        this.message = message;
        this.data = data;
    }
}
