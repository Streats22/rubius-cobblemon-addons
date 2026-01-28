package nl.streats1.rubiusaddons.block.entity;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import nl.streats1.rubiusaddons.RubiusCobblemonAdditions;
import nl.streats1.rubiusaddons.block.ModBlocks;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(
        BuiltInRegistries.BLOCK_ENTITY_TYPE, RubiusCobblemonAdditions.MOD_ID
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CreatePoweredHealingMachineBlockEntity>> CREATE_POWERED_HEALING_MACHINE = BLOCK_ENTITIES.register(
        "create_powered_healing_machine",
        () -> BlockEntityType.Builder.of(
            CreatePoweredHealingMachineBlockEntity::new,
            ModBlocks.CREATE_POWERED_HEALING_MACHINE.get()
        ).build(null)
    );

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
