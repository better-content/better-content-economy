package com.bettercontent.economy.trader;

import java.util.UUID;

/** Server side checks shared by a future local market packet; never locates or creates merchants. */
public final class LocalMarketValidation {
    public record Request(UUID merchant, String offerKey, int price, double distance) {}
    public record Offer(String key, int price, int remaining) {}
    private LocalMarketValidation() {}
    public static boolean accepts(Request request, UUID actualMerchant, Offer current, double maxDistance) {
        return request != null && actualMerchant != null && current != null
                && actualMerchant.equals(request.merchant())
                && current.key().equals(request.offerKey())
                && current.price() == request.price()
                && current.remaining() > 0
                && request.distance() >= 0 && request.distance() <= maxDistance;
    }
}
