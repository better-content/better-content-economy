package com.bettercontent.spiritcommerce.trader;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public final class LocalMarketScreen extends Screen {
    private static final int ROW_HEIGHT = 48;
    private final MerchantScreen origin;
    private List<LocalMarket.Row> rows = List.of();
    private List<LocalMarket.Row> visible = List.of();
    private EditBox search;
    private String query = "";
    private int page;
    private int pageSize;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private boolean loading = true;
    private boolean stale;

    public LocalMarketScreen(MerchantScreen origin) {
        super(Component.translatable("screen.better_spirit_commerce.local_market"));
        this.origin = origin;
    }

    public void update(LocalMarketNetwork.Snapshot snapshot) {
        boolean searchFocused = search != null && search.isFocused();
        rows = List.copyOf(snapshot.rows());
        loading = false;
        stale = snapshot.stale();
        rebuildWidgets();
        if (searchFocused) setFocused(search);
    }

    public void setSearchText(String value) {
        query = value;
        rebuildWidgets();
    }

    @Override protected void init() { rebuildWidgets(); }

    @Override protected void rebuildWidgets() {
        clearWidgets();
        panelWidth = Math.min(420, Math.max(1, width - 16));
        panelHeight = Math.min(340, Math.max(1, height - 16));
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        pageSize = Math.max(1, (panelHeight - 77) / ROW_HEIGHT);
        visible = rows.stream().filter(row -> LocalMarket.matches(row, query)).toList();
        int pages = Math.max(1, (visible.size() + pageSize - 1) / pageSize);
        page = Math.min(page, pages - 1);

        search = new EditBox(font, left + 8, top + 25, Math.max(20, panelWidth - 16), 20,
                Component.translatable("screen.better_spirit_commerce.local_market.search"));
        search.setMaxLength(80);
        search.setHint(Component.translatable("screen.better_spirit_commerce.local_market.search"));
        search.setValue(query);
        search.setResponder(value -> {
            query = value;
            page = 0;
            rebuildWidgets();
            setFocused(search);
        });
        addRenderableWidget(search);

        int start = page * pageSize;
        for (int i = 0; i < pageSize && start + i < visible.size(); i++) {
            addRenderableWidget(new OfferButton(left + 8, top + 47 + i * ROW_HEIGHT,
                    panelWidth - 16, ROW_HEIGHT - 2, visible.get(start + i)));
        }
        int footerY = top + panelHeight - 26;
        addRenderableWidget(Button.builder(Component.literal("‹"), button -> { page--; rebuildWidgets(); })
                .bounds(left + 8, footerY, 25, 20).build()).active = page > 0;
        addRenderableWidget(Button.builder(Component.literal("›"), button -> { page++; rebuildWidgets(); })
                .bounds(left + 37, footerY, 25, 20).build()).active = page + 1 < pages;
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> returnToMerchant())
                .bounds(left + panelWidth - 72, footerY, 64, 20).build());
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(left - 1, top - 1, left + panelWidth + 1, top + panelHeight + 1, 0xffb7a77f);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xf5222930);
        graphics.drawString(font, title, left + 8, top + 8, 0xfff3e5c7, false);
        Component status = stale
                ? Component.translatable("screen.better_spirit_commerce.local_market.stale")
                : Component.translatable("screen.better_spirit_commerce.local_market.count", visible.size());
        graphics.drawString(font, status, left + panelWidth - 8 - font.width(status), top + 8,
                stale ? 0xffffc779 : 0xffb9c6c5, false);
        if (loading || visible.isEmpty()) {
            String key = loading ? "loading" : rows.isEmpty() ? "empty" : "no_matches";
            graphics.drawCenteredString(font, Component.translatable("screen.better_spirit_commerce.local_market." + key),
                    width / 2, top + 82, 0xffcbd4d3);
        }
        int pages = Math.max(1, (visible.size() + pageSize - 1) / pageSize);
        graphics.drawString(font, Component.literal((page + 1) + " / " + pages),
                left + 68, top + panelHeight - 20, 0xffb9c6c5, false);
        super.render(graphics, mouseX, mouseY, partialTick);
        for (var child : children()) {
            if (child instanceof OfferButton offer && offer.isMouseOver(mouseX, mouseY)) {
                offer.renderOfferTooltip(graphics, mouseX, mouseY);
                break;
            }
        }
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= left && mouseX < left + panelWidth && mouseY >= top && mouseY < top + panelHeight) {
            int pages = Math.max(1, (visible.size() + pageSize - 1) / pageSize);
            page = Math.max(0, Math.min(pages - 1, page + (delta < 0 ? 1 : -1)));
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { returnToMerchant(); }

    private void returnToMerchant() {
        minecraft.setScreen(origin != null && minecraft.player != null
                && minecraft.player.containerMenu == origin.getMenu() ? origin : null);
    }

    private String clipped(String value, int maxWidth) {
        if (font.width(value) <= maxWidth) return value;
        return font.plainSubstrByWidth(value, Math.max(0, maxWidth - font.width("…"))) + "…";
    }

    private final class OfferButton extends AbstractButton {
        private final LocalMarket.Row row;

        private OfferButton(int x, int y, int width, int height, LocalMarket.Row row) {
            super(x, y, width, height, Component.literal(row.payment() + " → " + row.result()
                    + " · " + row.merchantName()));
            this.row = row;
        }

        @Override public void onPress() { LocalMarketNetwork.select(row); }

        @Override protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }

        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            graphics.fill(x, y, x + w, y + h, isHoveredOrFocused() ? 0xff405259 : 0xff303c43);
            graphics.fill(x, y + h - 1, x + w, y + h, 0xff718184);
            int contentWidth = Math.max(20, w - 72);
            graphics.renderItem(row.resultStack(), x + 4, y + 2);
            graphics.renderItemDecorations(font, row.resultStack(), x + 4, y + 2);
            graphics.drawString(font, clipped(row.result(), contentWidth - 23), x + 24, y + 6, 0xfff3eee1, false);
            graphics.renderItem(row.costA(), x + 4, y + 20);
            graphics.renderItemDecorations(font, row.costA(), x + 4, y + 20);
            int labelX = x + 24;
            if (!row.costB().isEmpty()) {
                graphics.renderItem(row.costB(), x + 21, y + 20);
                graphics.renderItemDecorations(font, row.costB(), x + 21, y + 20);
                labelX = x + 42;
            }
            graphics.drawString(font, clipped(row.payment(), contentWidth - (labelX - x)),
                    labelX, y + 24, 0xffd2dedb, false);
            String merchant = row.merchantName() + " @ " + row.x() + ", " + row.y() + ", " + row.z();
            graphics.drawString(font, clipped(merchant, contentWidth - 8), x + 4, y + 37, 0xffabc2bc, false);
            String stock = Component.translatable("screen.better_spirit_commerce.local_market.stock",
                    row.usesRemaining()).getString();
            int actionX = x + w - 64;
            graphics.drawCenteredString(font, stock, actionX + 29, y + 37, 0xffabc2bc);
            graphics.fill(actionX, y + 12, actionX + 58, y + 33,
                    isHoveredOrFocused() ? 0xffb18549 : 0xff82663f);
            graphics.drawCenteredString(font, Component.translatable("screen.better_spirit_commerce.local_market.trade"),
                    actionX + 29, y + 18, 0xffffffff);
        }

        private void renderOfferTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
            ItemStack stack = null;
            int localX = mouseX - getX(), localY = mouseY - getY();
            if (localX >= 4 && localX < 20 && localY >= 2 && localY < 18) stack = row.resultStack();
            else if (localX >= 4 && localX < 20 && localY >= 20 && localY < 36) stack = row.costA();
            else if (!row.costB().isEmpty() && localX >= 21 && localX < 37 && localY >= 20 && localY < 36)
                stack = row.costB();
            if (stack != null) {
                graphics.renderTooltip(font, stack, mouseX, mouseY);
                return;
            }
            if (localY >= 36 && localX < getWidth() - 64) {
                graphics.renderTooltip(font, List.of(
                        Component.literal(row.merchantName() + " @ " + row.x() + ", " + row.y() + ", " + row.z()),
                        Component.translatable("screen.better_spirit_commerce.local_market.stock", row.usesRemaining())),
                        Optional.empty(), ItemStack.EMPTY, mouseX, mouseY);
            }
        }
    }
}
