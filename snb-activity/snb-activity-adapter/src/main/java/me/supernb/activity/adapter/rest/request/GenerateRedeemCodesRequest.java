package me.supernb.activity.adapter.rest.request;

public record GenerateRedeemCodesRequest(String tier, String displayName, long groupId, int validityDays,
        int count, int sortOrderStart) {}
