package com.tanle.t_shorten_url.service.impl;

import com.tanle.t_shorten_url.entity.UrlHistory;
import com.tanle.t_shorten_url.repository.UrlHistoryRepository;
import com.tanle.t_shorten_url.service.UrlHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlHistoryServiceImpl implements UrlHistoryService {

    private final UrlHistoryRepository urlHistoryRepository;

    @Override
    public List<UrlHistory> getHistoryByShortUrlId(String shortUrlId) {
        log.info("Getting history for shortUrlId: {}", shortUrlId);
        return urlHistoryRepository.findUrlHistoriesByUrlShortId(shortUrlId);
    }

    @Override
    public UrlHistory save(UrlHistory urlHistory) {
        return urlHistoryRepository.save(urlHistory);
    }
}
