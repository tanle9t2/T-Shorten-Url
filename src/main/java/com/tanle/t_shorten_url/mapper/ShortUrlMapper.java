package com.tanle.t_shorten_url.mapper;


import com.tanle.t_shorten_url.entity.ShortUrl;
import com.tanle.t_shorten_url.event.ClickEvent;
import com.tanle.t_shorten_url.projection.TotalViewProjection;
import com.tanle.t_shorten_url.request.ShortUrlRequest;
import com.tanle.t_shorten_url.response.ShortUrlResponse;
import com.tanle.t_shorten_url.response.TotalViewUrlResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ShortUrlMapper {
    ShortUrlResponse convertToResponse(ShortUrl shortUrl);

    ClickEvent convertToClickEvent(ShortUrlRequest shortUrlRequest);

    TotalViewUrlResponse convertTotalView(TotalViewProjection totalViewProjection);
}
