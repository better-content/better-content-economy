package com.bettercontent.economy.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class EconomyConfig {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.BooleanValue RECURRING_VISITS;
    private static final ForgeConfigSpec.IntValue INITIAL_DELAY;
    private static final ForgeConfigSpec.IntValue VISIT_INTERVAL;
    private static final ForgeConfigSpec.IntValue RETRY_DELAY;
    private static final ForgeConfigSpec.BooleanValue ANNOUNCE_ARRIVAL;
    private static final ForgeConfigSpec.BooleanValue OBSERVATIONAL_EXPORT;

    static {
        var builder = new ForgeConfigSpec.Builder();
        builder.push("wanderingTrader");
        RECURRING_VISITS = builder.define("recurringVisits", true);
        INITIAL_DELAY = builder.defineInRange("initialDelay", 48_000, 0, Integer.MAX_VALUE);
        VISIT_INTERVAL = builder.defineInRange("visitInterval", 120_000, 1_200, Integer.MAX_VALUE);
        RETRY_DELAY = builder.defineInRange("retryDelay", 1_200, 20, 24_000);
        ANNOUNCE_ARRIVAL = builder.define("announceArrival", true);
        builder.pop();
        builder.push("observationalExport");
        OBSERVATIONAL_EXPORT = builder.define("enabled", false);
        builder.pop();
        SPEC = builder.build();
    }

    private EconomyConfig() {}
    public static boolean wanderingTraderRecurringVisits() { return RECURRING_VISITS.get(); }
    public static int wanderingTraderInitialDelay() { return INITIAL_DELAY.get(); }
    public static int wanderingTraderVisitInterval() { return VISIT_INTERVAL.get(); }
    public static int wanderingTraderRetryDelay() { return RETRY_DELAY.get(); }
    public static boolean wanderingTraderAnnounceArrival() { return ANNOUNCE_ARRIVAL.get(); }
    /** Owner export remains disabled until explicitly enabled in server configuration. */
    public static boolean observationalExportEnabled() { return OBSERVATIONAL_EXPORT.get(); }
}
