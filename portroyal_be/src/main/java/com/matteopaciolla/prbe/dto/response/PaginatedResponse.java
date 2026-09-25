package com.matteopaciolla.prbe.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public abstract class PaginatedResponse<T> extends BaseResponse<List<T>> {

    private int reqPageNum;
    private int reqPageSize;
    private int actPageSize;
    private int totalPages;
    private long totalElements;
    private String sortField;
    private String sortDirection;

    public PaginatedResponse(int reqPageNum, int reqPageSize, int totalPages, long totalElements, String sortField, String sortDirection, List<T> elements) {
        super(elements);
        this.reqPageNum = reqPageNum;
        this.reqPageSize = reqPageSize;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.actPageSize = elements != null ? elements.size() : 0;
        this.sortField = sortField;
        this.sortDirection = sortDirection;
    }
}
