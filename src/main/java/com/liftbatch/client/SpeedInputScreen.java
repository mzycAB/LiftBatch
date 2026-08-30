package com.liftbatch.client;

import com.liftbatch.duck.LiftSpeedDuck;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.mtr.core.data.Lift;
import org.mtr.core.operation.UpdateDataRequest;
import org.mtr.mod.InitClient;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.packet.PacketUpdateData;

public class SpeedInputScreen extends Screen {

	private static final double MIN_SPEED = 0.1;
	private static final double MAX_SPEED = 100;
	private static final double VANILLA_SPEED = 10;

	private static final int TITLE_COLOR = 0xFFFFFF;
	private static final int HINT_COLOR = 0xA0A0A0;
	private static final int ERROR_COLOR = 0xFF5555;

	private final Lift lift;
	private final double currentSpeed;
	private TextFieldWidget textField;
	private Text error = null;

	public SpeedInputScreen(Lift lift) {
		super(Text.translatable("screen.liftbatch.speed_input.title"));
		this.lift = lift;
		final double stored = ((LiftSpeedDuck) lift).liftbatch$getSpeedBlocksPerSec();
		this.currentSpeed = stored > 0 ? stored : VANILLA_SPEED;
	}

	@Override
	protected void init() {
		final int centerX = this.width / 2;
		this.textField = new TextFieldWidget(this.textRenderer, centerX - 100, 66, 200, 20, this.title);
		this.textField.setMaxLength(8);
		this.textField.setFocusUnlocked(false);
		this.textField.setText(formatSpeed(this.currentSpeed));
		this.addDrawableChild(this.textField);
		this.setInitialFocus(this.textField);
		this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.liftbatch.confirm"), button -> confirm())
				.dimensions(centerX - 105, 100, 100, 20).build());
		this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.liftbatch.cancel"), button -> close())
				.dimensions(centerX + 5, 100, 100, 20).build());
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
			confirm();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private void confirm() {
		final double speed;
		try {
			speed = Double.parseDouble(this.textField.getText().trim());
		} catch (NumberFormatException e) {
			this.error = Text.translatable("message.liftbatch.invalid_speed");
			return;
		}
		if (speed < MIN_SPEED || speed > MAX_SPEED) {
			this.error = Text.translatable("message.liftbatch.invalid_speed_range", MIN_SPEED, (int) MAX_SPEED);
			return;
		}

		((LiftSpeedDuck) this.lift).liftbatch$setSpeedBlocksPerSec(speed);
		InitClient.REGISTRY_CLIENT.sendPacketToServer(new PacketUpdateData(new UpdateDataRequest(MinecraftClientData.getInstance()).addLift(this.lift)));
		if (this.client != null && this.client.player != null) {
			this.client.player.sendMessage(Text.translatable("message.liftbatch.speed_set", formatSpeed(speed)), false);
		}
		close();
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context, mouseX, mouseY, delta);
		super.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 24, TITLE_COLOR);
		context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.liftbatch.speed_input.current", formatSpeed(this.currentSpeed)), this.width / 2, 38, HINT_COLOR);
		context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.liftbatch.speed_input.hint"), this.width / 2, 52, HINT_COLOR);
		if (this.error != null) {
			context.drawCenteredTextWithShadow(this.textRenderer, this.error, this.width / 2, 90, ERROR_COLOR);
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	private static String formatSpeed(double speed) {
		return speed == Math.floor(speed) ? String.valueOf((long) speed) : String.valueOf(speed);
	}
}
