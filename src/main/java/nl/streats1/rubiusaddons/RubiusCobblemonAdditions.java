package nl.streats1.rubiusaddons;

import nl.streats1.rubiusaddons.item.ModItems;
import nl.streats1.rubiusaddons.block.ModBlocks;
import nl.streats1.rubiusaddons.block.entity.ModBlockEntities;
import nl.streats1.rubiusaddons.block.entity.CreatePoweredHealingMachineBlockEntity;
import nl.streats1.rubiusaddons.creativetab.ModCreativeTabs;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import nl.streats1.rubiusaddons.command.DamagePokemonCommand;
import nl.streats1.rubiusaddons.integration.CobbleDollarsIntegration;
import nl.streats1.rubiusaddons.integration.VillagerCobbleDollarsHandler;
import nl.streats1.rubiusaddons.network.CobbleDollarsShopPayloadHandlers;
import nl.streats1.rubiusaddons.network.CobbleDollarsShopPayloads;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

// The RubiusAddon here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(RubiusCobblemonAdditions.MOD_ID)
public class RubiusCobblemonAdditions {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "rubius_cobblemon_additions";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public RubiusCobblemonAdditions(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        CobbleDollarsShopPayloadHandlers.registerPayloads(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(VillagerCobbleDollarsHandler.class);
        // Register blocks first, then items (items reference blocks)
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        
        // Register client-side extension points (will only register on client side)
        try {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        } catch (Exception e) {
            // Ignore if not on client side
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Log that blocks and items are registered
    }

    // Creative tab items are now handled in ModCreativeTabs.displayItems()
    // This method is no longer needed but kept for potential future use

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }
    
    // Register custom commands
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        DamagePokemonCommand.register(event.getDispatcher());
    }
    
    /** When CobbleDollars shop UI is enabled, right-clicking a villager opens our shop screen instead of vanilla trading. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        handleVillagerShopInteract(event.getTarget(), event.getLevel().isClientSide(), () -> {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }, () -> event.getTarget().getId());
    }

    /** Fires before EntityInteract; needed so we cancel before vanilla opens the merchant GUI (e.g. for villagers with professions). */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        handleVillagerShopInteract(event.getTarget(), event.getLevel().isClientSide(), () -> {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }, () -> event.getTarget().getId());
    }

    private void handleVillagerShopInteract(net.minecraft.world.entity.Entity target, boolean isClientSide,
            Runnable cancelAction, java.util.function.IntSupplier getId) {
        if (!Config.USE_COBBLEDOLLARS_SHOP_UI.get() || CobbleDollarsIntegration.isModLoaded()) return;
        if (!(target instanceof Villager) && !(target instanceof WanderingTrader)) return;

        cancelAction.run();
        if (isClientSide) {
            PacketDistributor.sendToServer(new CobbleDollarsShopPayloads.RequestShopData(getId.getAsInt()));
        }
    }

    // Handle right-click on Create-powered healing machine
    @SubscribeEvent
    public void onBlockRightClick(@NotNull net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        
        BlockPos pos = event.getPos();
        Level level = event.getLevel();
        
        if (level.getBlockState(pos).is(ModBlocks.CREATE_POWERED_HEALING_MACHINE.get())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof CreatePoweredHealingMachineBlockEntity healingMachine) {
                Player player = event.getEntity();
                InteractionResult result = healingMachine.onUse(player, event.getHand());
                if (result != InteractionResult.PASS) {
                    event.setCanceled(true);
                    event.setCancellationResult(result);
                }
            }
        }
    }
}
