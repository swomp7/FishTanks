package fish.tanks.screens;

import fish.tanks.FishTanks;
import fish.tanks.registries.FishTankItems;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class FilterScreen extends HandledScreen<FilterScreenHandler> {

    public static final Identifier BACKGROUND_TEXTURE = Identifier.of(FishTanks.MOD_ID, "textures/gui/filter.png");
    public static final Identifier STATUS_BAR_TEXTURE = Identifier.of(FishTanks.MOD_ID, "textures/gui/filter_status_bar.png");
    public static final Identifier BUBBLES_TEXTURE = Identifier.of(FishTanks.MOD_ID, "textures/gui/filter_bubbles.png");

    public FilterScreen(FilterScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundHeight = 143;
		this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
    }

    @Override
    protected void drawBackground(DrawContext context, float deltaTicks, int mouseX, int mouseY) {
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, BACKGROUND_TEXTURE, x, y, 0.0F, 0.0F, this.backgroundWidth, this.backgroundHeight, 256, 256);
        renderStatusBar(context, x, y);
        renderBubbles(context, x, y);
    }
    
    public void renderStatusBar(DrawContext context, int x, int y) {
        int[] status = handler.getFilterStatus();
        int n = handler.getScaledFilterStatus();
        if (status[0] > status[1] / 2) context.drawTexture(RenderPipelines.GUI_TEXTURED, STATUS_BAR_TEXTURE, x + 102, y + 43 - n, 0, 21 - n, 6, n, 18, 22);
        else if (status[0] > status[1] / 5) context.drawTexture(RenderPipelines.GUI_TEXTURED, STATUS_BAR_TEXTURE, x + 102, y + 43 - n, 6, 21 - n, 6, n, 18, 22);
        else context.drawTexture(RenderPipelines.GUI_TEXTURED, STATUS_BAR_TEXTURE, x + 102, y + 43 - n, 12, 21 - n, 6, n, 18, 22);

        if (status[0] != 0) {
            if (status[0] == status[1]) context.drawTexture(RenderPipelines.GUI_TEXTURED, STATUS_BAR_TEXTURE, x + 102, y + 22, 0, 21, 6, 1, 18, 22);
            context.drawTexture(RenderPipelines.GUI_TEXTURED, STATUS_BAR_TEXTURE, x + 102, y + 43, 0, 0, 6, 1, 18, 22);
        }
    }

    public void renderBubbles(DrawContext context, int x, int y) {
        if (handler.isFiltering()) {
            int n = handler.getScaledBubblingProgress();
            context.drawTexture(RenderPipelines.GUI_TEXTURED, BUBBLES_TEXTURE, x + 64, y + 17 + n, 0, n, 12, 29 - n, 12, 29);
        }
    }

    // Indicate the filter status on hover over the status bar
    @Override
    public void drawMouseoverTooltip(DrawContext context, int mouseX, int mouseY) {
        super.drawMouseoverTooltip(context, mouseX, mouseY);
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        if (handler.getCursorStack().isEmpty() && mouseX >= 102 + x && mouseX <= 107 + x && mouseY >= 22 + y && mouseY <= 43 + y) {
            int[] status = handler.getFilterStatus();
            if ((handler.getSlot(0).getStack().isOf(FishTankItems.FILTER_BAG) || handler.getSlot(0).getStack().isOf(FishTankItems.DIRTY_FILTER_BAG)) && status[1] != 0) {
                context.drawTooltip(textRenderer, Text.literal(Text.translatable("fish-tanks.filter_bag.filter_status").getString() + ((status[0] * 100) / status[1]) + "%"), mouseX, mouseY);
            }
        }
    }

    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        renderBackground(context, mouseX, mouseY, deltaTicks);
		super.render(context, mouseX, mouseY, deltaTicks);
		drawMouseoverTooltip(context, mouseX, mouseY);
	}
}
