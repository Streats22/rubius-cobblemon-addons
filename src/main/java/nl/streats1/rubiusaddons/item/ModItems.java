package nl.streats1.rubiusaddons.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import nl.streats1.rubiusaddons.RubiusCobblemonAdditions;
import nl.streats1.rubiusaddons.block.ModBlocks;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RubiusCobblemonAdditions.MOD_ID);

    // Register BlockItem for Create-powered healing machine
    public static final DeferredItem<Item> CREATE_POWERED_HEALING_MACHINE = ITEMS.register(
        "create_powered_healing_machine",
        () -> new BlockItem(ModBlocks.CREATE_POWERED_HEALING_MACHINE.get(), new Item.Properties())
    );

    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
}
