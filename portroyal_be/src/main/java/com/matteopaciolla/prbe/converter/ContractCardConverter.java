package com.matteopaciolla.prbe.converter;

import com.matteopaciolla.portroyal.core.Match;
import com.matteopaciolla.portroyal.core.Player;
import com.matteopaciolla.portroyal.core.cards.contracts.abst.ContractCard;
import com.matteopaciolla.portroyal.core.cards.contracts.abst.TwoProfessionsContractCard;
import com.matteopaciolla.portroyal.core.cards.contracts.impl.FrigateNemesis;
import com.matteopaciolla.portroyal.core.cards.contracts.impl.GalleonNemesis;
import com.matteopaciolla.prbe.dto.ContractCardDto;

import java.util.LinkedList;
import java.util.List;

public class ContractCardConverter {

    public static ContractCardDto toDto(ContractCard contractCard, Match match) {
        ContractCardDto dto = new ContractCardDto();
        dto.setId(contractCard.getId());
        dto.setType(ContractCardDto.ContractCardType.fromClass(contractCard));
        dto.setIcon(contractCard.getIcon());
        dto.setName(contractCard.getName());
        dto.setDescription(contractCard.getDescription());
        if (contractCard instanceof TwoProfessionsContractCard twoProfessionsContractCard) {
            dto.setProfession1(twoProfessionsContractCard.getProfession1Name());
            dto.setProfession2(twoProfessionsContractCard.getProfession2Name());
        }
        if (match != null) {
            if (contractCard instanceof GalleonNemesis galleonNemesis) {
                dto.setPark1(GalleonNemesis.getPark1PlayersNames(match));
            }
            if (contractCard instanceof FrigateNemesis frigateNemesis) {
                dto.setPark1(FrigateNemesis.getPark1PlayersNames(match));
                dto.setPark2(FrigateNemesis.getPark2PlayersNames(match));
            }
            dto.setSpots(match.getTable().getContractsBoard().getSignedPlayers(contractCard).stream().map(Player::getName).toList());
        }
        dto.setRewards(contractCard.getRewards());
        return dto;
    }

    public static ContractCardDto toDto(ContractCard contractCard) {
        return toDto(contractCard, null);
    }

    public static List<ContractCardDto> toDtoList(List<ContractCard> contractCards, Match match) {
        List<ContractCardDto> cardDtos = new LinkedList<>();
        contractCards.forEach(contractCard -> cardDtos.add(toDto(contractCard, match)));
        return cardDtos;
    }

    public static List<ContractCardDto> toDtoList(List<ContractCard> contractCards) {
        return toDtoList(contractCards, null);
    }
}
