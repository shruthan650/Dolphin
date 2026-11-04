package com.dolphin.validation;

import java.util.Locale;
import java.util.regex.Pattern;

public final class EmailAddress {

    private static final Pattern COLLEGE_EMAIL = Pattern.compile(
            "^[0-9]MT[0-9]{2}[A-Z]{2}[0-9]{3}@mite\\.ac\\.in$", Pattern.CASE_INSENSITIVE);
    private static final Pattern USN_LOCAL_PART = Pattern.compile(
            "^[0-9]MT[0-9]{2}[A-Z]{2}[0-9]{3}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern GENERIC_EMAIL = Pattern.compile(
            "^(?=.{1,254}$)(?=.{1,64}@)[A-Z0-9!#$%&'*+/=?^_`{|}~-]+"
                    + "(?:\\.[A-Z0-9!#$%&'*+/=?^_`{|}~-]+)*@"
                    + "[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?"
                    + "(?:\\.[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?)+$",
            Pattern.CASE_INSENSITIVE);

    private EmailAddress() {
    }

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        String email = value.trim();
        if (!GENERIC_EMAIL.matcher(email).matches()) {
            return false;
        }

        int at = email.lastIndexOf('@');
        String localPart = email.substring(0, at);
        String domain = email.substring(at + 1);
        if (domain.equalsIgnoreCase("mite.ac.in")) {
            return COLLEGE_EMAIL.matcher(email).matches();
        }
        return !USN_LOCAL_PART.matcher(localPart).matches();
    }

    public static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
