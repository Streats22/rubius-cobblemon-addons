package nl.streats1.rubiusaddons.integration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import nl.streats1.rubiusaddons.Config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.OptionalInt;

/**
 * Reads CobbleDollars config so we can stay in sync with their options.
 * Docs: https://harmex.gitbook.io/cobbledollars/configuration/default-shop
 * Bank: config/cobbledollars/bank.json with "bank": [{"item": "minecraft:emerald", "price": 500}]
 */
public final class CobbleDollarsConfigHelper {

    private static final String COBBLEDOLLARS_CONFIG_SUBDIR = "cobbledollars";
    private static final String BANK_FILE = "bank.json";
    private static final String EMERALD_ITEM = "minecraft:emerald";

    /** Cached emerald price from their bank config; -1 means not yet read or not found. */
    private static int cachedBankEmeraldPrice = -1;

    /**
     * Returns the CobbleDollars bank emerald price from config/cobbledollars/bank.json if present.
     * Format 2.X: {"bank": [{"item": "minecraft:emerald", "price": 500}]}
     * Returns empty if file missing or emerald not found.
     */
    public static OptionalInt getBankEmeraldPrice() {
        if (cachedBankEmeraldPrice >= 0) return OptionalInt.of(cachedBankEmeraldPrice);
        if (cachedBankEmeraldPrice == -2) return OptionalInt.empty(); // already checked, not found
        Path configDir = getConfigDirectory();
        if (configDir == null) {
            return OptionalInt.empty();
        }
        Path bankFile = configDir.resolve(COBBLEDOLLARS_CONFIG_SUBDIR).resolve(BANK_FILE);
        if (!Files.isRegularFile(bankFile)) {
            cachedBankEmeraldPrice = -2; // mark "already checked, not found"
            return OptionalInt.empty();
        }
        try {
            String content = Files.readString(bankFile);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            if (!root.has("bank")) {
                cachedBankEmeraldPrice = -2;
                return OptionalInt.empty();
            }
            JsonElement bankEl = root.get("bank");
            if (bankEl.isJsonArray()) {
                JsonArray bank = bankEl.getAsJsonArray();
                for (JsonElement entry : bank) {
                    if (!entry.isJsonObject()) continue;
                    JsonObject obj = entry.getAsJsonObject();
                    String item = obj.has("item") ? obj.get("item").getAsString() : null;
                    if (EMERALD_ITEM.equals(item) && obj.has("price")) {
                        int price = obj.get("price").getAsInt();
                        if (price > 0) {
                            cachedBankEmeraldPrice = price;
                            return OptionalInt.of(price);
                        }
                    }
                }
            }
            cachedBankEmeraldPrice = -2;
            return OptionalInt.empty();
        } catch (Exception e) {
            cachedBankEmeraldPrice = -2;
            return OptionalInt.empty();
        }
    }

    /**
     * Returns the emerald-to-CobbleDollars rate to use for villager trades.
     * If syncCobbleDollarsBankRate is true and CobbleDollars bank.json has emerald, uses that; otherwise uses our config.
     */
    public static int getEffectiveEmeraldRate() {
        if (Config.SYNC_COBBLEDOLLARS_BANK_RATE.get()) {
            OptionalInt bank = getBankEmeraldPrice();
            if (bank.isPresent()) return bank.getAsInt();
        }
        return Config.COBBLEDOLLARS_EMERALD_RATE.get();
    }

    private static Path getConfigDirectory() {
        try {
            Class<?> pathsClass = Class.forName("net.neoforged.fml.loading.FMLPaths");
            Object configDir = pathsClass.getField("CONFIGDIR").get(null);
            if (configDir instanceof Path p) return p;
        } catch (Throwable ignored) {
        }
        try {
            Class<?> pathsClass = Class.forName("net.minecraftforge.fml.loading.FMLPaths");
            Object configDir = pathsClass.getField("CONFIGDIR").get(null);
            if (configDir instanceof Path p) return p;
        } catch (Throwable ignored) {
        }
        return Path.of("config").toAbsolutePath();
    }
}
