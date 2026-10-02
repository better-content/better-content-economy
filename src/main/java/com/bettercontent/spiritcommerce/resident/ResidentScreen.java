package com.bettercontent.spiritcommerce.resident;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Compact journal-style barter view; all decisions are checked by the server. */
public final class ResidentScreen extends Screen {
    private static final int INK = 0xffe8e5dc;
    private static final int MUTED = 0xffaeb8b8;
    private static final int ACCENT = 0xffd1b879;
    private ResidentNetwork.Snapshot snapshot;
    private ResidentNetwork.QuoteResult currentQuote;
    private ItemStack wanted = ItemStack.EMPTY;
    private ItemStack payment = ItemStack.EMPTY;
    private List<ItemStack> paymentChoices = List.of();
    private int offerPage;
    private int paymentPage;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private boolean showPayments;

    public ResidentScreen(ResidentNetwork.Snapshot snapshot) {
        super(Component.literal(snapshot.name()));
        this.snapshot = snapshot;
    }

    void update(ResidentNetwork.Snapshot next) {
        if (!snapshot.resident().equals(next.resident())) return;
        snapshot = next;
        currentQuote = null;
        wanted = ItemStack.EMPTY;
        payment = ItemStack.EMPTY;
        rebuildWidgets();
    }

    public void quote(ResidentNetwork.QuoteResult next) {
        currentQuote = next;
        rebuildWidgets();
    }

    @Override protected void init() { rebuildWidgets(); }

    @Override protected void rebuildWidgets() {
        clearWidgets();
        panelWidth = Math.min(446, Math.max(1, width - 12));
        panelHeight = Math.min(326, Math.max(1, height - 12));
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        int middle = left + panelWidth / 2;
        boolean narrow = panelWidth < 380;
        int rows = Math.max(1, 1 + (panelHeight - 178) / 29);
        offerPage = Math.min(offerPage, Math.max(0, (snapshot.offers().size() - 1) / rows));
        paymentChoices = payments();
        paymentPage = Math.min(paymentPage, Math.max(0, (paymentChoices.size() - 1) / rows));
        if (narrow) {
            addRenderableWidget(Button.builder(Component.literal("Offers"), b -> { showPayments = false; rebuildWidgets(); })
                    .bounds(left + 10, top + 53, 95, 17).build());
            addRenderableWidget(Button.builder(Component.literal("Your goods"), b -> { showPayments = true; rebuildWidgets(); })
                    .bounds(left + 110, top + 53, 95, 17).build());
        }
        for (int i = 0; i < rows; i++) {
            int index = offerPage * rows + i;
            if ((!narrow || !showPayments) && index < snapshot.offers().size()) {
                ResidentBarter.Offer offer = snapshot.offers().get(index);
                ItemStack output = offer.result();
                addRenderableWidget(Button.builder(Component.literal(font.plainSubstrByWidth(output.getHoverName().getString(),
                                narrow ? panelWidth - 130 : panelWidth / 2 - 130)),
                        b -> { wanted = output.copy(); currentQuote = null; requestQuote(); rebuildWidgets(); })
                        .bounds(left + 30, top + 72 + i * 29,
                                narrow ? panelWidth - 40 : panelWidth / 2 - 36, 24).build());
            }
            index = paymentPage * rows + i;
            if ((!narrow || showPayments) && index < paymentChoices.size()) {
                ItemStack choice = paymentChoices.get(index);
                addRenderableWidget(Button.builder(Component.literal(font.plainSubstrByWidth(choice.getHoverName().getString(),
                                narrow ? panelWidth - 54 : panelWidth / 2 - 44)),
                        b -> { payment = choice.copyWithCount(1); currentQuote = null; requestQuote(); rebuildWidgets(); })
                        .bounds(narrow ? left + 30 : middle + 25, top + 72 + i * 29,
                                narrow ? panelWidth - 40 : panelWidth / 2 - 34, 24).build());
            }
        }
        int navY = top + panelHeight - 77;
        if (!narrow || !showPayments) {
            pageButton(left + 12, navY, "‹", () -> { offerPage--; rebuildWidgets(); }, offerPage > 0);
            pageButton(left + 39, navY, "›", () -> { offerPage++; rebuildWidgets(); },
                    (offerPage + 1) * rows < snapshot.offers().size());
        }
        if (!narrow || showPayments) {
            pageButton(narrow ? left + 12 : middle + 9, navY, "‹", () -> { paymentPage--; rebuildWidgets(); }, paymentPage > 0);
            pageButton(narrow ? left + 39 : middle + 36, navY, "›", () -> { paymentPage++; rebuildWidgets(); },
                    (paymentPage + 1) * rows < paymentChoices.size());
        }
        int maxPay = paymentChoices.stream().filter(s -> ItemStack.isSameItemSameTags(s, payment))
                .mapToInt(ItemStack::getCount).findFirst().orElse(0);
        pageButton(left + panelWidth - 61, navY, "−", () -> changePayment(-1), payment.getCount() > 1);
        pageButton(left + panelWidth - 34, navY, "+", () -> changePayment(1),
                !payment.isEmpty() && payment.getCount() < maxPay);
        int actionY = top + panelHeight - 26;
        addRenderableWidget(Button.builder(Component.literal("Give supplies"), b -> {
            if (!payment.isEmpty()) ResidentNetwork.request(snapshot.resident(), ItemStack.EMPTY, payment, 3);
        }).bounds(left + 10, actionY, 95, 20).build()).active = !payment.isEmpty();
        addRenderableWidget(Button.builder(Component.literal("Exchange"), b -> {
            if (!wanted.isEmpty() && !payment.isEmpty() && currentQuote != null && currentQuote.available())
                ResidentNetwork.request(snapshot.resident(), wanted, payment, 2);
        }).bounds(left + panelWidth - 170, actionY, 88, 20).build())
                .active = currentQuote != null && currentQuote.available();
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + panelWidth - 75, actionY, 65, 20).build());
    }

    private void pageButton(int x, int y, String label, Runnable action, boolean active) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> action.run())
                .bounds(x, y, 24, 20).build()).active = active;
    }

    private List<ItemStack> payments() {
        if (Minecraft.getInstance().player == null) return List.of();
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : Minecraft.getInstance().player.getInventory().items) {
            if (stack.isEmpty() || result.stream().anyMatch(existing -> ItemStack.isSameItemSameTags(existing, stack))) continue;
            result.add(stack.copy());
        }
        return List.copyOf(result);
    }

    private void requestQuote() {
        if (!wanted.isEmpty() && !payment.isEmpty())
            ResidentNetwork.request(snapshot.resident(), wanted, payment, 1);
    }

    private void changePayment(int delta) {
        if (payment.isEmpty()) return;
        payment.setCount(payment.getCount() + delta);
        currentQuote = null;
        requestQuote();
        rebuildWidgets();
    }

    private String fit(String value, int max) { return value.length() <= max ? value : value.substring(0, max - 1) + "…"; }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.fill(left - 1, top - 1, left + panelWidth + 1, top + panelHeight + 1, ACCENT);
        g.fill(left, top, left + panelWidth, top + panelHeight, 0xf5222930);
        g.drawString(font, font.plainSubstrByWidth(title.getString(), panelWidth - 20),
                left + 10, top + 8, INK, false);
        g.drawString(font, fit(snapshot.needs(), 48), left + 10, top + 24, ACCENT, false);
        g.drawString(font, fit(snapshot.doing().isEmpty() ? "Looking for work" : snapshot.doing(), 48),
                left + 10, top + 38, MUTED, false);
        int middle = left + panelWidth / 2;
        boolean narrow = panelWidth < 380;
        if (!narrow) {
            g.fill(middle, top + 60, middle + 1, top + panelHeight - 80, 0xff59676a);
            g.drawString(font, "OFFERS", left + 10, top + 59, INK, false);
            g.drawString(font, "YOUR GOODS", middle + 9, top + 59, INK, false);
        }
        int rows = Math.max(1, 1 + (panelHeight - 178) / 29);
        for (int i = 0; i < rows; i++) {
            int index = offerPage * rows + i;
            if ((!narrow || !showPayments) && index < snapshot.offers().size()) {
                ResidentBarter.Offer offer = snapshot.offers().get(index);
                g.renderItem(offer.result(), left + 11, top + 76 + i * 29);
                g.renderItemDecorations(font, offer.result(), left + 11, top + 76 + i * 29);
                if (ItemStack.isSameItemSameTags(offer.result(), wanted))
                    g.drawString(font, "•", left + 2, top + 78 + i * 29, ACCENT, false);
            }
            index = paymentPage * rows + i;
            if ((!narrow || showPayments) && index < paymentChoices.size()) {
                ItemStack choice = paymentChoices.get(index);
                int paymentX = narrow ? left + 11 : middle + 7;
                g.renderItem(choice, paymentX, top + 76 + i * 29);
                g.renderItemDecorations(font, choice, paymentX, top + 76 + i * 29);
                if (ItemStack.isSameItemSameTags(choice, payment))
                    g.drawString(font, "•", paymentX - 6, top + 78 + i * 29, ACCENT, false);
            }
        }
        if (!payment.isEmpty()) g.drawString(font, "Give " + payment.getCount(),
                left + panelWidth - 125, top + panelHeight - 71, INK, false);
        String message = currentQuote == null ? snapshot.message() : currentQuote.message();
        if (!message.isEmpty()) {
            int line = 0;
            for (var text : font.split(Component.literal(message), panelWidth - 20)) {
                if (line == 2) break;
                g.drawString(font, text, left + 10, top + panelHeight - 49 + line * 10,
                        currentQuote != null && !currentQuote.available() ? 0xffffae91 : INK, false);
                line++;
            }
        }
        super.render(g, mouseX, mouseY, partialTick);
        for (int i = 0; i < rows; i++) {
            int index = offerPage * rows + i;
            if ((!narrow || !showPayments) && index < snapshot.offers().size()) {
                String source = snapshot.offers().get(index).recipe() == null ? "STOCK" : "MAKE";
                g.drawString(font, source, (narrow ? left + panelWidth : middle) - font.width(source) - 14,
                        top + 80 + i * 29, ACCENT, false);
            }
        }
        int offerIndex = offerPage * rows + (mouseY - top - 72) / 29;
        if ((!narrow || !showPayments) && mouseY >= top + 72 && mouseY < top + 72 + rows * 29
                && mouseX >= left && (narrow || mouseX < middle)
                && offerIndex >= 0 && offerIndex < snapshot.offers().size())
            g.renderTooltip(font, snapshot.offers().get(offerIndex).result(), mouseX, mouseY);
        int paymentIndex = paymentPage * rows + (mouseY - top - 72) / 29;
        if ((!narrow || showPayments) && mouseY >= top + 72 && mouseY < top + 72 + rows * 29
                && (narrow || mouseX >= middle) && paymentIndex >= 0 && paymentIndex < paymentChoices.size())
            g.renderTooltip(font, paymentChoices.get(paymentIndex), mouseX, mouseY);
    }

    @Override public boolean isPauseScreen() { return false; }
}
