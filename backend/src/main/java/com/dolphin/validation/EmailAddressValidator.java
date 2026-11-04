package com.dolphin.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EmailAddressValidator implements ConstraintValidator<ValidEmailAddress, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.isBlank() || EmailAddress.isValid(value);
    }
}
