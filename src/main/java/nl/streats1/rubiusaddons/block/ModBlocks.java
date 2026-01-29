package nl.streats1.rubiusaddons.block;

import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.streats1.rubiusaddons.RubiusCobblemonAdditions;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RubiusCobblemonAdditions.MOD_ID);
    
    /**
     * Light emission like Cobblemon healing machine: always on when placed.
     * State 0 (RPM &lt; 12) = blue lights, level 5. State 1 (12-32 RPM) = yellow, level 5. State 2 (32+ RPM) = red, level 12.
     */
    public static final DeferredBlock<Block> CREATE_POWERED_HEALING_MACHINE = BLOCKS.register(
        "create_powered_healing_machine",
        () -> new CreatePoweredHealingMachineBlock(Block.Properties.of()
            .strength(3.5f)
            .requiresCorrectToolForDrops()
            .lightLevel(state -> state.getValue(CreatePoweredHealingMachineBlock.POWER_STATE) == 2 ? 12 : 5)
        )
    );

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
