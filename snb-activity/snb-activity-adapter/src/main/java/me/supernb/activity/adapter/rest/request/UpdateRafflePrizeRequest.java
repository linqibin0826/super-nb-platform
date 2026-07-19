package me.supernb.activity.adapter.rest.request;

public record UpdateRafflePrizeRequest(String tier, String displayName, String kind, String payload, int sortOrder) {}
