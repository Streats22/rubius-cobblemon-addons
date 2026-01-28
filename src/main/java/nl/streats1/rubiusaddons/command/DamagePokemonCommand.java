package nl.streats1.rubiusaddons.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import nl.streats1.rubiusaddons.RubiusCobblemonAdditions;

import java.util.Collection;

/**
 * Command to damage Pokemon for testing purposes.
 * Usage: /damagepokemon [player] [slot] [damage]
 * - player: The player whose Pokemon to damage (defaults to self)
 * - slot: The party slot (0-5, defaults to 0)
 * - damage: Amount of damage to deal (defaults to 50% of max HP)
 */
public class DamagePokemonCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("damagepokemon")
                .requires(source -> source.hasPermission(2)) // Requires OP level 2
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("slot", IntegerArgumentType.integer(0, 5))
                                .then(Commands.argument("damage", FloatArgumentType.floatArg(0.0f))
                                        .executes(context -> damagePokemon(
                                                context,
                                                EntityArgument.getPlayers(context, "targets"),
                                                IntegerArgumentType.getInteger(context, "slot"),
                                                FloatArgumentType.getFloat(context, "damage")
                                        ))
                                )
                                .executes(context -> damagePokemon(
                                        context,
                                        EntityArgument.getPlayers(context, "targets"),
                                        IntegerArgumentType.getInteger(context, "slot"),
                                        -1.0f // Use default damage (50% of max HP)
                                ))
                        )
                        .executes(context -> damagePokemon(
                                context,
                                EntityArgument.getPlayers(context, "targets"),
                                0, // Default to slot 0
                                -1.0f // Use default damage
                        ))
                )
                .executes(context -> damagePokemon(
                        context,
                        java.util.Collections.singleton(context.getSource().getPlayerOrException()),
                        0, // Default to slot 0
                        -1.0f // Use default damage
                ))
        );
    }

    private static int damagePokemon(CommandContext<CommandSourceStack> context,
                                     Collection<ServerPlayer> targets,
                                     int slot,
                                     float damage) {
        if (!ModList.get().isLoaded("cobblemon")) {
            context.getSource().sendFailure(Component.literal("Cobblemon is not loaded!"));
            return 0;
        }

        int successCount = 0;

        for (ServerPlayer player : targets) {
            try {
                if (damagePokemonForPlayer(player, slot, damage)) {
                    successCount++;
                    context.getSource().sendSuccess(() -> Component.literal(
                            "Successfully damaged Pokemon in slot " + slot + " for " + player.getName().getString()
                    ), true);
                } else {
                    context.getSource().sendFailure(Component.literal(
                            "Failed to damage Pokemon in slot " + slot + " for " + player.getName().getString() +
                                    ". Check server logs for details. Common issues: slot is empty, Pokemon already at 0 HP, or Cobblemon API issue."
                    ));
                }
            } catch (Exception e) {
                RubiusCobblemonAdditions.LOGGER.error("Error damaging Pokemon for player {}: {}",
                        player.getName().getString(), e.getMessage(), e);
                context.getSource().sendFailure(Component.literal(
                        "Error damaging Pokemon: " + e.getMessage()
                ));
            }
        }

        return successCount;
    }

    private static boolean damagePokemonForPlayer(ServerPlayer player, int slot, float damageAmount) {
        try {

            // Get Cobblemon instance
            Class<?> cobblemonClass = Class.forName("com.cobblemon.mod.common.Cobblemon");
            Object cobblemonInstance = null;

            try {
                var instanceField = cobblemonClass.getField("INSTANCE");
                cobblemonInstance = instanceField.get(null);
            } catch (NoSuchFieldException e) {
                try {
                    var getInstanceMethod = cobblemonClass.getMethod("getInstance");
                    cobblemonInstance = getInstanceMethod.invoke(null);
                } catch (NoSuchMethodException ignored) {
                    RubiusCobblemonAdditions.LOGGER.error("Could not get Cobblemon instance");
                    return false;
                }
            }

            if (cobblemonInstance == null) {
                RubiusCobblemonAdditions.LOGGER.error("Cobblemon instance is null");
                return false;
            }

            // Get storage manager
            var getStorageMethod = cobblemonClass.getMethod("getStorage");
            Object storageManager = getStorageMethod.invoke(cobblemonInstance);

            if (storageManager == null) {
                RubiusCobblemonAdditions.LOGGER.error("Storage manager is null");
                return false;
            }

            // Get party
            var getPartyMethod = storageManager.getClass().getMethod("getParty", ServerPlayer.class);
            Object partyStore = getPartyMethod.invoke(storageManager, player);

            if (partyStore == null) {
                RubiusCobblemonAdditions.LOGGER.error("Party store is null for player {}", player.getName().getString());
                return false;
            }


            // Try multiple methods to get Pokemon by slot
            Object pokemon = null;

            // Method 1: Try get() method with slot index
            try {
                var getMethod = partyStore.getClass().getMethod("get", int.class);
                pokemon = getMethod.invoke(partyStore, slot);
            } catch (NoSuchMethodException e) {
            } catch (Exception e) {
            }

            // Method 2: Use toGappyList() and get by index
            if (pokemon == null) {
                try {
                    var toGappyListMethod = partyStore.getClass().getMethod("toGappyList");
                    java.util.List<?> pokemonList = (java.util.List<?>) toGappyListMethod.invoke(partyStore);


                    if (pokemonList != null && pokemonList.size() > slot) {
                        pokemon = pokemonList.get(slot);
                    } else {
                        RubiusCobblemonAdditions.LOGGER.warn("Pokemon list is null or too small. Size: {}, requested slot: {}",
                                pokemonList != null ? pokemonList.size() : 0, slot);
                    }
                } catch (NoSuchMethodException e) {
                } catch (Exception e) {
                    RubiusCobblemonAdditions.LOGGER.error("Error using toGappyList(): {}", e.getMessage(), e);
                }
            }

            // Method 3: Try iterator and count to slot
            if (pokemon == null) {
                try {
                    var iteratorMethod = partyStore.getClass().getMethod("iterator");
                    var iterator = iteratorMethod.invoke(partyStore);
                    if (iterator != null) {
                        var hasNextMethod = iterator.getClass().getMethod("hasNext");
                        var nextMethod = iterator.getClass().getMethod("next");
                        int currentSlot = 0;
                        while ((Boolean) hasNextMethod.invoke(iterator)) {
                            var pkmn = nextMethod.invoke(iterator);
                            if (currentSlot == slot) {
                                pokemon = pkmn;
                                break;
                            }
                            currentSlot++;
                        }
                    }
                } catch (Exception e) {
                }
            }

            if (pokemon == null) {
                RubiusCobblemonAdditions.LOGGER.error("Could not find Pokemon in slot {} for player {}", slot, player.getName().getString());
                return false;
            }


            // Get current and max HP (Cobblemon uses int, not float)
            int currentHp = 0;
            int maxHp = 0;

            try {
                var hpMethod = pokemon.getClass().getMethod("getCurrentHealth");
                var maxHpMethod = pokemon.getClass().getMethod("getMaxHealth");
                currentHp = ((Number) hpMethod.invoke(pokemon)).intValue();
                maxHp = ((Number) maxHpMethod.invoke(pokemon)).intValue();
            } catch (NoSuchMethodException e) {
                try {
                    var hpMethod = pokemon.getClass().getMethod("getHp");
                    var maxHpMethod = pokemon.getClass().getMethod("getMaxHp");
                    currentHp = ((Number) hpMethod.invoke(pokemon)).intValue();
                    maxHp = ((Number) maxHpMethod.invoke(pokemon)).intValue();
                } catch (NoSuchMethodException e2) {
                    RubiusCobblemonAdditions.LOGGER.error("Could not find HP methods for Pokemon. Available methods: {}",
                            java.util.Arrays.toString(pokemon.getClass().getMethods()));
                    return false;
                }
            }

            // Check if already at 0 or very low HP
            if (currentHp <= 0) {
                RubiusCobblemonAdditions.LOGGER.warn("Pokemon is already at 0 HP (current: {})", currentHp);
                return false;
            }

            // Calculate damage amount if not specified
            float actualDamage = damageAmount;
            if (damageAmount < 0) {
                // Default: damage 50% of max HP
                actualDamage = maxHp * 0.5f;
            }

            // Calculate new HP (ensure it doesn't go below 1)
            int newHp = Math.max(1, (int) (currentHp - actualDamage));


            // Set new HP - Cobblemon uses setCurrentHealth(int), not float!
            // Try both getMethod and getDeclaredMethod since it might be a Kotlin method
            try {
                java.lang.reflect.Method setHpMethod = null;
                try {
                    setHpMethod = pokemon.getClass().getMethod("setCurrentHealth", int.class);
                } catch (NoSuchMethodException e) {
                    // Try declared method (might be needed for Kotlin)
                    setHpMethod = pokemon.getClass().getDeclaredMethod("setCurrentHealth", int.class);
                    setHpMethod.setAccessible(true);
                }

                if (setHpMethod != null) {
                    setHpMethod.invoke(pokemon, newHp);
                    return true;
                }
            } catch (NoSuchMethodException e) {
                // Try alternative method names
                try {
                    java.lang.reflect.Method setHpMethod = null;
                    try {
                        setHpMethod = pokemon.getClass().getMethod("setHp", int.class);
                    } catch (NoSuchMethodException e2) {
                        setHpMethod = pokemon.getClass().getDeclaredMethod("setHp", int.class);
                        setHpMethod.setAccessible(true);
                    }

                    if (setHpMethod != null) {
                        setHpMethod.invoke(pokemon, newHp);
                        return true;
                    }
                } catch (NoSuchMethodException e2) {
                    RubiusCobblemonAdditions.LOGGER.error("Could not find setCurrentHealth(int) or setHp(int) method for Pokemon");
                    // Log available methods that might be relevant
                    var methods = pokemon.getClass().getMethods();
                    for (var method : methods) {
                        if (method.getName().contains("Health") || method.getName().contains("Hp") || method.getName().contains("HP")) {
                            RubiusCobblemonAdditions.LOGGER.error("Found potential HP method: {} with parameters: {}",
                                    method.getName(), java.util.Arrays.toString(method.getParameterTypes()));
                        }
                    }
                    return false;
                }
            } catch (Exception e) {
                RubiusCobblemonAdditions.LOGGER.error("Error calling setCurrentHealth: {}", e.getMessage(), e);
                return false;
            }

        } catch (Exception e) {
            RubiusCobblemonAdditions.LOGGER.error("Error damaging Pokemon: {}", e.getMessage(), e);
            e.printStackTrace();
            return false;
        }
        return false;
    }
}

