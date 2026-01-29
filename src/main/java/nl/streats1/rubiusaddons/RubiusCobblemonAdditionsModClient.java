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
        // Note: Create's goggles system likely uses instanceof to check for IHaveGoggleInformation
        // Since Create is optional, we can't implement the interface directly.
        // Our block entity has the addToGoggleTooltip method, but Create might not detect it
        // if they use instanceof checks instead of reflection.
        // 
        // If goggles don't work, we may need to:
        // 1. Check Create's source code to see how they detect goggle information
        // 2. Use Create's event system if they have one for registering goggle providers
        // 3. Create a wrapper class that implements the interface (complex)
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
     * Only tints the indicator/lights area (tintindex: 1), like the normal Cobblemon healing machine.
     * Blue = RPM &lt; 12, Yellow = 12–32 RPM, Red = 32+ RPM.
     */
    @SubscribeEvent
    static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register(new BlockColor() {
            @Override
            public int getColor(BlockState state, BlockAndTintGetter level, BlockPos pos, int tintIndex) {
                if (tintIndex != 1) {
                    return -1;
                }
                int powerState = state.getValue(nl.streats1.rubiusaddons.block.CreatePoweredHealingMachineBlock.POWER_STATE);
                return switch (powerState) {
                    case 0 -> 0x0064FF;  // Blue – normal (like Cobblemon healing machine active/charged lights)
                    case 1 -> 0xFFCC00;  // Yellow – medium power (12 RPM / MD)
                    case 2 -> 0xFF0000;  // Red – full power (FL)
                    default -> 0x0064FF;
                };
            }
        }, ModBlocks.CREATE_POWERED_HEALING_MACHINE.get());
    }
}
