package com.matteopaciolla.prbe.constants.enums;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public enum SentinelAlertMessage {

    SUBSCRIPTION_CREATED(1001, "Subscription created"),
    SUBSCRIPTION_REMOVED(1002, "Subscription removed"),
    SUBSCRIPTION_ERROR(  1004, "Subscription removed by error"),
    SUBSCRIPTION_TIMEOUT(1003, "Subscription removed by timeout"),

    MATCH_STARTED(1101, "Match has started"),
    MATCH_ENDED(  1102, "Match has ended"),
    PLAYER_JOINED(1103, "A player joined the match"),
    MOVES_UPDATED(1104, "A player made a move"),
    YOUR_TURN(    1105, "It's your turn"),
    MATCH_CLOSED( 1106, "Match has been closed"),
    PLAYER_LEFT(  1107, "A player left the match");


    private final int code;
    private final String message;

    SentinelAlertMessage(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
