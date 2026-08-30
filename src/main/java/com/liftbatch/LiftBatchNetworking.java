package com.liftbatch;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class LiftBatchNetworking {

	public static final Identifier OPEN_FLOOR_INPUT = new Identifier(LiftBatchMod.MOD_ID, "open_floor_input");
	public static final Identifier FLOOR_INPUT = new Identifier(LiftBatchMod.MOD_ID, "floor_input");
	public static final Identifier OPEN_SPEED_INPUT = new Identifier(LiftBatchMod.MOD_ID, "open_speed_input");

	private static final double MAX_DISTANCE_SQ = 32.0 * 32.0;

	private LiftBatchNetworking() {
	}

	public static void register() {
		ServerPlayNetworking.registerGlobalReceiver(FLOOR_INPUT, (server, player, handler, buf, responseSender) -> {
			final BlockPos pos = buf.readBlockPos();
			final int startFloor = buf.readVarInt();
			server.execute(() -> {
				if (!(player.getWorld() instanceof ServerWorld world)) {
					return;
				}
				if (player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > MAX_DISTANCE_SQ) {
					return;
				}
				if (!LiftHelper.isLiftFloorBlock(world.getBlockState(pos).getBlock())) {
					player.sendMessage(Text.translatable("message.liftbatch.no_floors"), false);
					return;
				}
				final LiftHelper.RenumberResult result = LiftHelper.renumberFloors(world, pos, startFloor);
				if (result.count() == 0) {
					player.sendMessage(Text.translatable("message.liftbatch.no_floors"), false);
				} else {
					player.sendMessage(Text.translatable("message.liftbatch.renamed", result.count(), result.firstNumber(), result.lastNumber()), false);
				}
			});
		});
	}

	public static void openFloorInput(ServerPlayerEntity player, BlockPos anchor) {
		final PacketByteBuf buf = PacketByteBufs.create();
		buf.writeBlockPos(anchor);
		ServerPlayNetworking.send(player, OPEN_FLOOR_INPUT, buf);
	}

	public static void openSpeedInput(ServerPlayerEntity player, BlockPos anchor) {
		final PacketByteBuf buf = PacketByteBufs.create();
		buf.writeBlockPos(anchor);
		ServerPlayNetworking.send(player, OPEN_SPEED_INPUT, buf);
	}
}
