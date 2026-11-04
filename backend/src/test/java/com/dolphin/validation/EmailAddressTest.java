package com.dolphin.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailAddressTest {

    @Test
    void acceptsCollegeAndGenericEmailAddressesCaseInsensitively() {
        assertThat(EmailAddress.isValid("4MT24CS001@mite.ac.in")).isTrue();
        assertThat(EmailAddress.isValid("4MT24AE001@MITE.AC.IN")).isTrue();
        assertThat(EmailAddress.isValid("student@gmail.com")).isTrue();
        assertThat(EmailAddress.isValid("teacher@outlook.com")).isTrue();
    }

    @Test
    void rejectsMalformedCollegeAndGenericEmailAddresses() {
        assertThat(EmailAddress.isValid("4MT24C001@mite.ac.in")).isFalse();
        assertThat(EmailAddress.isValid("4MT24CS001@mite.com")).isFalse();
        assertThat(EmailAddress.isValid("missing-domain")).isFalse();
        assertThat(EmailAddress.isValid("student@mite.ac.in")).isFalse();
        assertThat(EmailAddress.isValid("student@@gmail.com")).isFalse();
    }

    @Test
    void trimsAndNormalizesEmailAddresses() {
        assertThat(EmailAddress.isValid("  4mt24ai001@MITE.AC.IN  ")).isTrue();
        assertThat(EmailAddress.normalize("  4MT24AI001@MITE.AC.IN  "))
                .isEqualTo("4mt24ai001@mite.ac.in");
    }
}
