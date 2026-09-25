package com.matteopaciolla.prbe.converter;

import com.matteopaciolla.portroyal.core.Match;
import com.matteopaciolla.portroyal.core.Table;
import com.matteopaciolla.prbe.dto.TableDto;

public class TableConverter {

    public static TableDto toDto(Table table, Match match) {
        TableDto tableDto = new TableDto();
        tableDto.setDrawPileSize(table.getDrawPileSize());
        tableDto.setDiscardPileSize(table.getDiscardPileSize());
        tableDto.setHarbor(table.getHarbor() != null && !table.getHarbor().isEmpty() ? CardConverter.toDtoList(table.getHarbor()) : null);
        tableDto.setExpeditions(table.getExpeditionCards() != null && !table.getExpeditionCards().isEmpty() ? CardConverter.toDtoList(table.getExpeditionCards()) : null);
        tableDto.setContracts(table.getContractsBoard() != null ? ContractCardConverter.toDtoList(table.getContractsBoard().getContracts(), match) : null);
        return tableDto;
    }
}
