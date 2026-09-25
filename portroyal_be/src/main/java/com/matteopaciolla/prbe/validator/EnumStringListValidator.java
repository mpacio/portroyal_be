package com.matteopaciolla.prbe.validator;

import com.matteopaciolla.prbe.annotation.EnumString;
import com.matteopaciolla.prbe.annotation.EnumStringList;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;

public class EnumStringListValidator implements ConstraintValidator<EnumStringList, List<String>>
{
    private EnumStringList annotation;

    @Override
    public void initialize(EnumStringList annotation) {
        this.annotation = annotation;
    }

    @Override
    public boolean isValid(List<String> valueForValidationList, ConstraintValidatorContext constraintValidatorContext) {
        if(valueForValidationList == null) {
            return true;
        }
        boolean result = false;

        Object[] enumValues = this.annotation.enumClass().getEnumConstants();

        if(enumValues != null) {
            for(String value : valueForValidationList) {
                boolean found = false;
                for(Object enumValue:enumValues) {
                    if(value.equals(enumValue.toString())
                            || (this.annotation.ignoreCase() && value.equalsIgnoreCase(enumValue.toString()))) {
                        found = true;
                        break;
                    }
                }
                if(!found) {
                    return false;
                }
            }
            result = true;
        }

        return result;
    }
}