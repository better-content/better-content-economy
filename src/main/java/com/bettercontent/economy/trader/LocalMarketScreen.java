package com.bettercontent.economy.trader;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;

import java.util.List;

final class LocalMarketScreen extends Screen {
    private static final int PAGE_SIZE = 8;
    private List<LocalMarket.Row> rows = List.of();
    private int page;
    private final MerchantScreen origin;

    LocalMarketScreen(MerchantScreen origin) {
        super(Component.translatable("screen.better_content_economy.local_market"));
        this.origin = origin;
    }

    void update(List<LocalMarket.Row> rows) {
        this.rows = List.copyOf(rows);
        this.page = Math.min(page, Math.max(0, (rows.size() - 1) / PAGE_SIZE));
        rebuildWidgets();
    }

    @Override protected void init() { rebuildWidgets(); }

    @Override protected void rebuildWidgets() {
        clearWidgets();
        int start = page * PAGE_SIZE;
        int rowWidth = Math.min(300, width);
        int rowX = (width - rowWidth) / 2;
        for (int visible = 0; visible < PAGE_SIZE && start + visible < rows.size(); visible++) {
            LocalMarket.Row row = rows.get(start + visible);
            int y = height / 2 - 82 + visible * 22;
            String label = row.merchantName() + " @ " + row.x() + ", " + row.y() + ", " + row.z()
                    + " · " + row.payment() + " → " + row.result()
                    + " · " + row.usesRemaining();
            int textWidth = Math.max(0, rowWidth - 14);
            String visibleLabel = font.plainSubstrByWidth(label, textWidth);
            if (!visibleLabel.equals(label) && textWidth >= font.width("…")) {
                visibleLabel = font.plainSubstrByWidth(label, textWidth - font.width("…")) + "…";
            }
            addRenderableWidget(Button.builder(Component.literal(visibleLabel), button -> {
                LocalMarketNetwork.select(row);
            }).tooltip(Tooltip.create(Component.literal(label))).bounds(rowX, y, rowWidth, 20).build());
        }
        int controlsY = Math.max(0, Math.min(height - 20, height / 2 + 100));
        int navWidth = Math.min(40, width);
        if (page > 0) addRenderableWidget(Button.builder(Component.literal("‹"), button -> { page--; rebuildWidgets(); })
                .bounds(0, controlsY, navWidth, 20).build());
        if ((page + 1) * PAGE_SIZE < rows.size()) addRenderableWidget(Button.builder(Component.literal("›"), button -> { page++; rebuildWidgets(); })
                .bounds(Math.max(0, width - navWidth), controlsY, navWidth, 20).build());
        int backWidth = Math.min(80, width);
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> returnToMerchant())
                .bounds((width - backWidth) / 2, controlsY, backWidth, 20).build());
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 103, 0xffffff);
        if (rows.isEmpty()) graphics.drawCenteredString(font,
                Component.translatable("screen.better_content_economy.local_market.empty"), width / 2, height / 2 - 10, 0xc0c0c0);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void onClose() { returnToMerchant(); }

    private void returnToMerchant() { minecraft.setScreen(origin); }
}
