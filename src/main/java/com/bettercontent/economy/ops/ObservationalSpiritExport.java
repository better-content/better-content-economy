package com.bettercontent.economy.ops;

import com.bettercontent.economy.spirit.CurrencyIdentity;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Permissioned aggregate export of physical releases and completed trades. */
public final class ObservationalSpiritExport {
    private static final int MAX_ROWS = 256;

    public record Exchange(CurrencyIdentity identity, int paid, String merchant) {
        public Exchange {
            Objects.requireNonNull(identity);
            if (paid <= 0) throw new IllegalArgumentException("paid must be positive");
            merchant = merchant == null ? "unknown" : merchant;
        }
    }

    private ObservationalSpiritExport() {}

    public static String csv(boolean enabled, int permission, Map<CurrencyIdentity, Integer> released,
                             List<Exchange> exchanges) {
        if (!allowed(enabled, permission) || !bounded(released, exchanges)) return "";
        StringBuilder out = new StringBuilder("kind,identity,value,merchant\n");
        for (CurrencyIdentity identity : CurrencyIdentity.values()) {
            int count = released.getOrDefault(identity, 0);
            if (count > 0) out.append("released,").append(identity.id()).append(',').append(count).append(",\n");
        }
        for (Exchange exchange : exchanges) {
            out.append("exchange,").append(exchange.identity().id()).append(',').append(exchange.paid())
                    .append(',').append(csv(exchange.merchant())).append('\n');
        }
        return out.toString();
    }

    public static String json(boolean enabled, int permission, Map<CurrencyIdentity, Integer> released,
                              List<Exchange> exchanges) {
        return json(enabled, permission, released, exchanges, 0L, Map.of(), Map.of());
    }

    public static String json(boolean enabled, int permission, Map<CurrencyIdentity, Integer> released,
                              List<Exchange> exchanges, long activity,
                              Map<String, Map<CurrencyIdentity,Integer>> regional,
                              Map<CurrencyIdentity,Integer> purchases) {
        if (!allowed(enabled, permission) || !bounded(released, exchanges)) return "";
        StringBuilder out = new StringBuilder("{\"schema\":3,\"activity\":").append(Math.max(0, activity))
                .append(",\"released\":").append(valuesJson(released)).append(",\"regionalMix\":");
        if (regional.isEmpty()) out.append("\"unknown\""); else {
            out.append('{'); int regions = 0;
            for (var entry : regional.entrySet()) {
                if (regions++ >= 64) break;
                if (regions > 1) out.append(',');
                out.append('"').append(json(entry.getKey())).append("\":").append(valuesJson(entry.getValue()));
            }
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
    private static boolean bounded(Map<?, ?> released, List<?> exchanges) {
        return released != null && exchanges != null && released.size() <= CurrencyIdentity.values().length
                && exchanges.size() <= MAX_ROWS;
    }
    private static String csv(String value) { return value.contains(",") || value.contains("\"") ? "\"" + value.replace("\"", "\"\"") + "\"" : value; }
    private static String json(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
    private static String valuesJson(Map<CurrencyIdentity, Integer> input) {
        EnumMap<CurrencyIdentity, Integer> values = new EnumMap<>(CurrencyIdentity.class);
        input.forEach((identity, count) -> { if (identity != null && count != null && count > 0) values.put(identity, count); });
        StringBuilder out = new StringBuilder("{"); int count = 0;
        for (CurrencyIdentity identity : CurrencyIdentity.values()) {
            Integer value = values.get(identity);
            if (value == null) continue;
            if (count++ > 0) out.append(',');
            out.append('"').append(identity.id()).append("\":").append(value);
        }
        return out.append('}').toString();
    }
}
