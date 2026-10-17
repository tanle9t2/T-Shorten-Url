package com.tanle.t_shorten_url.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserAgentClassifierTest {
    private static final String CHROME_WIN = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final String EDGE_WIN = CHROME_WIN + " Edg/120.0.0.0";
    private static final String OPERA_WIN = CHROME_WIN + " OPR/105.0.0.0";
    private static final String FIREFOX_LINUX = "Mozilla/5.0 (X11; Linux x86_64; rv:121.0) Gecko/20100101 Firefox/121.0";
    private static final String SAFARI_MAC = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 "
            + "(KHTML, like Gecko) Version/17.0 Safari/605.1.15";
    private static final String CHROME_ANDROID = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";
    private static final String SAFARI_IPHONE = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) "
            + "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1";

    @Test
    void browserChecksSpecificFamiliesBeforeChromeAndSafari() {
        assertEquals("Chrome", UserAgentClassifier.browser(CHROME_WIN));
        assertEquals("Edge", UserAgentClassifier.browser(EDGE_WIN));
        assertEquals("Opera", UserAgentClassifier.browser(OPERA_WIN));
        assertEquals("Firefox", UserAgentClassifier.browser(FIREFOX_LINUX));
        assertEquals("Safari", UserAgentClassifier.browser(SAFARI_MAC));
        assertEquals("Chrome", UserAgentClassifier.browser(CHROME_ANDROID));
        assertEquals("Other", UserAgentClassifier.browser("curl/8.0"));
    }

    @Test
    void osChecksMobileBeforeDesktopFamilies() {
        assertEquals("Windows", UserAgentClassifier.os(CHROME_WIN));
        assertEquals("Linux", UserAgentClassifier.os(FIREFOX_LINUX));
        assertEquals("macOS", UserAgentClassifier.os(SAFARI_MAC));
        assertEquals("Android", UserAgentClassifier.os(CHROME_ANDROID));
        assertEquals("iOS", UserAgentClassifier.os(SAFARI_IPHONE));
        assertEquals("Other", UserAgentClassifier.os("curl/8.0"));
    }

    @Test
    void missingUserAgentIsUnknown() {
        assertEquals("Unknown", UserAgentClassifier.browser(null));
        assertEquals("Unknown", UserAgentClassifier.os(" "));
    }
}
