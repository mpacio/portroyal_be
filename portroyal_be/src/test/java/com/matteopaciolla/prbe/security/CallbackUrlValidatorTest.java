package com.matteopaciolla.prbe.security;

import com.matteopaciolla.prbe.exceptions.common.MandatoryParamException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CallbackUrlValidatorTest {

    @Test
    void productionAllowsPublicHttpsAndRejectsPrivateOrPlainHttpTargets() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        CallbackUrlValidator validator = new CallbackUrlValidator(environment);

        assertThatCode(() -> validator.validate("https://8.8.8.8/alerts")).doesNotThrowAnyException();
        assertThatThrownBy(() -> validator.validate("http://8.8.8.8/alerts"))
                .isInstanceOf(MandatoryParamException.class);
        assertThatThrownBy(() -> validator.validate("https://127.0.0.1/alerts"))
                .isInstanceOf(MandatoryParamException.class);
    }

    @Test
    void developmentAllowsLocalHttpCallbackEndpoints() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");
        CallbackUrlValidator validator = new CallbackUrlValidator(environment);

        assertThatCode(() -> validator.validate("http://localhost:8080/callback"))
                .doesNotThrowAnyException();
    }
}
