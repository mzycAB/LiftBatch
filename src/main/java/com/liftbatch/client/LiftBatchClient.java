package com.liftbatch.client;

import com.liftbatch.LiftBatchNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.mtr.core.data.Lift;
import org.mtr.mod.Init;
import org.mtr.mod.client.MinecraftClientData;

public class LiftBatchClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(LiftBatchNetworking.OPEN_FLOOR_INPUT, (client, handler, buf, responseSender) -> {
			final BlockPos pos = buf.readBlockPos();
			client.execute(() -> client.setScreen(new FloorInputScreen(pos)));
		});

		ClientPlayNetworking.registerGlobalReceiver(LiftBatchNetworking.OPEN_SPEED_INPUT, (client, handler, buf, responseSender) -> {
			final BlockPos pos = buf.readBlockPos();
			client.execute(() -> openSpeedInput(client, pos));
		});
	}

	private static void openSpeedInput(MinecraftClient client, BlockPos pos) {
		final org.mtr.mapping.holder.BlockPos holderPos = new org.mtr.mapping.holder.BlockPos(pos.getX(), pos.getY(), pos.getZ());
		Lift target = null;
		for (Lift lift : MinecraftClientData.getInstance().lifts) {
			if (lift.getFloorIndex(Init.blockPosToPosition(holderPos)) >= 0) {
				target = lift;
				break;
			}
		}
		if (target == null) {
			if (client.player != null) {
				client.player.sendMessage(Text.translatable("message.liftbatch.speed_sync_fail"), false);
			}
			return;
		}
		client.setScreen(new SpeedInputScreen(target));
	}

	static void sendFloorInput(BlockPos anchor, int startFloor) {
		final PacketByteBuf buf = PacketByteBufs.create();
		buf.writeBlockPos(anchor);
		buf.writeVarInt(startFloor);
		ClientPlayNetworking.send(LiftBatchNetworking.FLOOR_INPUT, buf);
	}
}
