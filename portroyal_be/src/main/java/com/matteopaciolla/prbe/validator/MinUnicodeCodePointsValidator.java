package com.matteopaciolla.prbe.validator;

import com.matteopaciolla.prbe.annotation.MinUnicodeCodePoints;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MinUnicodeCodePointsValidator implements ConstraintValidator<MinUnicodeCodePoints, String> {

    private int minCodePoints;

    @Override
    public void initialize(MinUnicodeCodePoints constraintAnnotation) {
        minCodePoints = constraintAnnotation.value();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.codePointCount(0, value.length()) >= minCodePoints;
    }
}
