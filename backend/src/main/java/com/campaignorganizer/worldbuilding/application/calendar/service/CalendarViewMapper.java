package com.campaignorganizer.worldbuilding.application.calendar.service;

import com.campaignorganizer.worldbuilding.application.calendar.port.published.CalendarMonthView;
import com.campaignorganizer.worldbuilding.application.calendar.port.published.CalendarView;
import com.campaignorganizer.worldbuilding.domain.calendar.Calendar;
import com.campaignorganizer.worldbuilding.domain.calendar.Month;
import org.mapstruct.Mapper;

/** Maps the domain calendar to the published read model (MapStruct). */
@Mapper(componentModel = "spring")
public interface CalendarViewMapper {

    CalendarView toView(Calendar calendar);

    CalendarMonthView toMonthView(Month month);
}
