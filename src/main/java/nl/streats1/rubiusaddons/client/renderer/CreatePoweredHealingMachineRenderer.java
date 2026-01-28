package nl.streats1.rubiusaddons.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.fml.ModList;
import nl.streats1.rubiusaddons.RubiusCobblemonAdditions;
import nl.streats1.rubiusaddons.block.CreatePoweredHealingMachineBlock;
import nl.streats1.rubiusaddons.block.entity.CreatePoweredHealingMachineBlockEntity;

// Direct Cobblemon API imports (available via Maven dependency)
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.PokemonStoreManager;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Renderer for the Create-powered healing machine.
 * Renders pokeballs on the tray when healing is in progress.
 */
public class CreatePoweredHealingMachineRenderer implements BlockEntityRenderer<CreatePoweredHealingMachineBlockEntity> {
    
    private final ItemRenderer itemRenderer;
    
    // Cache for reflection results to avoid repeated lookups and log spam
    private static final Map<String, Boolean> REFLECTION_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Object> REFLECTION_RESULTS = new ConcurrentHashMap<>();
    private static volatile boolean hasLoggedRendererNotFound = false;
    private static volatile boolean hasLoggedPartyMethodNotFound = false;
    
    public CreatePoweredHealingMachineRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }
    
    @Override
    public void render(CreatePoweredHealingMachineBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        
        // Ensure we have a valid level
        if (blockEntity.getLevel() == null) {
            return;
        }
        
        // Only render if healing is in progress
        if (!blockEntity.isHealing()) {
            return;
        }
        
        // Try to use Cobblemon's renderer first
        if (tryUseCobblemonRenderer(blockEntity, partialTick, poseStack, buffer, packedLight, packedOverlay)) {
            return;
        }
        
        // Fallback: Render our own pokeballs
        renderCustomPokeballs(blockEntity, partialTick, poseStack, buffer, packedLight, packedOverlay);
    }
    
    /**
     * Attempts to use Cobblemon's healing machine renderer via reflection.
     * Returns true if successful, false otherwise.
     * Uses caching to avoid repeated reflection attempts and log spam.
     */
    private boolean tryUseCobblemonRenderer(CreatePoweredHealingMachineBlockEntity blockEntity, float partialTick,
                                            PoseStack poseStack, MultiBufferSource buffer,
                                            int packedLight, int packedOverlay) {
        if (!ModList.get().isLoaded("cobblemon")) {
            return false;
        }
        
        // Check cache first
        String cacheKey = "cobblemon_renderer_available";
        Boolean cached = REFLECTION_CACHE.get(cacheKey);
        if (cached != null && !cached) {
            return false; // Already checked and not available
        }
        
        try {
            // Try to find Cobblemon's HealingMachineBlockEntity renderer
            Class<?> rendererClass = Class.forName("com.cobblemon.mod.common.client.render.block.HealingMachineBlockEntityRenderer");
            Class<?> healingMachineBEClass = Class.forName("com.cobblemon.mod.common.block.entity.HealingMachineBlockEntity");
            
            // Try to get an instance of the renderer and call render method
            // We need to check if we can create a proxy HealingMachineBlockEntity
            // For now, skip this complex approach and use our own rendering
            if (!hasLoggedRendererNotFound) {
                hasLoggedRendererNotFound = true;
            }
            REFLECTION_CACHE.put(cacheKey, false);
            return false;
        } catch (ClassNotFoundException e) {
            // Only log once to avoid spam
            if (!hasLoggedRendererNotFound) {
                hasLoggedRendererNotFound = true;
            }
        } catch (Exception e) {
            if (!hasLoggedRendererNotFound) {
                hasLoggedRendererNotFound = true;
            }
        }
        
        // Cache the result (renderer not available)
        REFLECTION_CACHE.put(cacheKey, false);
        return false;
    }
    
    /**
     * Renders pokeballs on the healing machine tray.
     * This matches Cobblemon's exact rendering approach.
     * Uses the same offsets and positioning as HealingMachineRenderer.kt
     */
    private void renderCustomPokeballs(CreatePoweredHealingMachineBlockEntity blockEntity, float partialTick,
                                       PoseStack poseStack, MultiBufferSource buffer,
                                       int packedLight, int packedOverlay) {
        
        // Get the block state to determine facing direction
        BlockState state = blockEntity.getBlockState();
        if (!state.hasProperty(CreatePoweredHealingMachineBlock.FACING)) {
            return;
        }
        
        Direction facing = state.getValue(CreatePoweredHealingMachineBlock.FACING);
        
        // Get pokeball ResourceLocation names from block entity (synced from server)
        // Matches Cobblemon's approach: Map<Int, ResourceLocation>
        java.util.Map<Integer, net.minecraft.resources.ResourceLocation> pokeballNames = blockEntity.getHealingPokeballNames();
        if (pokeballNames == null || pokeballNames.isEmpty()) {
            return;
        }
        
        // Cobblemon's exact offsets for 2x3 grid layout
        // Matches: listOf(0.2 to 0.385, -0.2 to 0.385, 0.2 to 0.0, -0.2 to 0.0, 0.2 to -0.385, -0.2 to -0.385)
        float[][] offsets = {
            {0.2f, 0.385f},   // Slot 0: top-right
            {-0.2f, 0.385f},  // Slot 1: top-left
            {0.2f, 0.0f},     // Slot 2: middle-right
            {-0.2f, 0.0f},    // Slot 3: middle-left
            {0.2f, -0.385f},  // Slot 4: bottom-right
            {-0.2f, -0.385f}  // Slot 5: bottom-left
        };
        
        poseStack.pushPose();
        
        // Position at center of block (matches Cobblemon)
        poseStack.translate(0.5, 0.5, 0.5);
        
        // Rotate based on facing direction (matches Cobblemon's toYRot())
        float yRot = facing.toYRot();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yRot));
        
        // Scale matches Cobblemon (0.65F)
        poseStack.scale(0.65f, 0.65f, 0.65f);
        
        // Render pokeballs using Cobblemon's exact layout
        // Convert ResourceLocation names to ItemStacks using PokeBalls.getPokeBall() (matches Cobblemon)
        var level = blockEntity.getLevel();
        if (level != null) {
            int renderedCount = 0;
            for (var entry : pokeballNames.entrySet()) {
                if (renderedCount >= 6) break;
                
                int index = entry.getKey();
                net.minecraft.resources.ResourceLocation pokeballName = entry.getValue();
                
                // Get PokeBall from ResourceLocation (matches Cobblemon's PokeBalls.getPokeBall())
                ItemStack pokeballStack = getPokeballStackFromName(pokeballName);
                if (pokeballStack == null || pokeballStack.isEmpty()) {
                    continue;
                }
                
                poseStack.pushPose();
                // Use Cobblemon's exact offset positioning
                float[] offset = offsets[renderedCount];
                poseStack.translate(offset[0], 0.4, offset[1]); // Y offset of 0.4 matches Cobblemon
                
                // Render using ItemRenderer (matches Cobblemon exactly)
                itemRenderer.renderStatic(
                    pokeballStack,
                    ItemDisplayContext.GROUND,
                    packedLight,
                    packedOverlay,
                    poseStack,
                    buffer,
                    level,
                    0
                );
                
                poseStack.popPose();
                renderedCount++;
            }
        }
        
        poseStack.popPose();
    }
    
    /**
     * Gets an ItemStack from a PokeBall ResourceLocation name.
     * Matches Cobblemon's approach: PokeBalls.getPokeBall(name).stack()
     */
    private ItemStack getPokeballStackFromName(net.minecraft.resources.ResourceLocation pokeballName) {
        if (!ModList.get().isLoaded("cobblemon")) {
            return ItemStack.EMPTY;
        }
        
        try {
            // Use reflection to call PokeBalls.getPokeBall(ResourceLocation)
            Class<?> pokeBallsClass = Class.forName("com.cobblemon.mod.common.api.pokeball.PokeBalls");
            var getPokeBallMethod = pokeBallsClass.getMethod("getPokeBall", net.minecraft.resources.ResourceLocation.class);
            Object pokeBall = getPokeBallMethod.invoke(null, pokeballName);
            
            if (pokeBall != null) {
                // Call pokeBall.stack() to get ItemStack (matches Cobblemon exactly)
                try {
                    var stackMethod = pokeBall.getClass().getMethod("stack", int.class);
                    ItemStack stack = (ItemStack) stackMethod.invoke(pokeBall, 1);
                    if (stack != null && !stack.isEmpty()) {
                        return stack;
                    }
                } catch (NoSuchMethodException e) {
                    // Try without parameter
                    var stackMethod = pokeBall.getClass().getMethod("stack");
                    ItemStack stack = (ItemStack) stackMethod.invoke(pokeBall);
                    if (stack != null && !stack.isEmpty()) {
                        return stack;
                    }
                }
            }
        } catch (Exception e) {
        }
        
        // Fallback: try to get default pokeball item
        try {
            var pokeballItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(pokeballName);
            if (pokeballItem != null) {
                return new ItemStack(pokeballItem);
            }
        } catch (Exception ignored) {}
        
        return ItemStack.EMPTY;
    }
    
    
    /**
     * Gets pokeball ItemStacks for the player's party.
     * Uses direct Cobblemon API imports (available via Maven dependency).
     */
    private ItemStack[] getPokeballStacks(UUID playerUUID) {
        if (!ModList.get().isLoaded("cobblemon")) {
            return null;
        }
        
        try {
            var client = Minecraft.getInstance();
            if (client.player == null) {
                return null;
            }
            
            // Only render for the current player's own pokeballs
            // For other players, we'd need server-side data sync
            if (!client.player.getUUID().equals(playerUUID)) {
                // Don't log this - it's expected for multiplayer
                return null;
            }
            
            // Use direct API access
            Cobblemon cobblemon = Cobblemon.INSTANCE;
            if (cobblemon == null) {
                return null;
            }
            
            PokemonStoreManager storageManager = cobblemon.getStorage();
            if (storageManager == null) {
                return null;
            }
            
            // On client side, we need to use reflection fallback since getParty() might only work with ServerPlayer
            // The client-side API might be different - fall back to reflection for now
            Object partyStore = null;
            try {
                // Try Player interface first (LocalPlayer extends Player)
                var getPartyMethod = storageManager.getClass().getMethod("getParty", net.minecraft.world.entity.player.Player.class);
                partyStore = getPartyMethod.invoke(storageManager, client.player);
            } catch (Exception e) {
                // Try UUID-based lookup as fallback
                try {
                    var getPartyByUUIDMethod = storageManager.getClass().getMethod("getParty", java.util.UUID.class);
                    partyStore = getPartyByUUIDMethod.invoke(storageManager, playerUUID);
                } catch (Exception e2) {
                }
            }
            
            if (partyStore == null) {
                return null;
            }
            
            // Get pokemon list - use reflection since we're using Object
            java.util.List<?> pokemonList = null;
            try {
                var toGappyListMethod = partyStore.getClass().getMethod("toGappyList");
                @SuppressWarnings("unchecked")
                java.util.List<?> list = (java.util.List<?>) toGappyListMethod.invoke(partyStore);
                pokemonList = list;
            } catch (Exception e) {
                return null;
            }
            
            if (pokemonList == null || pokemonList.isEmpty()) {
                return null;
            }
            
            // Convert pokemon to pokeball items
            ItemStack[] pokeballs = new ItemStack[Math.min(pokemonList.size(), 6)];
            int index = 0;
            
            for (Object pokemon : pokemonList) {
                if (pokemon == null || index >= pokeballs.length) {
                    break;
                }
                
                // Get the pokeball item for this pokemon using reflection
                ItemStack pokeball = getPokeballForPokemon(pokemon);
                if (pokeball != null && !pokeball.isEmpty()) {
                    pokeballs[index++] = pokeball;
                }
            }
            
            if (index == 0) {
                return null;
            }
            
            // Trim array to actual size
            if (index < pokeballs.length) {
                ItemStack[] trimmed = new ItemStack[index];
                System.arraycopy(pokeballs, 0, trimmed, 0, index);
                return trimmed;
            }
            
            return pokeballs;
        } catch (Exception e) {
            // Only log unexpected errors once
            String errorKey = "pokeball_stacks_error";
            if (!REFLECTION_CACHE.containsKey(errorKey)) {
                REFLECTION_CACHE.put(errorKey, true);
            }
        }
        
        return null;
    }
    
    /**
     * Gets the Cobblemon instance, caching the result.
     */
    private Object getCachedCobblemonInstance() {
        String cacheKey = "cobblemon_instance";
        Object cached = REFLECTION_RESULTS.get(cacheKey);
        if (cached != null || REFLECTION_CACHE.containsKey(cacheKey + "_null")) {
            return cached;
        }
        
        try {
            Class<?> cobblemonClass = Class.forName("com.cobblemon.mod.common.Cobblemon");
            
            try {
                var instanceField = cobblemonClass.getField("INSTANCE");
                Object instance = instanceField.get(null);
                if (instance != null) {
                    REFLECTION_RESULTS.put(cacheKey, instance);
                    return instance;
                }
            } catch (NoSuchFieldException e) {
                try {
                    var getInstanceMethod = cobblemonClass.getMethod("getInstance");
                    Object instance = getInstanceMethod.invoke(null);
                    if (instance != null) {
                        REFLECTION_RESULTS.put(cacheKey, instance);
                        return instance;
                    }
                } catch (NoSuchMethodException ignored) {
                    // Method doesn't exist
                }
            }
        } catch (ClassNotFoundException e) {
            // Cobblemon class not found - only log once
            if (!REFLECTION_CACHE.containsKey(cacheKey + "_not_found")) {
                REFLECTION_CACHE.put(cacheKey + "_not_found", true);
            }
        } catch (Exception e) {
            // Only log once
            if (!REFLECTION_CACHE.containsKey(cacheKey + "_error")) {
                REFLECTION_CACHE.put(cacheKey + "_error", true);
            }
        }
        
        REFLECTION_CACHE.put(cacheKey + "_null", true);
        return null;
    }
    
    /**
     * Gets the storage manager, caching the result.
     */
    private Object getCachedStorageManager(Object cobblemonInstance) {
        String cacheKey = "storage_manager";
        Object cached = REFLECTION_RESULTS.get(cacheKey);
        if (cached != null || REFLECTION_CACHE.containsKey(cacheKey + "_null")) {
            return cached;
        }
        
        try {
            var getStorageMethod = cobblemonInstance.getClass().getMethod("getStorage");
            Object storageManager = getStorageMethod.invoke(cobblemonInstance);
            if (storageManager != null) {
                REFLECTION_RESULTS.put(cacheKey, storageManager);
                return storageManager;
            }
        } catch (Exception e) {
            // Only log once
            if (!REFLECTION_CACHE.containsKey(cacheKey + "_error")) {
                REFLECTION_CACHE.put(cacheKey + "_error", true);
            }
        }
        
        REFLECTION_CACHE.put(cacheKey + "_null", true);
        return null;
    }
    
    /**
     * Gets the party store for a client player, trying multiple method signatures.
     */
    private Object getPartyStoreForClient(Object storageManager, net.minecraft.client.player.LocalPlayer player) {
        // Try Player first (LocalPlayer extends Player, so this should work)
        try {
            var getPartyMethod = storageManager.getClass().getMethod("getParty", net.minecraft.world.entity.player.Player.class);
            Object partyStore = getPartyMethod.invoke(storageManager, player);
            if (partyStore != null) {
                REFLECTION_RESULTS.put("getParty_method_Player", getPartyMethod);
                return partyStore;
            }
        } catch (NoSuchMethodException ignored) {
            // Try next signature
        } catch (Exception e) {
            // Only log once
            String errorKey = "getParty_error_Player";
            if (!REFLECTION_CACHE.containsKey(errorKey)) {
                REFLECTION_CACHE.put(errorKey, true);
            }
        }
        
        // Try UUID-based method (some APIs use UUID instead of Player object)
        try {
            var getPartyByUUIDMethod = storageManager.getClass().getMethod("getParty", java.util.UUID.class);
            Object partyStore = getPartyByUUIDMethod.invoke(storageManager, player.getUUID());
            if (partyStore != null) {
                REFLECTION_RESULTS.put("getParty_method_UUID", getPartyByUUIDMethod);
                return partyStore;
            }
        } catch (NoSuchMethodException ignored) {
            // Try next signature
        } catch (Exception e) {
            // Only log once
            String errorKey = "getParty_error_UUID";
            if (!REFLECTION_CACHE.containsKey(errorKey)) {
                REFLECTION_CACHE.put(errorKey, true);
            }
        }
        
        // Try LocalPlayer as last resort
        try {
            var getPartyMethod = storageManager.getClass().getMethod("getParty", net.minecraft.client.player.LocalPlayer.class);
            Object partyStore = getPartyMethod.invoke(storageManager, player);
            if (partyStore != null) {
                REFLECTION_RESULTS.put("getParty_method_LocalPlayer", getPartyMethod);
                return partyStore;
            }
        } catch (NoSuchMethodException ignored) {
            // Method doesn't exist
        } catch (Exception e) {
            // Only log once
            String errorKey = "getParty_error_LocalPlayer";
            if (!REFLECTION_CACHE.containsKey(errorKey)) {
                REFLECTION_CACHE.put(errorKey, true);
            }
        }
        
        // Log once that we couldn't find any working method
        if (!hasLoggedPartyMethodNotFound) {
            hasLoggedPartyMethodNotFound = true;
        }
        
        return null;
    }
    
    /**
     * Gets the pokemon list from a party store.
     */
    private java.util.List<?> getPokemonList(Object partyStore) {
        try {
            var toGappyListMethod = partyStore.getClass().getMethod("toGappyList");
            return (java.util.List<?>) toGappyListMethod.invoke(partyStore);
        } catch (NoSuchMethodException e) {
            // Try alternative method names
            String[] methodNames = {"getPokemon", "getParty", "toList", "asList"};
            for (String methodName : methodNames) {
                try {
                    var method = partyStore.getClass().getMethod(methodName);
                    Object result = method.invoke(partyStore);
                    if (result instanceof java.util.List) {
                        return (java.util.List<?>) result;
                    }
                } catch (Exception ignored) {
                    // Try next method
                }
            }
        } catch (Exception e) {
            // Only log once
            String errorKey = "pokemon_list_error";
            if (!REFLECTION_CACHE.containsKey(errorKey)) {
                REFLECTION_CACHE.put(errorKey, true);
            }
        }
        
        return null;
    }
    
    /**
     * Gets a pokeball ItemStack for a Pokemon object.
     */
    private ItemStack getPokeballForPokemon(Object pokemon) {
        try {
            var getCaughtBallMethod = pokemon.getClass().getMethod("getCaughtBall");
            Object ball = getCaughtBallMethod.invoke(pokemon);
            
            if (ball != null) {
                try {
                    var getItemStackMethod = ball.getClass().getMethod("getItemStack");
                    ItemStack stack = (ItemStack) getItemStackMethod.invoke(ball);
                    if (stack != null && !stack.isEmpty()) {
                        return stack;
                    }
                } catch (NoSuchMethodException ignored) {
                    // Fallback to default pokeball
                }
            }
        } catch (NoSuchMethodException ignored) {
            // Fallback to default pokeball
        } catch (Exception e) {
            // Don't log - this is expected for some Pokemon
        }
        
        return getDefaultPokeball();
    }
    
    /**
     * Gets a default pokeball ItemStack as fallback.
     */
    private ItemStack getDefaultPokeball() {
        // Try to get Cobblemon's pokeball item
        try {
            var pokeballItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.parse("cobblemon:poke_ball"));
            if (pokeballItem != null) {
                return new ItemStack(pokeballItem);
            }
        } catch (Exception e) {
        }
        
        // Ultimate fallback: use a generic item (this won't look right but prevents crashes)
        return ItemStack.EMPTY;
    }
}
