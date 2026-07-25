package com.tanle.t_shorten_url.mapper;

import com.tanle.t_shorten_url.entity.ShortAnalyzed;
import com.tanle.t_shorten_url.event.ClickEvent;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ShortAnalyzedMapper {

    ShortAnalyzed convertToEntity(ClickEvent clickEvent);
}
