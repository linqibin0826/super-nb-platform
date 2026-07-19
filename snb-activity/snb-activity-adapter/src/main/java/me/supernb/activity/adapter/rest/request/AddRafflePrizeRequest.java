package me.supernb.activity.adapter.rest.request;

public record AddRafflePrizeRequest(String tier, String displayName, String kind, String payload, int sortOrder) {}
