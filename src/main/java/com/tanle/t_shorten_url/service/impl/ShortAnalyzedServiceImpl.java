package com.tanle.t_shorten_url.service.impl;

import com.tanle.t_shorten_url.entity.ShortAnalyzed;
import com.tanle.t_shorten_url.event.ClickEvent;
import com.tanle.t_shorten_url.mapper.ShortAnalyzedMapper;
import com.tanle.t_shorten_url.repository.ShortAnalyzedRepository;
import com.tanle.t_shorten_url.service.ShortAnalyzedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShortAnalyzedServiceImpl implements ShortAnalyzedService {

    private final ShortAnalyzedRepository shortAnalyzedRepository;
    private final ShortAnalyzedMapper shortAnalyzedMapper;

    @Override
    public List<ShortAnalyzed> getAnalyticsByShortUrlId(String shortCode) {
        log.info("Getting analytics for shortUrlId: {}", shortCode);
        return shortAnalyzedRepository.findByShortCode(shortCode);
    }

    @Override
    public ShortAnalyzed save(ShortAnalyzed shortAnalyzed) {
        return shortAnalyzedRepository.save(shortAnalyzed);
    }

    @Override
    @Transactional
    public void analyzeShortUrl(ClickEvent clickEvent) {
        log.info("Creating analytics info for shortUrlId: {}", clickEvent.shortCode());
        ShortAnalyzed shortAnalyzed = shortAnalyzedMapper.convertToEntity(clickEvent);

        shortAnalyzedRepository.save(shortAnalyzed);
    }
}
