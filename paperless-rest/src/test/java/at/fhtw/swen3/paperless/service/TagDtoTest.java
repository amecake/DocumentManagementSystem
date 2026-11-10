package at.fhtw.swen3.paperless.service;

import at.fhtw.swen3.paperless.service.dto.TagDto;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TagDtoTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void nameWithSlash_isInvalid() {
        assertFalse(validator.validate(new TagDto("a/b")).isEmpty());
    }

    @Test
    void nameWithBackslash_isInvalid() {
        assertFalse(validator.validate(new TagDto("a\\b")).isEmpty());
    }

    @Test
    void plainName_isValid() {
        assertTrue(validator.validate(new TagDto("finance")).isEmpty());
    }
}
