package nl.streats1.rubiusaddons.creativetab;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.streats1.rubiusaddons.RubiusCobblemonAdditions;
import nl.streats1.rubiusaddons.block.ModBlocks;
import nl.streats1.rubiusaddons.item.ModItems;

import java.util.function.Supplier;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(
        Registries.CREATIVE_MODE_TAB, RubiusCobblemonAdditions.MOD_ID
    );

    public static final Supplier<CreativeModeTab> RUBIUS_COBBLEMON_ADDITIONS_TAB = CREATIVE_TABS.register(
        "rubius_cobblemon_additions",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.rubiusadditions"))
            .icon(() -> {
                // Use the registered BlockItem for the icon
                try {
                    var item = ModItems.CREATE_POWERED_HEALING_MACHINE.get();
                    if (item != null && item != net.minecraft.world.item.Items.AIR) {
                        return new ItemStack(item);
                    }
                } catch (Exception e) {
                    // If item isn't ready, use a placeholder
                }
                // Fallback to a default item if block isn't ready
                return new ItemStack(net.minecraft.world.item.Items.IRON_BLOCK);
            })
            .displayItems((parameters, output) -> {
                // Add Create-powered healing machine using the registered item
                try {
                    var item = ModItems.CREATE_POWERED_HEALING_MACHINE.get();
                    if (item != null && item != net.minecraft.world.item.Items.AIR) {
                        ItemStack stack = new ItemStack(item);
                        if (!stack.isEmpty() && stack.getItem() != net.minecraft.world.item.Items.AIR) {
                            output.accept(stack);
                            RubiusCobblemonAdditions.LOGGER.info("Added item to creative tab: {}", stack);
                        } else {
                            RubiusCobblemonAdditions.LOGGER.warn("Item stack is empty or air");
                        }
                    } else {
                        RubiusCobblemonAdditions.LOGGER.warn("Item is null or air: {}", item);
                    }
                } catch (Exception e) {
                    RubiusCobblemonAdditions.LOGGER.error("Error adding item to creative tab: {}", e.getMessage(), e);
                }
            })
            .build()
    );

    public static void register(IEventBus eventBus) {
        CREATIVE_TABS.register(eventBus);
    }
}
