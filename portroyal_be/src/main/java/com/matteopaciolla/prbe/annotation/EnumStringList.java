package com.matteopaciolla.prbe.annotation;

import com.matteopaciolla.prbe.validator.EnumStringListValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.util.Arrays;

@Documented
@Constraint(validatedBy = {EnumStringListValidator.class})
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface EnumStringList
{
    public abstract String message() default "Invalid value in at least one of the elements. Please check the list of permitted values.";

    public abstract Class<?>[] groups() default {};

    public abstract Class<? extends Payload>[] payload() default {};

    public abstract Class<? extends Enum<?>> enumClass();

    public abstract boolean ignoreCase() default false;
}