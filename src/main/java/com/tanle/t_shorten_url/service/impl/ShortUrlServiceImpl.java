package com.tanle.t_shorten_url.service.impl;

import com.tanle.t_shorten_url.cache.DistributeLockService;
import com.tanle.t_shorten_url.cache.ShortUrlCacheService;
import com.tanle.t_shorten_url.entity.ShortUrl;
import com.tanle.t_shorten_url.event.ClickEvent;
import com.tanle.t_shorten_url.exception.ResourceNotFoundException;
import com.tanle.t_shorten_url.kafka.ShortUrlProducer;
import com.tanle.t_shorten_url.mapper.ShortUrlMapper;
import com.tanle.t_shorten_url.projection.TotalViewProjection;
import com.tanle.t_shorten_url.repository.ShortUrlRepository;
import com.tanle.t_shorten_url.request.ShortUrlCreatedRequest;
import com.tanle.t_shorten_url.request.ShortUrlUpdateRequest;
import com.tanle.t_shorten_url.response.ShortUrlDetailResponse;
import com.tanle.t_shorten_url.response.ShortUrlResponse;
import com.tanle.t_shorten_url.response.TotalViewUrlResponse;
import com.tanle.t_shorten_url.service.QRCodeService;
import com.tanle.t_shorten_url.service.ShortUrlService;
import com.tanle.t_shorten_url.util.ShortCodeGenerator;
import com.tanle.t_shorten_url.util.SnowflakeIdGenerator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.tanle.t_shorten_url.response.PageResponse;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.tanle.t_shorten_url.util.AppConstant.DEFAULT_SORT_DIRECTION;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShortUrlServiceImpl implements ShortUrlService {

    private final ShortUrlRepository shortUrlRepository;
    private final DistributeLockService distributeLockService;
    private final ShortUrlCacheService shortUrlCacheService;
    private final ShortUrlMapper shortUrlMapper;
    private final ShortUrlProducer shortUrlProducer;
    private final QRCodeService qrCodeService;
    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();
    @Value("${app.domain}")
    private String PREFIX_APP;

    @Override
    public ShortUrlResponse findByShortUrl(String code) {
        log.info("Finding ShortUrl by shortCode: {}", code);
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Not found " + code));

        return shortUrlMapper.convertToResponse(shortUrl);
    }

    @Override
    public ShortUrlDetailResponse findById(String id) {
        log.info("Finding ShortUrl by id: {}", id);
        ShortUrl shortUrl = shortUrlRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Not found " + id));

        ShortUrlDetailResponse shortUrlDetailResponse = shortUrlMapper.convertToDetailResponse(shortUrl);
        shortUrlDetailResponse.setShortCode(formatShorUrl(shortUrlDetailResponse.getShortCode()));
        return shortUrlDetailResponse;
    }

    @Override
    public String findRedirectUrl(String code, HttpServletRequest request) throws InterruptedException {
        log.info("Finding redirect by shortCode: {}", code);
        Optional<String> cacheUrl = shortUrlCacheService.getShortUrl(code);
        if (cacheUrl.isPresent()) {
            String url = cacheUrl.get();
            if (url.isEmpty()) {
                throw new ResourceNotFoundException("Not found " + code);
            }
            log.info("Hit cache for shortCode: {}", code);
            this.publishMessage(code, request);
            return url;
        }

        log.info("Missing cache for shortCode: {}", code);

        //Avoid bottleneck. Ex: 10 request have different code, thought they must be wait if we use Object for lock
        //Instead of locking by code, it helps to decrease bottleneck
        //However this way can solve problem when we have multiple instances,
        //because each instance has individually JVM -> if requests same key that are distributed to different instance,
        // they still query db
//        Object lock = locks.computeIfAbsent(code, (newCode) -> new Object());

//        synchronized (lock) {
//            Optional<String> reChecked = shortUrlCacheService.getShortUrl(code);
//            if (reChecked.isPresent()) {
//                String url = cacheUrl.get();
//                if (url.isEmpty()) {
//                    throw new ResourceNotFoundExeption("Not found " + code);
//                }
//                return url;
//            }
//            shortUrl = shortUrlRepository.findByShortCode(code)
//                    .or(() -> {
//                        //Avoid cache Penetration. EX: Attacker spam many code not exit, -> query directly DB
//                        shortUrlCacheService.cacheNotFound(code);
//                        return null;
//                    })
//                    .orElseThrow(() -> new ResourceNotFoundExeption("Not found " + code));
//        }
        String key = "url:" + code;
        boolean lock = distributeLockService.acquireLock(key);
        //Avoid Cache Breakdown. EX: 1 hot key expired when all request access to key -> query DB
        if (lock) {
            try {
                ShortUrl shortUrl = shortUrlRepository.findByShortCode(code)
                        .or(() -> {
                            //Avoid cache Penetration. EX: Attacker spam many code not exit, -> query directly DB
                            shortUrlCacheService.cacheNotFound(code);
                            return null;
                        })
                        .orElseThrow(() -> new ResourceNotFoundException("Not found " + code));
                shortUrlCacheService.createShortUrl(shortUrl.getShortCode(), shortUrl.getOriginalUrl());
                publishMessage(code, request);
                return shortUrl.getOriginalUrl();
            } finally {
                distributeLockService.releaseLock(key);
            }
        }

        //Waiting Another instance build cache
        // Cons: URL exists, but cache building takes longer than expected.
        Thread.sleep(500);
        cacheUrl = shortUrlCacheService.getShortUrl(code);
        if (!cacheUrl.isPresent()) {
            throw new ResourceNotFoundException("Not found " + code);
        }

        String url = cacheUrl.get();
        if (url.isEmpty()) {
            throw new ResourceNotFoundException("Not found " + code);
        }

        this.publishMessage(code, request);
        return url;
    }

    private void publishMessage(String code, HttpServletRequest request) {
        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        String referer = Optional.ofNullable(request.getHeader("Referer")).orElse("Unknow");

        ClickEvent clickEvent = ClickEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .shortCode(code)
                .ipAddress(ipAddress)
                .createdAt(Instant.now())
                .userAgent(userAgent)
                .referer(referer)
                .build();

        shortUrlProducer.publishMessage(clickEvent);
    }

    private String formatShorUrl(String code) {
        return String.format("%s/%s", PREFIX_APP, code);
    }

    @Override
    public ShortUrlResponse save(ShortUrlCreatedRequest shortUrl) {
        log.info("Saving ShortUrl: {}", shortUrl.getOriginalUrl());

        Long id = SnowflakeIdGenerator.nextId();
        String code = ShortCodeGenerator.encode(id);
        String formatCode = formatShorUrl(code);

        ShortUrl urlEntity = ShortUrl.builder()
                .originalUrl(shortUrl.getOriginalUrl())
                .id(String.valueOf(id))
                .shortCode(code)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .userId("1")
                .isActive(true)
                .build();
        if (shortUrl.isWithQRCode()) {
            String qrCode = qrCodeService.generateQrCode(formatCode);
            urlEntity.setQrCode(qrCode);
        }

        shortUrlRepository.save(urlEntity);
        ShortUrlResponse shortUrlResponse = ShortUrlResponse.builder()
                .id(urlEntity.getId())
                .shortCode(formatCode)
                .originalUrl(urlEntity.getOriginalUrl())
                .createdAt(urlEntity.getCreatedAt())
                .build();
        return shortUrlResponse;
    }

    @Override
    public void deleteShortUrlById(String id) {
        ShortUrl shortUrlEntity = shortUrlRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Not found " + id));

        shortUrlEntity.setActive(false);
        shortUrlRepository.save(shortUrlEntity);
    }

    @Override
    public TotalViewUrlResponse getTotalView(String code) {
        TotalViewProjection totalViewProjection = shortUrlRepository.findTotalViews(code)
                .orElseThrow(() -> new ResourceNotFoundException("Not found " + code));

        Long redisCounter = this.shortUrlCacheService.getView(code);
        TotalViewUrlResponse totalViewUrlResponse = this.shortUrlMapper.convertTotalView(totalViewProjection);
        totalViewUrlResponse.setViews(totalViewUrlResponse.getViews() + redisCounter);

        return totalViewUrlResponse;
    }

    @Override
    public void updateShortUrl(ShortUrlUpdateRequest shortUrl) {
        log.info("Update ShortUrl: {}", shortUrl.getOriginalUrl());
        ShortUrl shortUrlEntity = shortUrlRepository.findById(shortUrl.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Not found " + shortUrl.getId()));

        Long id = SnowflakeIdGenerator.nextId();
        String code = ShortCodeGenerator.encode(id);
        shortUrlEntity.setShortCode(code);

        shortUrlRepository.save(shortUrlEntity);
        this.shortUrlCacheService.invalidCache(code);
    }

    @Override
    public PageResponse<ShortUrlResponse> findByUserId(String userId, int page, int size, String sortBy, String order) {
        log.info("Finding ShortUrls by userId: {}", userId);

        Sort.Direction direction = order.equalsIgnoreCase(DEFAULT_SORT_DIRECTION) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortObj = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        Page<ShortUrl> shortUrlPage = shortUrlRepository.findByUserId(userId, pageable);

        List<ShortUrlResponse> content = shortUrlPage.getContent().stream()
                .map(shortUrlMapper::convertToResponse)
                .map(response -> {
                    response.setShortCode(formatShorUrl(response.getShortCode()));
                    return response;
                })
                .collect(Collectors.toList());

        return PageResponse.<ShortUrlResponse>builder()
                .content(content)
                .page(shortUrlPage.getNumber())
                .size(shortUrlPage.getSize())
                .totalElements(shortUrlPage.getTotalElements())
                .totalPages(shortUrlPage.getTotalPages())
                .last(shortUrlPage.isLast())
                .build();
    }
}
