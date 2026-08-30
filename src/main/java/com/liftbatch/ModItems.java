package com.liftbatch;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ModItems {

	public static final Item FLOOR_WRENCH = register("floor_wrench", "item.liftbatch.floor_wrench.tooltip");
	public static final Item SPEED_WRENCH = register("speed_wrench", "item.liftbatch.speed_wrench.tooltip");
	public static final Item DISPLAY_WRENCH = register("display_wrench", "item.liftbatch.display_wrench.tooltip");
	public static final Item CALL_BUTTON_WRENCH = register("call_button_wrench", "item.liftbatch.call_button_wrench.tooltip");
	public static final Item DISPLAY_UNBIND_WRENCH = register("display_unbind_wrench", "item.liftbatch.display_unbind_wrench.tooltip");
	public static final Item CALL_BUTTON_UNBIND_WRENCH = register("call_button_unbind_wrench", "item.liftbatch.call_button_unbind_wrench.tooltip");

	static {
		Registry.register(Registries.ITEM_GROUP, new Identifier(LiftBatchMod.MOD_ID, "general"), FabricItemGroup.builder()
				.icon(() -> new ItemStack(FLOOR_WRENCH))
				.displayName(Text.translatable("itemGroup.liftbatch"))
				.entries((context, entries) -> {
					entries.add(FLOOR_WRENCH);
					entries.add(SPEED_WRENCH);
					entries.add(DISPLAY_WRENCH);
					entries.add(CALL_BUTTON_WRENCH);
					entries.add(DISPLAY_UNBIND_WRENCH);
					entries.add(CALL_BUTTON_UNBIND_WRENCH);
				})
				.build());
	}

	private ModItems() {
	}

	private static Item register(String name, String tooltipKey) {
		return Registry.register(Registries.ITEM, new Identifier(LiftBatchMod.MOD_ID, name), new WrenchItem(new Item.Settings().maxCount(1), tooltipKey));
	}

	public static void registerAll() {
		// Class loading performs the registration
	}
}
