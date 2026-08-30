package com.liftbatch.mixin;

import com.liftbatch.duck.LiftSpeedDuck;
import org.mtr.core.data.Lift;
import org.mtr.core.tool.Utilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = Lift.class, remap = false)
public abstract class LiftMixin {

	/**
	 * Vanilla lift: max speed 0.01 blocks/ms (10 blocks/s), acceleration 4.0E-6 blocks/ms².
	 * Scale acceleration by the same factor as max speed so the acceleration/braking feel is preserved.
	 */
	@ModifyConstant(method = "tick", constant = @Constant(doubleValue = 4.0E-6), remap = false)
	private double liftbatch$modifyAccel(double original) {
		final double maxSpeed = ((LiftSpeedDuck) this).liftbatch$getMaxSpeedBlocksPerMs();
		return maxSpeed <= 0 ? original : original * (maxSpeed / 0.01);
	}

	/**
	 * The speed clamp call passes min = -0.01F (the only clamp in tick with a negative min;
	 * the railProgress clamp passes min = 0.0), so use that to identify it.
	 */
	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lorg/mtr/core/tool/Utilities;clamp(DDD)D"), remap = false)
	private double liftbatch$redirectClamp(double value, double min, double max) {
		if (min < 0) {
			final double maxSpeed = ((LiftSpeedDuck) this).liftbatch$getMaxSpeedBlocksPerMs();
			if (maxSpeed > 0) {
				return Utilities.clamp(value, -maxSpeed, maxSpeed);
			}
		}
		return Utilities.clamp(value, min, max);
	}
}
