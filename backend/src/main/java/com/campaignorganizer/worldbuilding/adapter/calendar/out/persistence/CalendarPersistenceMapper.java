package com.campaignorganizer.worldbuilding.adapter.calendar.out.persistence;

import com.campaignorganizer.worldbuilding.domain.calendar.Month;
import org.mapstruct.Mapper;

/** Maps calendar scalars/months between domain and persistence (MapStruct). */
@Mapper(componentModel = "spring")
public interface CalendarPersistenceMapper {

    /** Calendar scalar fields only; months live in a separate table (handled by the adapter). */
    CalendarJpaEntity toEntity(com.campaignorganizer.worldbuilding.domain.calendar.Calendar calendar);

    Month toMonth(CalendarMonthJpaEntity entity);
}
