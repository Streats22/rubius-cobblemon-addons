package nl.streats1.rubiusaddons.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.streats1.rubiusaddons.Config;
import nl.streats1.rubiusaddons.integration.CobbleDollarsConfigHelper;
import nl.streats1.rubiusaddons.network.CobbleDollarsShopPayloads;

import java.util.ArrayList;
import java.util.List;

/**
 * CobbleDollars-style shop screen for villager trading: tabs, item list with prices (K + €),
 * search, quantity, Buy button, and balance at bottom.
 */
@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
public class CobbleDollarsShopScreen extends Screen {

    private static final int WINDOW_WIDTH = 276;
    private static final int WINDOW_HEIGHT = 220;

    private final int villagerId;
    private long balance;
    private List<MerchantOffer> offers = new ArrayList<>();
    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private EditBox searchBox;
    private EditBox quantityBox;
    private int listVisibleRows = 6;
    private int listItemHeight = 24;

    public CobbleDollarsShopScreen(int villagerId, long balance) {
        super(Component.translatable("gui.rubius_cobblemon_additions.cobbledollars_shop"));
        this.villagerId = villagerId;
        this.balance = balance;
    }

    public static void openFromPayload(int villagerId, long balance) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity entity = mc.level.getEntity(villagerId);
        if (!(entity instanceof Villager) && !(entity instanceof WanderingTrader)) return;
        mc.setScreen(new CobbleDollarsShopScreen(villagerId, balance));
    }

    @Override
    protected void init() {
        super.init();
        if (minecraft == null || minecraft.level == null) return;

        Entity entity = minecraft.level.getEntity(villagerId);
        if (entity instanceof Villager villager) {
            offers = filterEmeraldTrades(villager.getOffers());
        } else if (entity instanceof WanderingTrader trader) {
            offers = filterEmeraldTrades(trader.getOffers());
        }

        int left = (width - WINDOW_WIDTH) / 2;
        int top = (height - WINDOW_HEIGHT) / 2;

        // Search bar (right panel top)
        int searchX = left + 160;
        int searchY = top + 18;
        searchBox = new EditBox(minecraft.font, searchX, searchY, 100, 18, Component.literal("Search"));
        searchBox.setHint(Component.translatable("gui.rubius_cobblemon_additions.search"));
        searchBox.setMaxLength(32);
        searchBox.setResponder(s -> filterAndScroll());
        addRenderableWidget(searchBox);

        // Quantity
        int qtyX = left + 160;
        int qtyY = top + 58;
        quantityBox = new EditBox(minecraft.font, qtyX, qtyY, 40, 18, Component.literal("Qty"));
        quantityBox.setValue("1");
        quantityBox.setMaxLength(3);
        addRenderableWidget(quantityBox);

        // Buy button
        addRenderableWidget(Button.builder(Component.translatable("gui.rubius_cobblemon_additions.buy"), this::onBuy)
                .bounds(left + 205, top + 56, 50, 20)
                .build());
    }

    private List<MerchantOffer> filterEmeraldTrades(List<MerchantOffer> list) {
        List<MerchantOffer> out = new ArrayList<>();
        for (MerchantOffer o : list) {
            if (!o.getCostA().isEmpty() && o.getCostA().is(Items.EMERALD)) {
                out.add(o);
            }
        }
        return out;
    }

    private void filterAndScroll() {
        // Could filter offers by search text; for now we keep all
    }

    private void onBuy(Button button) {
        if (selectedIndex < 0 || selectedIndex >= offers.size()) return;
        int qty = 1;
        try {
            if (quantityBox != null && !quantityBox.getValue().isEmpty()) {
                qty = Integer.parseInt(quantityBox.getValue());
                qty = Math.max(1, Math.min(qty, 64));
            }
        } catch (NumberFormatException ignored) {
        }
        int index = selectedIndex;
        PacketDistributor.sendToServer(new CobbleDollarsShopPayloads.BuyWithCobbleDollars(villagerId, index, qty));
        // Optionally close or refresh balance; for now stay open
        quantityBox.setValue("1");
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int left = (width - WINDOW_WIDTH) / 2;
        int top = (height - WINDOW_HEIGHT) / 2;

        // Dark panel background
        guiGraphics.fill(left, top, left + WINDOW_WIDTH, top + WINDOW_HEIGHT, 0xFF3C3C3C);
        guiGraphics.fill(left + 2, top + 2, left + WINDOW_WIDTH - 2, top + WINDOW_HEIGHT - 2, 0xFF2A2A2A);

        // Tabs
        guiGraphics.drawString(font, Component.translatable("gui.rubius_cobblemon_additions.trades"), left + 12, top + 10, 0xFFE0E0E0, false);

        // Left panel: list of trades
        int listTop = top + 28;
        int listWidth = 140;

        for (int i = 0; i < listVisibleRows; i++) {
            int idx = scrollOffset + i;
            if (idx >= offers.size()) break;
            MerchantOffer offer = offers.get(idx);
            int y = listTop + i * listItemHeight;
            boolean selected = idx == selectedIndex;
            boolean hover = mouseX >= left + 8 && mouseX < left + 8 + listWidth && mouseY >= y && mouseY < y + listItemHeight;

            if (selected) {
                guiGraphics.fill(left + 6, y, left + 6 + listWidth + 4, y + listItemHeight - 2, 0xFF4A4A4A);
            } else if (hover) {
                guiGraphics.fill(left + 6, y, left + 6 + listWidth + 4, y + listItemHeight - 2, 0xFF404040);
            }

            ItemStack result = offer.getResult();
            if (!result.isEmpty()) {
                guiGraphics.renderItem(result, left + 10, y + 2);
                guiGraphics.renderItemDecorations(font, result, left + 10, y + 2);
            }
            long price = (long) offer.getCostA().getCount() * getRate();
            String priceStr = formatPrice(price);
            guiGraphics.drawString(font, priceStr + getCurrencySymbol(), left + 32, y + 6, 0xFFDDDD00, false);
        }

        // Right panel: selected item and Buy
        if (selectedIndex >= 0 && selectedIndex < offers.size()) {
            MerchantOffer offer = offers.get(selectedIndex);
            int detailY = top + 42;
            guiGraphics.renderItem(offer.getResult(), left + 162, detailY);
            guiGraphics.renderItemDecorations(font, offer.getResult(), left + 162, detailY);
            long price = (long) offer.getCostA().getCount() * getRate();
            guiGraphics.drawString(font, formatPrice(price) + getCurrencySymbol(), left + 182, detailY + 4, 0xFFDDDD00, false);
        }

        // Balance at bottom
        String balanceStr = formatPrice(balance) + getCurrencySymbol();
        guiGraphics.drawString(font, balanceStr, left + 12, top + WINDOW_HEIGHT - 18, 0xFF00FF00, false);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private static String getCurrencySymbol() {
        try {
            return Config.COBBLEDOLLARS_CURRENCY_SYMBOL.get();
        } catch (Exception e) {
            return " C$";
        }
    }

    private int getRate() {
        return CobbleDollarsConfigHelper.getEffectiveEmeraldRate();
    }

    private static String formatPrice(long price) {
        if (price >= 1_000_000) return (price / 1_000_000) + "M";
        if (price >= 1_000) return (price / 1_000) + "K";
        return String.valueOf(price);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = (width - WINDOW_WIDTH) / 2;
        int listTop = (height - WINDOW_HEIGHT) / 2 + 28;
        int listWidth = 140;

        for (int i = 0; i < listVisibleRows; i++) {
            int idx = scrollOffset + i;
            if (idx >= offers.size()) break;
            int y = listTop + i * listItemHeight;
            if (mouseX >= left + 8 && mouseX < left + 8 + listWidth && mouseY >= y && mouseY < y + listItemHeight) {
                selectedIndex = idx;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int left = (width - WINDOW_WIDTH) / 2;
        int listTop = (height - WINDOW_HEIGHT) / 2 + 28;
        if (mouseX >= left && mouseX < left + 152 && mouseY >= listTop && mouseY < listTop + listVisibleRows * listItemHeight) {
            scrollOffset = (int) Math.max(0, Math.min(offers.size() - listVisibleRows, scrollOffset - scrollY));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
