package nl.streats1.rubiusaddons.block;

import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.streats1.rubiusaddons.RubiusCobblemonAdditions;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RubiusCobblemonAdditions.MOD_ID);
    
    /**
     * Light like default Cobblemon healing machine when no Create power; scales with RPM.
     * State 0 (RPM &lt; 12) = some light (5), state 1 (12–32 RPM) = more (8), state 2 (32+ RPM) = max (12).
     */
    public static final DeferredBlock<Block> CREATE_POWERED_HEALING_MACHINE = BLOCKS.register(
        "create_powered_healing_machine",
        () -> new CreatePoweredHealingMachineBlock(Block.Properties.of()
            .strength(3.5f)
            .requiresCorrectToolForDrops()
            .lightLevel(state -> switch (state.getValue(CreatePoweredHealingMachineBlock.POWER_STATE)) {
                case 2 -> 12;  // 32+ RPM = max
                case 1 -> 8;   // 12–32 RPM = more
                default -> 5;  // 0 RPM = some (like default Cobblemon machine)
            })
        )
    );

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
