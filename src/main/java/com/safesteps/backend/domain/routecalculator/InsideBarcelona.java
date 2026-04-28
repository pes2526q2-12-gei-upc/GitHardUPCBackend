package com.safesteps.backend.domain.routecalculator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = InsideBarcelonaValidator.class)
public @interface InsideBarcelona {

    String message() default "La coordenada debe estar dentro de los límites de Barcelona";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
