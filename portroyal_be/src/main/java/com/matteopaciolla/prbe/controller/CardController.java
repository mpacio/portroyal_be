package com.matteopaciolla.prbe.controller;

import com.matteopaciolla.portroyal.confs.BaseDeckDictionary;
import com.matteopaciolla.portroyal.confs.JOMC_ExpansionDeckDictionary;
import com.matteopaciolla.portroyal.core.cards.Card;
import com.matteopaciolla.portroyal.core.cards.contracts.abst.ContractCard;
import com.matteopaciolla.prbe.constants.Paths;
import com.matteopaciolla.prbe.exceptions.common.ResourceNotFoundException;
import com.matteopaciolla.prbe.converter.CardConverter;
import com.matteopaciolla.prbe.converter.ContractCardConverter;
import com.matteopaciolla.prbe.dto.CardDto;
import com.matteopaciolla.prbe.dto.ContractCardDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "Cards", description = "Card operations")
@SecurityRequirements({@SecurityRequirement(name = "basicAuth")})
@Slf4j
@RestController
public class CardController {

    @Operation(summary = "Get all cards", description = "Get all cards from the game",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of cards")})
    @GetMapping(path = Paths.CARD_PATH, produces = "application/json")
    public ResponseEntity<List<CardDto>> getAllCards() {
        List<Card> cards = new ArrayList<>(BaseDeckDictionary.DECK_LIST);
        cards.addAll(JOMC_ExpansionDeckDictionary.DECK_LIST);
        List<CardDto> cardDtos = new ArrayList<>();
        cards.forEach(card -> cardDtos.add(CardConverter.toDto(card)));
        return ResponseEntity.ok(cardDtos);
    }

    @Operation(summary = "Get all contract cards", description = "Get all contract cards from the game",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of contract cards")})
    @GetMapping(path = Paths.CONTRACT_CARDS_PATH, produces = "application/json")
    public ResponseEntity<List<ContractCardDto>> getContractCards() {
        List<ContractCard> contractCards = JOMC_ExpansionDeckDictionary.CONTRACTS_DECK_LIST;
        List<ContractCardDto> contractCardDtos = new ArrayList<>();
        contractCards.forEach(contractCard -> contractCardDtos.add(ContractCardConverter.toDto(contractCard)));
        return ResponseEntity.ok(contractCardDtos);
    }

    @Operation(summary = "Get card by id", description = "Get card by id from the game",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Card found",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CardDto.class))),
                    @ApiResponse(responseCode = "404", description = "Card not found",
                            content = @Content(mediaType = "application/json"))})
    @GetMapping(path = Paths.CARD_PATH + "/{id}", produces = "application/json")
    public ResponseEntity<CardDto> getCardById(@PathVariable int id) {
        Card card = BaseDeckDictionary.DECK_MAP.get(id);
        if (card == null) {
            card = JOMC_ExpansionDeckDictionary.DECK_MAP.get(id);
        }
        if (card == null) {
            throw new ResourceNotFoundException("Card with id " + id + " was not found.");
        }
        return ResponseEntity.ok(CardConverter.toDto(card));
    }

    @Operation(summary = "Get contract card by id", description = "Get contract card by id from the game",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Contract card found"),
                    @ApiResponse(responseCode = "404", description = "Contract card not found")})
    @GetMapping(path = Paths.CONTRACT_CARDS_PATH + "/{id}", produces = "application/json")
    public ResponseEntity<ContractCardDto> getContractCardById(@PathVariable int id) {
        ContractCard contractCard = JOMC_ExpansionDeckDictionary.CONTRACTS_DECK_MAP.get(id);
        if (contractCard == null) {
            throw new ResourceNotFoundException("Contract card with id " + id + " was not found.");
        }
        return ResponseEntity.ok(ContractCardConverter.toDto(contractCard));
    }
}
