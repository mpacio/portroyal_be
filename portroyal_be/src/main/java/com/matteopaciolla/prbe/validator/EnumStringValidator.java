package com.matteopaciolla.prbe.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import com.matteopaciolla.prbe.annotation.EnumString;

public class EnumStringValidator implements ConstraintValidator<EnumString, String>
{
    private EnumString annotation;

    @Override
    public void initialize(EnumString annotation) {
        this.annotation = annotation;
    }

    @Override
    public boolean isValid(String valueForValidation, ConstraintValidatorContext constraintValidatorContext) {
        if(valueForValidation == null) {
            return true;
        }
        boolean result = false;

        Object[] enumValues = this.annotation.enumClass().getEnumConstants();

        if(enumValues != null) {
            for(Object enumValue:enumValues) {
                if(valueForValidation.equals(enumValue.toString())
                        || (this.annotation.ignoreCase() && valueForValidation.equalsIgnoreCase(enumValue.toString()))) {
                    result = true;
                    break;
                }
            }
        }

        return result;
    }
}