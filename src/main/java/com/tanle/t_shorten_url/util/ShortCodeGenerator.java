package com.tanle.t_shorten_url.util;

import org.springframework.stereotype.Component;

/**
 * Short Code Generator using Base62 Encoding.
 * Converts a base-10 number (like a Snowflake ID) into a Base62 string.
 */
public class ShortCodeGenerator {

    // 62 characters: 0-9, a-z, A-Z
    private static final String BASE62_CHARACTERS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = BASE62_CHARACTERS.length();

    /**
     * Encodes a given long ID into a Base62 string.
     *
     * @param id The generated Snowflake ID (or any positive long)
     * @return Base62 encoded string
     */
    public static String encode(long id) {
        if (id == 0) {
            return String.valueOf(BASE62_CHARACTERS.charAt(0));
        }

        StringBuilder shortCode = new StringBuilder();

        while (id > 0) {
            int remainder = (int) (id % BASE);
            shortCode.append(BASE62_CHARACTERS.charAt(remainder));
            id = id / BASE;
        }

        // Reverse the string because the division method produces characters in reverse order
        return shortCode.reverse().toString();
    }
}
