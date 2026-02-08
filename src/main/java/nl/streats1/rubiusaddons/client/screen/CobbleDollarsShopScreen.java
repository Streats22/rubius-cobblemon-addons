package nl.streats1.rubiusaddons.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.streats1.rubiusaddons.Config;
import nl.streats1.rubiusaddons.integration.CobbleDollarsConfigHelper;
import nl.streats1.rubiusaddons.network.CobbleDollarsShopPayloads;
import nl.streats1.rubiusaddons.client.screen.widget.InvisibleButton;
import nl.streats1.rubiusaddons.client.screen.widget.TextureOnlyButton;

import java.util.List;

/**
 * CobbleDollars-style shop screen: layout aligned with CobbleDollars (balance top-left, category tabs, offer list, quantity ±, Buy/Sell).
 * See COBBLEDOLLARS_GUI_REFERENCE.md.
 */
@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
public class CobbleDollarsShopScreen extends Screen {

    private static final int WINDOW_WIDTH = 320;
    /** GUI is centered; no vertical offset. */
    private static final int GUI_Y_OFFSET = 0;
    private static final int RIGHT_PANEL_X = 200;
    private static final int RIGHT_PANEL_MARGIN = 12;
    private static final int LIST_TOP_OFFSET = 24;
    private static final int LIST_VISIBLE_ROWS = 4;
    private static final int LIST_ROW_HEIGHT = 18;
    /** List lives in right panel (blue square); below it: selected item, qty, button. */
    private static final int RIGHT_PANEL_DETAIL_Y = LIST_TOP_OFFSET + LIST_VISIBLE_ROWS * LIST_ROW_HEIGHT + 4;
    private static final int RIGHT_PANEL_QTY_Y = RIGHT_PANEL_DETAIL_Y + 26;
    private static final int RIGHT_PANEL_BUTTON_Y = RIGHT_PANEL_QTY_Y + 26;
    /** Keep GUI height fixed to the base texture; do not auto-adjust height. */
    private static final int TRADE_PANEL_HEIGHT = 120;
    private static final int SLOT_SIZE = 18;
    private static final int INVENTORY_ROWS = 4;
    private static final int INVENTORY_COLS = 9;
    private static final int INVENTORY_VISIBLE_ROWS = 4;
    private static final int INVENTORY_SLOTS = INVENTORY_ROWS * INVENTORY_COLS;
    private static final int INVENTORY_SLOT_SPACING = 18;
    private static final int INVENTORY_AREA_HEIGHT = INVENTORY_VISIBLE_ROWS * INVENTORY_SLOT_SPACING + 14;
    /** Use the base texture height (shop_base.png = 196) for centering. */
    private static final int WINDOW_HEIGHT = 196;
    private static final int TAB_HEIGHT = 22;
    private static final int SCROLLBAR_WIDTH = 8;
    /** Offer list is in the right panel (blue square); width = panel width minus scrollbar. */
    private static final int LIST_WIDTH = WINDOW_WIDTH - RIGHT_PANEL_X - RIGHT_PANEL_MARGIN - SCROLLBAR_WIDTH - 4;
    private static final int LEFT_STRIP_WIDTH = 66;
    private static final int TAB_OFFSET_X = 10;
    private static final int TAB_OUTLINE_OFFSET_X = -2;
    private static final int TAB_OUTLINE_OFFSET_Y = -4;
    /** Left panel positions for selected item + qty + buy. */
    private static final int LEFT_PANEL_X = 8;
    private static final int LEFT_PANEL_DETAIL_Y = 32;
    private static final int LEFT_PANEL_PRICE_Y = 34;
    private static final int LEFT_PANEL_QTY_X = 44;
    private static final int LEFT_PANEL_QTY_Y = 52;
    private static final int LEFT_PANEL_BUY_Y = 70;
    private static final int TAB_H = 30;
    /** Buy/Sell button lives in the left strip bar (next to currency), not in the right panel. */
    private static final int BUY_BUTTON_STRIP_X = 58;
    private static final int BUY_BUTTON_STRIP_Y = 34;
    private static final int BUY_BUTTON_STRIP_W = 52;
    private static final int BUY_BUTTON_STRIP_H = 20;
    private static final int LIST_LEFT_OFFSET = 190;
    private static final int LIST_ITEM_ICON_SIZE = 16;

    /** Alignment: content padding inside offer_background (73x16). Match CobbleDollars-style row content. */
    private static final int OFFER_ROW_PADDING_LEFT = 2;
    private static final int OFFER_ROW_PADDING_RIGHT = 2;
    private static final int OFFER_ROW_GAP_AFTER_ICON = 4;
    /** Inset for item in 18x18 slot so 16x16 icon is centered. */
    private static final int SLOT_ITEM_INSET = (SLOT_SIZE - LIST_ITEM_ICON_SIZE) / 2;
    /** Price text drawn lower so it doesn’t sit under the quantity/black box. */
    private static final int PRICE_TEXT_OFFSET_Y = 4;

    /** Texture paths: bank = textures/gui/bank/, shop = textures/gui/shop/ (CobbleDollars-style, bundled). */
    private static final String GUI_TEXTURES_NAMESPACE = "rubius_cobblemon_addons";

    // --- Bank GUI (assets/.../textures/gui/bank/) ---
    private static final ResourceLocation TEX_BANK_BASE = rl(GUI_TEXTURES_NAMESPACE, "textures/gui/bank/bank_base.png");
    private static final int TEX_BANK_BASE_W = 170;
    private static final int TEX_BANK_BASE_H = 204;

    // --- Shop GUI (assets/.../textures/gui/shop/) ---
    private static final ResourceLocation TEX_SHOP_BASE = rl(GUI_TEXTURES_NAMESPACE, "textures/gui/shop/shop_base.png");
    private static final ResourceLocation TEX_CATEGORY_BG = rl(GUI_TEXTURES_NAMESPACE, "textures/gui/shop/category_background.png");
    private static final ResourceLocation TEX_CATEGORY_OUTLINE = rl(GUI_TEXTURES_NAMESPACE, "textures/gui/shop/category_outline.png");
    private static final ResourceLocation TEX_OFFER_BG = rl(GUI_TEXTURES_NAMESPACE, "textures/gui/shop/offer_background.png");
    private static final ResourceLocation TEX_OFFER_OUTLINE = rl(GUI_TEXTURES_NAMESPACE, "textures/gui/shop/offer_outline.png");
    private static final ResourceLocation TEX_BUY_BUTTON = rl(GUI_TEXTURES_NAMESPACE, "textures/gui/shop/buy_button.png");
    private static final ResourceLocation TEX_AMOUNT_UP = rl(GUI_TEXTURES_NAMESPACE, "textures/gui/shop/amount_arrow_up.png");
    private static final ResourceLocation TEX_AMOUNT_DOWN = rl(GUI_TEXTURES_NAMESPACE, "textures/gui/shop/amount_arrow_down.png");
    private static final int TEX_SHOP_BASE_W = 252;
    private static final int TEX_SHOP_BASE_H = 196;
    private static final int TEX_CATEGORY_BG_W = 69;
    private static final int TEX_CATEGORY_BG_H = 11;
    private static final int TEX_CATEGORY_OUTLINE_W = 76;
    private static final int TEX_CATEGORY_OUTLINE_H = 19;
    private static final int TEX_OFFER_BG_W = 73;
    private static final int TEX_OFFER_BG_H = 16;
    private static final int TEX_BUY_BUTTON_W = 31;
    private static final int TEX_BUY_BUTTON_H = 42;
    private static final int TEX_AMOUNT_ARROW_W = 5;
    private static final int TEX_AMOUNT_ARROW_H = 10;

    // --- CobbleDollars currency logo (GuiUtilsKt: 54x14) ---
    private static final ResourceLocation TEX_COBBLEDOLLARS_LOGO = rl(GUI_TEXTURES_NAMESPACE, "textures/gui/cobbledollars_background.png");
    private static final int TEX_COBBLEDOLLARS_LOGO_W = 54;
    private static final int TEX_COBBLEDOLLARS_LOGO_H = 14;

    private static ResourceLocation rl(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    /** Blit texture region (0,0)-(texW,texH) to (x,y) at same size. */
    private static void blitFull(GuiGraphics guiGraphics, ResourceLocation tex, int x, int y, int texW, int texH) {
        guiGraphics.blit(tex, x, y, 0, 0, texW, texH, texW, texH);
    }

    /** Blit full texture scaled to (drawW, drawH). Uses (0,0)-(texW,texH) from texture. */
    private static void blitStretched(GuiGraphics guiGraphics, ResourceLocation tex, int x, int y, int drawW, int drawH, int texW, int texH) {
        guiGraphics.blit(tex, x, y, 0, 0f, 0f, drawW, drawH, texW, texH);
    }

    private final int villagerId;
    private long balance;
    private final List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOffers;
    private final List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOffers;
    private final boolean buyOffersFromConfig;
    /** 0 = Buy, 1 = Sell */
    private int selectedTab = 0;
    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private EditBox quantityBox;
    private Button actionButton;
    private Button amountMinusButton;
    private Button amountPlusButton;
    private int listVisibleRows = LIST_VISIBLE_ROWS;
    private int listItemHeight = LIST_ROW_HEIGHT;

    public CobbleDollarsShopScreen(int villagerId, long balance,
                                   List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOffers,
                                   List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOffers,
                                   boolean buyOffersFromConfig) {
        super(Component.translatable("gui.rubius_cobblemon_additions.cobbledollars_shop"));
        this.villagerId = villagerId;
        this.balance = balance;
        this.buyOffers = buyOffers != null ? buyOffers : List.of();
        this.sellOffers = sellOffers != null ? sellOffers : List.of();
        this.buyOffersFromConfig = buyOffersFromConfig;
        if (!this.buyOffers.isEmpty()) {
            selectedTab = 0;
            selectedIndex = 0;
        } else if (!this.sellOffers.isEmpty()) {
            selectedTab = 1;
            selectedIndex = 0;
        }
    }

    /** List for the currently selected tab. */
    private List<CobbleDollarsShopPayloads.ShopOfferEntry> currentOffers() {
        return selectedTab == 0 ? buyOffers : sellOffers;
    }

    private boolean isSellTab() {
        return selectedTab == 1;
    }

    public static void openFromPayload(int villagerId, long balance,
                                       List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOffers,
                                       List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOffers,
                                       boolean buyOffersFromConfig) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        mc.setScreen(new CobbleDollarsShopScreen(villagerId, balance, buyOffers, sellOffers, buyOffersFromConfig));
    }

    @Override
    protected void init() {
        super.init();
        if (minecraft == null) return;

        int w = guiWidth();
        int h = guiHeight();
        int left = (w - WINDOW_WIDTH) / 2;
        int top = (h - WINDOW_HEIGHT) / 2;

        // Right panel: quantity row, then Buy/Sell button below
        int qtyRowY = top + LEFT_PANEL_QTY_Y;
        int rx = left + LEFT_PANEL_X + LEFT_PANEL_QTY_X;
        amountMinusButton = new InvisibleButton(rx - 14, qtyRowY, 12, 12, Component.literal("−"), b -> adjustQuantity(-1));
        addRenderableWidget(amountMinusButton);
        quantityBox = new EditBox(minecraft.font, rx, qtyRowY, 26, 12, Component.literal("Qty"));
        quantityBox.setValue("1");
        quantityBox.setMaxLength(3);
        quantityBox.setBordered(false); // No black box so our quantity row texture shows; remove if your MC version has no setBordered
        addRenderableWidget(quantityBox);
        amountPlusButton = new InvisibleButton(rx + 28, qtyRowY, 12, 12, Component.literal("+"), b -> adjustQuantity(1));
        addRenderableWidget(amountPlusButton);
        actionButton = new TextureOnlyButton(left + LEFT_PANEL_X + 66, top + LEFT_PANEL_BUY_Y, 40, 14, Component.translatable("gui.rubius_cobblemon_additions.buy"), this::onAction);
        addRenderableWidget(actionButton);

        // Close button (top-right)
        int closeSize = 14;
        int closeX = left + WINDOW_WIDTH - closeSize - 6;
        int closeY = top + 4;
        addRenderableWidget(Button.builder(Component.literal("×"), b -> onClose())
                .bounds(closeX, closeY, closeSize, closeSize)
                .build());
    }

    private void adjustQuantity(int delta) {
        if (quantityBox == null) return;
        int qty = parseQuantity();
        qty = Math.max(1, Math.min(64, qty + delta));
        quantityBox.setValue(String.valueOf(qty));
    }

    private void onAction(Button button) {
        var offers = currentOffers();
        if (selectedIndex < 0 || selectedIndex >= offers.size()) return;
        int qty = parseQuantity();
        if (isSellTab()) {
            PacketDistributor.sendToServer(new CobbleDollarsShopPayloads.SellForCobbleDollars(villagerId, selectedIndex, qty));
        } else {
            PacketDistributor.sendToServer(new CobbleDollarsShopPayloads.BuyWithCobbleDollars(villagerId, selectedIndex, qty, buyOffersFromConfig));
        }
        if (quantityBox != null) quantityBox.setValue("1");
    }

    private int parseQuantity() {
        int qty = 1;
        try {
            if (quantityBox != null && !quantityBox.getValue().isEmpty()) {
                qty = Integer.parseInt(quantityBox.getValue());
                qty = Math.max(1, Math.min(qty, 64));
            }
        } catch (NumberFormatException ignored) {
        }
        return qty;
    }

    /** Use the game's scaled GUI dimensions so the shop respects GUI scale setting. */
    private int guiWidth() {
        if (minecraft != null && minecraft.getWindow() != null) {
            return minecraft.getWindow().getGuiScaledWidth();
        }
        return width;
    }

    private int guiHeight() {
        if (minecraft != null && minecraft.getWindow() != null) {
            return minecraft.getWindow().getGuiScaledHeight();
        }
        return height;
    }

    /** No full-screen overlay – world shows through; only the shop_base is the GUI. */
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Don't draw a dark fill – no black background besides the GUI
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int w = guiWidth();
        int h = guiHeight();
        int left = (w - WINDOW_WIDTH) / 2;
        int top = (h - WINDOW_HEIGHT) / 2;

        // Only shop_base texture; no grey around the GUI
        blitFull(guiGraphics, TEX_SHOP_BASE, left, top, TEX_SHOP_BASE_W, TEX_SHOP_BASE_H);

        // Left strip: text only (no bank/logo textures)
        int stripX = left + 8;
        guiGraphics.drawString(font, Component.translatable("gui.rubius_cobblemon_additions.shop"), stripX, top + 6, 0xFFE0E0E0, false);
        guiGraphics.drawString(font, formatPrice(balance) + getCurrencySymbol(), stripX, top + 20, 0xFF00DD00, false);

        // Tab bar: Buy above Sell (stacked vertically) with category textures
        int tabX = left + LEFT_STRIP_WIDTH + 5 + TAB_OFFSET_X;
        int tabY = top + 4;
        int tabGapY = 2;
        int buyY = tabY;
        int sellY = tabY + TAB_H + tabGapY;
        // Backgrounds (inactive) + outline for active tab
        blitFull(guiGraphics, TEX_CATEGORY_BG, tabX, buyY, TEX_CATEGORY_BG_W, TEX_CATEGORY_BG_H);
        blitFull(guiGraphics, TEX_CATEGORY_BG, tabX, sellY, TEX_CATEGORY_BG_W, TEX_CATEGORY_BG_H);
        if (selectedTab == 0) {
            blitStretched(guiGraphics, TEX_CATEGORY_OUTLINE, tabX + TAB_OUTLINE_OFFSET_X, buyY + TAB_OUTLINE_OFFSET_Y, TEX_CATEGORY_OUTLINE_W, TEX_CATEGORY_OUTLINE_H, TEX_CATEGORY_OUTLINE_W, TEX_CATEGORY_OUTLINE_H);
        } else {
            blitStretched(guiGraphics, TEX_CATEGORY_OUTLINE, tabX + TAB_OUTLINE_OFFSET_X, sellY + TAB_OUTLINE_OFFSET_Y, TEX_CATEGORY_OUTLINE_W, TEX_CATEGORY_OUTLINE_H, TEX_CATEGORY_OUTLINE_W, TEX_CATEGORY_OUTLINE_H);
        }
        guiGraphics.drawString(font, Component.translatable("gui.rubius_cobblemon_additions.buy"), tabX + 4, buyY + (TAB_H - font.lineHeight) / 2, selectedTab == 0 ? 0xFFE0E0E0 : 0xFFA0A0A0, false);
        guiGraphics.drawString(font, Component.translatable("gui.rubius_cobblemon_additions.sell"), tabX + 4, sellY + (TAB_H - font.lineHeight) / 2, selectedTab == 1 ? 0xFFE0E0E0 : 0xFFA0A0A0, false);

        // Offer list: items and text only (no row backgrounds)
        int listTop = top + LIST_TOP_OFFSET;
        int rowL = left + LIST_LEFT_OFFSET - 10;
        int rowR = rowL + LIST_WIDTH;
        var offers = currentOffers();

        for (int i = 0; i < listVisibleRows; i++) {
            int idx = scrollOffset + i;
            if (idx >= offers.size()) break;
            CobbleDollarsShopPayloads.ShopOfferEntry entry = offers.get(idx);
            int y = listTop + i * listItemHeight;

            int rowH = listItemHeight;
            // Align content: left padding, icon then price, vertically centered in row
            int iconX = rowL + OFFER_ROW_PADDING_LEFT;
            int iconY = y + (rowH - LIST_ITEM_ICON_SIZE) / 2;
            int textY = y + (rowH - font.lineHeight) / 2;
            ItemStack result = resultStackFrom(entry);
            if (!result.isEmpty()) {
                guiGraphics.renderItem(result, iconX, iconY);
                guiGraphics.renderItemDecorations(font, result, iconX, iconY);
            }
            long price = priceForDisplay(entry);
            String priceStr = isSellTab() ? "→ " + formatPrice(price) + getCurrencySymbol() : formatPrice(price) + getCurrencySymbol();
            int priceX = iconX + LIST_ITEM_ICON_SIZE + OFFER_ROW_GAP_AFTER_ICON;
            guiGraphics.drawString(font, priceStr, priceX, textY + PRICE_TEXT_OFFSET_Y, 0xFFFFFFFF, false);
            if (!isSellTab()) {
                ItemStack costB = costBStackFrom(entry);
                if (!costB.isEmpty()) {
                    int costBX = priceX + font.width(priceStr) + OFFER_ROW_GAP_AFTER_ICON;
                    guiGraphics.drawString(font, "+", costBX - 2, textY + PRICE_TEXT_OFFSET_Y, 0xFFAAAAAA, false);
                    guiGraphics.renderItem(costB, costBX + 2, iconY);
                    guiGraphics.renderItemDecorations(font, costB, costBX + 2, iconY);
                }
            }
        }

        if (offers.isEmpty()) {
            Component emptyMsg = Component.translatable("gui.rubius_cobblemon_additions.no_trades");
            int msgW = font.width(emptyMsg);
            guiGraphics.drawString(font, emptyMsg, rowL + (LIST_WIDTH + 4 - msgW) / 2, listTop + listVisibleRows * listItemHeight / 2 - font.lineHeight / 2, 0xFF888888, false);
        }

        int listHeight = listVisibleRows * listItemHeight;
        if (offers.size() > listVisibleRows) {
            int scrollX = rowR;
            int range = offers.size() - listVisibleRows;
            int thumbHeight = Math.max(20, (listVisibleRows * listHeight) / Math.max(1, offers.size()));
            thumbHeight = Math.min(thumbHeight, listHeight - 4);
            int thumbY = listTop + (range <= 0 ? 0 : (scrollOffset * (listHeight - thumbHeight) / range));
            guiGraphics.fill(scrollX + 1, thumbY, scrollX + SCROLLBAR_WIDTH - 1, thumbY + thumbHeight, 0xFF505050);
        }

        // Right panel: no backgrounds (UI is the background)
        int rx = left + RIGHT_PANEL_X;
        if (actionButton != null) {
            actionButton.setMessage(Component.translatable(isSellTab() ? "gui.rubius_cobblemon_additions.sell" : "gui.rubius_cobblemon_additions.buy"));
            actionButton.active = selectedIndex >= 0 && selectedIndex < offers.size();
        }
        // Selected item: show in the left panel (black box area), price below, quantity next to price
        if (selectedIndex >= 0 && selectedIndex < offers.size()) {
            CobbleDollarsShopPayloads.ShopOfferEntry entry = offers.get(selectedIndex);
            int detailX = left + LEFT_PANEL_X;
            int detailY = top + LEFT_PANEL_DETAIL_Y;
            ItemStack result = resultStackFrom(entry);
            if (!result.isEmpty()) {
                guiGraphics.renderItem(result, detailX, detailY);
                guiGraphics.renderItemDecorations(font, result, detailX, detailY);
            }
            long price = priceForDisplay(entry);
            String priceStr = isSellTab() ? "→ " + formatPrice(price) + getCurrencySymbol() : formatPrice(price) + getCurrencySymbol();
            guiGraphics.drawString(font, priceStr, detailX, top + LEFT_PANEL_PRICE_Y, 0xFFFFFFFF, false);
        }

        // Player inventory
        renderPlayerInventory(guiGraphics, left, top);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    /** Player inventory: 4 rows (3 main + hotbar), spaced out, no slot backgrounds – UI is the background. */
    private void renderPlayerInventory(GuiGraphics guiGraphics, int left, int top) {
        if (minecraft == null || minecraft.player == null) return;
        var inv = minecraft.player.getInventory();
        int invTop = top + TRADE_PANEL_HEIGHT - 19;
        int invLeft = left + 4;
        final int slotSpacingX = INVENTORY_SLOT_SPACING;
        final int slotSpacingY = INVENTORY_SLOT_SPACING;
        final int itemInset = (slotSpacingX - LIST_ITEM_ICON_SIZE) / 3;

        for (int visRow = 0; visRow < INVENTORY_VISIBLE_ROWS; visRow++) {
            for (int col = 0; col < INVENTORY_COLS; col++) {
                int slot = visRow == 3 ? col : (9 + visRow * 9 + col);
                if (slot >= inv.getContainerSize()) continue;
                int sx = invLeft + col * slotSpacingX;
                int sy = invTop + visRow * slotSpacingY;
                ItemStack stack = inv.getItem(slot);
                if (!stack.isEmpty()) {
                    guiGraphics.renderItem(stack, sx + itemInset, sy + itemInset);
                    guiGraphics.renderItemDecorations(font, stack, sx + itemInset, sy + itemInset);
                }
            }
        }
    }

    /** Only the symbol (e.g. " €" or " C"); no extra dollar text. */
    private static String getCurrencySymbol() {
        try {
            return Config.COBBLEDOLLARS_CURRENCY_SYMBOL.get();
        } catch (Exception e) {
            return " ";
        }
    }

    private static ItemStack resultStackFrom(CobbleDollarsShopPayloads.ShopOfferEntry entry) {
        var item = BuiltInRegistries.ITEM.get(entry.resultItemId());
        if (item == null || item == Items.AIR) return ItemStack.EMPTY;
        return new ItemStack(item, Math.max(1, entry.resultCount()));
    }

    /** For buy offers with an extra item cost (e.g. book for enchant), returns that item stack. */
    private static ItemStack costBStackFrom(CobbleDollarsShopPayloads.ShopOfferEntry entry) {
        if (entry == null || !entry.hasCostB()) return ItemStack.EMPTY;
        var item = BuiltInRegistries.ITEM.get(entry.costBItemId());
        if (item == null || item == Items.AIR) return ItemStack.EMPTY;
        return new ItemStack(item, Math.max(1, entry.costBCount()));
    }

    private int getRate() {
        return CobbleDollarsConfigHelper.getEffectiveEmeraldRate();
    }

    /** C$ amount to display for this offer. Buy: directPrice ? emeraldCount : emeraldCount*rate; Sell: emeraldCount*rate. */
    private long priceForDisplay(CobbleDollarsShopPayloads.ShopOfferEntry entry) {
        if (isSellTab()) return (long) entry.emeraldCount() * getRate();
        return entry.directPrice() ? entry.emeraldCount() : (long) entry.emeraldCount() * getRate();
    }

    private static String formatPrice(long price) {
        if (price >= 1_000_000) return (price / 1_000_000) + "M";
        if (price >= 1_000) return (price / 1_000) + "K";
        return String.valueOf(price);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = (guiWidth() - WINDOW_WIDTH) / 2;
        int top = (guiHeight() - WINDOW_HEIGHT) / 2;
        int listTop = top + LIST_TOP_OFFSET;
        int tabX = left + LEFT_STRIP_WIDTH + 5 + TAB_OFFSET_X;
        int tabY = top + 4;
        int tabW = RIGHT_PANEL_X - (LEFT_STRIP_WIDTH + 8);
        int tabGapY = 2;

        if (mouseX >= tabX && mouseX < tabX + tabW) {
            if (mouseY >= tabY && mouseY < tabY + TAB_H) {
                selectedTab = 0;
                var off = currentOffers();
                selectedIndex = off.isEmpty() ? -1 : 0;
                scrollOffset = 0;
                return true;
            }
            if (mouseY >= tabY + TAB_H + tabGapY && mouseY < tabY + TAB_H + tabGapY + TAB_H) {
                selectedTab = 1;
                var off = currentOffers();
                selectedIndex = off.isEmpty() ? -1 : 0;
                scrollOffset = 0;
                return true;
            }
        }

        int rowL = left + LIST_LEFT_OFFSET;
        int rowR = rowL + LIST_WIDTH + 4;
        var offers = currentOffers();
        for (int i = 0; i < listVisibleRows; i++) {
            int idx = scrollOffset + i;
            if (idx >= offers.size()) break;
            int y = listTop + i * listItemHeight;
            if (mouseX >= rowL && mouseX < rowR && mouseY >= y && mouseY < y + listItemHeight) {
                selectedIndex = idx;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int left = (guiWidth() - WINDOW_WIDTH) / 2;
        int top = (guiHeight() - WINDOW_HEIGHT) / 2;
        int listTop = top + LIST_TOP_OFFSET;
        var offers = currentOffers();
        int rowL = left + LIST_LEFT_OFFSET;
        int rowR = rowL + LIST_WIDTH + 4 + 2 + SCROLLBAR_WIDTH;
        if (mouseX >= rowL && mouseX < rowR && mouseY >= listTop && mouseY < listTop + listVisibleRows * listItemHeight) {
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
