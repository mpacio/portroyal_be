package com.matteopaciolla.prbe.model;

import com.matteopaciolla.prbe.constants.enums.SentinelAlertMessage;
import com.matteopaciolla.prbe.dto.AlertDto;
import com.matteopaciolla.prbe.model.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SentinelSubscription {

    @EqualsAndHashCode.Include
    private UserEntity user;

    private SseEmitter emitter;
    private DeferredResult<AlertDto> deferredResult;
    private String keyCode;

    public SentinelSubscription(UserEntity user, String keyCode, SseEmitter emitter, DeferredResult<AlertDto> deferredResult) {
        this.user = user;
        this.keyCode = keyCode;
        this.emitter = emitter;
        this.deferredResult = deferredResult;
    }

    public void stop() {
        if (emitter != null) {
            emitter.complete();
        }
        if (deferredResult != null) {
            AlertDto alert = new AlertDto(SentinelAlertMessage.SUBSCRIPTION_REMOVED, keyCode);
            deferredResult.setErrorResult(alert);
        }
    }

    public String toString() {
        String userString = user.getUsername() + " (" + user.getId() + ")";
        String subType = emitter != null ? "SSE" : "Long Polling";
        return userString + " - " + subType;
    }
}
