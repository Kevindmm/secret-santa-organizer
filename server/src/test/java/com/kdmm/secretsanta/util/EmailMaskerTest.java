package com.kdmm.secretsanta.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class EmailMaskerTest {

    @Test
    void masksMostOfTheLocalPart() {
        assertEquals("an***@test.com", EmailMasker.mask("ana@test.com"));
        assertEquals("ab***@test.com", EmailMasker.mask("ab@test.com"));
        assertEquals("a***@test.com", EmailMasker.mask("a@test.com"));
    }

    @Test
    void keepsTheDomainVisible() {
        assertEquals("an***@sub.domain.com", EmailMasker.mask("ana@sub.domain.com"));
    }

    @Test
    void neverReturnsTheFullAddress() {
        String masked = EmailMasker.mask("persona.larga@empresa.com");

        assertFalse(masked.contains("persona.larga"));
        assertEquals("pe***@empresa.com", masked);
    }

    @Test
    void trimsSurroundingSpaces() {
        assertEquals("an***@test.com", EmailMasker.mask("  ana@test.com  "));
    }

    @Test
    void handlesNullOrMalformedValuesWithoutLeaking() {
        assertNull(EmailMasker.mask(null));
        assertEquals("***", EmailMasker.mask("no-email"));
        assertEquals("***", EmailMasker.mask("a@b@c.com"));
        assertEquals("***", EmailMasker.mask("@test.com"));
        assertEquals("***", EmailMasker.mask("ana@"));
    }
}
