package com.eventease.eventservice.repository;

import java.math.BigDecimal;

public interface TicketTypeAvailability {

    Long getId();

    String getName();

    BigDecimal getPrice();

    Integer getTotalQuantity();

    Integer getAvailableQuantity();

    Long getEventId();
}
