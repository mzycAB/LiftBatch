package com.liftbatch.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

public class FloorInputScreen extends Screen {

	private static final int TITLE_COLOR = 0xFFFFFF;
	private static final int HINT_COLOR = 0xA0A0A0;
	private static final int ERROR_COLOR = 0xFF5555;

	private final BlockPos anchor;
	private TextFieldWidget textField;
	private Text error = null;

	public FloorInputScreen(BlockPos anchor) {
		super(Text.translatable("screen.liftbatch.floor_input.title"));
		this.anchor = anchor;
	}

	@Override
	protected void init() {
		final int centerX = this.width / 2;
		this.textField = new TextFieldWidget(this.textRenderer, centerX - 100, 60, 200, 20, this.title);
		this.textField.setMaxLength(6);
		this.textField.setFocusUnlocked(false);
		this.addDrawableChild(this.textField);
		this.setInitialFocus(this.textField);
		this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.liftbatch.confirm"), button -> confirm())
				.dimensions(centerX - 105, 95, 100, 20).build());
		this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.liftbatch.cancel"), button -> close())
				.dimensions(centerX + 5, 95, 100, 20).build());
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
		final int startFloor;
		try {
			startFloor = Integer.parseInt(this.textField.getText().trim());
		} catch (NumberFormatException e) {
			this.error = Text.translatable("message.liftbatch.invalid_input");
			return;
		}
		LiftBatchClient.sendFloorInput(this.anchor, startFloor);
		close();
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context, mouseX, mouseY, delta);
		super.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 30, TITLE_COLOR);
		context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.liftbatch.floor_input.hint"), this.width / 2, 45, HINT_COLOR);
		if (this.error != null) {
			context.drawCenteredTextWithShadow(this.textRenderer, this.error, this.width / 2, 85, ERROR_COLOR);
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
