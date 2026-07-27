package com.cadence.authservice.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StrongPasswordValidator.class)
@Documented
public @interface StrongPassword {
    String message() default "Password must be at least 8 characters and include an uppercase letter, a lowercase letter, a digit, and a special character";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
