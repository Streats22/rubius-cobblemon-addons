# CobbleDollars GUI Reference

This document summarizes how **CobbleDollars** structures its shop UI and data so our villager CobbleDollars shop can match the look and behaviour without copying their code or assets. Reference: CobbleDollars JAR (e.g. `run/mods/CobbleDollars-neoforge-*.jar`) or extracted at **`e:\Cobbledollars`**.

## Layout (from `shop_base.png`)

- **Left side**
  - Top: small slot + narrow label area + **currency display** (e.g. “€ 1” / balance).
  - Bottom: **Player inventory** – 9×3 grid + hotbar (9 slots + row below).
- **Right side**
  - **Top panel**: category/tab bar (e.g. “Balls”, “Items”) – one row of tabs.
  - **Bottom panel**: scrollable list of **offers** for the selected category (icon + price in CobbleDollars).

So: **tabs = categories** at top right, **list = offers** below; left = balance + inventory.

## Textures (assets in JAR)

All under `assets/cobbledollars/textures/gui/`:

| Path | Purpose |
|------|--------|
| `shop/shop_base.png` | Full shop background (left inventory + right panels). |
| `shop/category_background.png` | Tab/category bar background. |
| `shop/category_background_short.png` | Shorter variant. |
| `shop/category_outline.png` | Tab outline. |
| `shop/category_outline_short.png` | Shorter outline. |
| `shop/offer_background.png` | Single offer row background. |
| `shop/offer_outline.png` | Offer row outline. |
| `shop/buy_button.png` | Buy button. |
| `shop/amount_arrow_up.png` | Quantity increase. |
| `shop/amount_arrow_down.png` | Quantity decrease. |
| `shop/bank_button.png` | Bank shortcut. |
| `shop/stock.png` | Stock indicator. |
| `cobbledollars_background.png` | CobbleDollars symbol (C with two lines). |

Our `CobbleDollarsShopScreen` **uses these textures** from our own assets (copied from CobbleDollars-style art for consistency and safety): `assets/rubius_cobblemon_addons/textures/gui/shop/` (shop_base, category_background, category_outline, offer_background, buy_button, amount_arrow_up, amount_arrow_down) and `textures/gui/bank/` (bank_base). They are bundled in the mod so the shop looks consistent regardless of CobbleDollars version.

## Classes (from JAR – for structure only)

- **Screens**: `fr/harmex/cobbledollars/.../screen/ShopScreen.class`, `BankScreen.class`
- **Widgets**:
  - `CategoryListWidget` (+ `CategoryEntry`) – category/tab list.
  - `OfferListWidget` (+ `OfferEntry`) – offer list.
  - `AmountButton` – quantity up/down.
  - `BuyButton`, `SellButton` – actions.
  - `ShopButton`, `BankButton` – navigation.
- **Data**: `Shop`, `Category`, `Offer` under `.../item/trading/shop/`
- **Menu**: `ShopMenu` (container)
- **Config**: `ShopConfig`, `ClientShopConfig`; sync via `SyncShopConfigPacket`, `SyncShopPacket`

So: **categories → offers per category**; separate Buy and Sell actions; quantity controlled by amount buttons.

---

## Learned from CobbleDollars .class files (javap)

Inspecting the compiled CobbleDollars JAR with `javap -p -constants` yields the following. Use this to match layout and behaviour without decompiling.

### ShopScreen (`common/client/gui/screen/ShopScreen.class`)

- **Extends**: `AbstractContainerScreen<ShopMenu>` (container-based screen with menu).
- **Fields**: `bgLocation`, `stockLocation` (ResourceLocation); `x`, `y` (position); `shopSynced`; `categoryList` (CategoryListWidget); `offerList` (OfferListWidget); `searchCategoryBox`, `searchOfferBox`, `buyAmountBox` (EditBox); `buyButton` (BuyButton); `buyAmount` (int); edit boxes for category name, offer price, offer stock; `isEditMode`.
- **Key methods**: `init()`, `renderBg()`, `renderLabels()`, `render()`, `resize()`, `updateWidgetPos()`, `syncShop()`, `syncShopToServer()`, `buy()`, `canBuy()`, `amountButtonClick(boolean)`, `updateBuyAmount(String)`, `toggleEditCategoryNameBox(boolean)`, etc.

### BankScreen (`common/client/gui/screen/BankScreen.class`)

- **Extends**: `AbstractContainerScreen<BankMenu>`; **implements** `ContainerListener`.
- **Fields**: `bgLocation`; `x`, `y`; `sellButton` (SellButton); `sellPrice` (BigInteger).
- **Key methods**: `init()`, `renderBg()`, `renderLabels()`, `render()`, `sell()`, `canSell()`, `containerChanged(Container)`.

### OfferListWidget (`common/client/gui/screen/widget/OfferListWidget.class`)

- **Extends**: `ObjectSelectionList<OfferListWidget$OfferEntry>`.
- **Constants** (from `javap -constants`):
  - `WIDTH = 79`
  - `HEIGHT = 173`
  - `ENTRY_WIDTH = 75`
  - `ENTRY_HEIGHT = 18`
- **Textures**: `OFFER_BACKGROUND_LOCATION`, `OFFER_OUTLINE_LOCATION`.
- **Methods**: `createOfferEntries(Category)`, `search(String)`, `editOfferPrice(String)`, `editOfferStock(String)`; overrides `renderListBackground`, `renderHeader`, `renderListSeparators`, `renderSelection`, `renderDecorations`, `renderWidget`; `getRowWidth()`, `getRowLeft()`, `getRowRight()`, `getRowTop(int)`, `getRowBottom(int)`, `getScrollbarPosition()`.

So CobbleDollars uses **offer row height 18** and list area **79×173** with **75×18** per entry; they draw `OFFER_BACKGROUND` and `OFFER_OUTLINE` in the list.

### OfferListWidget$OfferEntry

- **Extends**: `ObjectSelectionList$Entry<OfferEntry>`.
- **Fields**: `offer` (Offer), `isAddOffer`, `offerIndex`, `listIndex`, `x`, `y`; item, price (BigInteger), stock.
- **Methods**: `render(...)` (full entry), `renderBack(...)` (background), `isMouseOverCross(int,int)`.

### CategoryListWidget (`common/client/gui/screen/widget/CategoryListWidget.class`)

- **Extends**: `ObjectSelectionList<CategoryListWidget$CategoryEntry>`.
- **Constants**:
  - `WIDTH = 78`
  - `HEIGHT = 75`
  - `ENTRY_WIDTH = 71`
  - `ENTRY_HEIGHT = 13`
- **Textures**: `CATEGORY_BACKGROUND_LOCATION`, `CATEGORY_BACKGROUND_SHORT_LOCATION`, `CATEGORY_OUTLINE_LOCATION`, `CATEGORY_OUTLINE_SHORT_LOCATION`.
- **Methods**: `createCategoryEntries()`, `search(String)`, `editCategoryName(String)`; same render/row/scrollbar overrides as OfferListWidget.

So category/tab rows are **13px** high, list area **78×75**, entry **71×13**.

### AmountButton (`AmountButton.class`)

- **Extends**: `Button`.
- **Fields**: `up` (boolean).
- **Constants**: `WIDTH`, `HEIGHT`; `UP_LOCATION`, `DOWN_LOCATION` (ResourceLocation).
- **Methods**: `renderWidget(GuiGraphics, int, int, float)`, `playDownSound(SoundManager)`.

### BuyButton / SellButton

- **Extend**: `Button`.
- **Fields**: BuyButton holds `shopScreen`; SellButton holds `bankScreen`.
- **Constants**: `WIDTH`, `HEIGHT`; `TEXTURE_LOCATION`.
- **Methods**: `renderWidget(...)`, `playDownSound(...)`.

### GuiUtilsKt (`common/client/utils/GuiUtilsKt.class`)

- **Static**: `TEXTURE_WIDTH`, `TEXTURE_HEIGHT`; `CROSS_LOCATION`, `textureLocation`; `tick`, `lastAmount`, `lastAmountDifference`.
- **Methods**: `tick()`, `renderCobbleDollarsElement(GuiGraphics, x, y, boolean, BigInteger, boolean, Context, Integer)` (and overload), `renderAnimation(...)`.

Used to draw the CobbleDollars currency element (e.g. “€” + amount) with optional animation.

### CobbleDollarsOverlay (`common/client/gui/CobbleDollarsOverlay.class`)

- **Extends**: `Gui`.
- **Fields**: `screenException` – `List<Class<? extends Screen>>` (screens on which the overlay is not drawn).
- **Methods**: `render(GuiGraphics, DeltaTracker)`.

So the balance/currency overlay is drawn on top of the game HUD unless the current screen is in the exception list.

### GuiUtilsKt, Context, OverlayPosition (from javap)

- **GuiUtilsKt**: `TEXTURE_WIDTH = 54`, `TEXTURE_HEIGHT = 14` (CobbleDollars logo texture). `renderCobbleDollarsElement(GuiGraphics, x, y, boolean, BigInteger, boolean, Context, Integer)` draws the currency element (logo + amount). `textureLocation` and `CROSS_LOCATION` for assets. We use the same 54×14 logo for balance in the shop left strip.
- **Context** (enum): `PLAYER`, `SHOP`, `BANK` – context for where the currency element is drawn.
- **OverlayPosition** (enum): `DISABLED`, `TOP_LEFT`, `TOP_RIGHT`, `BOTTOM_LEFT`, `BOTTOM_RIGHT` – HUD overlay position for balance.
- **CobbleMerchantRenderer**: Renders `CobbleMerchant` (custom villager) with `VillagerModel` and texture `COBBLE_MERCHANT_BASE_SKIN`. We use normal villagers; no renderer change needed.

### Using these values in our mod

- **Offer list**: CobbleDollars uses **ENTRY_HEIGHT = 18** and **ENTRY_WIDTH = 75**; list widget **79×173**. Our `listItemHeight` and list width can be set to 18 and ~75 if we want pixel-accurate match.
- **Category/tabs**: **ENTRY_HEIGHT = 13**, **ENTRY_WIDTH = 71**; we use category_background (69×11) and category_outline (76×19) textures.
- **Rendering**: They use `renderSelection` for the selected row and draw `OFFER_BACKGROUND`/`OFFER_OUTLINE` per entry; we removed the dark offer_background blit and use a light tint for selected/hover only.

## Lang keys (CobbleDollars)

- `screen.cobbledollars.shop` = "Shop"
- `screen.cobbledollars.bank` = "Bank"
- `gui.cobbledollars.add_offer` = "Click to add %s as an offer"

We keep our own keys under `gui.rubius_cobblemon_additions.*` but can mirror wording (e.g. “Shop”, “Buy”, “Sell”).

## Default shop config (runtime, not in JAR)

Per [CobbleDollars wiki](https://harmex.gitbook.io/cobbledollars/configuration/default-shop):  
Config is at `config/cobbledollars/default_shop.json` with:

- `defaultShop`: array of categories.
- Each category: `{ "Category Name": [ offers ] }`.
- Offer: `item` or `tag`, `price` (number or e.g. `"2k"`), optional `components`.

Our villager shop does **not** use this file; we build offers from the villager’s `MerchantOffer` list. The reference is only for understanding how CobbleDollars names and structures categories/offers.

## Aligning our villager shop UI

1. **Tabs**: We already use a top tab bar (“Buy” | “Sell”) instead of a single mixed list – matches “category bar” at top.
2. **List**: One list per tab (buy offers or sell offers) with icon + price – matches “offer list” per category.
3. **Right panel**: Selected offer + quantity + one action button (Buy or Sell) – same idea as their detail + Buy/Sell + amount.
4. **Balance**: We show balance at bottom; they show currency (e.g. “€ 1”) on the left. We can keep bottom or add a small balance strip on the left if we want to mirror them more.
5. **Optional later**: Add our own textures (e.g. `category_background`, `offer_background`, `buy_button`) under `assets/rubius_cobblemon_additions/textures/gui/shop/` and blit them in `CobbleDollarsShopScreen` so the look matches CobbleDollars more closely without using their assets.

## Datapacks / config

CobbleDollars ships datapacks under `data/cobbledollars/datapacks/` (e.g. disable villager spawning). Their **shop content** is config-driven (`default_shop.json`), not datapack-defined. Our integration is villager-based and does not depend on that config.
