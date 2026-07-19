package me.supernb.activity.adapter.rest.request;

import java.math.BigDecimal;
import java.time.Instant;

public record UpdateRaffleCampaignRequest(String name, Instant entryOpenAt, Instant entryCloseAt, Instant drawAt,
        String gateType, BigDecimal gateAmount, Instant gateFrom, Integer minAccountAgeDays, String weightMode) {}
