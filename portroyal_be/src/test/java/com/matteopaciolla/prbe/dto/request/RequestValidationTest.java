package com.matteopaciolla.prbe.dto.request;

import com.matteopaciolla.prbe.controller.GameController;
import com.matteopaciolla.prbe.controller.MatchController;
import com.matteopaciolla.prbe.dto.request.MatchConfigReqDto;
import com.matteopaciolla.prbe.dto.request.AddAiPlayerReqDto;
import com.matteopaciolla.prbe.repository.MatchRepository;
import com.matteopaciolla.prbe.repository.MoveRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.executable.ExecutableValidator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class RequestValidationTest {

    private static Validator validator;
    private static ValidatorFactory validatorFactory;
    private static ExecutableValidator executableValidator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
        executableValidator = validator.forExecutables();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void userPayloadRejectsInvalidFieldFormatsAndLengths() {
        UserReqDto request = validUserRequest();
        request.setEmail("not-an-email");
        request.setTelegramId("123456789012345678901");
        request.setFirstName("Alice1");
        request.setPassword("€".repeat(25));

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("email", "telegramId", "firstName", "password");
    }

    @Test
    void userPayloadAcceptsPasswordAtTheBcryptByteLimit() {
        UserReqDto request = validUserRequest();
        request.setPassword("€".repeat(24));

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void passwordsRequireAtLeastEightUnicodeCodePoints() {
        UserReqDto registration = validUserRequest();
        registration.setPassword("😀".repeat(7));
        assertThat(validator.validate(registration))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("password"));

        registration.setPassword("😀".repeat(8));
        assertThat(validator.validate(registration)).isEmpty();

        ChangePasswordReqDto change = new ChangePasswordReqDto();
        change.setOldPassword("current-password");
        change.setNewPassword("short7");
        assertThat(validator.validate(change))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("newPassword"));
    }

    @Test
    void passwordChangeRequiresBothPasswordsAndEnforcesBcryptByteLimit() {
        ChangePasswordReqDto request = new ChangePasswordReqDto();
        request.setOldPassword("current-password");
        request.setNewPassword("€".repeat(25));

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("newPassword");
    }

    @Test
    void passwordResetPayloadsValidateEmailTokenAndNewPassword() {
        PasswordResetRequestDto request = new PasswordResetRequestDto();
        request.setEmail("invalid");
        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("email"));

        PasswordResetConfirmRequestDto confirm = new PasswordResetConfirmRequestDto();
        confirm.setToken(" ");
        confirm.setNewPassword("short7");
        assertThat(validator.validate(confirm))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("token", "newPassword");
    }

    @Test
    void matchConfigurationAndAiNamesRespectStorageBounds() {
        MatchConfigReqDto configuration = new MatchConfigReqDto();
        configuration.setId(-1);
        configuration.setName("x".repeat(256));
        assertThat(validator.validate(configuration))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("id", "name");

        AddAiPlayerReqDto aiRequest = new AddAiPlayerReqDto();
        aiRequest.setDifficulty(com.matteopaciolla.portroyal.core.enums.BotDifficulty.EASY);
        aiRequest.setName("x".repeat(256));
        assertThat(validator.validate(aiRequest))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("name"));
    }

    @Test
    void matchAndMovePaginationRejectsOutOfRangePageSizes() throws Exception {
        MatchController matchController = new MatchController(null, null, null);
        Method matchesMethod = MatchController.class.getMethod(
                "getAllMatches",
                Boolean.class,
                Integer.class,
                Integer.class,
                MatchRepository.SortField.class,
                Sort.Direction.class,
                String.class);
        assertThat(executableValidator.validateParameters(
                matchController,
                matchesMethod,
                new Object[]{null, 0, 101, MatchRepository.SortField.CREATED_AT, Sort.Direction.DESC, null}))
                .anyMatch(violation -> violation.getPropertyPath().toString().contains("pageSize"));

        GameController gameController = new GameController();
        Method movesMethod = GameController.class.getMethod(
                "getMoves",
                String.class,
                Integer.class,
                Integer.class,
                MoveRepository.SortField.class,
                Sort.Direction.class);
        assertThat(executableValidator.validateParameters(
                gameController,
                movesMethod,
                new Object[]{"ABC123", -1, 10, MoveRepository.SortField.TIME_INDEX, Sort.Direction.ASC}))
                .anyMatch(violation -> violation.getPropertyPath().toString().contains("pageNumber"));
    }

    private static UserReqDto validUserRequest() {
        UserReqDto request = new UserReqDto();
        request.setUsername("alice42");
        request.setEmail("alice@example.com");
        request.setPassword("a-valid-password");
        return request;
    }
}
