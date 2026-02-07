package nl.streats1.rubiusaddons.mixin;

import net.minecraft.world.entity.Entity;
import nl.streats1.rubiusaddons.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Extends despawn time for wild Pokémon that are shiny, rare, or ultra rare
 * when Cobblemon would normally despawn them. Only cancels discard when the
 * entity appears to be a Cobblemon PokemonEntity and is within our extended
 * time window (avoids affecting catch/other removal when possible).
 */
@Mixin(Entity.class)
public abstract class ExtendedDespawnMixin {

    @Inject(method = "discard", at = @At("HEAD"), cancellable = true, require = 0)
    private void rubiusAddons$maybePreventDespawn(Entity.RemovalReason reason, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.level().isClientSide()) return;
        if (reason != Entity.RemovalReason.DISCARDED) return;
        if (!rubiusAddons$isCobblemonPokemonEntity(self)) return;

        Object pokemon = rubiusAddons$getPokemon(self);
        if (pokemon == null) return;
        if (!rubiusAddons$isWild(pokemon)) return;

        double multiplier = rubiusAddons$getExtendedDespawnMultiplier(pokemon, self);
        if (multiplier <= 1.0) return;

        long minAge = rubiusAddons$getCobblemonDespawnerMinAgeTicks();
        long maxAge = rubiusAddons$getCobblemonDespawnerMaxAgeTicks();
        if (minAge < 0 || maxAge <= 0) return;

        int tickCount = self.tickCount;
        long effectiveMaxAge = (long) (maxAge * multiplier);
        if (tickCount >= minAge && tickCount < effectiveMaxAge) {
            ci.cancel();
        }
    }

    @Unique
    private static boolean rubiusAddons$isCobblemonPokemonEntity(Entity entity) {
        return entity != null && "com.cobblemon.mod.common.entity.pokemon.PokemonEntity".equals(entity.getClass().getName());
    }

    @Unique
    private static Object rubiusAddons$getPokemon(Entity entity) {
        try {
            var m = entity.getClass().getMethod("getPokemon");
            return m.invoke(entity);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Unique
    private static boolean rubiusAddons$isWild(Object pokemon) {
        try {
            var m = pokemon.getClass().getMethod("isWild");
            return Boolean.TRUE.equals(m.invoke(pokemon));
        } catch (Exception ignored) {
            try {
                var getStore = pokemon.getClass().getMethod("getStoreCoordinates");
                Object coords = getStore.invoke(pokemon);
                if (coords != null) {
                    var getMethod = coords.getClass().getMethod("get");
                    return getMethod.invoke(coords) == null;
                }
                return true;
            } catch (Exception ignored2) {
                return false;
            }
        }
    }

    @Unique
    private static double rubiusAddons$getExtendedDespawnMultiplier(Object pokemon, Entity entity) {
        double mult = 1.0;
        try {
            var isShiny = pokemon.getClass().getMethod("getShiny");
            if (Boolean.TRUE.equals(isShiny.invoke(pokemon))) {
                mult = Math.max(mult, Config.EXTENDED_DESPAWN_MULTIPLIER_SHINY.get());
            }
        } catch (Exception ignored) {
        }
        try {
            var getSpawnContext = pokemon.getClass().getMethod("getSpawnContext");
            Object ctx = getSpawnContext.invoke(pokemon);
            if (ctx != null) {
                var getRarity = ctx.getClass().getMethod("getRarity");
                Object rarity = getRarity.invoke(ctx);
                if (rarity != null) {
                    String name = rarity.toString().toLowerCase();
                    if (name.contains("ultra") || name.contains("ultrarare")) {
                        mult = Math.max(mult, Config.EXTENDED_DESPAWN_MULTIPLIER_ULTRA_RARE.get());
                    } else if (name.contains("rare")) {
                        mult = Math.max(mult, Config.EXTENDED_DESPAWN_MULTIPLIER_RARE.get());
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return mult;
    }

    @Unique
    private static long rubiusAddons$getCobblemonDespawnerMinAgeTicks() {
        try {
            Class<?> cobblemon = Class.forName("com.cobblemon.mod.common.Cobblemon");
            Object config = cobblemon.getField("config").get(null);
            if (config == null) return -1;
            Class<?> configClass = config.getClass();
            try {
                var f = configClass.getField("despawnerMinAgeTicks");
                return ((Number) f.get(config)).longValue();
            } catch (NoSuchFieldException e) {
                var m = configClass.getMethod("getDespawnerMinAgeTicks");
                return ((Number) m.invoke(config)).longValue();
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    @Unique
    private static long rubiusAddons$getCobblemonDespawnerMaxAgeTicks() {
        try {
            Class<?> cobblemon = Class.forName("com.cobblemon.mod.common.Cobblemon");
            Object config = cobblemon.getField("config").get(null);
            if (config == null) return -1;
            Class<?> configClass = config.getClass();
            try {
                var f = configClass.getField("despawnerMaxAgeTicks");
                return ((Number) f.get(config)).longValue();
            } catch (NoSuchFieldException e) {
                var m = configClass.getMethod("getDespawnerMaxAgeTicks");
                return ((Number) m.invoke(config)).longValue();
            }
        } catch (Exception ignored) {
        }
        return -1;
    }
}
