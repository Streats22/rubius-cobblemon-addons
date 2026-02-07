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
import nl.streats1.rubiusaddons.block.CreatePoweredHealingMachineBlock;
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
     * Accepts RPM from bottom, left, and right via any Create kinetic block (shafts, belts, gearboxes, etc.), but not cogs.
     *
     * Implements Create's IHaveGoggleInformation interface via reflection to support goggles display.
     */
public class CreatePoweredHealingMachineBlockEntity extends BlockEntity {
    
    // Cache for Create's IHaveGoggleInformation interface check
    private static Class<?> goggleInterface = null;
    private static boolean goggleInterfaceChecked = false;
    
    /**
     * Checks if Create's IHaveGoggleInformation interface is available and caches it.
     */
    private static Class<?> getGoggleInterface() {
        if (goggleInterfaceChecked) {
            return goggleInterface;
        }
        goggleInterfaceChecked = true;
        
        if (!ModList.get().isLoaded("create")) {
            return null;
        }
        
        try {
            goggleInterface = Class.forName("com.simibubi.create.content.contraptions.goggles.IHaveGoggleInformation");
        } catch (ClassNotFoundException e) {
            // Interface not found - Create might be different version
            goggleInterface = null;
        }
        
        return goggleInterface;
    }
    
    /**
     * Checks if this block entity should be treated as implementing Create's goggles interface.
     * This allows Create's goggles system to detect and use our block entity.
     */
    public boolean isInstanceOfGoggleInterface() {
        return getGoggleInterface() != null;
    }
    
    // SU (Stress Units) thresholds for recharge time reduction
    // SU represents the stress capacity/usage of the Create rotation network
    private static final int MIN_SU_FOR_BOOST = 256; // Minimum SU to start reducing recharge time
    private static final int MAX_SU_FOR_INSTANT = 1024; // SU for instant recharge

    // RPM thresholds for light/power state (0 = blue, 1 = yellow, 2 = red)
    private static final float RPM_THRESHOLD_MEDIUM = 12.0f;  // >= 12 RPM = medium (yellow)
    private static final float RPM_THRESHOLD_FULL = 32.0f;   // >= 32 RPM = full (red)
    
    // Recharge time constants (in seconds)
    // Try to get the actual recharge time from Cobblemon's healing machine
    private static final int BASE_RECHARGE_TIME = getCobblemonRechargeTime(); // Match Cobblemon's default recharge time
    private static final int MIN_RECHARGE_TIME = 350; // ~5.8 minutes at MIN_SU+ SU
    private static final int INSTANT_RECHARGE_TIME = 0; // Instant at MAX_SU SU
    
    /**
     * Attempts to get the recharge time from Cobblemon's healing machine block entity.
     * Falls back to 300 seconds (5 minutes) if not found, which is a common default.
     */
    private static int getCobblemonRechargeTime() {
        if (!ModList.get().isLoaded("cobblemon")) {
            return 300; // Default fallback: 5 minutes
        }
        
        try {
            // Try to find Cobblemon's healing machine block entity class
            Class<?> healingMachineClass = Class.forName("com.cobblemon.mod.common.block.entity.HealingMachineBlockEntity");
            
            // Try to get a static field or method that contains the recharge time
            // Common field names: RECHARGE_TIME, COOLDOWN_TIME, RECHARGE_TICKS, etc.
            String[] possibleFieldNames = {"RECHARGE_TIME", "COOLDOWN_TIME", "RECHARGE_TICKS", "COOLDOWN_TICKS", "RECHARGE_TIME_SECONDS"};
            
            for (String fieldName : possibleFieldNames) {
                try {
                    var field = healingMachineClass.getField(fieldName);
                    Object value = field.get(null); // Static field
                    if (value instanceof Number) {
                        int ticks = ((Number) value).intValue();
                        // Convert ticks to seconds if needed (assume it's in ticks if > 1000, seconds if < 1000)
                        if (ticks > 1000) {
                            return ticks / 20; // Convert ticks to seconds
                        } else {
                            return ticks; // Already in seconds
                        }
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    // Try next field name
                }
            }
            
            // Try to find a method that returns recharge time
            String[] possibleMethodNames = {"getRechargeTime", "getCooldownTime", "getRechargeTicks", "getCooldownTicks"};
            for (String methodName : possibleMethodNames) {
                try {
                    var method = healingMachineClass.getMethod(methodName);
                    Object result = method.invoke(null); // Static method
                    if (result instanceof Number) {
                        int ticks = ((Number) result).intValue();
                        if (ticks > 1000) {
                            return ticks / 20; // Convert ticks to seconds
                        } else {
                            return ticks; // Already in seconds
                        }
                    }
                } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
                    // Try next method name
                }
            }
        } catch (ClassNotFoundException e) {
            // Cobblemon class not found, use default
        } catch (Exception e) {
            // Any other error, use default
        }
        
        // Default fallback: 5 minutes (300 seconds) - a common value for healing machines
        return 300;
    }
    
    // Cache for reflection results to avoid repeated lookups and log spam
    private static final Map<String, Boolean> REFLECTION_CACHE = new ConcurrentHashMap<>();
    private static volatile boolean hasLoggedBattleClassNotFound = false;
    private static volatile boolean hasLoggedCompanionCanHealError = false;
    
    // Current Stress Units (SU) from Create rotation system
    private float currentSU = 0.0f;
    
    // Current RPM (rotation speed) from Create rotation system (for goggles display)
    private float currentRPM = 0.0f;
    
    // Healing progress tracking
    private long healingStartTime = 0;
    private boolean isHealing = false;
    private Player healingPlayer = null; // Track which player is being healed
    private UUID healingPlayerUUID = null; // Store UUID for persistence
    
    // Pokeball data for client-side rendering (synced from server)
    // Store pokeball ResourceLocation names (like Cobblemon does) instead of ItemStacks
    // This matches Cobblemon's approach: Map<Int, PokeBall> stored as ResourceLocation strings
    private java.util.Map<Integer, net.minecraft.resources.ResourceLocation> healingPokeballNames = new java.util.HashMap<>();
    
    // Recharge tracking (time when healing completed, used to calculate recharge cooldown)
    private long rechargeStartTime = 0; // Game time when recharge started (after healing completed)
    
    // Last power state (0/1/2) so we update block state when RPM crosses thresholds (level/animation change)
    private int lastPowerState = -1;
    
    public CreatePoweredHealingMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CREATE_POWERED_HEALING_MACHINE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CreatePoweredHealingMachineBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }
        
        // Update SU and RPM from Create rotation system
        float oldSU = blockEntity.currentSU;
        blockEntity.updateSU(level, pos);
        
        // Update block state when SU changes or when RPM crosses thresholds (so level/animation updates)
        int newPowerState = blockEntity.getPowerStateFromRPM();
        boolean suChanged = Math.abs(oldSU - blockEntity.currentSU) > 0.1f;
        boolean powerStateChanged = newPowerState != blockEntity.lastPowerState;
        if (suChanged || powerStateChanged) {
            if (powerStateChanged) {
                blockEntity.lastPowerState = newPowerState;
            }
            blockEntity.updateBlockState(blockEntity.isHealing);
        }
        
        // Handle healing logic if Cobblemon is loaded
        if (ModList.get().isLoaded("cobblemon")) {
            blockEntity.tickHealing(level);
        }
    }
    
    
    /**
     * Updates the current SU and RPM by checking Create's rotation system.
     * Only checks bottom, left, and right for kinetic blocks (shafts, belts, gearboxes, etc.; no cogs).
     * Left and right are relative to the block's facing direction.
     */
    private void updateSU(Level level, BlockPos pos) {
        if (!ModList.get().isLoaded("create")) {
            currentSU = 0.0f;
            return;
        }
        
        // Get the block's facing direction to determine left/right
        BlockState state = getBlockState();
        Direction facing = Direction.NORTH; // Default
        if (state.hasProperty(CreatePoweredHealingMachineBlock.FACING)) {
            facing = state.getValue(CreatePoweredHealingMachineBlock.FACING);
        }
        
        // Calculate left and right directions relative to facing
        Direction left = facing.getCounterClockWise();  // Left side relative to facing
        Direction right = facing.getClockWise();       // Right side relative to facing
        
        // Check for Create kinetic connections from bottom, left, and right only (no cogs)
        float maxSU = 0.0f;
        float maxRPM = 0.0f;
        
        // Bottom (DOWN) - always below the block
        BlockPos bottomPos = pos.below();
        float[] bottomData = getSUAndRPMFromShaft(level, bottomPos, Direction.UP);
        if (bottomData[0] > maxSU) {
            maxSU = bottomData[0];
            maxRPM = bottomData[1];
        }
        
        // Left side (relative to facing direction)
        BlockPos leftPos = pos.relative(left);
        float[] leftData = getSUAndRPMFromShaft(level, leftPos, left.getOpposite());
        if (leftData[0] > maxSU) {
            maxSU = leftData[0];
            maxRPM = leftData[1];
        }
        
        // Right side (relative to facing direction)
        BlockPos rightPos = pos.relative(right);
        float[] rightData = getSUAndRPMFromShaft(level, rightPos, right.getOpposite());
        if (rightData[0] > maxSU) {
            maxSU = rightData[0];
            maxRPM = rightData[1];
        }
        
        currentSU = maxSU;
        currentRPM = maxRPM;
    }
    
    /**
     * Gets SU (Stress Units) and RPM from an adjacent Create kinetic block.
     * Accepts any kinetic block (shafts, belts, gearboxes, etc.) except cogs.
     * Only checks bottom, left, and right sides.
     * 
     * @param level The level
     * @param pos Position of the adjacent block
     * @param fromDirection Direction from which we're receiving power
     * @return Array with [SU, RPM] values
     */
    private float[] getSUAndRPMFromShaft(Level level, BlockPos pos, Direction fromDirection) {
        // Return [SU, RPM]
        float[] result = getSUAndRPMFromShaftInternal(level, pos, fromDirection);
        return result;
    }
    
    /**
     * Legacy method for backward compatibility - delegates to getSUAndRPMFromShaft.
     * @deprecated Use getSUAndRPMFromShaft instead
     */
    @Deprecated
    private float getSUFromShaft(Level level, BlockPos pos, Direction fromDirection) {
        return getSUAndRPMFromShaft(level, pos, fromDirection)[0];
    }
    
    /**
     * Internal method to get SU and RPM from an adjacent Create kinetic block.
     * Accepts any KineticBlockEntity (shafts, belts, gearboxes, etc.) except cogs.
     *
     * @param level The level
     * @param pos Position of the adjacent block
     * @param fromDirection Direction from which we're receiving power
     * @return Array with [SU, RPM] values
     */
    private float[] getSUAndRPMFromShaftInternal(Level level, BlockPos pos, Direction fromDirection) {
        if (!ModList.get().isLoaded("create")) {
            return new float[]{0.0f, 0.0f}; // [SU, RPM]
        }
        
        try {
            var blockState = level.getBlockState(pos);
            var block = blockState.getBlock();
            String blockName = block.getDescriptionId();
            
            // Exclude cogs: do not accept power from cogwheels
            if (blockName.contains("cog")) {
                return new float[]{0.0f, 0.0f};
            }
            
            var blockEntity = level.getBlockEntity(pos);
            if (blockEntity == null) {
                return new float[]{0.0f, 0.0f};
            }
            
            Class<?> kineticClass = Class.forName("com.simibubi.create.content.kinetics.base.KineticBlockEntity");
            if (!kineticClass.isInstance(blockEntity)) {
                return new float[]{0.0f, 0.0f};
            }
            
            // Any Create kinetic block (shaft, belt, gearbox, etc.) – get speed
            try {
                var getSpeedMethod = kineticClass.getMethod("getSpeed");
                float speed = ((Number) getSpeedMethod.invoke(blockEntity)).floatValue();
                float rpm = Math.abs(speed);
                float su = rpm * 16.0f;
                if (rpm > 0.1f) {
                    return new float[]{su, rpm}; // [SU, RPM]
                }
            } catch (NoSuchMethodException e) {
                try {
                    var getCapacityMethod = kineticClass.getMethod("getCapacity");
                    float capacity = ((Number) getCapacityMethod.invoke(blockEntity)).floatValue();
                    if (capacity > 0.1f) {
                        float estimatedRPM = capacity / 16.0f;
                        return new float[]{capacity, estimatedRPM}; // [SU, RPM]
                    }
                } catch (NoSuchMethodException e2) {
                    // No methods available
                }
            }
        } catch (ClassNotFoundException e) {
            // Create classes not found
        } catch (Exception e) {
            // Create not available or API changed
        }
        
        return new float[]{0.0f, 0.0f}; // [SU, RPM]
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
     * - At 0 SU: Matches Cobblemon's default recharge time (attempts to read from Cobblemon, falls back to 300 seconds/5 minutes)
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
                    var message = net.minecraft.network.chat.Component.translatable("message.rubius_cobblemon_addons.healing_machine.in.battle");
                    try {
                        serverPlayer.sendSystemMessage(message, true); // Action bar message
                    } catch (Exception e) {
                        // Fallback message if translation key doesn't exist
                        var fallbackMessage = net.minecraft.network.chat.Component.literal("You cannot heal during battle!");
                        serverPlayer.sendSystemMessage(fallbackMessage, true); // Action bar message
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
                        "message.rubius_cobblemon_addons.healing_machine.recharging", 
                        minutes, seconds
                    );
                    try {
                        serverPlayer.sendSystemMessage(message, true); // Action bar message
                    } catch (Exception e) {
                        // Fallback message if translation key doesn't exist
                        var fallbackMessage = net.minecraft.network.chat.Component.literal(
                            String.format("Healing machine is recharging... %d:%02d remaining", minutes, seconds)
                        );
                        serverPlayer.sendSystemMessage(fallbackMessage, true); // Action bar message
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
                            var message = net.minecraft.network.chat.Component.translatable("message.rubius_cobblemon_addons.healing_machine.in.use");
                            serverPlayer.sendSystemMessage(message, true); // Action bar message
                        }
                        return InteractionResult.CONSUME;
                    }
                }
            } else {
                // Player has no Pokemon that need healing
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    var message = net.minecraft.network.chat.Component.translatable("message.rubius_cobblemon_addons.healing_machine.no.heal.needed");
                    serverPlayer.sendSystemMessage(message, true); // Action bar message
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
                return false;
            }
            
            if (pokemonList == null || pokemonList.isEmpty()) {
                return false;
            }
            
            // Check if any Pokemon have less than full HP
            for (Object pokemon : pokemonList) {
                if (pokemon == null) {
                    continue; // Skip empty slots
                }
                
                try {
                    // Try getCurrentHealth/getMaxHealth first
                    int currentHp = 0;
                    int maxHp = 0;
                    
                    try {
                        var hpMethod = pokemon.getClass().getMethod("getCurrentHealth");
                        var maxHpMethod = pokemon.getClass().getMethod("getMaxHealth");
                        currentHp = ((Number) hpMethod.invoke(pokemon)).intValue();
                        maxHp = ((Number) maxHpMethod.invoke(pokemon)).intValue();
                    } catch (NoSuchMethodException e) {
                        // Try alternative method names
                        try {
                            var hpMethod = pokemon.getClass().getMethod("getHp");
                            var maxHpMethod = pokemon.getClass().getMethod("getMaxHp");
                            currentHp = ((Number) hpMethod.invoke(pokemon)).intValue();
                            maxHp = ((Number) maxHpMethod.invoke(pokemon)).intValue();
                        } catch (NoSuchMethodException e2) {
                            // Can't check HP, skip this Pokemon
                            continue;
                        }
                    }
                    
                    // If this Pokemon has less than full HP, healing is needed
                    if (currentHp < maxHp) {
                        return true;
                    }
                } catch (Exception e) {
                    // Error checking this Pokemon, skip it
                    continue;
                }
            }
            
            // All Pokemon are at full health, no healing needed
            return false;
            
        } catch (Exception e) {
        }
        
        return false;
    }
    
    /**
     * Starts the healing process.
     * Uses the same animation/behavior as Cobblemon's healing machine.
     */
    private void startHealing(Player player) {
        if (level != null && !level.isClientSide) {
            isHealing = true;
            healingStartTime = level.getGameTime();
            healingPlayer = player;
            healingPlayerUUID = player.getUUID();
            
            // Get pokeball ResourceLocation names from party on server side (matches Cobblemon's approach)
            if (player instanceof ServerPlayer serverPlayer) {
                healingPokeballNames = getPokeballNamesFromParty(serverPlayer);
            } else {
                healingPokeballNames.clear();
            }
            
            // Update block state to show healing animation
            updateHealingState(true);
            
            // Sync to client immediately so renderer can show pokeballs
            setChanged();
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                // Force sync block entity data to client
                serverLevel.getChunkSource().blockChanged(worldPosition);
                // Send block entity update packet directly to all nearby players
                var packet = getUpdatePacket();
                if (packet != null) {
                    var players = serverLevel.getEntitiesOfClass(
                        net.minecraft.server.level.ServerPlayer.class,
                        new net.minecraft.world.phys.AABB(worldPosition).inflate(64.0),
                        p -> true
                    );
                    for (var nearbyPlayer : players) {
                        nearbyPlayer.connection.send(packet);
                    }
                }
            }
            
            // Play healing start sound (try Cobblemon's sound first, fallback to note block)
            playHealingSound(true);
            
            // Send healing started message (same as normal healing machine)
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                try {
                    var message = net.minecraft.network.chat.Component.translatable("message.rubius_cobblemon_addons.healing_machine.start");
                    serverPlayer.sendSystemMessage(message, true); // Action bar message
                } catch (Exception e) {
                    // Fallback message if translation key doesn't exist
                    var message = net.minecraft.network.chat.Component.literal("Healing started...");
                    serverPlayer.sendSystemMessage(message, true); // Action bar message
                }
            }
            
        }
    }
    
    /**
     * Gets pokeball ResourceLocation names from the player's party (server-side only).
     * Matches Cobblemon's approach: stores PokeBall.name (ResourceLocation) instead of ItemStacks.
     * This is synced to the client for rendering.
     */
    private java.util.Map<Integer, net.minecraft.resources.ResourceLocation> getPokeballNamesFromParty(ServerPlayer serverPlayer) {
        java.util.Map<Integer, net.minecraft.resources.ResourceLocation> pokeballMap = new java.util.HashMap<>();
        
        if (!ModList.get().isLoaded("cobblemon")) {
            return pokeballMap;
        }
        
        try {
            // Use direct API access
            Cobblemon cobblemon = Cobblemon.INSTANCE;
            if (cobblemon == null) {
                return pokeballMap;
            }
            
            PokemonStoreManager storageManager = cobblemon.getStorage();
            if (storageManager == null) {
                return pokeballMap;
            }
            
            // Get party store - use Object to avoid Kotlin interop issues
            Object partyStoreObj = storageManager.getParty(serverPlayer);
            if (partyStoreObj == null) {
                return pokeballMap;
            }
            
            // Get pokemon list using reflection (toGappyList() is a Kotlin method)
            java.util.List<?> pokemonList = null;
            try {
                var toGappyListMethod = partyStoreObj.getClass().getMethod("toGappyList");
                @SuppressWarnings("unchecked")
                java.util.List<?> list = (java.util.List<?>) toGappyListMethod.invoke(partyStoreObj);
                pokemonList = list;
            } catch (Exception e) {
                return pokeballMap;
            }
            
            if (pokemonList == null || pokemonList.isEmpty()) {
                return pokeballMap;
            }
            
            // Convert pokemon to pokeball ResourceLocation names (matches Cobblemon's approach)
            for (int index = 0; index < pokemonList.size(); index++) {
                Object pokemon = pokemonList.get(index);
                if (pokemon == null) {
                    continue;
                }
                
                // Get the pokeball ResourceLocation name (matches Cobblemon's pokemon.caughtBall.name)
                net.minecraft.resources.ResourceLocation pokeballName = getPokeballNameFromPokemon(pokemon);
                if (pokeballName != null) {
                    pokeballMap.put(index, pokeballName);
                }
            }
            
        } catch (Exception e) {
        }
        
        return pokeballMap;
    }
    
    /**
     * Gets a pokeball ResourceLocation name from a Pokemon object (server-side).
     * Matches Cobblemon's approach: pokemon.caughtBall.name
     */
    private net.minecraft.resources.ResourceLocation getPokeballNameFromPokemon(Object pokemon) {
        try {
            // Try to get the caught ball (matches Cobblemon's pokemon.caughtBall)
            var getCaughtBallMethod = pokemon.getClass().getMethod("getCaughtBall");
            Object pokeBall = getCaughtBallMethod.invoke(pokemon);
            
            if (pokeBall != null) {
                // Get the name property (matches Cobblemon's pokeBall.name)
                try {
                    var getNameMethod = pokeBall.getClass().getMethod("getName");
                    Object name = getNameMethod.invoke(pokeBall);
                    if (name instanceof net.minecraft.resources.ResourceLocation resourceLocation) {
                        return resourceLocation;
                    } else if (name != null) {
                        // Try to convert to ResourceLocation
                        return net.minecraft.resources.ResourceLocation.parse(name.toString());
                    }
                } catch (NoSuchMethodException e) {
                }
            }
        } catch (Exception e) {
        }
        
        // Fallback: default pokeball
        return net.minecraft.resources.ResourceLocation.parse("cobblemon:poke_ball");
    }
    
    /**
     * Gets a pokeball ItemStack for a Pokemon object (server-side).
     * Uses Cobblemon's PokeBall.stack() method, matching their exact approach.
     */
    private net.minecraft.world.item.ItemStack getPokeballForPokemon(Object pokemon) {
        try {
            // Try to get the caught ball (matches Cobblemon's pokemon.caughtBall)
            var getCaughtBallMethod = pokemon.getClass().getMethod("getCaughtBall");
            Object pokeBall = getCaughtBallMethod.invoke(pokemon);
            
            if (pokeBall != null) {
                // Use Cobblemon's PokeBall.stack() method (matches their exact approach)
                try {
                    var stackMethod = pokeBall.getClass().getMethod("stack", int.class);
                    net.minecraft.world.item.ItemStack stack = (net.minecraft.world.item.ItemStack) stackMethod.invoke(pokeBall, 1);
                    if (stack != null && !stack.isEmpty()) {
                        return stack;
                    }
                } catch (NoSuchMethodException e) {
                    // Try without parameter (default count = 1)
                    try {
                        var stackMethod = pokeBall.getClass().getMethod("stack");
                        net.minecraft.world.item.ItemStack stack = (net.minecraft.world.item.ItemStack) stackMethod.invoke(pokeBall);
                        if (stack != null && !stack.isEmpty()) {
                            return stack;
                        }
                    } catch (NoSuchMethodException e2) {
                        // Fallback to old method names
                        try {
                            var getItemStackMethod = pokeBall.getClass().getMethod("getItemStack");
                            net.minecraft.world.item.ItemStack stack = (net.minecraft.world.item.ItemStack) getItemStackMethod.invoke(pokeBall);
                            if (stack != null && !stack.isEmpty()) {
                                return stack;
                            }
                        } catch (NoSuchMethodException ignored) {}
                    }
                }
            }
        } catch (Exception e) {
        }
        
        // Fallback: Get default pokeball item
        try {
            var pokeballItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.parse("cobblemon:poke_ball"));
            if (pokeballItem != null) {
                return new net.minecraft.world.item.ItemStack(pokeballItem);
            }
        } catch (Exception e) {
        }
        
        return net.minecraft.world.item.ItemStack.EMPTY;
    }
    
    /**
     * Returns power state (0/1/2) from current RPM for block state / model selection.
     */
    private int getPowerStateFromRPM() {
        if (currentRPM < RPM_THRESHOLD_MEDIUM) {
            return 0;
        }
        if (currentRPM < RPM_THRESHOLD_FULL) {
            return 1;
        }
        return 2;
    }
    
    /**
     * Updates the block state to reflect healing status and power state (for animations and colors).
     */
    private void updateHealingState(boolean healing) {
        updateBlockState(healing);
    }
    
    /**
     * Updates the block state based on healing status and power state.
     * Power state: 0 = unpowered (red), 1 = powered (yellow)
     * Healing overrides power state visually.
     */
    private void updateBlockState(boolean healing) {
        if (level != null && !level.isClientSide) {
            BlockState currentState = getBlockState();
            BlockState newState = currentState;
            
            // Update healing state
            if (currentState.hasProperty(nl.streats1.rubiusaddons.block.CreatePoweredHealingMachineBlock.HEALING)) {
                newState = newState.setValue(
                    nl.streats1.rubiusaddons.block.CreatePoweredHealingMachineBlock.HEALING, 
                    healing
                );
            }
            
            // Update power state from RPM: 0 = blue (RPM < 12), 1 = yellow (12–32 RPM), 2 = red (32+ RPM)
            if (currentState.hasProperty(nl.streats1.rubiusaddons.block.CreatePoweredHealingMachineBlock.POWER_STATE)) {
                int powerState = getPowerStateFromRPM();
                lastPowerState = powerState;
                newState = newState.setValue(
                    nl.streats1.rubiusaddons.block.CreatePoweredHealingMachineBlock.POWER_STATE,
                    powerState
                );
            }
            
            if (newState != currentState) {
                level.setBlock(worldPosition, newState, 3);
                setChanged();
                // Sync to client so pokeball rendering and colors work
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
        healingPokeballNames.clear(); // Clear pokeballs when done
        
        // Update block state to stop healing animation and update power state
        updateBlockState(false);
        
        setChanged();
        
        int rechargeTime = calculateRechargeTime();
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
                    return;
                } catch (NoSuchMethodException ignored) {}
            }
            
            // Set health directly to max
            try {
                var setHpMethod = pokemon.getClass().getMethod("setCurrentHealth", float.class);
                var maxHpMethod = pokemon.getClass().getMethod("getMaxHealth");
                var maxHp = ((Number) maxHpMethod.invoke(pokemon)).floatValue();
                setHpMethod.invoke(pokemon, maxHp);
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
            var messageComponent = net.minecraft.network.chat.Component.translatable("message.rubius_cobblemon_addons.healing_machine.complete");
            serverPlayer.sendSystemMessage(messageComponent, true); // Action bar message
        } catch (Exception e) {
            // Translation key might not exist, use fallback
            var messageComponent = net.minecraft.network.chat.Component.literal("Your Pokemon have been healed!");
            serverPlayer.sendSystemMessage(messageComponent, true); // Action bar message
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
        
        if (start) {
            // Use Cobblemon's exact sound (matches their activate() method)
            // CobblemonSounds.HEALING_MACHINE_ACTIVE = "block.healing_machine.active"
            try {
                var soundEvent = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.get(
                    ResourceLocation.parse("cobblemon:block.healing_machine.active")
                );
                if (soundEvent != null) {
                    level.playSound(null, worldPosition, soundEvent, SoundSource.BLOCKS, 1.0f, 1.0f);
                    return;
                }
            } catch (Exception e) {
            }
        }
        // No sound for completion (Cobblemon doesn't play a sound on completion)
    }
    
    /**
     * Gets the current SU (Stress Units) of the machine.
     */
    public float getCurrentSU() {
        return currentSU;
    }
    
    /**
     * Gets the current RPM (rotation speed) from Create's rotation system.
     * Used for Create goggles display.
     */
    public float getCurrentRPM() {
        return currentRPM;
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
    
    /**
     * Gets the pokeball ResourceLocation names for rendering (synced from server).
     * Matches Cobblemon's approach: returns Map<Int, ResourceLocation>.
     */
    public java.util.Map<Integer, net.minecraft.resources.ResourceLocation> getHealingPokeballNames() {
        return healingPokeballNames != null ? healingPokeballNames : new java.util.HashMap<>();
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
        tag.putFloat("CurrentRPM", currentRPM);
        tag.putBoolean("IsHealing", isHealing);
        tag.putLong("HealingStartTime", healingStartTime);
        tag.putLong("RechargeStartTime", rechargeStartTime);
        if (healingPlayerUUID != null) {
            tag.putUUID("HealingPlayerUUID", healingPlayerUUID);
        }
        
        // Save pokeball ResourceLocation names (matches Cobblemon's approach)
        if (healingPokeballNames != null && !healingPokeballNames.isEmpty()) {
            var pokeballsTag = new net.minecraft.nbt.CompoundTag();
            for (var entry : healingPokeballNames.entrySet()) {
                pokeballsTag.putString(entry.getKey().toString(), entry.getValue().toString());
            }
            tag.put("HealingPokeballNames", pokeballsTag);
            if (level != null && !level.isClientSide) {
            }
        } else {
            tag.remove("HealingPokeballNames");
        }
    }
    
    @Override
    public void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        currentSU = tag.getFloat("CurrentSU");
        currentRPM = tag.contains("CurrentRPM") ? tag.getFloat("CurrentRPM") : 0.0f;
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
        
        // Load pokeball ResourceLocation names (matches Cobblemon's approach)
        healingPokeballNames.clear();
        if (tag.contains("HealingPokeballNames", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            var pokeballsTag = tag.getCompound("HealingPokeballNames");
            for (String key : pokeballsTag.getAllKeys()) {
                try {
                    int index = Integer.parseInt(key);
                    String pokeballId = pokeballsTag.getString(key);
                    if (!pokeballId.isEmpty()) {
                        var resourceLocation = net.minecraft.resources.ResourceLocation.parse(pokeballId);
                        healingPokeballNames.put(index, resourceLocation);
                    }
                } catch (Exception e) {
                }
            }
            if (level != null && level.isClientSide) {
            }
        } else {
            if (level != null && level.isClientSide) {
            }
        }
        
        // Sync power state so next tick doesn’t think it changed
        lastPowerState = getPowerStateFromRPM();
        // Update block state to match healing status and power state (so client sees correct level/RPM)
        if (level != null && !level.isClientSide) {
            updateBlockState(isHealing);
        }
    }
    
    /**
     * Provides information for Create's goggles display system.
     * This method matches Create's IHaveGoggleInformation interface signature.
     * Create's goggles system will call this method when a player looks at this block entity.
     * 
     * @param tooltip List to add tooltip lines to
     * @param isPlayerSneaking Whether the player is sneaking (for additional info)
     * @return true if information was added
     */
    public boolean addToGoggleTooltip(java.util.List<net.minecraft.network.chat.Component> tooltip, boolean isPlayerSneaking) {
        if (!ModList.get().isLoaded("create")) {
            return false;
        }
        
        // Add RPM/Speed information (matches Create's format)
        if (currentRPM > 0.1f) {
            tooltip.add(net.minecraft.network.chat.Component.literal("Speed: " + String.format("%.0f", currentRPM) + " RPM"));
        } else {
            tooltip.add(net.minecraft.network.chat.Component.literal("Speed: 0 RPM"));
        }
        
        // Add Stress Capacity information (matches Create's format)
        if (currentSU > 0.1f) {
            tooltip.add(net.minecraft.network.chat.Component.literal("Stress Capacity: " + String.format("%.0f", currentSU) + " SU"));
        } else {
            tooltip.add(net.minecraft.network.chat.Component.literal("Stress Capacity: 0 SU"));
        }
        
        // Add Total Stress (currently 0, as we're consuming stress, not generating it)
        tooltip.add(net.minecraft.network.chat.Component.literal("Total Stress: 0 SU"));
        
        // Add healing status
        if (isHealing) {
            tooltip.add(net.minecraft.network.chat.Component.translatable("message.rubius_cobblemon_addons.healing_machine.start"));
        } else if (rechargeStartTime > 0 && level != null) {
            int remainingSeconds = getRemainingRechargeTime(level);
            if (remainingSeconds > 0) {
                int minutes = remainingSeconds / 60;
                int seconds = remainingSeconds % 60;
                tooltip.add(net.minecraft.network.chat.Component.translatable("message.rubius_cobblemon_addons.healing_machine.recharging", minutes, seconds));
            } else {
                tooltip.add(net.minecraft.network.chat.Component.literal("Ready to heal"));
            }
        } else {
            tooltip.add(net.minecraft.network.chat.Component.literal("Ready to heal"));
        }
        
        return true;
    }
}
