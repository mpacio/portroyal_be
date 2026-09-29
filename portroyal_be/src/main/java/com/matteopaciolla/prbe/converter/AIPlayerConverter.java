package com.matteopaciolla.prbe.converter;

import com.matteopaciolla.prbe.constants.enums.UserRole;
import com.matteopaciolla.prbe.dto.UserDto;
import com.matteopaciolla.prbe.model.entity.AIPlayerEntity;

import java.util.List;

/**
 * Converts {@link AIPlayerEntity} rows (stored separately from real accounts, see
 * {@code ai_players} table) into the same {@link UserDto} shape used for human players, so API
 * consumers can keep displaying a single, uniform list of match participants.
 */
public class AIPlayerConverter {

    public static UserDto toDtoLight(AIPlayerEntity aiPlayerEntity) {
        UserDto userDto = new UserDto();
        userDto.setUsername(aiPlayerEntity.getUsername());
        userDto.setFirstName(aiPlayerEntity.getDisplayName());
        userDto.setRoles(List.of(UserRole.AI.name()));
        userDto.setAiDifficulty(aiPlayerEntity.getDifficulty() != null ? aiPlayerEntity.getDifficulty().name() : null);
        userDto.setEnabled(false);
        return userDto;
    }
}
