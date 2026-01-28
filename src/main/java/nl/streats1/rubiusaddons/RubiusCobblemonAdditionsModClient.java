package nl.streats1.rubiusaddons;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import nl.streats1.rubiusaddons.block.ModBlocks;
import nl.streats1.rubiusaddons.block.entity.ModBlockEntities;
import nl.streats1.rubiusaddons.client.renderer.CreatePoweredHealingMachineRenderer;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
// Use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = RubiusCobblemonAdditions.MOD_ID, value = Dist.CLIENT)
public class RubiusCobblemonAdditionsModClient {
    // Register client-side initialization in the main mod class constructor instead
    // This avoids the "dangling entrypoint" error
    
    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Client-side setup code goes here
    }
    
    /**
     * Register block entity renderers.
     */
    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
            ModBlockEntities.CREATE_POWERED_HEALING_MACHINE.get(),
            CreatePoweredHealingMachineRenderer::new
        );
    }
    
    /**
     * Register block color handlers for power state visualization.
     * Only tints the active indicator area (tintindex: 1), not the entire block.
     * Red = unpowered, Yellow = powered by Create SU
     */
    @SubscribeEvent
    static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register(new BlockColor() {
            @Override
            public int getColor(BlockState state, BlockAndTintGetter level, BlockPos pos, int tintIndex) {
                // Only tint the active indicator area (tintindex: 1)
                if (tintIndex != 1) {
                    return -1; // No tint for other faces
                }
                
                // Check if healing (powered=true) - use active color (no special tint)
                if (state.getValue(nl.streats1.rubiusaddons.block.CreatePoweredHealingMachineBlock.HEALING)) {
                    return -1; // No tint when healing (use active texture)
                }
                
                // Check power state: 0 = unpowered (red), 1 = powered (yellow)
                int powerState = state.getValue(nl.streats1.rubiusaddons.block.CreatePoweredHealingMachineBlock.POWER_STATE);
                
                if (powerState == 0) {
                    // Unpowered - red tint (RGB: 255, 0, 0)
                    return 0xFF0000;
                } else if (powerState == 1) {
                    // Powered - yellow tint (RGB: 255, 255, 0)
                    return 0xFFFF00;
                }
                
                return -1; // Default: no tint
            }
        }, ModBlocks.CREATE_POWERED_HEALING_MACHINE.get());
    }
}
