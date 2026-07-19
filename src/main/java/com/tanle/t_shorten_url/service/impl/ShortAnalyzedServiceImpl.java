package com.tanle.t_shorten_url.service.impl;

import com.tanle.t_shorten_url.entity.ShortAnalyzed;
import com.tanle.t_shorten_url.repository.ShortAnalyzedRepository;
import com.tanle.t_shorten_url.service.ShortAnalyzedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShortAnalyzedServiceImpl implements ShortAnalyzedService {
    
    private final ShortAnalyzedRepository shortAnalyzedRepository;
    
    @Override
    public List<ShortAnalyzed> getAnalyticsByShortUrlId(String shortUrlId) {
        log.info("Getting analytics for shortUrlId: {}", shortUrlId);
        return shortAnalyzedRepository.findByShortUrlId(shortUrlId);
    }
    
    @Override
    public ShortAnalyzed save(ShortAnalyzed shortAnalyzed) {
        return shortAnalyzedRepository.save(shortAnalyzed);
    }
}
