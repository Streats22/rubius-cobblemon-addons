package nl.streats1.rubiusaddons.integration;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import nl.streats1.rubiusaddons.RubiusCobblemonAdditions;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Integration helper for Cobblemon API access.
 * Provides a cleaner API wrapper around Cobblemon's reflection-based access.
 * 
 * With the Cobblemon MDK/Maven dependency, you can also use direct imports:
 *   import com.cobblemon.mod.common.Cobblemon;
 *   import com.cobblemon.mod.common.api.storage.PokemonStoreManager;
 *   import com.cobblemon.mod.common.api.storage.party.PartyStore;
 * 
 * This class provides a reflection-based wrapper that works regardless of
 * whether direct imports are available.
 */
public class CobblemonIntegration {
    
    private static final Map<String, Object> CACHE = new ConcurrentHashMap<>();
    
    /**
     * Gets the Cobblemon API instance.
     * @return Cobblemon API instance or null if not available
     */
    @Nullable
    public static Object getCobblemonInstance() {
        String cacheKey = "cobblemon_instance";
        Object cached = CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        try {
            Class<?> cobblemonClass = Class.forName("com.cobblemon.mod.common.Cobblemon");
            var instanceField = cobblemonClass.getField("INSTANCE");
            Object instance = instanceField.get(null);
            if (instance != null) {
                CACHE.put(cacheKey, instance);
                return instance;
            }
        } catch (Exception e) {
        }
        
        return null;
    }
    
    /**
     * Gets the PokemonStoreManager from Cobblemon API.
     * @param cobblemonInstance The Cobblemon instance
     * @return PokemonStoreManager or null if not available
     */
    @Nullable
    public static Object getStorageManager(Object cobblemonInstance) {
        if (cobblemonInstance == null) {
            return null;
        }
        
        String cacheKey = "storage_manager";
        Object cached = CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        try {
            var getStorageMethod = cobblemonInstance.getClass().getMethod("getStorage");
            Object storageManager = getStorageMethod.invoke(cobblemonInstance);
            if (storageManager != null) {
                CACHE.put(cacheKey, storageManager);
                return storageManager;
            }
        } catch (Exception e) {
        }
        
        return null;
    }
    
    /**
     * Gets the party store for a server player.
     * @param storageManager The PokemonStoreManager instance
     * @param player The server player
     * @return PartyStore or null if not available
     */
    @Nullable
    public static Object getPartyStore(Object storageManager, ServerPlayer player) {
        if (storageManager == null || player == null) {
            return null;
        }
        
        try {
            var getPartyMethod = storageManager.getClass().getMethod("getParty", ServerPlayer.class);
            return getPartyMethod.invoke(storageManager, player);
        } catch (Exception e) {
        }
        
        return null;
    }
    
    /**
     * Gets the party store for a client player (Player interface).
     * Tries multiple method signatures for compatibility.
     * @param storageManager The PokemonStoreManager instance
     * @param player The player (can be LocalPlayer or ServerPlayer)
     * @return PartyStore or null if not available
     */
    @Nullable
    public static Object getPartyStoreForClient(Object storageManager, Player player) {
        if (storageManager == null || player == null) {
            return null;
        }
        
        // Try Player interface first (works for both LocalPlayer and ServerPlayer)
        try {
            var getPartyMethod = storageManager.getClass().getMethod("getParty", Player.class);
            Object partyStore = getPartyMethod.invoke(storageManager, player);
            if (partyStore != null) {
                return partyStore;
            }
        } catch (NoSuchMethodException ignored) {
            // Try UUID-based method
            try {
                var getPartyByUUIDMethod = storageManager.getClass().getMethod("getParty", UUID.class);
                Object partyStore = getPartyByUUIDMethod.invoke(storageManager, player.getUUID());
                if (partyStore != null) {
                    return partyStore;
                }
            } catch (NoSuchMethodException ignored2) {
                // Try LocalPlayer if player is LocalPlayer
                if (player.getClass().getSimpleName().equals("LocalPlayer")) {
                    try {
                        var getPartyMethod = storageManager.getClass().getMethod("getParty", player.getClass());
                        return getPartyMethod.invoke(storageManager, player);
                    } catch (Exception ignored3) {
                        // All methods failed
                    }
                }
            } catch (Exception e) {
            }
        } catch (Exception e) {
        }
        
        return null;
    }
    
    /**
     * Heals all Pokemon in a party store.
     * @param partyStore The PartyStore instance
     * @return true if healing was successful
     */
    public static boolean healParty(Object partyStore) {
        if (partyStore == null) {
            return false;
        }
        
        try {
            var healMethod = partyStore.getClass().getMethod("heal");
            healMethod.invoke(partyStore);
            return true;
        } catch (Exception e) {
        }
        
        return false;
    }
    
    /**
     * Gets the Pokemon list from a party store.
     * @param partyStore The PartyStore instance
     * @return List of Pokemon or null if not available
     */
    @Nullable
    public static java.util.List<?> getPokemonList(Object partyStore) {
        if (partyStore == null) {
            return null;
        }
        
        try {
            var toGappyListMethod = partyStore.getClass().getMethod("toGappyList");
            return (java.util.List<?>) toGappyListMethod.invoke(partyStore);
        } catch (Exception e) {
        }
        
        return null;
    }
}
