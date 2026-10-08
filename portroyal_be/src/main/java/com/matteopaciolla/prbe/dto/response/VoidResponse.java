package com.matteopaciolla.prbe.dto.response;

public class VoidResponse extends BaseResponse<Void> {

    public VoidResponse() {
        super();
    }

    public VoidResponse(String message) {
        super(message, null);
    }

    public VoidResponse(int status, String message) {
        super(status, message, null);
    }
}
