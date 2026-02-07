package nl.streats1.rubiusaddons.network;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import nl.streats1.rubiusaddons.Config;
import nl.streats1.rubiusaddons.integration.CobbleDollarsConfigHelper;
import nl.streats1.rubiusaddons.integration.CobbleDollarsIntegration;

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
                        nl.streats1.rubiusaddons.client.screen.CobbleDollarsShopScreen.openFromPayload(data.villagerId(), data.balance()))
        );

        // Client -> Server: buy with CobbleDollars
        registrar.playToServer(
                Objects.requireNonNull(CobbleDollarsShopPayloads.BuyWithCobbleDollars.TYPE),
                Objects.requireNonNull(CobbleDollarsShopPayloads.BuyWithCobbleDollars.STREAM_CODEC),
                CobbleDollarsShopPayloadHandlers::handleBuy
        );
    }

    private static void handleRequestShopData(CobbleDollarsShopPayloads.RequestShopData data, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) return;
            if (!Config.VILLAGERS_ACCEPT_COBBLEDOLLARS.get()) return;
            if (!CobbleDollarsIntegration.isAvailable()) return;

            long balance = CobbleDollarsIntegration.getBalance(serverPlayer);
            if (balance < 0) balance = 0;

            PacketDistributor.sendToPlayer(serverPlayer, new CobbleDollarsShopPayloads.ShopData(data.villagerId(), balance));
        });
    }

    private static void handleBuy(CobbleDollarsShopPayloads.BuyWithCobbleDollars data, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) return;
            if (!Config.VILLAGERS_ACCEPT_COBBLEDOLLARS.get()) return;
            if (!CobbleDollarsIntegration.isAvailable()) return;
            if (data.quantity() < 1) return;

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
}
