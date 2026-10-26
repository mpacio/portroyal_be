package com.matteopaciolla.prbe.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MoveHistoryIdentityServiceTest {

    @Test
    void usernamePatternReplacesWholePlayerNameWithoutChangingSimilarNames() {
        String notes = "alice42 paid a fee; alice42-ai-1 hired a captain.";

        String result = MoveHistoryIdentityService.usernamePattern("alice42")
                .matcher(notes)
                .replaceAll("deleted-player");

        assertThat(result).isEqualTo("deleted-player paid a fee; alice42-ai-1 hired a captain.");
    }
}
