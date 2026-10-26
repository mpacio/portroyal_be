package com.matteopaciolla.prbe.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

import static org.assertj.core.api.Assertions.assertThat;

class DevAccountSeedConfigurationTest {

    @Test
    void accountSeeder_isRestrictedToDevProfile() {
        Profile profile = DevAccountSeedConfiguration.class.getAnnotation(Profile.class);

        assertThat(profile).isNotNull();
        assertThat(profile.value()).containsExactly("dev");
    }
}
