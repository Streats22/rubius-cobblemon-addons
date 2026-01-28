package nl.streats1.rubiusaddons.block;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.minecraft.world.level.block.Block;
import nl.streats1.rubiusaddons.RubiusCobblemonAdditions;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RubiusCobblemonAdditions.MOD_ID);
    
    public static final DeferredBlock<Block> CREATE_POWERED_HEALING_MACHINE = BLOCKS.register(
        "create_powered_healing_machine",
        () -> new CreatePoweredHealingMachineBlock(Block.Properties.of()
            .strength(3.5f)
            .requiresCorrectToolForDrops()
            .lightLevel(state -> 8) // Always emit blue light (level 8) when placed, like original healing machine
        )
    );

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
