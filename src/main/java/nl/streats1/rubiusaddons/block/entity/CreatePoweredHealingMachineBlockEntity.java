package nl.streats1.rubiusaddons.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import nl.streats1.rubiusaddons.RubiusCobblemonAdditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// Direct Cobblemon API imports (available via Maven dependency)
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.PokemonStoreManager;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.battles.BattleRegistry;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Block entity for the Create-powered healing machine.
 * Integrates Create's rotation system with Cobblemon's healing machine functionality.
 */
public class CreatePoweredHealingMachineBlockEntity extends BlockEntity {
    
    // SU (Stress Units) thresholds for recharge time reduction
    // SU represents the stress capacity/usage of the Create rotation network
    private static final int MIN_SU_FOR_BOOST = 256; // Minimum SU to start reducing recharge time
    private static final int MAX_SU_FOR_INSTANT = 1024; // SU for instant recharge
    
    // Recharge time constants (in seconds)
    private static final int BASE_RECHARGE_TIME = 900; // 15 minutes at 0 SU
    private static final int MIN_RECHARGE_TIME = 350; // ~5.8 minutes at MIN_SU+ SU
    private static final int INSTANT_RECHARGE_TIME = 0; // Instant at MAX_SU SU
    
    // Cache for reflection results to avoid repeated lookups and log spam
    private static final Map<String, Boolean> REFLECTION_CACHE = new ConcurrentHashMap<>();
    private static volatile boolean hasLoggedBattleClassNotFound = false;
    private static volatile boolean hasLoggedCompanionCanHealError = false;
    
    // Current Stress Units (SU) from Create rotation system
    private float currentSU = 0.0f;
    
    // Healing progress tracking
    private long healingStartTime = 0;
    private boolean isHealing = false;
    private Player healingPlayer = null; // Track which player is being healed
    private UUID healingPlayerUUID = null; // Store UUID for persistence
    
    // Recharge tracking (time when healing completed, used to calculate recharge cooldown)
    private long rechargeStartTime = 0; // Game time when recharge started (after healing completed)
    
    public CreatePoweredHealingMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CREATE_POWERED_HEALING_MACHINE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CreatePoweredHealingMachineBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }
        
        // Update SU from Create rotation system
        blockEntity.updateSU(level, pos);
        
        // Handle healing logic if Cobblemon is loaded
        if (ModList.get().isLoaded("cobblemon")) {
            blockEntity.tickHealing(level);
        }
    }
    
    
    /**
     * Updates the current SU (Stress Units) by checking Create's rotation system.
     * SU represents the stress capacity/usage of the rotation network.
     */
    private void updateSU(Level level, BlockPos pos) {
        if (!ModList.get().isLoaded("create")) {
            currentSU = 0.0f;
            return;
        }
        
        // Check for rotation sources from Create
        // We'll check all 6 directions for rotation sources
        float maxSU = 0.0f;
        
        for (Direction direction : Direction.values()) {
            BlockPos checkPos = pos.relative(direction);
            float su = getSUFromCreate(level, checkPos, direction.getOpposite());
            if (su > maxSU) {
                maxSU = su;
            }
        }
        
        currentSU = maxSU;
    }
    
    /**
     * Gets SU (Stress Units) from Create's rotation system.
     * SU represents the stress capacity/usage of the rotation network.
     * This checks for rotation sources connected to this block.
     */
    private float getSUFromCreate(Level level, BlockPos pos, Direction direction) {
        if (!ModList.get().isLoaded("create")) {
            return 0.0f;
        }
        
        try {
            // Check if the adjacent block is a Create rotation source
            var blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                // Try to access Create's KineticBlockEntity
                Class<?> kineticClass = Class.forName("com.simibubi.create.content.kinetics.base.KineticBlockEntity");
                if (kineticClass.isInstance(blockEntity)) {
                    // Get stress capacity/usage from the kinetic network
                    try {
                        // Try to get stress capacity (total SU available)
                        var getCapacityMethod = kineticClass.getMethod("getCapacity");
                        float capacity = ((Number) getCapacityMethod.invoke(blockEntity)).floatValue();
                        
                        // Also try to get stress usage (current SU being used)
                        try {
                            var getUsageMethod = kineticClass.getMethod("getUsage");
                            float usage = ((Number) getUsageMethod.invoke(blockEntity)).floatValue();
                            
                            // Use the higher of capacity or usage as our SU value
                            float su = Math.max(capacity, usage);
                            if (su > 0.1f) {
                                RubiusCobblemonAdditions.LOGGER.debug("Found SU from Create: {} (capacity: {}, usage: {}) at {}", su, capacity, usage, pos);
                                return su;
                            }
                        } catch (NoSuchMethodException e) {
                            // If usage method doesn't exist, just use capacity
                            if (capacity > 0.1f) {
                                RubiusCobblemonAdditions.LOGGER.debug("Found SU capacity from Create: {} at {}", capacity, pos);
                                return capacity;
                            }
                        }
                    } catch (NoSuchMethodException e) {
                        // Fallback: Calculate SU from rotation speed
                        // Higher speed = more SU (rough approximation)
                        var speedMethod = kineticClass.getMethod("getSpeed");
                        float speed = ((Number) speedMethod.invoke(blockEntity)).floatValue();
                        
                        // Convert speed to approximate SU
                        // SU roughly correlates with rotation speed: SU ≈ speed * 16 (rough approximation)
                        float su = Math.abs(speed) * 16.0f;
                        if (su > 0.1f) {
                            RubiusCobblemonAdditions.LOGGER.debug("Calculated SU from speed: {} at {}", su, pos);
                            return su;
                        }
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            // Create classes not found - Create might not be loaded
            RubiusCobblemonAdditions.LOGGER.debug("Create classes not found: {}", e.getMessage());
        } catch (Exception e) {
            // Create not available or API changed - return 0
            RubiusCobblemonAdditions.LOGGER.debug("Could not get SU from Create: {}", e.getMessage());
        }
        
        return 0.0f;
    }
    
    /**
     * Ticks the healing process, checking if healing should complete.
     * The healing animation itself is quick (a few seconds), but the recharge
     * time after healing is what gets reduced by SU (Stress Units).
     */
    private void tickHealing(Level level) {
        if (!isHealing) {
            return;
        }
        
        long currentTime = level.getGameTime();
        long elapsedTicks = currentTime - healingStartTime;
        
        // Healing animation time (quick, not affected by SU)
        // Cobblemon healing machines typically take a few seconds for the animation
        long healingAnimationTime = 100L; // 5 seconds at 20 TPS
        
        // Check if healing animation is complete
        if (elapsedTicks >= healingAnimationTime) {
            completeHealing(level);
        }
    }
    
    /**
     * Calculates the recharge time in seconds based on current SU (Stress Units).
     * Rules:
     * - At 0 SU: 900 seconds (15 minutes) - default Cobblemon recharge time
     * - At MIN_SU+ (256 SU): 350 seconds (~5.8 minutes)
     * - At MAX_SU (1024 SU): 0 seconds (instant recharge)
     * - Linear scaling between MIN_SU and MAX_SU
     * 
     * @return Recharge time in seconds
     */
    private int calculateRechargeTime() {
        if (currentSU < MIN_SU_FOR_BOOST) {
            // No SU boost - use default recharge time
            return BASE_RECHARGE_TIME;
        }
        
        if (currentSU >= MAX_SU_FOR_INSTANT) {
            // Instant recharge at max SU
            return INSTANT_RECHARGE_TIME;
        }
        
        // Linear scaling between MIN_SU (256) and MAX_SU (1024)
        // At MIN_SU: MIN_RECHARGE_TIME (350 seconds)
        // At MAX_SU: INSTANT_RECHARGE_TIME (0 seconds)
        float suRange = MAX_SU_FOR_INSTANT - MIN_SU_FOR_BOOST; // 768 SU range
        float suAboveThreshold = currentSU - MIN_SU_FOR_BOOST;
        
        // Calculate recharge time: starts at MIN_RECHARGE_TIME (350), scales down to 0
        // Formula: recharge = 350 - (suAboveThreshold / 768) * 350
        float rechargeSeconds = MIN_RECHARGE_TIME - 
            (suAboveThreshold / suRange) * MIN_RECHARGE_TIME;
        
        return Math.max(0, (int) rechargeSeconds);
    }
    
    /**
     * Checks if the healing machine is recharged and ready to use.
     * 
     * @param level The level to get current game time
     * @return true if the machine is recharged, false if still on cooldown
     */
    private boolean isRecharged(Level level) {
        if (rechargeStartTime == 0) {
            // Never used before, or recharge completed
            return true;
        }
        
        long currentTime = level.getGameTime();
        long elapsedTicks = currentTime - rechargeStartTime;
        
        // Calculate recharge time based on current SU (recharge time can change if SU changes)
        int rechargeTimeSeconds = calculateRechargeTime();
        long rechargeTimeTicks = rechargeTimeSeconds * 20L; // Convert seconds to ticks
        
        return elapsedTicks >= rechargeTimeTicks;
    }
    
    /**
     * Gets the remaining recharge time in seconds.
     * 
     * @param level The level to get current game time
     * @return Remaining recharge time in seconds, or 0 if recharged
     */
    public int getRemainingRechargeTime(Level level) {
        if (rechargeStartTime == 0 || isRecharged(level)) {
            return 0;
        }
        
        long currentTime = level.getGameTime();
        long elapsedTicks = currentTime - rechargeStartTime;
        int rechargeTimeSeconds = calculateRechargeTime();
        long rechargeTimeTicks = rechargeTimeSeconds * 20L;
        
        long remainingTicks = Math.max(0, rechargeTimeTicks - elapsedTicks);
        return (int) (remainingTicks / 20L); // Convert ticks to seconds
    }
    
    /**
     * Handles player interaction with the healing machine.
     * Uses the same interaction logic as Cobblemon's healing machine.
     */
    public InteractionResult onUse(Player player, InteractionHand hand) {
        if (!ModList.get().isLoaded("cobblemon")) {
            return InteractionResult.PASS;
        }
        
        // Use reflection to interact with Cobblemon's healing machine API
        try {
            // Check if player is in battle - prevent healing during battle
            if (isPlayerInBattle(player)) {
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    var message = net.minecraft.network.chat.Component.translatable("cobblemon.healing.machine.in.battle");
                    try {
                        serverPlayer.sendSystemMessage(message);
                    } catch (Exception e) {
                        // Fallback message if translation key doesn't exist
                        var fallbackMessage = net.minecraft.network.chat.Component.literal("You cannot heal during battle!");
                        serverPlayer.sendSystemMessage(fallbackMessage);
                    }
                }
                return InteractionResult.CONSUME;
            }
            
            // Check if machine is recharged
            if (!isRecharged(level)) {
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    int remainingSeconds = getRemainingRechargeTime(level);
                    int minutes = remainingSeconds / 60;
                    int seconds = remainingSeconds % 60;
                    var message = net.minecraft.network.chat.Component.translatable(
                        "cobblemon.healing.machine.recharging", 
                        minutes, seconds
                    );
                    try {
                        serverPlayer.sendSystemMessage(message);
                    } catch (Exception e) {
                        // Fallback message if translation key doesn't exist
                        var fallbackMessage = net.minecraft.network.chat.Component.literal(
                            String.format("Healing machine is recharging... %d:%02d remaining", minutes, seconds)
                        );
                        serverPlayer.sendSystemMessage(fallbackMessage);
                    }
                }
                return InteractionResult.CONSUME;
            }
            
            // Check if player can heal
            if (canHealPokemon(player)) {
                if (!isHealing) {
                    startHealing(player);
                    return InteractionResult.SUCCESS;
                } else {
                    // Already healing - check if it's the same player
                    if (healingPlayer != null && healingPlayer.equals(player)) {
                        return InteractionResult.CONSUME;
                    } else {
                        // Different player trying to use - show message
                        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                            var message = net.minecraft.network.chat.Component.translatable("cobblemon.healing.machine.in.use");
                            serverPlayer.sendSystemMessage(message);
                        }
                        return InteractionResult.CONSUME;
                    }
                }
            } else {
                // Player has no Pokemon that need healing
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    var message = net.minecraft.network.chat.Component.translatable("cobblemon.healing.machine.no.heal.needed");
                    serverPlayer.sendSystemMessage(message);
                }
                return InteractionResult.PASS;
            }
        } catch (Exception e) {
            RubiusCobblemonAdditions.LOGGER.error("Error interacting with Cobblemon healing machine: {}", e.getMessage(), e);
        }
        
        return InteractionResult.PASS;
    }
    
    /**
     * Helper method to get Cobblemon API instance using aggressive reflection.
     * Tries all possible access patterns.
     */
    private Object getCobblemonAPI() {
        try {
            Class<?> cobblemonClass = Class.forName("com.cobblemon.mod.common.Cobblemon");
            
            // Try all possible access patterns
            Object[] candidates = new Object[]{null, null, null, null}; // INSTANCE, Companion, static, direct
            
            // Pattern 1: INSTANCE
            try {
                var instanceField = cobblemonClass.getField("INSTANCE");
                var instance = instanceField.get(null);
                candidates[0] = instance;
                
                // Try ALL no-arg methods on INSTANCE
                var allMethods = instance.getClass().getMethods();
                var allDeclaredMethods = instance.getClass().getDeclaredMethods();
                
                for (var method : allMethods) {
                    if (method.getParameterCount() == 0) {
                        try {
                            method.setAccessible(true);
                            var result = method.invoke(instance);
                            if (result != null) {
                                // Check if result has getStorage method
                                try {
                                    result.getClass().getMethod("getStorage");
                                    RubiusCobblemonAdditions.LOGGER.debug("Found API via INSTANCE.{}()", method.getName());
                                    return result;
                                } catch (NoSuchMethodException ignored) {}
                            }
                        } catch (Exception ignored) {}
                    }
                }
                
                for (var method : allDeclaredMethods) {
                    if (method.getParameterCount() == 0 && !java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                        try {
                            method.setAccessible(true);
                            var result = method.invoke(instance);
                            if (result != null) {
                                try {
                                    result.getClass().getMethod("getStorage");
                                    RubiusCobblemonAdditions.LOGGER.debug("Found API via INSTANCE.{}() (declared)", method.getName());
                                    return result;
                                } catch (NoSuchMethodException ignored) {}
                            }
                        } catch (Exception ignored) {}
                    }
                }
                
                // Try all fields on INSTANCE
                var allFields = instance.getClass().getDeclaredFields();
                for (var field : allFields) {
                    try {
                        field.setAccessible(true);
                        var result = field.get(instance);
                        if (result != null) {
                            try {
                                result.getClass().getMethod("getStorage");
                                RubiusCobblemonAdditions.LOGGER.debug("Found API via INSTANCE.{} field", field.getName());
                                return result;
                            } catch (NoSuchMethodException ignored) {}
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
            
            // Pattern 2: Companion
            try {
                var companionField = cobblemonClass.getField("Companion");
                var companion = companionField.get(null);
                candidates[1] = companion;
                
                var allMethods = companion.getClass().getMethods();
                var allDeclaredMethods = companion.getClass().getDeclaredMethods();
                
                for (var method : allMethods) {
                    if (method.getParameterCount() == 0) {
                        try {
                            method.setAccessible(true);
                            var result = method.invoke(companion);
                            if (result != null) {
                                try {
                                    result.getClass().getMethod("getStorage");
                                    RubiusCobblemonAdditions.LOGGER.debug("Found API via Companion.{}()", method.getName());
                                    return result;
                                } catch (NoSuchMethodException ignored) {}
                            }
                        } catch (Exception ignored) {}
                    }
                }
                
                for (var method : allDeclaredMethods) {
                    if (method.getParameterCount() == 0) {
                        try {
                            method.setAccessible(true);
                            var result = method.invoke(companion);
                            if (result != null) {
                                try {
                                    result.getClass().getMethod("getStorage");
                                    RubiusCobblemonAdditions.LOGGER.debug("Found API via Companion.{}() (declared)", method.getName());
                                    return result;
                                } catch (NoSuchMethodException ignored) {}
                            }
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception ignored) {}
            
            // Pattern 3: Static methods on Cobblemon class
            var staticMethods = cobblemonClass.getDeclaredMethods();
            for (var method : staticMethods) {
                if (java.lang.reflect.Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 0) {
                    try {
                        method.setAccessible(true);
                        var result = method.invoke(null);
                        if (result != null) {
                            try {
                                result.getClass().getMethod("getStorage");
                                RubiusCobblemonAdditions.LOGGER.debug("Found API via static {}()", method.getName());
                                return result;
                            } catch (NoSuchMethodException ignored) {}
                        }
                    } catch (Exception ignored) {}
                }
            }
            
            // Pattern 4: Direct api() method
            try {
                var apiMethod = cobblemonClass.getMethod("api");
                var result = apiMethod.invoke(null);
                if (result != null) {
                    try {
                        result.getClass().getMethod("getStorage");
                        RubiusCobblemonAdditions.LOGGER.debug("Found API via Cobblemon.api()");
                        return result;
                    } catch (NoSuchMethodException ignored) {}
                }
            } catch (Exception ignored) {}
            
            // Pattern 5: CobblemonAPI service class
            try {
                Class<?> apiServiceClass = Class.forName("com.cobblemon.mod.common.api.CobblemonAPI");
                var getInstanceMethod = apiServiceClass.getMethod("getInstance");
                var result = getInstanceMethod.invoke(null);
                if (result != null) {
                    try {
                        result.getClass().getMethod("getStorage");
                        RubiusCobblemonAdditions.LOGGER.debug("Found API via CobblemonAPI.getInstance()");
                        return result;
                    } catch (NoSuchMethodException ignored) {}
                }
            } catch (Exception ignored) {}
            
            // Pattern 6: Try to find any class with "API" in the name that has getStorage
            try {
                // Search through common API class names
                String[] apiClassNames = {
                    "com.cobblemon.mod.common.api.CobblemonAPI",
                    "com.cobblemon.mod.common.Cobblemon$Companion",
                    "com.cobblemon.mod.common.Cobblemon$DefaultImpls"
                };
                
                for (String className : apiClassNames) {
                    try {
                        Class<?> apiClass = Class.forName(className);
                        var methods = apiClass.getMethods();
                        var declaredMethods = apiClass.getDeclaredMethods();
                        
                        // Try all static methods
                        for (var method : methods) {
                            if (java.lang.reflect.Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 0) {
                                try {
                                    method.setAccessible(true);
                                    var result = method.invoke(null);
                                    if (result != null) {
                                        try {
                                            result.getClass().getMethod("getStorage");
                                            RubiusCobblemonAdditions.LOGGER.debug("Found API via {}.{}()", className, method.getName());
                                            return result;
                                        } catch (NoSuchMethodException ignored) {}
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                        
                        for (var method : declaredMethods) {
                            if (java.lang.reflect.Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 0) {
                                try {
                                    method.setAccessible(true);
                                    var result = method.invoke(null);
                                    if (result != null) {
                                        try {
                                            result.getClass().getMethod("getStorage");
                                            RubiusCobblemonAdditions.LOGGER.debug("Found API via {}.{}() (declared)", className, method.getName());
                                            return result;
                                        } catch (NoSuchMethodException ignored) {}
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                    } catch (ClassNotFoundException ignored) {}
                }
            } catch (Exception ignored) {}
            
        } catch (ClassNotFoundException e) {
            RubiusCobblemonAdditions.LOGGER.error("Cobblemon class not found: {}", e.getMessage());
        } catch (Exception e) {
            RubiusCobblemonAdditions.LOGGER.error("Error in getCobblemonAPI: {}", e.getMessage(), e);
        }
        
        return null;
    }
    
    /**
     * Checks if the player is currently in a battle.
     * Prevents healing during battle.
     */
    /**
     * Checks if the player is currently in a battle using Cobblemon's API.
     * Uses direct API imports (available via Maven dependency).
     */
    private boolean isPlayerInBattle(Player player) {
        if (!ModList.get().isLoaded("cobblemon")) {
            return false;
        }
        
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        
        try {
            // Use direct API access
            Cobblemon cobblemon = Cobblemon.INSTANCE;
            if (cobblemon == null) {
                return false;
            }
            
            BattleRegistry battleRegistry = cobblemon.getBattleRegistry();
            if (battleRegistry == null) {
                return false;
            }
            
            // Check if player is in a battle - getBattle() accepts UUID
            return battleRegistry.getBattle(serverPlayer.getUUID()) != null;
            
        } catch (Exception e) {
            // Only log unexpected errors once
            String errorKey = "isPlayerInBattle_error";
            if (!REFLECTION_CACHE.containsKey(errorKey)) {
                RubiusCobblemonAdditions.LOGGER.debug("Error checking if player is in battle: {}", e.getMessage());
                REFLECTION_CACHE.put(errorKey, true);
            }
        }
        
        // If we can't determine, allow healing (fail-safe)
        return false;
    }
    
    /**
     * Checks if the player can heal their Pokemon using Cobblemon's API.
     * Uses direct API imports (available via Maven dependency).
     */
    private boolean canHealPokemon(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        
        try {
            // Use direct API access
            Cobblemon cobblemon = Cobblemon.INSTANCE;
            if (cobblemon == null) {
                return false;
            }
            
            PokemonStoreManager storageManager = cobblemon.getStorage();
            if (storageManager == null) {
                return false;
            }
            
            // Get party store - use Object to avoid Kotlin interop issues when calling Kotlin methods
            Object partyStoreObj = storageManager.getParty(serverPlayer);
            if (partyStoreObj == null) {
                return false;
            }
            
            // Check if any Pokemon need healing by checking the party
            // Use toGappyList() to get all Pokemon (including null slots)
            // Note: toGappyList() is a Kotlin method, so we use reflection to avoid Kotlin interop issues
            java.util.List<?> pokemonList = null;
            try {
                var toGappyListMethod = partyStoreObj.getClass().getMethod("toGappyList");
                @SuppressWarnings("unchecked")
                java.util.List<?> list = (java.util.List<?>) toGappyListMethod.invoke(partyStoreObj);
                pokemonList = list;
            } catch (Exception e) {
                RubiusCobblemonAdditions.LOGGER.debug("Could not get pokemon list: {}", e.getMessage());
                return false;
            }
            
            if (pokemonList == null || pokemonList.isEmpty()) {
                return false;
            }
            
            // Check if any Pokemon have less than full HP
            // We'll assume if there are Pokemon, they might need healing
            // (Cobblemon's healing machine doesn't check HP before allowing healing)
            return true;
            
        } catch (Exception e) {
            RubiusCobblemonAdditions.LOGGER.debug("Error checking if player can heal Pokemon: {}", e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Starts the healing process.
     * Uses the same animation/behavior as Cobblemon's healing machine.
     */
    private void startHealing(Player player) {
        if (level != null) {
            isHealing = true;
            healingStartTime = level.getGameTime();
            healingPlayer = player;
            healingPlayerUUID = player.getUUID();
            
            // Update block state to show healing animation
            updateHealingState(true);
            
            // Sync to client immediately so renderer can show pokeballs
            setChanged();
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.getChunkSource().blockChanged(worldPosition);
            }
            
            // Play healing start sound (try Cobblemon's sound first, fallback to note block)
            playHealingSound(true);
            
            // Send healing started message (same as normal healing machine)
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                try {
                    var message = net.minecraft.network.chat.Component.translatable("cobblemon.healing.machine.start");
                    serverPlayer.sendSystemMessage(message);
                } catch (Exception e) {
                    // Fallback message if translation key doesn't exist
                    var message = net.minecraft.network.chat.Component.literal("Healing started...");
                    serverPlayer.sendSystemMessage(message);
                }
            }
            
            RubiusCobblemonAdditions.LOGGER.info("Healing started for player: {} at {}", player.getName().getString(), worldPosition);
        }
    }
    
    /**
     * Updates the block state to reflect healing status (for animations).
     */
    private void updateHealingState(boolean healing) {
        if (level != null && !level.isClientSide) {
            BlockState currentState = getBlockState();
            if (currentState.hasProperty(nl.streats1.rubiusaddons.block.CreatePoweredHealingMachineBlock.HEALING)) {
                BlockState newState = currentState.setValue(
                    nl.streats1.rubiusaddons.block.CreatePoweredHealingMachineBlock.HEALING, 
                    healing
                );
                level.setBlock(worldPosition, newState, 3);
                setChanged();
                // Sync to client so pokeball rendering works
                level.sendBlockUpdated(worldPosition, currentState, newState, 3);
            }
        }
    }
    
    /**
     * Completes the healing process.
     * Uses Cobblemon's healing API to actually heal the player's Pokemon.
     * This uses the same healing logic as the normal healing machine.
     */
    private void completeHealing(Level level) {
        // Get the player if we have their UUID but not the player object
        if (healingPlayer == null && healingPlayerUUID != null && level.getServer() != null) {
            healingPlayer = level.getServer().getPlayerList().getPlayer(healingPlayerUUID);
        }
        
        if (healingPlayer != null) {
            // Actually heal the player's Pokemon using Cobblemon's API
            healPlayerPokemon(healingPlayer);
        }
        
        // Start recharge timer (based on current SU)
        rechargeStartTime = level.getGameTime();
        
        isHealing = false;
        healingStartTime = 0;
        healingPlayer = null;
        healingPlayerUUID = null;
        
        // Update block state to stop healing animation
        updateHealingState(false);
        
        setChanged();
        
        int rechargeTime = calculateRechargeTime();
        RubiusCobblemonAdditions.LOGGER.info("Healing complete at SU: {} (recharge time: {} seconds)", currentSU, rechargeTime);
    }
    
    /**
     * Actually heals a player's Pokemon using Cobblemon's internal tools.
     * This uses the same methods as Cobblemon's healing machine to ensure
     * the same behavior and animations.
     * 
     * The correct approach is:
     * 1. Get Cobblemon.INSTANCE
     * 2. Call getStorage() to get PokemonStoreManager
     * 3. Call getParty(ServerPlayer) to get PartyStore
     * 4. Call partyStore.heal() to heal all Pokemon
     */
    /**
     * Heals all Pokemon in the player's party using Cobblemon's API.
     * Uses direct API imports (available via Maven dependency).
     */
    private void healPlayerPokemon(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        
        try {
            // Use direct API access (no reflection needed!)
            Cobblemon cobblemon = Cobblemon.INSTANCE;
            if (cobblemon == null) {
                RubiusCobblemonAdditions.LOGGER.error("Cobblemon.INSTANCE is null");
                return;
            }
            
            PokemonStoreManager storageManager = cobblemon.getStorage();
            if (storageManager == null) {
                RubiusCobblemonAdditions.LOGGER.error("PokemonStoreManager is null");
                return;
            }
            
            // Get party store - use Object to avoid Kotlin interop issues
            Object partyStoreObj = storageManager.getParty(serverPlayer);
            if (partyStoreObj == null) {
                RubiusCobblemonAdditions.LOGGER.error("PartyStore is null for player: {}", serverPlayer.getName().getString());
                return;
            }
            
            // Heal all Pokemon in the party using reflection (heal() is a Kotlin method)
            try {
                var healMethod = partyStoreObj.getClass().getMethod("heal");
                healMethod.invoke(partyStoreObj);
            } catch (Exception e) {
                RubiusCobblemonAdditions.LOGGER.error("Could not heal party: {}", e.getMessage());
                return;
            }
            
            RubiusCobblemonAdditions.LOGGER.info("Successfully healed all Pokemon for player: {}", serverPlayer.getName().getString());
            
            // Play healing complete sound
            playHealingSound(false);
            
            sendHealingCompleteMessage(serverPlayer);
            
        } catch (Exception e) {
            RubiusCobblemonAdditions.LOGGER.error("Error healing Pokemon: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Heals a single Pokemon using various possible method names.
     */
    private void healSinglePokemon(Object pokemon) {
        try {
            // Try different possible method names that Cobblemon might use
            String[] healMethodNames = {"heal", "healToFull", "restoreHealth", "fullHeal"};
            for (String methodName : healMethodNames) {
                try {
                    var healMethod = pokemon.getClass().getMethod(methodName);
                    healMethod.invoke(pokemon);
                    RubiusCobblemonAdditions.LOGGER.debug("Healed Pokemon using {}.{}()", pokemon.getClass().getSimpleName(), methodName);
                    return;
                } catch (NoSuchMethodException ignored) {}
            }
            
            // Set health directly to max
            try {
                var setHpMethod = pokemon.getClass().getMethod("setCurrentHealth", float.class);
                var maxHpMethod = pokemon.getClass().getMethod("getMaxHealth");
                var maxHp = ((Number) maxHpMethod.invoke(pokemon)).floatValue();
                setHpMethod.invoke(pokemon, maxHp);
                RubiusCobblemonAdditions.LOGGER.debug("Healed Pokemon by setting HP to max");
            } catch (NoSuchMethodException e) {
                RubiusCobblemonAdditions.LOGGER.warn("Could not find heal method for Pokemon: {}", e.getMessage());
            }
        } catch (Exception e) {
            RubiusCobblemonAdditions.LOGGER.warn("Error healing single Pokemon: {}", e.getMessage());
        }
    }
    
    /**
     * Sends the healing complete message to the player.
     */
    private void sendHealingCompleteMessage(net.minecraft.server.level.ServerPlayer serverPlayer) {
        try {
            var messageComponent = net.minecraft.network.chat.Component.translatable("cobblemon.healing.machine.complete");
            serverPlayer.sendSystemMessage(messageComponent);
        } catch (Exception e) {
            // Translation key might not exist, use fallback
            var messageComponent = net.minecraft.network.chat.Component.literal("Your Pokemon have been healed!");
            serverPlayer.sendSystemMessage(messageComponent);
        }
    }
    
    /**
     * Plays healing machine sound effects.
     * Tries to use Cobblemon's healing machine sounds, falls back to Minecraft sounds.
     * 
     * @param start true for healing start sound, false for healing complete sound
     */
    private void playHealingSound(boolean start) {
        if (level == null || level.isClientSide) {
            return;
        }
        
        try {
            // Try to use Cobblemon's healing machine sounds
            ResourceLocation soundLocation;
            if (start) {
                soundLocation = ResourceLocation.parse("cobblemon:healing_machine_start");
            } else {
                soundLocation = ResourceLocation.parse("cobblemon:healing_machine_complete");
            }
            
            // Try to get the sound event from registry
            var soundEvent = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.get(soundLocation);
            if (soundEvent != null) {
                level.playSound(null, worldPosition, soundEvent, SoundSource.BLOCKS, 1.0f, 1.0f);
                return;
            }
        } catch (Exception e) {
            // Cobblemon sound not found, use fallback
        }
        
        // Fallback: Use Minecraft sounds
        // Start: Note block pling (pleasant sound)
        // Complete: Bell sound (success sound)
        if (start) {
            var startSound = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("minecraft:block.note_block.pling"));
            if (startSound != null) {
                level.playSound(null, worldPosition, startSound, SoundSource.BLOCKS, 0.7f, 1.2f);
            }
        } else {
            var completeSound = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("minecraft:block.bell.use"));
            if (completeSound != null) {
                level.playSound(null, worldPosition, completeSound, SoundSource.BLOCKS, 0.8f, 1.5f);
            }
        }
    }
    
    /**
     * Gets the current SU (Stress Units) of the machine.
     */
    public float getCurrentSU() {
        return currentSU;
    }
    
    /**
     * Checks if the machine is currently healing.
     */
    public boolean isHealing() {
        return isHealing;
    }
    
    /**
     * Gets the healing start time (for client-side rendering).
     */
    public long getHealingStartTime() {
        return healingStartTime;
    }
    
    /**
     * Gets the healing player UUID (for client-side rendering).
     */
    @Nullable
    public UUID getHealingPlayerUUID() {
        return healingPlayerUUID;
    }
    
    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    
    @Override
    public @NotNull CompoundTag getUpdateTag(@NotNull HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }
    
    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }
    
    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("CurrentSU", currentSU);
        tag.putBoolean("IsHealing", isHealing);
        tag.putLong("HealingStartTime", healingStartTime);
        tag.putLong("RechargeStartTime", rechargeStartTime);
        if (healingPlayerUUID != null) {
            tag.putUUID("HealingPlayerUUID", healingPlayerUUID);
        }
    }
    
    @Override
    public void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        currentSU = tag.getFloat("CurrentSU");
        isHealing = tag.getBoolean("IsHealing");
        healingStartTime = tag.getLong("HealingStartTime");
        rechargeStartTime = tag.getLong("RechargeStartTime");
        if (tag.hasUUID("HealingPlayerUUID")) {
            healingPlayerUUID = tag.getUUID("HealingPlayerUUID");
            // Try to find the player on load
            if (level != null && !level.isClientSide && healingPlayerUUID != null) {
                if (level.getServer() != null) {
                    healingPlayer = level.getServer().getPlayerList().getPlayer(healingPlayerUUID);
                }
            }
        }
        
        // Update block state to match healing status
        if (level != null && !level.isClientSide) {
            updateHealingState(isHealing);
        }
    }
}
