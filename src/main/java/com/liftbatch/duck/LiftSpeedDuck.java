package com.liftbatch.duck;

public interface LiftSpeedDuck {

	double liftbatch$getSpeedBlocksPerSec();

	void liftbatch$setSpeedBlocksPerSec(double speedBlocksPerSec);

	default double liftbatch$getMaxSpeedBlocksPerMs() {
		final double speed = liftbatch$getSpeedBlocksPerSec();
		return speed <= 0 ? -1 : speed / 1000.0;
	}
}
