package com.tanle.t_shorten_url.service;

import com.tanle.t_shorten_url.entity.ShortUrl;
import com.tanle.t_shorten_url.exception.ResourceNotFoundException;
import com.tanle.t_shorten_url.repository.AnalyticsRepository;
import com.tanle.t_shorten_url.repository.AnalyticsRepository.ClickCount;
import com.tanle.t_shorten_url.repository.AnalyticsRepository.ValueCount;
import com.tanle.t_shorten_url.repository.ShortUrlRepository;
import com.tanle.t_shorten_url.response.ShortCodeAnalyticsResponse;
import com.tanle.t_shorten_url.response.ShortCodeAnalyticsResponse.Share;
import com.tanle.t_shorten_url.service.impl.AnalyticsServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShortCodeAnalyticsServiceTest {
    private static final LocalDate TODAY = LocalDate.now(ZoneOffset.UTC);
    private static final String CHROME_WIN = "Mozilla/5.0 (Windows NT 10.0) Chrome/120.0 Safari/537.36";

    private final ShortUrlRepository shortUrlRepository = mock(ShortUrlRepository.class);
    private final AnalyticsRepository analyticsRepository = mock(AnalyticsRepository.class);
    private final AnalyticsService service = new AnalyticsServiceImpl(shortUrlRepository, analyticsRepository);

    private void ownedLink() {
        when(shortUrlRepository.findByShortCode("abc")).thenReturn(Optional.of(
                ShortUrl.builder().id("1").shortCode("abc").originalUrl("https://x").userId("u").build()));
        when(analyticsRepository.countClicksByDay(any(), any(), any(), any())).thenReturn(List.of());
        when(analyticsRepository.countClicksBy(any(), any(), any(), any())).thenReturn(List.of());
    }

    private ShortCodeAnalyticsResponse call() {
        return service.getShortCodeAnalytics("u", "abc", 7, null, null, "UTC");
    }

    @Test
    void unknownOrForeignCodeIsNotFound() {
        when(shortUrlRepository.findByShortCode("abc")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, this::call);

        when(shortUrlRepository.findByShortCode("abc")).thenReturn(Optional.of(
                ShortUrl.builder().shortCode("abc").userId("other").build()));
        assertThrows(ResourceNotFoundException.class, this::call);
    }

    @Test
    void emptyRangeReturnsZeroFilledSeriesAndEmptyBreakdowns() {
        ownedLink();

        ShortCodeAnalyticsResponse response = call();

        assertEquals(7, response.clickSeries().size());
        assertEquals(0, response.summary().current().totalClicks());
        assertTrue(response.browsers().isEmpty());
        assertTrue(response.referers().isEmpty());
    }

    @Test
    void totalsSplitBetweenCurrentAndPreviousRange() {
        ownedLink();
        when(analyticsRepository.countClicksByDay(any(), any(), any(), any())).thenReturn(List.of(
                new ClickCount("abc", TODAY.toString(), 3),
                new ClickCount("abc", TODAY.minusDays(7).toString(), 5)));

        ShortCodeAnalyticsResponse response = call();

        assertEquals(3, response.summary().current().totalClicks());
        assertEquals(5, response.summary().previous().totalClicks());
        assertEquals(3, response.clickSeries().get(6).clicks());
    }

    @Test
    void userAgentsAreMergedIntoBrowserAndOs() {
        ownedLink();
        when(analyticsRepository.countClicksByDay(any(), any(), any(), any())).thenReturn(List.of(
                new ClickCount("abc", TODAY.toString(), 5)));
        when(analyticsRepository.countClicksBy(eq("userAgent"), any(), any(), any())).thenReturn(List.of(
                new ValueCount(CHROME_WIN, 3), new ValueCount(CHROME_WIN + " x", 1), new ValueCount(null, 1)));

        ShortCodeAnalyticsResponse response = call();

        assertEquals(new Share("Chrome", 4, 0.8), response.browsers().get(0));
        assertEquals(new Share("Windows", 4, 0.8), response.os().get(0));
        assertEquals("Unknown", response.browsers().get(1).name());
    }

    @Test
    void referersCollapseToHostAndDirect() {
        ownedLink();
        when(analyticsRepository.countClicksByDay(any(), any(), any(), any())).thenReturn(List.of(
                new ClickCount("abc", TODAY.toString(), 6)));
        when(analyticsRepository.countClicksBy(eq("referer"), any(), any(), any())).thenReturn(List.of(
                new ValueCount("https://www.google.com/search?q=a", 2),
                new ValueCount("https://www.google.com/other", 1),
                new ValueCount("Unknow", 2),
                new ValueCount(null, 1)));

        List<Share> referers = call().referers();

        assertEquals(List.of("direct", "www.google.com"), referers.stream().map(Share::name).toList());
        assertEquals(3, referers.get(0).clicks());
    }

    @Test
    void breakdownFoldsTailIntoOther() {
        ownedLink();
        when(analyticsRepository.countClicksByDay(any(), any(), any(), any())).thenReturn(List.of(
                new ClickCount("abc", TODAY.toString(), 7)));
        when(analyticsRepository.countClicksBy(eq("country"), any(), any(), any())).thenReturn(
                IntStream.rangeClosed(1, 7).mapToObj(i -> new ValueCount("C" + i, 1)).toList());

        List<Share> countries = call().countries();

        assertEquals(6, countries.size());
        assertEquals(new Share("Other", 2, 2.0 / 7), countries.get(5));
        assertEquals(7, countries.stream().mapToLong(Share::clicks).sum());
    }

    @Test
    void foldedTailMergesIntoExistingOtherRow() {
        ownedLink();
        when(analyticsRepository.countClicksByDay(any(), any(), any(), any())).thenReturn(List.of(
                new ClickCount("abc", TODAY.toString(), 10)));
        when(analyticsRepository.countClicksBy(eq("userAgent"), any(), any(), any())).thenReturn(List.of(
                new ValueCount("curl/8", 4), new ValueCount("wget", 1),
                new ValueCount("Mozilla/5.0 (Windows NT 10.0) Chrome/1 Safari/1", 1),
                new ValueCount("Mozilla/5.0 (Macintosh) Safari/1", 1),
                new ValueCount("Mozilla/5.0 (X11; Linux) Firefox/1", 1),
                new ValueCount("Mozilla/5.0 (Windows NT 10.0) Edg/1", 1),
                new ValueCount("Mozilla/5.0 (Windows NT 10.0) OPR/1", 1)));

        List<Share> browsers = call().browsers();

        assertEquals(1, browsers.stream().filter(b -> b.name().equals("Other")).count());
        assertEquals(10, browsers.stream().mapToLong(Share::clicks).sum());
    }

    @Test
    void refererHostIsLowercased() {
        ownedLink();
        when(analyticsRepository.countClicksBy(eq("referer"), any(), any(), any())).thenReturn(List.of(
                new ValueCount("https://WWW.Google.com/a", 1), new ValueCount("https://www.google.com/b", 1)));

        assertEquals(List.of(new Share("www.google.com", 2, 0)), call().referers());
    }

    @Test
    void rejectedCodeNeverQueriesAnalytics() {
        when(shortUrlRepository.findByShortCode("abc")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, this::call);

        verify(analyticsRepository, never()).countClicksByDay(any(), any(), any(), any());
        verify(analyticsRepository, never()).countClicksBy(any(), any(), any(), any());
    }
}
