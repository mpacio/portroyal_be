package com.matteopaciolla.prbe.converter;

import com.matteopaciolla.portroyal.core.Event;
import com.matteopaciolla.portroyal.core.MoveRecord;
import com.matteopaciolla.portroyal.core.cards.Card;
import com.matteopaciolla.portroyal.core.cards.enums.ExpeditionEmployee;
import com.matteopaciolla.prbe.dto.MoveDto;
import com.matteopaciolla.prbe.dto.EventDto;
import com.matteopaciolla.prbe.model.entity.MatchEntity;
import com.matteopaciolla.prbe.model.entity.MoveEntity;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import static com.matteopaciolla.prbe.constants.Formats.DATE_TIME_FORMATTER;

public class MoveConverter {

    // MoveDto -> MoveRecord |lib| -> MoveEntity |db|
    // MoveDto <- MoveEntity |db|

    public static MoveDto toDto(MoveEntity moveEntity) {
        MoveDto dto = new MoveDto();
        dto.setId(moveEntity.getId());
        dto.setCreatedAt(moveEntity.getCreatedAt().format(DATE_TIME_FORMATTER));
        dto.setLibVersion(moveEntity.getLibVersion());
        dto.setConfigurationId(moveEntity.getConfigurationId());
        dto.setTimeIndex(moveEntity.getTimeIndex());
        dto.setActivePlayerUsername(moveEntity.getActivePlayerUsername());
        dto.setActivePlayerIndex(moveEntity.getActivePlayerIndex());
        dto.setRunningPlayerUsername(moveEntity.getRunningPlayerUsername());
        dto.setRunningPlayerIndex(moveEntity.getRunningPlayerIndex());
        dto.setMoveName(moveEntity.getMoveName().toString());
        dto.setParameterIndex(moveEntity.getParameterIndex());
        dto.setPickPlayerIndex(moveEntity.getPickPlayerIndex());
        dto.setExpeditionEmployeesList(moveEntity.getExpeditionEmployeesList());
        dto.setNotes(moveEntity.getNotes());
        return dto;
    }

    public static MoveDto toDto(MoveEntity moveEntity, String keyCode) {
        MoveDto dto = toDto(moveEntity);
        dto.setMatchKeyCode(keyCode);
        return dto;
    }

    public static MoveDto toDto(MoveRecord record) {
        MoveDto dto = new MoveDto();
        dto.setTimeIndex(record.getTimeIndex());
        dto.setRunningPlayerIndex(record.getRunningPlayerIndex());
        dto.setMoveName(record.getMove().toString());
        dto.setParameterIndex(record.getChoiceIndex());
        dto.setPickPlayerIndex(record.getPickPlayerIndex());
        dto.setExpeditionEmployeesList(record.getExpeditionEmployees() != null ? record.getExpeditionEmployees().stream().map(ExpeditionEmployee::name).collect(Collectors.toList()) : null);
        dto.setNotes(record.getNotes());
        dto.setSideEvents(record.getSideEvents() != null ? record.getSideEvents().stream().map(MoveConverter::toEventDto).collect(Collectors.toList()) : null);
        dto.setMainEvent(record.getMainEvent() != null ? toEventDto(record.getMainEvent()) : null);
        return dto;
    }

    public static MoveDto toDto(MoveRecord record, String currentPhaseName) {
        MoveDto dto = toDto(record);
        dto.setCurrentPhaseName(currentPhaseName);
        return dto;
    }

    public static MoveEntity toEntity(MoveRecord move, MatchEntity matchEntity, String libVersion) {
        MoveEntity entity = new MoveEntity();
        entity.setCreatedAt(LocalDateTime.now());
        entity.setLibVersion(libVersion);
        entity.setConfigurationId(matchEntity.getConfigurationId());
        entity.setMatch(matchEntity);
        entity.setTimeIndex(move.getTimeIndex());
        String runningPlayerUsername = matchEntity.getAllPlayerUsernames().get(move.getRunningPlayerIndex());
        entity.setActivePlayerUsername(runningPlayerUsername);
        entity.setActivePlayerIndex(move.getRunningPlayerIndex());
        entity.setRunningPlayerUsername(runningPlayerUsername);
        entity.setRunningPlayerIndex(move.getRunningPlayerIndex());
        entity.setMoveName(move.getMove());
        entity.setParameterIndex(move.getChoiceIndex());
        entity.setPickPlayerIndex(move.getPickPlayerIndex());
        entity.setExpeditionEmployeesList(move.getExpeditionEmployees() != null ?
                move.getExpeditionEmployees().stream().map(ExpeditionEmployee::name).collect(Collectors.toList()) : null);
        entity.setNotes(move.getNotes());
        return entity;
    }

    public static MoveRecord toRecord(MoveEntity moveEntity) {
        MoveRecord record = new MoveRecord();
        record.setTimeIndex(moveEntity.getTimeIndex());
        record.setRunningPlayerIndex(moveEntity.getRunningPlayerIndex());
        record.setMove(moveEntity.getMoveName());
        record.setChoiceIndex(moveEntity.getParameterIndex() != null ? moveEntity.getParameterIndex() : -1);
        record.setPickPlayerIndex(moveEntity.getPickPlayerIndex() != null ? moveEntity.getPickPlayerIndex() : -1);
        record.setExpeditionEmployees(moveEntity.getExpeditionEmployeesList() != null ?
                moveEntity.getExpeditionEmployeesList().stream().map(ExpeditionEmployee::valueOf).collect(Collectors.toList()) : null);
        return record;
    }

    public static EventDto toEventDto(Event event) {
        EventDto dto = new EventDto();
        dto.setTypeCode(event.getType().toString());
        dto.setTypeDesc(event.getType().getDescription());
        dto.setPlayerUsername(event.getPlayer().getName());
        dto.setInvolvedCardIds(event.getInvolvedCards() != null ? event.getInvolvedCards().stream().map(Card::getId).collect(Collectors.toList()) : null);
        dto.setValue(event.getValue());
        return dto;
    }
}
