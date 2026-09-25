package com.matteopaciolla.prbe.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.matteopaciolla.portroyal.core.cards.contracts.abst.*;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContractCardDto {

    private Integer id;
    private ContractCardType type;
    private String icon;
    private String name;
    private String description;
    private String profession1;
    private String profession2;
    private List<String> park1;
    private List<String> park2;
    private List<String> spots;
    private int[] rewards;

    public enum ContractCardType {
        AUTOMATIC,
        MANUAL,
        AUTOMATIC_RENOUNCING,
        MANUAL_TWO_PROFESSIONS;

        public static ContractCardType fromClass(ContractCard classType) {
            return switch (classType) {
                case RenouncingContractCard ignored -> AUTOMATIC_RENOUNCING;
                case TwoProfessionsContractCard ignored -> MANUAL_TWO_PROFESSIONS;
                case AutomaticContractCard ignored -> AUTOMATIC;
                case ManualContractCard ignored -> MANUAL;
                default -> throw new IllegalArgumentException("Invalid contract card type: " + classType.getClass().getSimpleName());
            };
        }
    }
}
