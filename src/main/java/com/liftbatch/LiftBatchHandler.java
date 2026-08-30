package com.liftbatch;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LiftBatchHandler {

	private enum WrenchMode {
		RENUMBER, SPEED, DISPLAY_BIND, BUTTONS_BIND, DISPLAY_UNBIND, BUTTONS_UNBIND;

		boolean isTwoStep() {
			return this == DISPLAY_BIND || this == BUTTONS_BIND || this == DISPLAY_UNBIND || this == BUTTONS_UNBIND;
		}

		boolean handlesPanels() {
			return this == DISPLAY_BIND || this == DISPLAY_UNBIND;
		}

		boolean handlesButtons() {
			return this == BUTTONS_BIND || this == BUTTONS_UNBIND;
		}

		boolean isUnbind() {
			return this == DISPLAY_UNBIND || this == BUTTONS_UNBIND;
		}
	}

	private record Selection(BlockPos floorAnchor, RegistryKey<World> worldKey, long expiresAt) {
	}

	private static final long SELECTION_TIMEOUT_MS = 300_000;
	private static final Map<UUID, Selection> SELECTIONS = new ConcurrentHashMap<>();
	private static final Map<Item, WrenchMode> WRENCHES = Map.of(
			ModItems.FLOOR_WRENCH, WrenchMode.RENUMBER,
			ModItems.SPEED_WRENCH, WrenchMode.SPEED,
			ModItems.DISPLAY_WRENCH, WrenchMode.DISPLAY_BIND,
			ModItems.CALL_BUTTON_WRENCH, WrenchMode.BUTTONS_BIND,
			ModItems.DISPLAY_UNBIND_WRENCH, WrenchMode.DISPLAY_UNBIND,
			ModItems.CALL_BUTTON_UNBIND_WRENCH, WrenchMode.BUTTONS_UNBIND
	);

	private LiftBatchHandler() {
	}

	public static void register() {
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			final ItemStack stack = player.getStackInHand(hand);
			final WrenchMode mode = WRENCHES.get(stack.getItem());
			if (mode == null) {
				return ActionResult.PASS;
			}

			final BlockPos pos = hitResult.getBlockPos();
			final Block block = world.getBlockState(pos).getBlock();
			final boolean isFloor = LiftHelper.isLiftFloorBlock(block);
			final boolean isPanel = LiftHelper.isLiftPanelBlock(block);
			final boolean isButtons = LiftHelper.isLiftButtonsBlock(block);
			if (!isFloor && !isPanel && !isButtons) {
				return ActionResult.PASS;
			}

			// Consume the click on the client so no vanilla/MTR GUI opens; the server does the work.
			if (world.isClient()) {
				return ActionResult.SUCCESS;
			}
			if (!(player instanceof ServerPlayerEntity serverPlayer) || !(world instanceof ServerWorld serverWorld)) {
				return ActionResult.PASS;
			}

			if (!mode.isTwoStep()) {
				if (!isFloor) {
					serverPlayer.sendMessage(Text.translatable("message.liftbatch.need_floor_block"), false);
					return ActionResult.SUCCESS;
				}
				if (mode == WrenchMode.RENUMBER) {
					LiftBatchNetworking.openFloorInput(serverPlayer, pos);
				} else {
					LiftBatchNetworking.openSpeedInput(serverPlayer, pos);
				}
				return ActionResult.SUCCESS;
			}

			if (isFloor) {
				SELECTIONS.put(serverPlayer.getUuid(), new Selection(pos.toImmutable(), serverWorld.getRegistryKey(), System.currentTimeMillis() + SELECTION_TIMEOUT_MS));
				serverPlayer.sendMessage(Text.translatable(selectionKey(mode), pos.getX(), pos.getY(), pos.getZ()), false);
				return ActionResult.SUCCESS;
			}

			final Selection selection = SELECTIONS.get(serverPlayer.getUuid());
			if (selection == null || System.currentTimeMillis() > selection.expiresAt()) {
				SELECTIONS.remove(serverPlayer.getUuid());
				serverPlayer.sendMessage(Text.translatable("message.liftbatch.select_shaft_first"), false);
				return ActionResult.SUCCESS;
			}
			if (!selection.worldKey().equals(serverWorld.getRegistryKey())) {
				serverPlayer.sendMessage(Text.translatable("message.liftbatch.select_shaft_first"), false);
				return ActionResult.SUCCESS;
			}

			if (mode.handlesPanels()) {
				if (!isPanel) {
					serverPlayer.sendMessage(Text.translatable("message.liftbatch.use_buttons_wrench" + suffix(mode)), false);
					return ActionResult.SUCCESS;
				}
				if (mode.isUnbind()) {
					final int count = LiftHelper.unbindColumn(serverWorld, selection.floorAnchor(), pos, true);
					reportUnbind(serverPlayer, count, "message.liftbatch.unbound_display", "message.liftbatch.none_bound_display");
				} else {
					report(serverPlayer, LiftHelper.bindColumn(serverWorld, selection.floorAnchor(), pos, true), "message.liftbatch.bound_display");
				}
			} else {
				if (!isButtons) {
					serverPlayer.sendMessage(Text.translatable("message.liftbatch.use_display_wrench" + suffix(mode)), false);
					return ActionResult.SUCCESS;
				}
				if (mode.isUnbind()) {
					final int count = LiftHelper.unbindColumn(serverWorld, selection.floorAnchor(), pos, false);
					reportUnbind(serverPlayer, count, "message.liftbatch.unbound_buttons", "message.liftbatch.none_bound_buttons");
				} else {
					report(serverPlayer, LiftHelper.bindColumn(serverWorld, selection.floorAnchor(), pos, false), "message.liftbatch.bound_buttons");
				}
			}
			return ActionResult.SUCCESS;
		});
	}

	private static String suffix(WrenchMode mode) {
		return mode.isUnbind() ? "_unbind" : "_bind";
	}

	private static String selectionKey(WrenchMode mode) {
		return switch (mode) {
			case DISPLAY_BIND -> "message.liftbatch.shaft_selected_display";
			case BUTTONS_BIND -> "message.liftbatch.shaft_selected_buttons";
			case DISPLAY_UNBIND -> "message.liftbatch.shaft_selected_display_unbind";
			default -> "message.liftbatch.shaft_selected_buttons_unbind";
		};
	}

	private static void report(ServerPlayerEntity player, LiftHelper.BindResult result, String key) {
		if (result.count() == 0) {
			player.sendMessage(Text.translatable("message.liftbatch.no_targets"), false);
		} else {
			player.sendMessage(Text.translatable(key, result.count(), result.firstNumber(), result.lastNumber()), false);
		}
	}

	private static void reportUnbind(ServerPlayerEntity player, int count, String successKey, String noneKey) {
		if (count == 0) {
			player.sendMessage(Text.translatable(noneKey), false);
		} else {
			player.sendMessage(Text.translatable(successKey, count), false);
		}
	}
}
