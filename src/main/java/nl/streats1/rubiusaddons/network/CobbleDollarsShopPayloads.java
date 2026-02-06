package nl.streats1.rubiusaddons.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import nl.streats1.rubiusaddons.RubiusCobblemonAdditions;

import java.util.Objects;

/**
 * Payloads for the CobbleDollars-style villager shop UI.
 */
@SuppressWarnings("null") // StreamCodec composite generics trigger null checker variance warnings
public final class CobbleDollarsShopPayloads {

    private static final String PREFIX = "cobbledollars_shop/";
    private static final StreamCodec<ByteBuf, Integer> VAR_INT = Objects.requireNonNull(ByteBufCodecs.VAR_INT);
    private static final StreamCodec<ByteBuf, Long> VAR_LONG = Objects.requireNonNull(ByteBufCodecs.VAR_LONG);

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

    /** Server -> Client: balance and villager id for the shop UI. */
    public record ShopData(int villagerId, long balance) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ShopData> TYPE =
                new CustomPacketPayload.Type<>(Objects.requireNonNull(id("shop_data")));
        public static final StreamCodec<ByteBuf, ShopData> STREAM_CODEC =
                Objects.requireNonNull(StreamCodec.composite(
                        VAR_INT,
                        ShopData::villagerId,
                        VAR_LONG,
                        ShopData::balance,
                        (villagerId, balance) -> new ShopData(Objects.requireNonNull(villagerId), Objects.requireNonNull(balance))
                ));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> Server: execute a purchase with CobbleDollars. */
    public record BuyWithCobbleDollars(int villagerId, int offerIndex, int quantity) implements CustomPacketPayload {
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
                        (villagerId, offerIndex, quantity) -> new BuyWithCobbleDollars(
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
