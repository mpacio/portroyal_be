package com.matteopaciolla.prbe.dto.response;

import com.matteopaciolla.prbe.dto.MatchDto;

public class MatchResponse extends BaseResponse<MatchDto> {

    public MatchResponse(MatchDto data) {
        super(data);
    }

    public MatchResponse(String message, MatchDto data) {
        super(message, data);
    }
}
