package com.campaignorganizer.worldbuilding.domain.calendar;

import com.campaignorganizer.shared.domain.ValidationException;

/** A month in a custom calendar (value object). */
public record Month(String name, int days) {

    public Month {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Month name must not be blank");
        }
        if (days <= 0) {
            throw new ValidationException("A month must have a positive number of days");
        }
    }
}
