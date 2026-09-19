package com.bettercontent.economy.ops;

import com.bettercontent.economy.spirit.CurrencyIdentity;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Small, opt-in owner export for aggregate economy observations.  Inputs are snapshots of
 * authoritative ledgers and successful trade events; this class never derives players, motives,
 * or exchanges from inventory state.
 */
public final class ObservationalSpiritExport {
    private static final int MAX_ROWS = 256;
    public record CreditSnapshot(Map<CurrencyIdentity, Integer> issued,
                                 Map<CurrencyIdentity, Integer> pending) {
        public CreditSnapshot {
            issued = positive(issued);
            pending = positive(pending);
        }
    }

    /** A trade observed after vanilla accepted it.  No player identity is retained. */
    public record Exchange(CurrencyIdentity identity, int paid, String merchant) {
        public Exchange {
            Objects.requireNonNull(identity);
            if (paid <= 0) throw new IllegalArgumentException("paid must be positive");
            merchant = merchant == null ? "unknown" : merchant;
        }
    }

    private ObservationalSpiritExport() {}

    public static String csv(boolean enabled, int permission, List<CreditSnapshot> credits,
                             List<Exchange> exchanges) {
        if (!allowed(enabled, permission) || !bounded(credits, exchanges)) return "";
        StringBuilder out = new StringBuilder("kind,identity,value,merchant\n")
                .append("activity,aggregate,").append(credits.size()).append(",\n")
                .append("regional_mix,unknown,,\n")
                .append("purchases,unknown,,\n");
        for (CreditSnapshot snapshot : credits) {
            for (CurrencyIdentity identity : CurrencyIdentity.values()) {
                int issued = snapshot.issued().getOrDefault(identity, 0);
                int pending = snapshot.pending().getOrDefault(identity, 0);
                if (issued > 0) out.append("issued,").append(identity.id()).append(',').append(issued).append(",\n");
                if (pending > 0) out.append("pending,").append(identity.id()).append(',').append(pending).append(",\n");
            }
        }
        for (Exchange exchange : exchanges) {
            out.append("exchange,").append(exchange.identity().id()).append(',').append(exchange.paid())
                    .append(',').append(csv(exchange.merchant())).append('\n');
        }
        return out.toString();
    }

    public static String json(boolean enabled, int permission, List<CreditSnapshot> credits,
                              List<Exchange> exchanges) {
        return json(enabled, permission, credits, exchanges, Map.of(), Map.of());
    }
    public static String json(boolean enabled, int permission, List<CreditSnapshot> credits,
                              List<Exchange> exchanges, Map<String, Map<CurrencyIdentity,Integer>> regional,
                              Map<CurrencyIdentity,Integer> purchases) {
        return json(enabled, permission, credits, exchanges, 0L, regional, purchases);
    }
    public static String json(boolean enabled, int permission, List<CreditSnapshot> credits,
                              List<Exchange> exchanges, long activity,
                              Map<String, Map<CurrencyIdentity,Integer>> regional,
                              Map<CurrencyIdentity,Integer> purchases) {
        if (!allowed(enabled, permission) || !bounded(credits, exchanges)) return "";
        long issued = credits.stream().mapToLong(c -> total(c.issued())).sum();
        long pending = credits.stream().mapToLong(c -> total(c.pending())).sum();
        StringBuilder out = new StringBuilder("{\"activity\":").append(Math.max(0, activity))
                .append(",\"issued\":").append(issued)
                .append(",\"pending\":").append(pending)
                .append(",\"regionalMix\":");
        if (regional.isEmpty()) out.append("\"unknown\""); else {
            out.append('{'); int regions = 0;
            for (var entry : regional.entrySet()) { if (regions++ >= 64) break; if (regions > 1) out.append(','); out.append('"').append(json(entry.getKey())).append("\":").append(valuesJson(entry.getValue())); }
            out.append('}');
        }
        out.append(",\"purchases\":").append(purchases.isEmpty() ? "\"unknown\"" : valuesJson(purchases))
                .append(",\"exchanges\":[");
        for (int i = 0; i < exchanges.size(); i++) {
            if (i > 0) out.append(',');
            Exchange e = exchanges.get(i);
            out.append("{\"identity\":\"").append(e.identity().id()).append("\",\"paid\":")
                    .append(e.paid()).append(",\"merchant\":\"").append(json(e.merchant())).append("\"}");
        }
        return out.append("]}").toString();
    }

    private static boolean allowed(boolean enabled, int permission) { return enabled && permission >= 2; }
    private static boolean bounded(List<?> credits, List<?> exchanges) {
        return credits != null && exchanges != null && credits.size() <= MAX_ROWS && exchanges.size() <= MAX_ROWS;
    }
    private static int total(Map<CurrencyIdentity, Integer> values) { return values.values().stream().mapToInt(Integer::intValue).sum(); }
    private static Map<CurrencyIdentity, Integer> positive(Map<CurrencyIdentity, Integer> input) {
        EnumMap<CurrencyIdentity, Integer> result = new EnumMap<>(CurrencyIdentity.class);
        if (input != null) input.forEach((k, v) -> { if (k != null && v != null && v > 0) result.put(k, v); });
        return Map.copyOf(result);
    }
    private static String csv(String value) { return value.contains(",") || value.contains("\"") ? "\"" + value.replace("\"", "\"\"") + "\"" : value; }
    private static String json(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
    private static String valuesJson(Map<CurrencyIdentity, Integer> values) {
        StringBuilder out = new StringBuilder("{"); int count = 0;
        for (CurrencyIdentity identity : CurrencyIdentity.values()) {
            Integer value = values.get(identity); if (value == null || value <= 0) continue;
            if (count++ > 0) out.append(','); out.append('"').append(identity.id()).append("\":").append(Math.min(value, Integer.MAX_VALUE));
        }
        return out.append('}').toString();
    }
}
