package euphy.upo.create_cultivation.content.cultivation_base;

import euphy.upo.create_cultivation.CreateCultivationCraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client screen for the Cultivation Base. Renders the 176x166 background texture
 * with 8 output slots on top and the player inventory below.
 */
public class CultivationBaseScreen extends AbstractContainerScreen<CultivationBaseMenu> {

    public static final ResourceLocation TEXTURE = CreateCultivationCraft.asResource("textures/gui/cultivation_base.png");

    private static final int LABEL_COLOR = 0xF4E7C5;

    public CultivationBaseScreen(CultivationBaseMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Use the overload with explicit texture dimensions: the 6-int blit()
        // variant assumes a 256x256 texture and would sample/scale incorrectly.
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, LABEL_COLOR, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, LABEL_COLOR, false);
    }
}
