package euphy.upo.create_cultivation.content.cultivation_base;

import euphy.upo.create_cultivation.CreateCultivationCraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client screen for the Cultivation Base. Renders the 176x166 background texture
 * with 8 output slots on top and the player inventory below. Inside the dark
 * panel between the catalyst slot and the output slots, scrolling animations
 * reflect the current boost state: brass arrows while the catalyst is active,
 * blue water lines while the tank is watered, and both together (1.5x faster)
 * while both are active.
 */
public class CultivationBaseScreen extends AbstractContainerScreen<CultivationBaseMenu> {

    public static final ResourceLocation TEXTURE = CreateCultivationCraft.asResource("textures/gui/cultivation_base.png");
    public static final ResourceLocation CATALYST_ARROWS_TEXTURE = CreateCultivationCraft.asResource("textures/gui/catalyst_arrows.png");
    public static final ResourceLocation WATER_ARROWS_TEXTURE = CreateCultivationCraft.asResource("textures/gui/catalyst_arrows_2.png");

    private static final int LABEL_COLOR = 0xF4E7C5;

    // Catalyst arrow strip: 128x128 texture, arrows occupy y 51..58 and repeat
    // horizontally with a period of 13 px starting at x=17.
    private static final int CATALYST_V = 51;
    private static final int CATALYST_HEIGHT = 8;
    private static final int CATALYST_BASE_U = 17;
    private static final int CATALYST_PERIOD = 13;
    private static final double CATALYST_SPEED_PX_PER_SEC = 5.0; // 0.25 px/tick * 20 tps

    // Water strip: 128x128 texture, lines occupy rows y=58 and y=71 (panel top
    // and bottom) and repeat horizontally with a period of 7 px starting at x=13.
    private static final int WATER_V = 58;
    private static final int WATER_HEIGHT = 14;
    private static final int WATER_BASE_U = 13;
    private static final int WATER_PERIOD = 7;
    private static final double WATER_SPEED_PX_PER_SEC = 3.75; // slightly slower than catalyst arrows

    // When both animations play, both scroll 1.5x faster.
    private static final float BOTH_SPEED_BONUS = 1.5f;

    // Dark panel in the main texture: (38,24,54x14). The catalyst arrow window
    // is centred inside it; the water strip covers the whole panel.
    private static final int PANEL_X = 38;
    private static final int PANEL_Y = 24;
    private static final int PANEL_WIDTH = 54;
    private static final int WINDOW_X = 39;
    private static final int WINDOW_Y = 27;
    private static final int WINDOW_WIDTH = 52;
    private static final int TEXTURE_SIZE = 128;

    // Wall-clock animation epoch. System.nanoTime() is monotonic per JVM and
    // fully independent of server time sync packets, integrated-server MSPT
    // fluctuations and vanilla's tick-rate catch-up adjustments — every one of
    // those can step a tick-based clock and show up as periodic stutter in GUI
    // animations. Purely cosmetic motion only needs real elapsed time.
    private static final double ANIM_EPOCH = System.nanoTime();

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

        boolean catalyst = this.menu.isCatalystActive();
        boolean watered = this.menu.isWatered();
        if (catalyst || watered) {
            // Both states active: each animation plays 1.5x faster.
            float speedBonus = catalyst && watered ? BOTH_SPEED_BONUS : 1.0f;
            if (watered) {
                renderWaterArrows(graphics, speedBonus);
            }
            if (catalyst) {
                renderCatalystArrows(graphics, speedBonus);
            }
        }
    }

    /**
     * Draws a 52x8 window of the brass arrow strip centred inside the dark panel.
     * The sample offset moves left over time, which makes the arrows travel right.
     * Because the strip tiles with a 13 px period, the wrapped offset is seamless.
     */
    private void renderCatalystArrows(GuiGraphics graphics, float speedBonus) {
        float u = scrollingU(CATALYST_BASE_U, CATALYST_PERIOD, CATALYST_SPEED_PX_PER_SEC * speedBonus);
        graphics.blit(CATALYST_ARROWS_TEXTURE, this.leftPos + WINDOW_X, this.topPos + WINDOW_Y,
                u, CATALYST_V, WINDOW_WIDTH, CATALYST_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    /**
     * Draws the blue water lines across the full dark panel. Only rows 58 and 71
     * of the strip are opaque, so the panel centre stays clear for the catalyst
     * arrows. The strip tiles with a 7 px period.
     */
    private void renderWaterArrows(GuiGraphics graphics, float speedBonus) {
        float u = scrollingU(WATER_BASE_U, WATER_PERIOD, WATER_SPEED_PX_PER_SEC * speedBonus);
        graphics.blit(WATER_ARROWS_TEXTURE, this.leftPos + PANEL_X, this.topPos + PANEL_Y,
                u, WATER_V, PANEL_WIDTH, WATER_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    /**
     * Current texture U for a strip scrolling right: base + (period - offset),
     * driven by real wall-clock time. Unlike game time or tick-based holders,
     * elapsed real time is a strictly monotonic float source, so the scroll
     * offset can never jump backward or step; frame pacing only changes how far
     * it moves per frame, never the direction or continuity.
     */
    private float scrollingU(int baseU, int period, double pixelsPerSecond) {
        double seconds = (System.nanoTime() - ANIM_EPOCH) * 1.0e-9;
        double scroll = (seconds * pixelsPerSecond) % period;
        return (float) (baseU + (period - scroll));
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
