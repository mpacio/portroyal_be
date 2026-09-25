package com.matteopaciolla.prbe.converter;

import com.matteopaciolla.portroyal.core.Player;
import com.matteopaciolla.prbe.dto.PlayerDto;
import com.matteopaciolla.prbe.model.entity.UserEntity;

public class PlayerConverter {

    public static PlayerDto toDto(Player player, UserEntity userEntity) {
        PlayerDto playerDto = new PlayerDto();
        if (userEntity != null) playerDto.setUser(UserConverter.toDto(userEntity));
        playerDto.setUsername(player.getName());
        playerDto.setCoins(player.getMoneyValue());
        playerDto.setPoints(player.getPointsValue());
        playerDto.setPower(player.getPowerValue());
        playerDto.setEmployees(CardConverter.toDtoList(player.getEmployees()));
        playerDto.setExpeditions(CardConverter.toDtoList(player.getExpeditionCards()));
        playerDto.setContractsCompleted(player.getContractsCompleted());
        playerDto.setTradingCapacity(player.getTradingCapacity());
        playerDto.setRedShipRenounced(player.getRedShipRenounced());
        playerDto.setBlackShipRenounced(player.getBlackShipRenounced());
        playerDto.setShipColorsRepelled(player.getShipColorsRepelled().stream().map(Enum::toString).toList());
        playerDto.setTaxed(player.isTaxed());
        playerDto.setMinorSpeculator(player.getMinorSpeculator());
        playerDto.setMajorSpeculator(player.isMajorSpeculator());
        playerDto.setWentBust(player.isWentBust());
        return playerDto;
    }

    public static PlayerDto toDto(Player player) {
        return toDto(player, null);
    }
}
