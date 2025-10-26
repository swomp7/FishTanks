package fish.tanks.screens;

import java.util.List;

import fish.tanks.FishTanks;
import fish.tanks.registries.FishTankComponents;
import fish.tanks.tank.FishStatusDataCarrier;
import fish.tanks.tank.FishStatus.FishDescription;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class FishTankScreen extends HandledScreen<FishTankScreenHandler> {
    
    public static final Identifier SMALL_TEXTURE = Identifier.of(FishTanks.MOD_ID, "textures/gui/small_fish_tank.png");
    public static final Identifier LARGE_TEXTURE = Identifier.of(FishTanks.MOD_ID, "textures/gui/large_fish_tank.png");
    public static final Identifier FOOD_ARROW_TEXTURE = Identifier.of(FishTanks.MOD_ID, "textures/gui/food_arrow.png");
    public static final Identifier SLOT_TEXTURE = Identifier.of(FishTanks.MOD_ID, "textures/gui/slot.png");
    public static final Identifier SCROLLER_TEXTURE = Identifier.ofVanilla("container/creative_inventory/scroller");
	public static final Identifier SCROLLER_DISABLED_TEXTURE = Identifier.ofVanilla("container/creative_inventory/scroller_disabled");

    private float scrollPosition;
	private boolean scrolling;
    private final int size;

    public FishTankScreen(FishTankScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.size = handler.getSlots();
        this.backgroundWidth = size <= 21 ? 176 : 195;
        this.playerInventoryTitleY = this.backgroundHeight - 93;
        this.playerInventoryTitleX += size <= 21 ? 0 : 1;
    }

    @Override
    protected void init() {
        super.init();
        titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
    }

    @Override
    protected void drawBackground(DrawContext context, float var2, int var3, int var4) {
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        
        if (size <= 21) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, SMALL_TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight, 256, 256);
            drawFeedingArrow(context, x + 15, y + 37);
        } else {
            int i = this.x + 175;
            int j = this.y + 18;
            int k = j + 142;
            context.drawTexture(RenderPipelines.GUI_TEXTURED, LARGE_TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight, 256, 256);
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, SCROLLER_TEXTURE, i, j + (int)((float)(k - j - 17) * this.scrollPosition), 12, 15);
            drawFeedingArrow(context, x + 16, y + 37);
        }
        drawSlots(context, var2, var3, var4);
    }

    private void drawFeedingArrow(DrawContext context, int x, int y) {
        if (handler.isFeeding()) context.drawTexture(RenderPipelines.GUI_TEXTURED, FOOD_ARROW_TEXTURE, x, y, 0, 0, 9, handler.getScaledFoodStatus(), 9, 14);
    }

    // Draw only the needed number of inventory slots
    private void drawSlots(DrawContext context, float var2, int var3, int var4) {
        if (size <= 21) {
            int rows = size <= 7 ? 1 : size <= 14 ? 2 : 3;
            for (int i = 0; i < rows - 1; i++) {
                for (int j = 0; j < 7; j++) {
                    context.drawTexture(RenderPipelines.GUI_TEXTURED, SLOT_TEXTURE, x + 40 + (j * 18), y + 17 + (i * 18), 0, 0, 18, 18, 18, 18);
                }
            }
            for (int i = 0; i < size - ((rows - 1) * 7); i++) {
                context.drawTexture(RenderPipelines.GUI_TEXTURED, SLOT_TEXTURE, x + 40 + (i * 18), y + 17 + (rows - 1) * 18, 0, 0, 18, 18, 18, 18);
            }
        } else {
            int i = this.handler.getRow(this.scrollPosition);
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 7; k++) {
                    int currentSlot = k + (j + i) * 7;
                    if (currentSlot >= 0 && currentSlot < size) {
                        context.drawTexture(RenderPipelines.GUI_TEXTURED, SLOT_TEXTURE, x + 41 + (k * 18), y + 17 + (j * 18), 0, 0, 18, 18, 18, 18);
                    }
                }
            }
        }
    }

    // Scroll the screen -- from Minecraft code

    public void resize(MinecraftClient client, int width, int height) {
        int i = ((FishTankScreenHandler)this.handler).getRow(this.scrollPosition);
		this.scrollPosition = ((FishTankScreenHandler)this.handler).getScrollPosition(i);
		((FishTankScreenHandler)this.handler).scrollItems(this.scrollPosition);
	}

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			if (size > 21 && this.isClickInScrollbar(mouseX, mouseY)) {
				this.scrolling = true;
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (button == 0) {
			this.scrolling = false;
		}

		return super.mouseReleased(mouseX, mouseY, button);
	}

    private boolean hasScrollbar() {
		return size > 21;
	}

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
			return true;
		} else if (!this.hasScrollbar()) {
			return false;
		} else {
			this.scrollPosition = ((FishTankScreenHandler)this.handler).getScrollPosition(this.scrollPosition, verticalAmount);
			((FishTankScreenHandler)this.handler).scrollItems(this.scrollPosition);
			return true;
		}
	}

    protected boolean isClickInScrollbar(double mouseX, double mouseY) {
		int i = this.x;
		int j = this.y;
		int k = i + 175;
		int l = j + 18;
		int m = k + 14;
		int n = l + 142;
		return mouseX >= (double)k && mouseY >= (double)l && mouseX < (double)m && mouseY < (double)n;
	}

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (this.scrolling) {
			int i = this.y + 18;
			int j = i + 112;
			this.scrollPosition = ((float)mouseY - (float)i - 7.5F) / ((float)(j - i) - 15.0F);
			this.scrollPosition = MathHelper.clamp(this.scrollPosition, 0.0F, 1.0F);
			((FishTankScreenHandler)this.handler).scrollItems(this.scrollPosition);
			return true;
		} else {
			return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
		}
	}

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    private boolean isItemTooltipSticky(ItemStack item) {
		return (Boolean)item.getTooltipData().map(TooltipComponent::of).map(TooltipComponent::isSticky).orElse(false);
	}

    // Write in the fish status when its slot is hovered over
    @Override
    protected void drawMouseoverTooltip(DrawContext context, int x, int y) {
        int invY = (height - backgroundHeight) / 2;
        if (this.focusedSlot != null && this.focusedSlot.hasStack() && y < invY + 76) {
			ItemStack stack = this.focusedSlot.getStack();
            if (stack.contains(FishTankComponents.FISH_STATUS)) {
                FishStatusDataCarrier carrier = stack.get(FishTankComponents.FISH_STATUS);
                int health = (int)((carrier.health() / carrier.maxHealth()) * 100);
                int hunger = (int)((carrier.hunger() / carrier.maxHunger()) * 100);
                int happiness = carrier.happiness();
                FishDescription status = carrier.status();
                List<Text> text = List.of(
                    Text.empty().append(Text.translatable("fish-tanks.fish_tank_screen.health").formatted(Formatting.BOLD, Formatting.GRAY)).append(Text.literal(" " + health + "%").formatted(health > 50 ? Formatting.DARK_GREEN : health > 25 ? Formatting.YELLOW : Formatting.DARK_RED)),
                    Text.empty().append(Text.translatable("fish-tanks.fish_tank_screen.hunger").formatted(Formatting.BOLD, Formatting.GRAY)).append(Text.literal(" " + hunger + "%").formatted(hunger > 50 ? Formatting.DARK_GREEN : hunger > 25 ? Formatting.YELLOW : Formatting.DARK_RED)),
                    Text.empty().append(Text.translatable("fish-tanks.fish_tank_screen.happiness").formatted(Formatting.BOLD, Formatting.GRAY)).append(Text.literal(" " + happiness + "%").formatted(happiness > 50 ? Formatting.DARK_GREEN : happiness > 25 ? Formatting.YELLOW : Formatting.DARK_RED)),
                    Text.empty().append(Text.translatable("fish-tanks.fish_tank_screen.status").formatted(Formatting.BOLD, Formatting.GRAY)).append(Text.literal(" ")).append(status.toText())
                );
                if (this.handler.getCursorStack().isEmpty() || this.isItemTooltipSticky(stack)) {
                    List<Text> texts = this.getTooltipFromItem(stack);
                    texts.add(Text.literal(""));
                    text.forEach(value -> texts.add(value));
                    context.drawTooltip(this.textRenderer, texts, stack.getTooltipData(), x, y, (Identifier)stack.get(DataComponentTypes.TOOLTIP_STYLE));
                }
                else context.drawTooltip(this.textRenderer, text, x, y);
            }
        }
        super.drawMouseoverTooltip(context, x, y);
    }
}
