package nl.streats1.rubiusaddons.network;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import nl.streats1.rubiusaddons.Config;
import nl.streats1.rubiusaddons.integration.CobbleDollarsConfigHelper;
import nl.streats1.rubiusaddons.integration.CobbleDollarsIntegration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Server-side and registration for CobbleDollars shop payloads.
 * Client handler (opening the screen) is in client package.
 */
public final class CobbleDollarsShopPayloadHandlers {

    public static void registerPayloads(net.neoforged.bus.api.IEventBus modEventBus) {
        modEventBus.addListener(CobbleDollarsShopPayloadHandlers::onRegisterPayloads);
    }

    private static void onRegisterPayloads(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");

        // Client -> Server: request shop data
        registrar.playToServer(
                Objects.requireNonNull(CobbleDollarsShopPayloads.RequestShopData.TYPE),
                Objects.requireNonNull(CobbleDollarsShopPayloads.RequestShopData.STREAM_CODEC),
                CobbleDollarsShopPayloadHandlers::handleRequestShopData
        );

        // Server -> Client: open shop screen (handler runs only on client)
        registrar.playToClient(
                Objects.requireNonNull(CobbleDollarsShopPayloads.ShopData.TYPE),
                Objects.requireNonNull(CobbleDollarsShopPayloads.ShopData.STREAM_CODEC),
                (data, context) -> context.enqueueWork(() ->
                        nl.streats1.rubiusaddons.client.screen.CobbleDollarsShopScreen.openFromPayload(
                                data.villagerId(), data.balance(), data.buyOffers(), data.sellOffers(), data.buyOffersFromConfig()))
        );

        // Client -> Server: buy with CobbleDollars
        registrar.playToServer(
                Objects.requireNonNull(CobbleDollarsShopPayloads.BuyWithCobbleDollars.TYPE),
                Objects.requireNonNull(CobbleDollarsShopPayloads.BuyWithCobbleDollars.STREAM_CODEC),
                CobbleDollarsShopPayloadHandlers::handleBuy
        );

        // Client -> Server: sell for CobbleDollars
        registrar.playToServer(
                Objects.requireNonNull(CobbleDollarsShopPayloads.SellForCobbleDollars.TYPE),
                Objects.requireNonNull(CobbleDollarsShopPayloads.SellForCobbleDollars.STREAM_CODEC),
                CobbleDollarsShopPayloadHandlers::handleSell
        );
    }

    private static void handleRequestShopData(CobbleDollarsShopPayloads.RequestShopData data, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) return;
            if (!Config.VILLAGERS_ACCEPT_COBBLEDOLLARS.get()) return;
            if (!CobbleDollarsIntegration.isAvailable()) return;

            long balance = CobbleDollarsIntegration.getBalance(serverPlayer);
            if (balance < 0) balance = 0;

            List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOffers = new ArrayList<>();
            List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOffers = new ArrayList<>();
            boolean buyOffersFromConfig = false;

            ServerLevel level = serverPlayer.serverLevel();
            Entity entity = level.getEntity(data.villagerId());
            if (entity instanceof Villager villager) {
                buildOfferLists(villager.getOffers(), buyOffers, sellOffers);
            } else if (entity instanceof WanderingTrader trader) {
                buildOfferLists(trader.getOffers(), buyOffers, sellOffers);
            }
            if (buyOffers.isEmpty()) {
                List<CobbleDollarsShopPayloads.ShopOfferEntry> configBuy = CobbleDollarsConfigHelper.getDefaultShopBuyOffers();
                if (!configBuy.isEmpty()) {
                    buyOffers.addAll(configBuy);
                    buyOffersFromConfig = true;
                }
                if (entity instanceof Villager villager) {
                    buildSellOffersOnly(villager.getOffers(), sellOffers);
                } else if (entity instanceof WanderingTrader trader) {
                    buildSellOffersOnly(trader.getOffers(), sellOffers);
                }
            }

            PacketDistributor.sendToPlayer(serverPlayer, new CobbleDollarsShopPayloads.ShopData(data.villagerId(), balance, buyOffers, sellOffers, buyOffersFromConfig));
        });
    }

    private static void handleBuyFromConfig(ServerPlayer serverPlayer, int offerIndex, int quantity) {
        List<CobbleDollarsShopPayloads.ShopOfferEntry> configOffers = CobbleDollarsConfigHelper.getDefaultShopBuyOffers();
        if (offerIndex < 0 || offerIndex >= configOffers.size()) return;
        CobbleDollarsShopPayloads.ShopOfferEntry entry = configOffers.get(offerIndex);
        long cost = (long) entry.emeraldCount() * quantity;
        long balance = CobbleDollarsIntegration.getBalance(serverPlayer);
        if (balance < cost) return;
        if (!CobbleDollarsIntegration.addBalance(serverPlayer, -cost)) return;
        var item = BuiltInRegistries.ITEM.get(entry.resultItemId());
        if (item != null && item != Items.AIR) {
            ItemStack stack = new ItemStack(item, entry.resultCount() * quantity);
            if (!serverPlayer.getInventory().add(stack)) {
                serverPlayer.drop(stack, false);
            }
        }
    }

    private static void buildSellOffersOnly(List<MerchantOffer> allOffers, List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOut) {
        for (MerchantOffer o : allOffers) {
            ItemStack costA = o.getCostA();
            ItemStack result = o.getResult();
            if (result.isEmpty() || !result.is(Items.EMERALD) || costA.isEmpty()) continue;
            sellOut.add(new CobbleDollarsShopPayloads.ShopOfferEntry(
                    BuiltInRegistries.ITEM.getKey(costA.getItem()),
                    costA.getCount(),
                    result.getCount(),
                    BuiltInRegistries.ITEM.getKey(Items.AIR),
                    0,
                    false
            ));
        }
    }

    /**
     * Build buy (emerald/C$ + optional item → result) and sell (item → C$) offer lists for client.
     * Villager trades like "book + emeralds → enchanted book" are fully supported: costB is sent to the
     * client and handleBuy consumes the extra item from the player's inventory when they buy.
     */
    private static void buildOfferLists(List<MerchantOffer> allOffers,
                                        List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOut,
                                        List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOut) {
        for (MerchantOffer o : allOffers) {
            ItemStack costA = o.getCostA();
            ItemStack costB = o.getCostB();
            ItemStack result = o.getResult();
            if (result.isEmpty()) continue;
            // Buy from villager: player pays emerald (+ optional item) → gets item
            if (!costA.isEmpty() && costA.is(Items.EMERALD)) {
                ResourceLocation costBId = costB.isEmpty() ? BuiltInRegistries.ITEM.getKey(Items.AIR) : BuiltInRegistries.ITEM.getKey(costB.getItem());
                int costBCount = costB.isEmpty() ? 0 : costB.getCount();
                buyOut.add(new CobbleDollarsShopPayloads.ShopOfferEntry(
                        BuiltInRegistries.ITEM.getKey(result.getItem()),
                        result.getCount(),
                        costA.getCount(),
                        costBId,
                        costBCount,
                        false
                ));
                continue;
            }
            // Sell to villager: player gives item → gets emerald (we pay C$ instead)
            if (result.is(Items.EMERALD) && !costA.isEmpty()) {
                sellOut.add(new CobbleDollarsShopPayloads.ShopOfferEntry(
                        BuiltInRegistries.ITEM.getKey(costA.getItem()),
                        costA.getCount(),
                        result.getCount(),
                        BuiltInRegistries.ITEM.getKey(Items.AIR),
                        0,
                        false
                ));
            }
        }
    }

    private static void handleBuy(CobbleDollarsShopPayloads.BuyWithCobbleDollars data, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) return;
            if (!Config.VILLAGERS_ACCEPT_COBBLEDOLLARS.get()) return;
            if (!CobbleDollarsIntegration.isAvailable()) return;
            if (data.quantity() < 1) return;

            if (data.fromConfigShop()) {
                handleBuyFromConfig(serverPlayer, data.offerIndex(), data.quantity());
                return;
            }

            ServerLevel level = serverPlayer.serverLevel();
            Entity entity = level.getEntity(data.villagerId());
            if (!(entity instanceof Villager) && !(entity instanceof WanderingTrader)) return;

            List<MerchantOffer> allOffers = entity instanceof Villager v ? v.getOffers() : ((WanderingTrader) entity).getOffers();
            var emerald = Objects.requireNonNull(net.minecraft.world.item.Items.EMERALD);
            List<MerchantOffer> offers = allOffers.stream()
                    .filter(o -> !o.getCostA().isEmpty() && o.getCostA().is(emerald))
                    .toList();
            if (data.offerIndex() < 0 || data.offerIndex() >= offers.size()) return;

            MerchantOffer offer = offers.get(data.offerIndex());
            ItemStack costA = offer.getCostA();
            if (costA.isEmpty() || !costA.is(emerald)) return;

            int emeraldCount = costA.getCount() * data.quantity();
            int rate = CobbleDollarsConfigHelper.getEffectiveEmeraldRate();
            long cost = (long) emeraldCount * rate;

            long balance = CobbleDollarsIntegration.getBalance(serverPlayer);
            if (balance < cost) return;

            if (!CobbleDollarsIntegration.addBalance(serverPlayer, -cost)) return;

            // If trade requires an extra item (e.g. book for enchant), take it from player
            ItemStack costB = offer.getCostB();
            if (!costB.isEmpty()) {
                int totalNeeded = costB.getCount() * data.quantity();
                int have = 0;
                var inv = serverPlayer.getInventory();
                for (int slot = 0; slot < inv.getContainerSize(); slot++) {
                    ItemStack stack = inv.getItem(slot);
                    if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, costB))
                        have += stack.getCount();
                }
                if (have < totalNeeded) return;
                int remaining = totalNeeded;
                for (int slot = 0; slot < inv.getContainerSize() && remaining > 0; slot++) {
                    ItemStack stack = inv.getItem(slot);
                    if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, costB)) continue;
                    int take = Math.min(remaining, stack.getCount());
                    stack.shrink(take);
                    remaining -= take;
                }
            }

            ItemStack result = offer.getResult().copy();
            result.setCount(result.getCount() * data.quantity());
            if (!serverPlayer.getInventory().add(result)) {
                serverPlayer.drop(result, false);
            }

            // Match vanilla: update trade uses and notify merchant so villager gains XP and can level up
            Merchant merchant = (Merchant) entity;
            for (int i = 0; i < data.quantity(); i++) {
                offer.increaseUses();
                merchant.notifyTrade(offer);
            }
        });
    }

    private static void handleSell(CobbleDollarsShopPayloads.SellForCobbleDollars data, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) return;
            if (!Config.VILLAGERS_ACCEPT_COBBLEDOLLARS.get()) return;
            if (!CobbleDollarsIntegration.isAvailable()) return;
            if (data.quantity() < 1) return;

            ServerLevel level = serverPlayer.serverLevel();
            Entity entity = level.getEntity(data.villagerId());
            if (!(entity instanceof Villager) && !(entity instanceof WanderingTrader)) return;

            List<MerchantOffer> allOffers = entity instanceof Villager v ? v.getOffers() : ((WanderingTrader) entity).getOffers();
            List<MerchantOffer> sellOffers = allOffers.stream()
                    .filter(o -> !o.getResult().isEmpty() && o.getResult().is(Items.EMERALD) && !o.getCostA().isEmpty())
                    .toList();
            if (data.offerIndex() < 0 || data.offerIndex() >= sellOffers.size()) return;

            MerchantOffer offer = sellOffers.get(data.offerIndex());
            ItemStack costA = offer.getCostA();
            if (costA.isEmpty()) return;

            int perTrade = costA.getCount();
            int totalNeeded = perTrade * data.quantity();
            int have = 0;
            var inv = serverPlayer.getInventory();
            for (int slot = 0; slot < inv.getContainerSize(); slot++) {
                ItemStack stack = inv.getItem(slot);
                if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, costA))
                    have += stack.getCount();
            }
            if (have < totalNeeded) return;

            int emeraldCount = offer.getResult().getCount() * data.quantity();
            int rate = CobbleDollarsConfigHelper.getEffectiveEmeraldRate();
            long toAdd = (long) emeraldCount * rate;
            if (!CobbleDollarsIntegration.addBalance(serverPlayer, toAdd)) return;

            int remaining = totalNeeded;
            for (int slot = 0; slot < inv.getContainerSize() && remaining > 0; slot++) {
                ItemStack stack = inv.getItem(slot);
                if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, costA)) continue;
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }

            Merchant merchant = (Merchant) entity;
            for (int i = 0; i < data.quantity(); i++) {
                offer.increaseUses();
                merchant.notifyTrade(offer);
            }
        });
    }
}
