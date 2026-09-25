package com.matteopaciolla.prbe.converter;

import com.matteopaciolla.portroyal.confs.Configuration;
import com.matteopaciolla.prbe.model.entity.ConfigPropertyEntity;
import com.matteopaciolla.prbe.dto.request.MatchConfigReqDto;

import java.util.List;

public class MatchConfigConverter {

    public static MatchConfigReqDto toDto(Configuration configuration, Integer id, String name) {
        MatchConfigReqDto matchConfigReqDto = new MatchConfigReqDto();
        matchConfigReqDto.setId(id);
        matchConfigReqDto.setName(name);
        matchConfigReqDto.setInitialCoins(configuration.getInitialCoins());
        matchConfigReqDto.setFirstPlayerRandomlyChosen(configuration.isFirstPlayerRandomlyChosen());
        matchConfigReqDto.setTaxedMoney(configuration.getTaxedMoney());
        matchConfigReqDto.setTaxRate(configuration.getTaxRate());
        matchConfigReqDto.setBigExpeditionMinimumPlayersNumber(configuration.getBigExpeditionMinimumPlayersNumber());
        matchConfigReqDto.setJOMC_ExpansionUsed(configuration.isJOMC_ExpansionUsed());
        matchConfigReqDto.setCargoCoinToPoorestPlayer(configuration.isCargoCoinToPoorestPlayer());
        matchConfigReqDto.setContractsCardNumber(configuration.getContractsCardNumber());
        matchConfigReqDto.setMaxContractsCompletablePerPlayer(configuration.getMaxContractsCompletablePerPlayer());
        return matchConfigReqDto;
    }

    public static Configuration toLibEntity(MatchConfigReqDto matchConfigReqDto) {
        return Configuration.builder()
                .initialCoins(matchConfigReqDto.getInitialCoins())
                .firstPlayerRandomlyChosen(matchConfigReqDto.getFirstPlayerRandomlyChosen())
                .taxedMoney(matchConfigReqDto.getTaxedMoney())
                .taxRate(matchConfigReqDto.getTaxRate())
                .bigExpeditionMinimumPlayersNumber(matchConfigReqDto.getBigExpeditionMinimumPlayersNumber())
                .JOMC_ExpansionUsed(matchConfigReqDto.getJOMC_ExpansionUsed())
                .cargoCoinToPoorestPlayer(matchConfigReqDto.getCargoCoinToPoorestPlayer())
                .contractsCardNumber(matchConfigReqDto.getContractsCardNumber())
                .maxContractsCompletablePerPlayer(matchConfigReqDto.getMaxContractsCompletablePerPlayer())
                .build();
    }

    public static Configuration toLibEntity(List<ConfigPropertyEntity> properties){
        MatchConfigReqDto matchConfigReqDto = fromList(properties);
        return toLibEntity(matchConfigReqDto);
    }

    public static MatchConfigReqDto fromList(List<ConfigPropertyEntity> properties){
        MatchConfigReqDto matchConfigReqDto = new MatchConfigReqDto();
        matchConfigReqDto.setId(properties.getFirst().getId());
        matchConfigReqDto.setName(properties.getFirst().getConfigName());
        properties.forEach(property -> {
            switch (property.getPropName()){
                case "initialCoins":
                    matchConfigReqDto.setInitialCoins(Integer.parseInt(property.getPropValue()));
                    break;
                case "firstPlayerRandomlyChosen":
                    matchConfigReqDto.setFirstPlayerRandomlyChosen(Boolean.parseBoolean(property.getPropValue()));
                    break;
                case "taxedMoney":
                    matchConfigReqDto.setTaxedMoney(Integer.parseInt(property.getPropValue()));
                    break;
                case "taxRate":
                    matchConfigReqDto.setTaxRate(Integer.parseInt(property.getPropValue()));
                    break;
                case "bigExpeditionMinimumPlayersNumber":
                    matchConfigReqDto.setBigExpeditionMinimumPlayersNumber(Integer.parseInt(property.getPropValue()));
                    break;
                case "JOMC_ExpansionUsed":
                    matchConfigReqDto.setJOMC_ExpansionUsed(Boolean.parseBoolean(property.getPropValue()));
                    break;
                case "cargoCoinToPoorestPlayer":
                    matchConfigReqDto.setCargoCoinToPoorestPlayer(Boolean.parseBoolean(property.getPropValue()));
                    break;
                case "contractsCardNumber":
                    matchConfigReqDto.setContractsCardNumber(Integer.parseInt(property.getPropValue()));
                    break;
                case "maxContractsCompletablePerPlayer":
                    matchConfigReqDto.setMaxContractsCompletablePerPlayer(Integer.parseInt(property.getPropValue()));
                    break;
            }
        });
        return matchConfigReqDto;
    }

    public static List<ConfigPropertyEntity> toList(MatchConfigReqDto matchConfigReqDto){
        String name = matchConfigReqDto.getName();
        Integer configId = matchConfigReqDto.getId();
        return List.of(
                new ConfigPropertyEntity(configId, name, "initialCoins", String.valueOf(matchConfigReqDto.getInitialCoins())),
                new ConfigPropertyEntity(configId, name, "firstPlayerRandomlyChosen", String.valueOf(matchConfigReqDto.getFirstPlayerRandomlyChosen())),
                new ConfigPropertyEntity(configId, name, "taxedMoney", String.valueOf(matchConfigReqDto.getTaxedMoney())),
                new ConfigPropertyEntity(configId, name, "taxRate", String.valueOf(matchConfigReqDto.getTaxRate())),
                new ConfigPropertyEntity(configId, name, "bigExpeditionMinimumPlayersNumber", String.valueOf(matchConfigReqDto.getBigExpeditionMinimumPlayersNumber())),
                new ConfigPropertyEntity(configId, name, "JOMC_ExpansionUsed", String.valueOf(matchConfigReqDto.getJOMC_ExpansionUsed())),
                new ConfigPropertyEntity(configId, name, "cargoCoinToPoorestPlayer", String.valueOf(matchConfigReqDto.getCargoCoinToPoorestPlayer())),
                new ConfigPropertyEntity(configId, name, "contractsCardNumber", String.valueOf(matchConfigReqDto.getContractsCardNumber())),
                new ConfigPropertyEntity(configId, name, "maxContractsCompletablePerPlayer", String.valueOf(matchConfigReqDto.getMaxContractsCompletablePerPlayer()))
        );
    }
}
