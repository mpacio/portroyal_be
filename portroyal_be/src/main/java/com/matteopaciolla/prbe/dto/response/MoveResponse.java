package com.matteopaciolla.prbe.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.matteopaciolla.prbe.dto.CardDto;
import com.matteopaciolla.prbe.dto.MoveDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MoveResponse extends BaseResponse<MoveDto> {

    public MoveResponse(String message, MoveDto move) {
        super(HttpStatus.OK.value(), message, move);
    }
}

