package nl.streats1.rubiusaddons.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import nl.streats1.rubiusaddons.RubiusCobblemonAdditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Payloads for the CobbleDollars-style villager shop UI.
 */
@SuppressWarnings("null") // StreamCodec composite generics trigger null checker variance warnings
public final class CobbleDollarsShopPayloads {

    private static final String PREFIX = "cobbledollars_shop/";
    private static final StreamCodec<ByteBuf, Integer> VAR_INT = Objects.requireNonNull(ByteBufCodecs.VAR_INT);
    private static final StreamCodec<ByteBuf, Long> VAR_LONG = Objects.requireNonNull(ByteBufCodecs.VAR_LONG);
    private static final StreamCodec<ByteBuf, ResourceLocation> RESOURCE_LOCATION =
            ByteBufCodecs.STRING_UTF8.map(ResourceLocation::parse, ResourceLocation::toString);
    private static final StreamCodec<ByteBuf, Boolean> BOOL = ByteBufCodecs.BOOL;

    /** One trade entry sent server→client. For buy: optional costB; directPrice=true means emeraldCount is price in C$ (no rate). */
    public record ShopOfferEntry(ResourceLocation resultItemId, int resultCount, int emeraldCount,
                                ResourceLocation costBItemId, int costBCount, boolean directPrice) {
        public static final StreamCodec<ByteBuf, ShopOfferEntry> STREAM_CODEC =
                StreamCodec.composite(
                        RESOURCE_LOCATION,
                        ShopOfferEntry::resultItemId,
                        VAR_INT,
                        ShopOfferEntry::resultCount,
                        VAR_INT,
                        ShopOfferEntry::emeraldCount,
                        RESOURCE_LOCATION,
                        ShopOfferEntry::costBItemId,
                        VAR_INT,
                        ShopOfferEntry::costBCount,
                        BOOL,
                        ShopOfferEntry::directPrice,
                        ShopOfferEntry::new
                );

        /** True if this buy offer requires an extra item from the player (e.g. book for enchant). */
        public boolean hasCostB() {
            return costBCount > 0 && costBItemId != null && !costBItemId.getPath().equals("air");
        }
    }

    private static final StreamCodec<ByteBuf, List<ShopOfferEntry>> OFFERS_LIST_CODEC =
            StreamCodec.of(
                    (buf, list) -> {
                        VAR_INT.encode(buf, list.size());
                        for (ShopOfferEntry e : list) ShopOfferEntry.STREAM_CODEC.encode(buf, e);
                    },
                    buf -> {
                        int n = VAR_INT.decode(buf);
                        List<ShopOfferEntry> out = new ArrayList<>(n);
                        for (int i = 0; i < n; i++) out.add(ShopOfferEntry.STREAM_CODEC.decode(buf));
                        return out;
                    }
            );

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(RubiusCobblemonAdditions.MOD_ID, PREFIX + path);
    }

    /** Client -> Server: request to open shop (server responds with balance). */
    public record RequestShopData(int villagerId) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RequestShopData> TYPE =
                new CustomPacketPayload.Type<>(Objects.requireNonNull(id("request_shop_data")));
        public static final StreamCodec<ByteBuf, RequestShopData> STREAM_CODEC =
                Objects.requireNonNull(StreamCodec.composite(
                        VAR_INT,
                        RequestShopData::villagerId,
                        id -> new RequestShopData(Objects.requireNonNull(id))
                ));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server -> Client: balance, villager id, buy offers, sell offers, and whether buy list is from default_shop config. */
    public record ShopData(int villagerId, long balance, List<ShopOfferEntry> buyOffers, List<ShopOfferEntry> sellOffers, boolean buyOffersFromConfig) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ShopData> TYPE =
                new CustomPacketPayload.Type<>(Objects.requireNonNull(id("shop_data")));
        public static final StreamCodec<ByteBuf, ShopData> STREAM_CODEC =
                Objects.requireNonNull(StreamCodec.composite(
                        VAR_INT,
                        ShopData::villagerId,
                        VAR_LONG,
                        ShopData::balance,
                        OFFERS_LIST_CODEC,
                        ShopData::buyOffers,
                        OFFERS_LIST_CODEC,
                        ShopData::sellOffers,
                        BOOL,
                        ShopData::buyOffersFromConfig,
                        (villagerId, balance, buyOffers, sellOffers, buyOffersFromConfig) -> new ShopData(
                                Objects.requireNonNull(villagerId),
                                Objects.requireNonNull(balance),
                                Objects.requireNonNull(buyOffers),
                                Objects.requireNonNull(sellOffers),
                                buyOffersFromConfig)
                ));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> Server: execute a purchase with CobbleDollars (player pays C$, gets item). fromConfigShop=true when buy list came from default_shop. */
    public record BuyWithCobbleDollars(int villagerId, int offerIndex, int quantity, boolean fromConfigShop) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<BuyWithCobbleDollars> TYPE =
                new CustomPacketPayload.Type<>(Objects.requireNonNull(id("buy")));
        public static final StreamCodec<ByteBuf, BuyWithCobbleDollars> STREAM_CODEC =
                Objects.requireNonNull(StreamCodec.composite(
                        VAR_INT,
                        BuyWithCobbleDollars::villagerId,
                        VAR_INT,
                        BuyWithCobbleDollars::offerIndex,
                        VAR_INT,
                        BuyWithCobbleDollars::quantity,
                        BOOL,
                        BuyWithCobbleDollars::fromConfigShop,
                        (villagerId, offerIndex, quantity, fromConfigShop) -> new BuyWithCobbleDollars(
                                Objects.requireNonNull(villagerId),
                                Objects.requireNonNull(offerIndex),
                                Objects.requireNonNull(quantity),
                                fromConfigShop)
                ));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> Server: sell items for CobbleDollars (player gives item, gets C$). */
    public record SellForCobbleDollars(int villagerId, int offerIndex, int quantity) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SellForCobbleDollars> TYPE =
                new CustomPacketPayload.Type<>(Objects.requireNonNull(id("sell")));
        public static final StreamCodec<ByteBuf, SellForCobbleDollars> STREAM_CODEC =
                Objects.requireNonNull(StreamCodec.composite(
                        VAR_INT,
                        SellForCobbleDollars::villagerId,
                        VAR_INT,
                        SellForCobbleDollars::offerIndex,
                        VAR_INT,
                        SellForCobbleDollars::quantity,
                        (villagerId, offerIndex, quantity) -> new SellForCobbleDollars(
                                Objects.requireNonNull(villagerId),
                                Objects.requireNonNull(offerIndex),
                                Objects.requireNonNull(quantity)
                        )
                ));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
