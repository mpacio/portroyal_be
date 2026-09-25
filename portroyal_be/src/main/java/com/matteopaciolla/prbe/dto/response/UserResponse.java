package com.matteopaciolla.prbe.dto.response;

import com.matteopaciolla.prbe.dto.UserDto;

public class UserResponse extends BaseResponse<UserDto> {

    public UserResponse(UserDto data) {
        super(data);
    }

    public UserResponse(String message, UserDto data) {
        super(message, data);
    }

    public UserResponse(Integer status, String message, UserDto data) {
        super(status, message, data);
    }
}
