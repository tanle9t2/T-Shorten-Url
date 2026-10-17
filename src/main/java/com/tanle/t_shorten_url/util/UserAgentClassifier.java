package com.tanle.t_shorten_url.util;

public final class UserAgentClassifier {
    public static final String UNKNOWN = "Unknown";
    private static final String OTHER = "Other";

    private UserAgentClassifier() {
    }

    // shortcut: coarse families only, no versions or bots. Upgrade to a parser library when exact data is needed (TD-003).
    public static String browser(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return UNKNOWN;
        }
        // Order matters: Edge and Opera also announce "Chrome", and Chrome also announces "Safari".
        if (contains(userAgent, "Edg/", "Edge/", "EdgA/", "EdgiOS/")) {
            return "Edge";
        }
        if (contains(userAgent, "OPR/", "Opera")) {
            return "Opera";
        }
        if (contains(userAgent, "Firefox/", "FxiOS/")) {
            return "Firefox";
        }
        if (contains(userAgent, "Chrome/", "CriOS/")) {
            return "Chrome";
        }
        if (userAgent.contains("Safari/")) {
            return "Safari";
        }
        return OTHER;
    }

    public static String os(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return UNKNOWN;
        }
        // Android and iOS UAs also contain "Linux" / "Mac OS X", so they must be checked first.
        if (userAgent.contains("Android")) {
            return "Android";
        }
        if (contains(userAgent, "iPhone", "iPad", "iPod")) {
            return "iOS";
        }
        if (userAgent.contains("Windows")) {
            return "Windows";
        }
        if (contains(userAgent, "Macintosh", "Mac OS X")) {
            return "macOS";
        }
        if (contains(userAgent, "Linux", "X11")) {
            return "Linux";
        }
        return OTHER;
    }

    private static boolean contains(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
