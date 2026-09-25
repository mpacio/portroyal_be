package com.matteopaciolla.prbe.dto.request;

import lombok.Data;

@Data
public class ChangePasswordReqDto {
    private String oldPassword;
    private String newPassword;
}
