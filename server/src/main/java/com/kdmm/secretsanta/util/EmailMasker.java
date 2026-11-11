package com.kdmm.secretsanta.util;

/**
 * Masks email addresses before they are exposed in API responses or logs.
 * Only the first two characters of the local part and the domain are kept.
 */
public final class EmailMasker {

    private EmailMasker() {}

    public static String mask(String email) {
        if (email == null) {
            return null;
        }

        String value = email.trim();
        int at = value.indexOf('@');
        if (at <= 0 || at == value.length() - 1 || at != value.lastIndexOf('@')) {
            return "***";
        }

        String local = value.substring(0, at);
        String domain = value.substring(at);

        if (local.length() == 1) {
            return local.charAt(0) + "***" + domain;
        }

        return local.substring(0, 2) + "***" + domain;
    }
}
