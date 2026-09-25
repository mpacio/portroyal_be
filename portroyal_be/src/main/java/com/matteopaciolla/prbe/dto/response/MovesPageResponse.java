package com.matteopaciolla.prbe.dto.response;

import com.matteopaciolla.prbe.dto.MoveDto;

import java.util.List;

public class MovesPageResponse extends PaginatedResponse<MoveDto> {

    public MovesPageResponse(int reqPageNum, int reqPageSize, int totalPages, long totalElements, String sortField, String sortDirection, List<MoveDto> elements) {
        super(reqPageNum, reqPageSize, totalPages, totalElements, sortField, sortDirection, elements);
    }
}
