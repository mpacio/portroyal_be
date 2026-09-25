package com.matteopaciolla.prbe.exceptions.common;

import com.matteopaciolla.prbe.constants.CommonConstants;

import java.util.List;

public class MandatoryBotParamException extends MandatoryParamException {

        public MandatoryBotParamException() {
            super("Bot must provide mandatory header in order to access this endpoint",
                    List.of(CommonConstants.BOT_MANDATORY_HEADER));
        }
}
