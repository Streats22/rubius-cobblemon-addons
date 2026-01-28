package nl.streats1.rubiusaddons;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
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
}
