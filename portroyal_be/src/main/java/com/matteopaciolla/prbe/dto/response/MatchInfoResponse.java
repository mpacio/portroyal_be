package com.matteopaciolla.prbe.dto.response;

import com.matteopaciolla.prbe.dto.MatchInfoDto;

public class MatchInfoResponse extends BaseResponse<MatchInfoDto> {

    public MatchInfoResponse(MatchInfoDto data) {
        super(data);
    }

    public MatchInfoResponse(String message, MatchInfoDto data) {
        super(message, data);
    }
}
