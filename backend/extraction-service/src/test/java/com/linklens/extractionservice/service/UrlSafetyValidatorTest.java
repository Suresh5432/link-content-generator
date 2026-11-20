package com.linklens.extractionservice.service;

import com.linklens.extractionservice.exception.UnsafeUrlException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UrlSafetyValidatorTest {
    private final UrlSafetyValidator validator = new UrlSafetyValidator();

    @Test void rejectsNonHttpSchemesAndCredentials() {
        assertThrows(UnsafeUrlException.class, () -> validator.validate("file:///etc/passwd"));
        assertThrows(UnsafeUrlException.class, () -> validator.validate("http://user:pass@example.com/"));
    }

    @Test void rejectsLocalAndPrivateAddressTargets() {
        assertThrows(UnsafeUrlException.class, () -> validator.validate("http://localhost/"));
        assertThrows(UnsafeUrlException.class, () -> validator.validate("http://127.0.0.1/"));
        assertThrows(UnsafeUrlException.class, () -> validator.validate("http://169.254.169.254/latest/meta-data/"));
        assertThrows(UnsafeUrlException.class, () -> validator.validate("http://service.internal/"));
    }

    @Test void rejectsCustomPortsAndCarrierGradeNat() {
        assertThrows(UnsafeUrlException.class, () -> validator.validate("http://example.com:8080/"));
        assertThrows(UnsafeUrlException.class, () -> validator.validate("http://host.test/"));
    }
}
