package com.matteopaciolla.prbe.dto;

import lombok.Data;

import java.util.List;

@Data
public class EventDto {
    private String typeCode;
    private String typeDesc;
    private String playerUsername;
    private List<Integer> involvedCardIds;
    private Integer value;
}
