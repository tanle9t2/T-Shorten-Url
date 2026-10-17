package com.tanle.t_shorten_url.service;

import com.tanle.t_shorten_url.entity.ShortUrl;
import com.tanle.t_shorten_url.repository.AnalyticsRepository;
import com.tanle.t_shorten_url.repository.AnalyticsRepository.ClickCount;
import com.tanle.t_shorten_url.repository.ShortUrlRepository;
import com.tanle.t_shorten_url.response.AnalyticsResponse;
import com.tanle.t_shorten_url.service.impl.AnalyticsServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalyticsServiceTest {
    private final ShortUrlRepository shortUrlRepository = mock(ShortUrlRepository.class);
    private final AnalyticsRepository analyticsRepository = mock(AnalyticsRepository.class);
    private final AnalyticsService service = new AnalyticsServiceImpl(shortUrlRepository, analyticsRepository);

    private static final LocalDate TODAY = LocalDate.now(ZoneOffset.UTC);

    private static ShortUrl link(String code, LocalDate createdDay) {
        return ShortUrl.builder().id("id-" + code).shortCode(code).originalUrl("https://x/" + code)
                .createdAt(createdDay.atStartOfDay().toInstant(ZoneOffset.UTC)).build();
    }

    @Test
    void emptyAccountReturnsZerosAndFullSeries() {
        when(shortUrlRepository.findAllByUserId("u")).thenReturn(List.of());

        AnalyticsResponse response = service.getAnalytics("u", 7, null, null, "UTC");

        assertEquals(7, response.activitySeries().size());
        assertEquals(0, response.summary().current().totalClicks());
        assertEquals(0, response.summary().current().averageClicks());
        assertTrue(response.topLinks().isEmpty());
    }

    @Test
    void aggregatesCurrentAndPreviousRange() {
        when(shortUrlRepository.findAllByUserId("u")).thenReturn(List.of(link("a", TODAY), link("b", TODAY.minusDays(30))));
        when(analyticsRepository.countClicksByDay(any(), any(), any(), any())).thenReturn(List.of(
                new ClickCount("a", TODAY.toString(), 3),
                new ClickCount("b", TODAY.toString(), 1),
                new ClickCount("b", TODAY.minusDays(8).toString(), 5)));

        AnalyticsResponse response = service.getAnalytics("u", 7, null, null, "UTC");

        assertEquals(4, response.summary().current().totalClicks());
        assertEquals(2, response.summary().current().linksWithClicks());
        assertEquals(2.0, response.summary().current().averageClicks());
        assertEquals(1, response.summary().current().linksCreated());
        assertEquals(5, response.summary().previous().totalClicks());
        assertEquals(4, response.activitySeries().get(6).clicks());
        assertEquals(1, response.activitySeries().get(6).linksCreated());
        assertEquals("a", response.topLinks().get(0).shortCode());
        assertEquals(0.75, response.topLinks().get(0).share());
    }
}
