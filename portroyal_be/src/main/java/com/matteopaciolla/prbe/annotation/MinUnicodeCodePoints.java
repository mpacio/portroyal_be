package com.matteopaciolla.prbe.annotation;

import com.matteopaciolla.prbe.validator.MinUnicodeCodePointsValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = MinUnicodeCodePointsValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface MinUnicodeCodePoints {
    String message() default "must contain at least the minimum number of Unicode code points";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int value();
}
