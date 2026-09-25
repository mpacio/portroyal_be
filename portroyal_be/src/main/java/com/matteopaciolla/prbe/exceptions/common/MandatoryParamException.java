package com.matteopaciolla.prbe.exceptions.common;

import com.matteopaciolla.prbe.exceptions.BaseClientCausedException;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class MandatoryParamException extends BaseClientCausedException {

    private List<String> missingParams;
    private String description;

    public MandatoryParamException() {
        super("Mandatory parameter missing");
        this.missingParams = null;
    }

    public MandatoryParamException(String description) {
        this();
        this.description = description;
    }

    public MandatoryParamException(List<String> missingParams) {
        this();
        this.missingParams = missingParams;
    }

    public MandatoryParamException(String description, List<String> missingParams) {
        this();
        this.description = description;
        this.missingParams = missingParams;
    }

    @Override
    public String getDescription() {
        if (this.description != null) {
            return this.description;
        } else if (this.missingParams != null && !this.missingParams.isEmpty()) {
            return "Mandatory parameters missing: " + String.join(", ", this.missingParams);
        } else {
            return super.getDescription();
        }
    }

    @Override
    public Map<String, String> getDetails() {
        if (this.missingParams != null) {
            return missingParams.stream().collect(Collectors.toMap(param -> param, param -> "missing"));
        } else {
            return null;
        }
    }
}
