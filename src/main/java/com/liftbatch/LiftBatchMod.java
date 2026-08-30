package com.liftbatch;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.mtr.core.data.Lift;

public class LiftBatchMod implements ModInitializer {

	public static final String MOD_ID = "liftbatch";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.registerAll();
		LiftBatchHandler.register();
		LiftBatchNetworking.register();
		// Force-load the MTR lift class so the speed mixins apply (and fail fast) at startup
		LOGGER.info("MTR lift hook: {}", Lift.class.getName());
		LOGGER.info("MTR Lift Batch Helper initialized");
	}
}
