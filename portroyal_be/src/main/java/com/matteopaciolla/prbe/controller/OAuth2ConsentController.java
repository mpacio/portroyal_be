package com.matteopaciolla.prbe.controller;

import com.matteopaciolla.prbe.exceptions.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class OAuth2ConsentController {

    private final RegisteredClientRepository registeredClientRepository;

    @GetMapping("/oauth2/consent")
    public String consent(
            @RequestParam("client_id") String clientId,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam("scope") String scopes,
            Model model) {
        RegisteredClient client = registeredClientRepository.findByClientId(clientId);
        if (client == null) {
            throw new ResourceNotFoundException("The OAuth client could not be found");
        }

        List<String> requestedScopes = Arrays.stream(scopes.split(" "))
                .filter(scope -> !scope.isBlank())
                .toList();
        model.addAttribute("clientName", client.getClientName());
        model.addAttribute("clientId", clientId);
        model.addAttribute("state", state);
        model.addAttribute("scopes", requestedScopes);
        return "oauth2-consent";
    }
}
