package com.cadence.authservice.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Enforced centrally as a bean-validation constraint (rather than ad-hoc
 * checks in the service layer) so every DTO that accepts a password
 * automatically gets the same policy, and the rule is documented once.
 */
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final Pattern SPECIAL = Pattern.compile("[^A-Za-z0-9]");

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.length() < 8) {
            return false;
        }
        return UPPERCASE.matcher(password).find()
                && LOWERCASE.matcher(password).find()
                && DIGIT.matcher(password).find()
                && SPECIAL.matcher(password).find();
    }
}
