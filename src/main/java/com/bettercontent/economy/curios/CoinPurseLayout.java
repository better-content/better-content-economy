package com.bettercontent.economy.curios;

public final class CoinPurseLayout {
    public static final int INVENTORY_WIDTH = 176;
    public static final int INVENTORY_HEIGHT = 166;
    public static final int STRIP_HEIGHT = 22;
    public static final int SLOT_COUNT = 7;
    public static final int FIRST_SLOT_X = 48;
    public static final int SLOT_Y = 169;
    public static final int SLOT_STRIDE = 18;

    private CoinPurseLayout() {}

    public static int slotX(final int index) {
        return FIRST_SLOT_X + index * SLOT_STRIDE;
    }
}
