package com.matteopaciolla.prbe.dto.response;

import com.matteopaciolla.prbe.dto.MatchInfoDto;

import java.util.List;

public class MatchInfosPageResponse extends PaginatedResponse<MatchInfoDto> {
    public MatchInfosPageResponse(int reqPageNum, int reqPageSize, int totalPages, long totalElements, String sortField, String sortDirection, List<MatchInfoDto> elements) {
        super(reqPageNum, reqPageSize, totalPages, totalElements, sortField, sortDirection, elements);
    }
}
