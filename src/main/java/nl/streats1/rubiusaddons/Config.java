package nl.streats1.rubiusaddons;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    public static final ModConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    // a list of strings that are treated as resource locations for items
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), () -> "", Config::validateItemName);

    // CobbleDollars integration: exchange rate for villager trades (1 emerald = X CobbleDollars)
    // See CobbleDollars docs: https://harmex.gitbook.io/cobbledollars/configuration/default-shop
    // Bank config: config/cobbledollars/bank.json has "bank": [{"item": "minecraft:emerald", "price": 500}]
    public static final ModConfigSpec.IntValue COBBLEDOLLARS_EMERALD_RATE = BUILDER
            .comment("CobbleDollars per 1 emerald when paying villagers. Default 500 matches CobbleDollars bank. Ignored if syncCobbleDollarsBankRate is true and their bank.json is found.")
            .defineInRange("cobbledollarsEmeraldRate", 500, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue SYNC_COBBLEDOLLARS_BANK_RATE = BUILDER
            .comment("If true, use the emerald price from CobbleDollars' config/cobbledollars/bank.json so villager rate matches their bank (1 emerald = X). Falls back to cobbledollarsEmeraldRate if file not found.")
            .define("syncCobbleDollarsBankRate", true);

    public static final ModConfigSpec.ConfigValue<String> COBBLEDOLLARS_CURRENCY_SYMBOL = BUILDER
            .comment("Currency symbol shown in the villager shop UI (e.g. \" C$\" or \" €\" to match CobbleDollars).")
            .define("cobbleDollarsCurrencySymbol", " C$");

    public static final ModConfigSpec.BooleanValue VILLAGERS_ACCEPT_COBBLEDOLLARS = BUILDER
            .comment("If true (and CobbleDollars mod is present), villager trades that cost emeralds can be paid with CobbleDollars balance instead.")
            .define("villagersAcceptCobbleDollars", true);

    /** When true and CobbleDollars is present, right-clicking a villager opens the CobbleDollars-style shop screen instead of the vanilla trading GUI. */
    public static final ModConfigSpec.BooleanValue USE_COBBLEDOLLARS_SHOP_UI = BUILDER
            .comment("Use CobbleDollars-style shop UI when trading with villagers (tabs, list, search, balance). Requires CobbleDollars mod.")
            .define("useCobbleDollarsShopUi", true);

    // --- Extended despawn (rare / ultra rare / shiny) ---
    public static final ModConfigSpec.DoubleValue EXTENDED_DESPAWN_MULTIPLIER_SHINY = BUILDER
            .comment("Extended despawn: shiny wild Pokémon stay longer. Multiplier on Cobblemon despawn time (e.g. 2.0 = twice as long). 1.0 = no change.")
            .defineInRange("extendedDespawn.shinyMultiplier", 2.0, 1.0, 20.0);

    public static final ModConfigSpec.DoubleValue EXTENDED_DESPAWN_MULTIPLIER_RARE = BUILDER
            .comment("Extended despawn: rare spawn wild Pokémon stay longer. 1.0 = no change.")
            .defineInRange("extendedDespawn.rareMultiplier", 1.5, 1.0, 20.0);

    public static final ModConfigSpec.DoubleValue EXTENDED_DESPAWN_MULTIPLIER_ULTRA_RARE = BUILDER
            .comment("Extended despawn: ultra rare spawn wild Pokémon stay longer. 1.0 = no change.")
            .defineInRange("extendedDespawn.ultraRareMultiplier", 2.0, 1.0, 20.0);

    static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }
}
