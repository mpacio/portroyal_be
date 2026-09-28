package com.matteopaciolla.prbe.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
@Schema(name = "BaseResponse", description = "Generic envelope used by most REST endpoints: status code, message and payload.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class BaseResponse<T> {

    @Schema(description = "HTTP status code returned by the endpoint.", example = "200")
    private Integer status;

    @Schema(description = "Human-readable response message summarizing the outcome.", example = "Match created successfully")
    private String message;

    @Schema(description = "Payload returned by the endpoint. The shape depends on the endpoint and may be null for empty responses.")
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
