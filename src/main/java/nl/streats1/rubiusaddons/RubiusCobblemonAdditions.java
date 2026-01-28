package nl.streats1.rubiusaddons;

import nl.streats1.rubiusaddons.item.ModItems;
import nl.streats1.rubiusaddons.block.ModBlocks;
import nl.streats1.rubiusaddons.block.entity.ModBlockEntities;
import nl.streats1.rubiusaddons.block.entity.CreatePoweredHealingMachineBlockEntity;
import nl.streats1.rubiusaddons.creativetab.ModCreativeTabs;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;
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

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);
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
    
    // Handle right-click on Create-powered healing machine
    @SubscribeEvent
    public void onBlockRightClick(@NotNull net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        
        BlockPos pos = event.getPos();
        Level level = (Level) event.getLevel();
        
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
