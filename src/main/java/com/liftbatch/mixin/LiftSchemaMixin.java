package com.liftbatch.mixin;

import com.liftbatch.duck.LiftSpeedDuck;
import org.mtr.core.generated.data.LiftSchema;
import org.mtr.core.serializer.ReaderBase;
import org.mtr.core.serializer.WriterBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LiftSchema.class, remap = false)
public abstract class LiftSchemaMixin implements LiftSpeedDuck {

	@Unique
	private double liftbatch$speedBlocksPerSec = -1;

	@Override
	public double liftbatch$getSpeedBlocksPerSec() {
		return this.liftbatch$speedBlocksPerSec;
	}

	@Override
	public void liftbatch$setSpeedBlocksPerSec(double speedBlocksPerSec) {
		this.liftbatch$speedBlocksPerSec = speedBlocksPerSec;
	}

	@Inject(method = "updateData", at = @At("TAIL"), remap = false)
	private void liftbatch$readSpeed(ReaderBase readerBase, CallbackInfo ci) {
		readerBase.unpackDouble("liftbatch_speed", value -> this.liftbatch$speedBlocksPerSec = value);
	}

	@Inject(method = "serializeData", at = @At("TAIL"), remap = false)
	private void liftbatch$writeSpeed(WriterBase writerBase, CallbackInfo ci) {
		if (this.liftbatch$speedBlocksPerSec > 0) {
			writerBase.writeDouble("liftbatch_speed", this.liftbatch$speedBlocksPerSec);
		}
	}
}
