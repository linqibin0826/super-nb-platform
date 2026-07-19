package me.supernb.activity.adapter.rest.request;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CreateRaffleCampaignRequest(String name, Instant entryOpenAt, Instant entryCloseAt, Instant drawAt,
        String gateType, BigDecimal gateAmount, Instant gateFrom, Integer minAccountAgeDays, String weightMode,
        List<PrizeSkeleton> prizes) {

    public record PrizeSkeleton(String tier, String displayName, String kind, int sortOrder) {}
}
