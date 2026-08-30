package com.liftbatch;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.mtr.core.data.Lift;
import org.mtr.core.data.LiftFloor;
import org.mtr.core.data.Position;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mod.Init;
import org.mtr.mod.block.BlockLiftButtons;
import org.mtr.mod.block.BlockLiftPanelBase;
import org.mtr.mod.block.BlockLiftTrackFloor;
import org.mtr.mod.client.MinecraftClientData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class LiftHelper {

	public record RenumberResult(int count, String firstNumber, String lastNumber) {
	}

	public record BindResult(int count, String firstNumber, String lastNumber) {
	}

	private LiftHelper() {
	}

	public static boolean isLiftFloorBlock(Block block) {
		return block instanceof BlockLiftTrackFloor;
	}

	public static boolean isLiftPanelBlock(Block block) {
		return block instanceof BlockLiftPanelBase;
	}

	public static boolean isLiftButtonsBlock(Block block) {
		return block instanceof BlockLiftButtons;
	}

	/**
	 * Scans the full vertical column at the anchor's (x, z) and collects every block
	 * of the given type (MTR lift floor block / lift panel / lift buttons), sorted by Y ascending.
	 */
	public static List<BlockPos> scanColumn(ServerWorld world, BlockPos anchor, Class<?> blockClass) {
		final List<BlockPos> result = new ArrayList<>();
		final int x = anchor.getX();
		final int z = anchor.getZ();
		final int minY = world.getBottomY();
		final int maxY = minY + world.getHeight();
		for (int y = minY; y < maxY; y++) {
			final BlockPos pos = new BlockPos(x, y, z);
			if (blockClass.isInstance(world.getBlockState(pos).getBlock())) {
				result.add(pos);
			}
		}
		result.sort(Comparator.comparingInt(BlockPos::getY));
		return result;
	}

	/**
	 * Renames every lift floor block in the column at the anchor's (x, z), numbering them
	 * from {@code startFloor} upward and skipping floor 0, then refreshes the lift data
	 * in the MTR save (same mechanism as MTR's own lift refresher).
	 */
	public static RenumberResult renumberFloors(ServerWorld world, BlockPos anchor, int startFloor) {
		final List<BlockPos> floors = scanColumn(world, anchor, BlockLiftTrackFloor.class);
		if (floors.isEmpty()) {
			return new RenumberResult(0, "", "");
		}

		final ObjectArrayList<LiftFloor> liftFloors = new ObjectArrayList<>();
		int current = startFloor;
		String first = null;
		String last = null;
		for (BlockPos pos : floors) {
			final BlockEntity blockEntity = world.getBlockEntity(pos);
			if (blockEntity instanceof BlockLiftTrackFloor.BlockEntity floorEntity) {
				final String number = String.valueOf(current);
				floorEntity.setData(number, floorEntity.getFloorDescription(), floorEntity.getShouldDing());
				liftFloors.add(new LiftFloor(new Position(pos.getX(), pos.getY(), pos.getZ()), number, floorEntity.getFloorDescription()));
				if (first == null) {
					first = number;
				}
				last = number;
			}
			current++;
			if (current == 0) {
				current = 1;
			}
		}

		if (liftFloors.size() >= 2) {
			regenerateLift(world, liftFloors);
		}
		return new RenumberResult(liftFloors.size(), first == null ? "" : first, last == null ? "" : last);
	}

	/**
	 * Binds every display panel (or call buttons block) in the column at the target's (x, z)
	 * to the matching floor of the previously selected elevator shaft column.
	 * Each target binds to the highest floor at or below its own Y level.
	 */
	public static BindResult bindColumn(ServerWorld world, BlockPos floorAnchor, BlockPos targetAnchor, boolean isPanel) {
		final List<BlockPos> floors = scanColumn(world, floorAnchor, BlockLiftTrackFloor.class);
		final List<BlockPos> targets = scanColumn(world, targetAnchor, isPanel ? BlockLiftPanelBase.class : BlockLiftButtons.class);
		if (floors.isEmpty() || targets.isEmpty()) {
			return new BindResult(0, "", "");
		}

		final org.mtr.mapping.holder.World holderWorld = new org.mtr.mapping.holder.World(world);
		int bound = 0;
		String first = null;
		String last = null;
		for (BlockPos target : targets) {
			final BlockPos floorPos = matchFloor(floors, target.getY());
			final BlockEntity floorEntity = world.getBlockEntity(floorPos);
			final BlockEntity targetEntity = world.getBlockEntity(target);
			if (!(floorEntity instanceof BlockLiftTrackFloor.BlockEntity)) {
				continue;
			}
			final String number = ((BlockLiftTrackFloor.BlockEntity) floorEntity).getFloorNumber();
			final org.mtr.mapping.holder.BlockPos holderFloorPos =
					new org.mtr.mapping.holder.BlockPos(floorPos.getX(), floorPos.getY(), floorPos.getZ());
			if (isPanel) {
				if (targetEntity instanceof BlockLiftPanelBase.BlockEntityBase panelEntity) {
					panelEntity.registerFloor(holderWorld, holderFloorPos, true);
					bound++;
					if (first == null) {
						first = number;
					}
					last = number;
				}
			} else {
				if (targetEntity instanceof BlockLiftButtons.BlockEntity buttonsEntity) {
					buttonsEntity.registerFloor(holderFloorPos, true);
					bound++;
					if (first == null) {
						first = number;
					}
					last = number;
				}
			}
		}
		return new BindResult(bound, first == null ? "" : first, last == null ? "" : last);
	}

	/**
	 * Unbinds every display panel (or call buttons block) in the column at the target's (x, z)
	 * from the lift whose floors are in the shaft column at the anchor's (x, z).
	 * Bindings to other lifts are left untouched.
	 */
	public static int unbindColumn(ServerWorld world, BlockPos floorAnchor, BlockPos targetAnchor, boolean isPanel) {
		final List<BlockPos> shaftFloors = scanColumn(world, floorAnchor, BlockLiftTrackFloor.class);
		if (shaftFloors.isEmpty()) {
			return 0;
		}
		final Set<BlockPos> shaftFloorSet = new HashSet<>(shaftFloors);
		final List<BlockPos> targets = scanColumn(world, targetAnchor, isPanel ? BlockLiftPanelBase.class : BlockLiftButtons.class);
		final org.mtr.mapping.holder.World holderWorld = new org.mtr.mapping.holder.World(world);
		int unbound = 0;
		for (BlockPos target : targets) {
			final BlockEntity targetEntity = world.getBlockEntity(target);
			if (isPanel) {
				if (targetEntity instanceof BlockLiftPanelBase.BlockEntityBase panelEntity) {
					final org.mtr.mapping.holder.BlockPos bound = panelEntity.getTrackPosition();
					if (bound != null && shaftFloorSet.contains(new BlockPos(bound.getX(), bound.getY(), bound.getZ()))) {
						panelEntity.registerFloor(holderWorld, bound, false);
						unbound++;
					}
				}
			} else {
				if (targetEntity instanceof BlockLiftButtons.BlockEntity buttonsEntity) {
					final List<org.mtr.mapping.holder.BlockPos> toRemove = new ArrayList<>();
					buttonsEntity.forEachTrackPosition(trackPos -> {
						if (shaftFloorSet.contains(new BlockPos(trackPos.getX(), trackPos.getY(), trackPos.getZ()))) {
							toRemove.add(trackPos);
						}
					});
					for (org.mtr.mapping.holder.BlockPos trackPos : toRemove) {
						buttonsEntity.registerFloor(trackPos, false);
					}
					if (!toRemove.isEmpty()) {
						unbound++;
					}
				}
			}
		}
		return unbound;
	}

	private static BlockPos matchFloor(List<BlockPos> floorsAscending, int y) {
		BlockPos best = null;
		for (BlockPos floor : floorsAscending) {
			if (floor.getY() <= y) {
				best = floor;
			} else {
				break;
			}
		}
		return best != null ? best : floorsAscending.get(0);
	}

	private static void regenerateLift(ServerWorld world, ObjectArrayList<LiftFloor> liftFloors) {
		final Lift lift = new Lift(new MinecraftClientData());
		lift.setFloors(liftFloors);
		lift.setDimensions(3.0, 2.0, 2.0, 0.0, 0.0, 0.0);
		lift.setStyle("default_transparent");
		Init.sendMessageC2S("generate_by_lift", new org.mtr.mapping.holder.MinecraftServer(world.getServer()),
				new org.mtr.mapping.holder.World(world), lift, null, null);
	}
}
