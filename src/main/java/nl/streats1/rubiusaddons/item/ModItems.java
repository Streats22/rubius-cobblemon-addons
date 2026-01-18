package nl.streats1.rubiusaddons.item;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.streats1.rubiusaddons.RubiusCobblemonAdditions;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RubiusCobblemonAdditions.MOD_ID);

    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
}
