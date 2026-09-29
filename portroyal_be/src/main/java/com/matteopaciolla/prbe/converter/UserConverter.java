package com.matteopaciolla.prbe.converter;

import com.matteopaciolla.prbe.constants.enums.UserRole;
import com.matteopaciolla.prbe.dto.request.UserReqDto;
import com.matteopaciolla.prbe.dto.UserDto;
import com.matteopaciolla.prbe.model.entity.UserEntity;

import java.util.List;

public class UserConverter {

    public static UserDto toDto(UserEntity userEntity) {
        UserDto userDto = new UserDto();
        userDto.setId(userEntity.getId());
        userDto.setUsername(userEntity.getUsername());
        userDto.setFirstName(userEntity.getFirstName());
        userDto.setLastName(userEntity.getLastName());
        userDto.setEmail(userEntity.getEmail());
        userDto.setTelegramId(userEntity.getTelegramId());
        userDto.setRoles(userEntity.getRoles().stream().map(UserRole::name).toList());
        userDto.setEnabled(userEntity.isEnabled());
        return userDto;
    }

    public static UserDto toDtoLight(UserEntity userEntity, boolean includeTgId) {
        UserDto userDto = new UserDto();
        userDto.setUsername(userEntity.getUsername());
        if (includeTgId) {
            userDto.setTelegramId(userEntity.getTelegramId());
        }
        userDto.setRoles(userEntity.getRoles().stream().map(UserRole::name).toList());
        return userDto;
    }

    public static UserEntity toEntity(UserReqDto userReqDto, List<UserRole> roles) {
        UserEntity userEntity = new UserEntity(userReqDto.getUsername(), userReqDto.getPassword(), roles);
        userEntity.setFirstName(userReqDto.getFirstName());
        userEntity.setLastName(userReqDto.getLastName());
        userEntity.setEmail(userReqDto.getEmail());
        userEntity.setTelegramId(userReqDto.getTelegramId());
        return userEntity;
    }
}
