package com.bettercontent.spiritcommerce.resident;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Exact offer editor plus physical stock and proceeds. */
public final class PlayerStallScreen extends Screen {
    private static final int INK = 0xfff2e9d5;
    private static final int MUTED = 0xffb9b6aa;
    private static final int ACCENT = 0xffdcb763;
    private PlayerStallNetwork.Snapshot snapshot;
    private int tab; // 0 offers, 1 stock, 2 proceeds
    private int selectedOffer;
    private int offerPage;
    private int pickerFor;
    private int pickerPage;
    private String pickerQuery = "";
    private ItemStack draftSale = ItemStack.EMPTY;
    private ItemStack draftPayment = ItemStack.EMPTY;
    private boolean draftEnabled = true;
    private EditBox saleCount;
    private EditBox paymentCount;
    private EditBox search;
    private int left, top, panelWidth, panelHeight;

    public PlayerStallScreen(PlayerStallNetwork.Snapshot snapshot) {
        super(Component.literal("Player Stall"));
        this.snapshot = snapshot;
        loadOffer(0);
    }

    void update(PlayerStallNetwork.Snapshot next) {
        if (!snapshot.pos().equals(next.pos())) return;
        snapshot = next;
        if (next.message().equals("Saved") && tab == 0 && pickerFor == 0) loadOffer(selectedOffer);
        rebuildWidgets();
    }

    @Override protected void init() { rebuildWidgets(); }

    @Override protected void rebuildWidgets() {
        clearWidgets();
        panelWidth = Math.min(500, Math.max(1, width - 12));
        panelHeight = Math.min(330, Math.max(1, height - 12));
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        if (pickerFor != 0) { buildPicker(); return; }
        int tabWidth = Math.max(60, (panelWidth - 24) / 3);
        button(left + 8, top + 35, tabWidth, "Offers", () -> switchTab(0));
        button(left + 8 + tabWidth, top + 35, tabWidth, "Stock", () -> switchTab(1));
        button(left + 8 + tabWidth * 2, top + 35, tabWidth, "Proceeds", () -> switchTab(2));
        if (tab == 0) buildOffers(); else buildInventory();
        button(left + panelWidth - 73, top + panelHeight - 25, 65, "Done", this::onClose);
        button(left + 8, top + panelHeight - 25, 64, "Refresh", () -> send(0, 0));
    }

    private void switchTab(int next) { tab = next; rebuildWidgets(); }

    private void buildOffers() {
        int editY = top + panelHeight - 113;
        int rows = Math.max(1, Math.min(5, (editY - top - 74) / 23));
        offerPage = Math.max(0, Math.min(offerPage, Math.max(0, (PlayerStallBlockEntity.OFFERS - 1) / rows)));
        for (int i = 0; i < rows; i++) {
            int index = offerPage * rows + i;
            if (index >= PlayerStallBlockEntity.OFFERS) break;
            PlayerStallBlockEntity.Offer offer = snapshot.offers().get(index);
            String label = !offer.valid() ? "Empty offer" : offer.sale().getCount() + " "
                    + offer.sale().getHoverName().getString() + " → " + offer.payment().getCount()
                    + " " + offer.payment().getHoverName().getString();
            String status = !offer.enabled() ? "OFF" : stockCount(offer.sale()) < offer.sale().getCount()
                    ? "OUT" : "ON";
            int chosen = index;
            button(left + 9, top + 66 + i * 23, panelWidth - 70,
                    (selectedOffer == index ? "▸ " : "") + fit(label, panelWidth - 105),
                    () -> { selectedOffer = chosen; loadOffer(chosen); rebuildWidgets(); });
            button(left + panelWidth - 57, top + 66 + i * 23, 48, status,
                    () -> {
                        if (!snapshot.owner()) return;
                        selectedOffer = chosen; loadOffer(chosen); draftEnabled = !draftEnabled; saveOffer();
                    });
        }
        button(left + panelWidth - 113, top + 66 + rows * 23, 24, "‹", () -> { offerPage = Math.max(0, offerPage - 1); rebuildWidgets(); });
        button(left + panelWidth - 84, top + 66 + rows * 23, 24, "›", () -> {
            offerPage = Math.min((PlayerStallBlockEntity.OFFERS - 1) / rows, offerPage + 1); rebuildWidgets();
        });
        if (!snapshot.owner()) return;
        int half = (panelWidth - 30) / 2;
        button(left + 8, editY + 8, half - 48, fit("Sell: " + name(draftSale), half - 60),
                () -> { pickerFor = 1; rebuildWidgets(); });
        saleCount = countBox(left + half - 34, editY + 8, draftSale.getCount());
        button(left + half + 5, editY + 8, half - 48, fit("Get: " + name(draftPayment), half - 60),
                () -> { pickerFor = 2; rebuildWidgets(); });
        paymentCount = countBox(left + panelWidth - 44, editY + 8, draftPayment.getCount());
        button(left + 8, editY + 36, 92, draftEnabled ? "Enabled: yes" : "Enabled: no",
                () -> { captureCounts(); draftEnabled = !draftEnabled; rebuildWidgets(); });
        button(left + 104, editY + 36, 65, "Save", this::saveOffer);
        button(left + 173, editY + 36, 65, "Clear", () -> {
            draftSale = ItemStack.EMPTY; draftPayment = ItemStack.EMPTY; draftEnabled = false;
            send(4, selectedOffer);
        });
        if (!snapshot.events().isEmpty())
            button(left + 8, editY + 65, panelWidth - 16,
                    fit("Last: " + snapshot.events().get(snapshot.events().size() - 1), panelWidth - 35), () -> {});
    }

    private void buildInventory() {
        if (Minecraft.getInstance().player == null) return;
        int cell = Math.max(24, Math.min(52, (panelWidth - 20) / 9));
        int startX = left + (panelWidth - cell * 9) / 2;
        int playerY = top + 82;
        for (int index = 0; index < 36; index++) {
            ItemStack stack = Minecraft.getInstance().player.getInventory().items.get(index);
            if (stack.isEmpty() || !snapshot.owner() || tab != 1) continue;
            int slot = index;
            button(startX + (index % 9) * cell, playerY + (index / 9) * 24, cell - 2, "",
                    () -> send(1, slot));
        }
        int stallY = playerY + 4 * 24 + 29;
        List<ItemStack> slots = tab == 1 ? snapshot.stock() : snapshot.proceeds();
        for (int index = 0; index < PlayerStallBlockEntity.SLOTS; index++) {
            if (slots.get(index).isEmpty() || !snapshot.owner()) continue;
            int slot = index;
            button(startX + (index % 9) * cell, stallY + (index / 9) * 24, cell - 2, "",
                    () -> send(tab == 1 ? 2 : 3, slot));
        }
    }

    private void buildPicker() {
        button(left + 8, top + 35, 65, "Back", () -> { pickerFor = 0; rebuildWidgets(); });
        button(left + 77, top + 35, 104, "Use held item", () -> {
            if (Minecraft.getInstance().player != null) choose(Minecraft.getInstance().player.getMainHandItem());
        });
        search = new EditBox(font, left + 8, top + 64, panelWidth - 105, 20, Component.literal("Find item"));
        search.setValue(pickerQuery);
        addRenderableWidget(search);
        button(left + panelWidth - 91, top + 64, 83, "Search", () -> {
            pickerQuery = search.getValue().trim().toLowerCase(java.util.Locale.ROOT);
            pickerPage = 0; rebuildWidgets();
        });
        List<ItemStack> matches = matches();
        int rows = Math.max(1, Math.min(8, (panelHeight - 145) / 24));
        pickerPage = Math.max(0, Math.min(pickerPage, Math.max(0, (matches.size() - 1) / rows)));
        for (int i = 0; i < rows; i++) {
            int index = pickerPage * rows + i;
            if (index >= matches.size()) break;
            ItemStack item = matches.get(index);
            button(left + 32, top + 96 + i * 24, panelWidth - 42,
                    fit(item.getHoverName().getString() + "  [" + BuiltInRegistries.ITEM.getKey(item.getItem()) + "]",
                            panelWidth - 55), () -> choose(item));
        }
        button(left + 8, top + panelHeight - 25, 28, "‹", () -> { pickerPage = Math.max(0, pickerPage - 1); rebuildWidgets(); });
        button(left + 40, top + panelHeight - 25, 28, "›", () -> {
            pickerPage = Math.min(Math.max(0, (matches.size() - 1) / rows), pickerPage + 1); rebuildWidgets();
        });
    }

    private List<ItemStack> matches() {
        if (pickerQuery.isBlank()) return List.of();
        List<ItemStack> result = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            ItemStack stack = new ItemStack(item);
            if (id.toString().contains(pickerQuery)
                    || stack.getHoverName().getString().toLowerCase(java.util.Locale.ROOT).contains(pickerQuery))
                result.add(stack);
        }
        result.sort(Comparator.comparing(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString()));
        return result;
    }

    private void choose(ItemStack stack) {
        if (stack.isEmpty()) return;
        captureCounts();
        if (pickerFor == 1) draftSale = stack.copyWithCount(Math.max(1, Math.min(stack.getMaxStackSize(), draftSale.getCount())));
        else draftPayment = stack.copyWithCount(Math.max(1, Math.min(stack.getMaxStackSize(), draftPayment.getCount())));
        pickerFor = 0; rebuildWidgets();
    }

    private void loadOffer(int index) {
        PlayerStallBlockEntity.Offer offer = snapshot.offers().get(index);
        draftSale = offer.sale().copy(); draftPayment = offer.payment().copy(); draftEnabled = offer.enabled();
    }

    private void captureCounts() {
        if (saleCount != null && !draftSale.isEmpty()) draftSale.setCount(parsed(saleCount, draftSale.getCount(), draftSale.getMaxStackSize()));
        if (paymentCount != null && !draftPayment.isEmpty())
            draftPayment.setCount(parsed(paymentCount, draftPayment.getCount(), draftPayment.getMaxStackSize()));
    }

    private static int parsed(EditBox box, int fallback, int max) {
        try { return Math.max(1, Math.min(max, Integer.parseInt(box.getValue()))); }
        catch (RuntimeException ignored) { return Math.max(1, fallback); }
    }

    private EditBox countBox(int x, int y, int amount) {
        EditBox box = new EditBox(font, x, y, 34, 20, Component.literal("Count"));
        box.setMaxLength(2); box.setValue(Integer.toString(Math.max(1, amount)));
        addRenderableWidget(box); return box;
    }

    private void saveOffer() { captureCounts(); send(4, selectedOffer); }
    private void send(int operation, int index) {
        PlayerStallNetwork.request(snapshot.pos(), operation, index, draftSale.copy(), draftPayment.copy(), draftEnabled);
    }

    private void button(int x, int y, int width, String label, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> action.run())
                .bounds(x, y, Math.max(20, width), 20).build());
    }

    private String fit(String value, int px) { return font.plainSubstrByWidth(value, Math.max(10, px)); }
    private static String name(ItemStack item) { return item.isEmpty() ? "Choose item" : item.getHoverName().getString(); }
    private int stockCount(ItemStack wanted) { return PlayerStallBlockEntity.count(snapshot.stock(), wanted); }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.fill(left - 1, top - 1, left + panelWidth + 1, top + panelHeight + 1, ACCENT);
        g.fill(left, top, left + panelWidth, top + panelHeight, 0xf42c3436);
        g.drawString(font, "PLAYER STALL", left + 9, top + 9, INK, false);
        String status = snapshot.visiting() ? "Customer approaching" : "Waiting for a buyer";
        g.drawString(font, snapshot.owner() ? status : "Viewing another player's stall", left + 106, top + 9,
                MUTED, false);
        if (!snapshot.message().isEmpty()) g.drawString(font, fit(snapshot.message(), panelWidth - 20),
                left + 9, top + 23, ACCENT, false);
        super.render(g, mouseX, mouseY, partialTick);
        if (pickerFor != 0) {
            List<ItemStack> matches = matches();
            int rows = Math.max(1, Math.min(8, (panelHeight - 145) / 24));
            for (int i = 0; i < rows; i++) {
                int index = pickerPage * rows + i;
                if (index >= matches.size()) break;
                g.renderItem(matches.get(index), left + 11, top + 98 + i * 24);
            }
        } else if (tab == 0) {
            int editY = top + panelHeight - 113;
            g.drawString(font, fit("Offer " + (selectedOffer + 1) + ": " + snapshot.statuses().get(selectedOffer),
                    panelWidth - 16), left + 8, editY - 12, ACCENT, false);
            if (!draftSale.isEmpty()) g.renderItem(draftSale, left + 12, editY + 10);
            if (!draftPayment.isEmpty()) g.renderItem(draftPayment, left + (panelWidth - 30) / 2 + 10, editY + 10);
        } else if (Minecraft.getInstance().player != null) {
            int cell = Math.max(24, Math.min(52, (panelWidth - 20) / 9));
            int startX = left + (panelWidth - cell * 9) / 2;
            int playerY = top + 82;
            g.drawString(font, "YOUR INVENTORY" + (tab == 1 ? " — click to stock" : ""), startX, playerY - 13, ACCENT, false);
            for (int i = 0; i < 36; i++) {
                ItemStack stack = Minecraft.getInstance().player.getInventory().items.get(i);
                slot(g, startX + i % 9 * cell, playerY + i / 9 * 24, cell, stack);
            }
            int stallY = playerY + 4 * 24 + 29;
            g.drawString(font, tab == 1 ? "FOR SALE — click to withdraw" : "EARNINGS — click to collect",
                    startX, stallY - 13, ACCENT, false);
            List<ItemStack> items = tab == 1 ? snapshot.stock() : snapshot.proceeds();
            for (int i = 0; i < PlayerStallBlockEntity.SLOTS; i++)
                slot(g, startX + i % 9 * cell, stallY + i / 9 * 24, cell, items.get(i));
        }
    }

    private void slot(GuiGraphics g, int x, int y, int cell, ItemStack stack) {
        g.fill(x, y, x + cell - 2, y + 20, 0xff4b5455);
        if (stack.isEmpty()) return;
        g.renderItem(stack, x + 2, y + 2);
        g.renderItemDecorations(font, stack, x + 2, y + 2);
    }

    @Override public boolean isPauseScreen() { return false; }
}
