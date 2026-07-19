package com.tanle.t_shorten_url.service;

import com.tanle.t_shorten_url.entity.UrlHistory;
import java.util.List;

public interface UrlHistoryService {
    List<UrlHistory> getHistoryByShortUrlId(String shortUrlId);
    UrlHistory save(UrlHistory urlHistory);
}
