package euphy.upo.create_cultivation.content.greenhouse;

import org.jetbrains.annotations.Nullable;

import com.mojang.math.Axis;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import euphy.upo.create_cultivation.CreateCultivationCraft;
import euphy.upo.create_cultivation.content.climate.CCDataMaps;
import euphy.upo.create_cultivation.content.climate.ClimateUnits;
import euphy.upo.create_cultivation.registry.CCBlocks;
import euphy.upo.create_cultivation.registry.CCSounds;
import euphy.upo.create_cultivation.infrastructure.network.GreenhouseSetpointsPayload;
import euphy.upo.create_cultivation.infrastructure.network.GreenhouseSnapshotPayload;

import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Greenhouse controller screen: three layers on top of the user's 256x256
 * art (panel at (0,0), scroll-row template at the texture bottom).
 *
 * <p>Layer 1 - flap display strip (texture 18,21..238,36): device / crop /
 * volume counters as {@link FlapTextWidget}s.
 * Layer 2 - two 180px gauge bars (texture rows y70..74 / y99..103, x39..218):
 * the big arrow sets the target (drag), the small arrow marks the current
 * climate; the value shows in the round slot above each bar (x114..142).
 * Layer 3 - crop rows cloned from the texture bottom template (y231..248,
 * 18px tall): item render with bottom-right count, mini temp/humidity bars
 * with optimal and survival range arrows from widgets.png. Scrolling is the
 * mouse wheel; no scrollbar art needed.
 */
public class GreenhouseControllerScreen extends AbstractContainerScreen<GreenhouseControllerMenu> {

    public static final ResourceLocation BACKGROUND =
            CreateCultivationCraft.asResource("textures/gui/greenhouse_gui.png");
    public static final ResourceLocation WIDGETS =
            CreateCultivationCraft.asResource("textures/gui/greenhouse_widgets.png");

    private static final int BG_WIDTH = 256;
    private static final int BG_HEIGHT = 256;
    /** The texture's bottom band is the row template strip, not panel art. */
    private static final int PANEL_DRAW_HEIGHT = 221;

    // layer 1: flap strip (texture rect 18,21..238,36)
    private static final int FLAP_TY0 = 21;
    private static final int FLAP_TY1 = 36;

    // layer 2: gauge bars
    private static final int TEMP_BAR_X = 39;
    private static final int TEMP_BAR_Y = 70;
    private static final int HUM_BAR_Y = 99;
    private static final int BAR_LENGTH = 180;
    private static final int DISC_CX = 128;
    // disc digits: centre X, raised 4px above the slot centre
    private static final int TEMP_DISC_CY = 59;
    private static final int HUM_DISC_CY = 88;

    // layer 3: scroll row template (texture y231..248, panel width)
    private static final int ROW_TX = 15;
    private static final int ROW_TY = 231;
    private static final int ROW_W = 227;
    private static final int ROW_H = 18;
    /** list rows render 4px apart (18px template + 4px gap) */
    private static final int ROW_STRIDE = 22;
    private static final int SLOT_TX = 24;
    private static final int MINI_TX = 47;
    private static final int MINI_TY = 237;
    private static final int MINI_TY2 = 244;
    private static final int MINI_LENGTH = 180;
    /** list window top edge = the visible upper display limit */
    private static final int LIST_Y = 123;
    private static final int VIEW_ROWS = 3;
    /** rows render until this y (exclusive); keeps the window edge clean */
    private static final int LIST_CULL_Y = 190;

    // confirm button: hook art baked into the panel at (217,197)..(234,214)
    private static final int CONFIRM_X = 217;
    private static final int CONFIRM_Y = 197;
    private static final int CONFIRM_W = 18;
    private static final int CONFIRM_H = 18;

    // auto-setpoint toggle switches (right of each gauge bar), skinned from
    // Create's widgets.png TRAINMAP_TOGGLE sprites (12x7); server-authoritative
    private static final int AUTO_BTN_X = 207;
    private static final int AUTO_BTN_W = 12;
    private static final int AUTO_BTN_H = 7;
    private static final int AUTO_TOGGLE_OFF_U = 219, AUTO_TOGGLE_OFF_V = 27;
    private static final int AUTO_TOGGLE_ON_U = 219, AUTO_TOGGLE_ON_V = 19;

    // widgets.png icons (connected components)
    private static final int W_DRAG_U = 6, W_DRAG_V = 4, W_DRAG_W = 5, W_DRAG_H = 5;
    private static final int W_CUR_U = 22, W_CUR_V = 4, W_CUR_W = 3, W_CUR_H = 5;
    private static final int W_DRAG_HUM_V = 14, W_CUR_HUM_V = 14;
    private static final int W_OPT_U = 37, W_OPT_V = 4, W_OPT_W = 3, W_OPT_H = 6;
    private static final int W_SURV_U = 52, W_SURV_V = 4;
    // horizontally mirrored arrow sprites for range MAXIMA (point left)
    private static final int W_OPT_MAX_U = 64, W_SURV_MAX_U = 80;
    // 50%-alpha range canvases painted onto the mini bar track
    private static final int CANVAS_OPT = 0x801DAE19;  // #1dae19 green
    private static final int CANVAS_SURV = 0x80D05D1F; // #d05d1f orange
    // 50%-alpha stall frame: outside the survival range
    private static final int FRAME_STALL = 0x80C30E0E; // #c30e0e red
    // 50%-alpha survival frame: inside survival, outside optimal
    private static final int FRAME_SURV = 0x80F4C86A; // #f4c86a

    // tooltip border gradients {top, bottom}: hues follow the frame colours
    private static final int[] BORDER_OPTIMAL = {0xFF1DAE19, 0xFF11680F};
    private static final int[] BORDER_SURVIVAL = {0xFFF4C86A, 0xFF92783F};
    private static final int[] BORDER_STALL = {0xFFC30E0E, 0xFF750808};

    private static final int VIEW_MAX_COLS = 16; // 96px per mini bar scale

    // gear-detent rhythm of the setpoint drag: one click per this many
    // pixels of horizontal travel (uniform ratchet on both bars)
    private static final double DETENT_STEP_PX = 6.0;

    private FlapTextWidget flapDevices;
    private FlapTextWidget flapVolume;
    private FlapTextWidget flapDevicesLabel;
    private FlapTextWidget flapVolumeLabel;

    private float displayedTemp = 20f;
    private float displayedHum = 50f;

    /** Staged setpoints - only sent when the hook button is clicked. */
    private Integer stagedTempC10;
    private Integer stagedHum10;

    /** StockKeeper-style smooth list scroll: integer target, exp chase. */
    private final LerpedFloat itemScroll = LerpedFloat.linear().startWithValue(0);
    private boolean draggingTemp;
    private boolean draggingHum;
    /** X of the last played detent click; NaN = the drag is not engaged yet. */
    private double lastClickX = Double.NaN;

    /** Hovered crop row's tooltip. Rendered only after the scissor window
     * closes: the box sits above the cursor, so drawing it inside the window
     * would cut the part that reaches past the window's top edge. */
    @Nullable
    private RowTooltip cropRowTooltip;
    private int cropRowTooltipX;
    private int cropRowTooltipY;
    /** Border gradient of exactly the next tooltip our Color handler colours. */
    @Nullable
    private int[] pendingTooltipBorder;

    /** A crop-list tooltip: text plus the border gradient pair to render with. */
    private record RowTooltip(Component text, int borderStart, int borderEnd) {}

    public GreenhouseControllerScreen(GreenhouseControllerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        imageWidth = BG_WIDTH;
        imageHeight = BG_HEIGHT;
        super.init();

        flapDevices = new FlapTextWidget(0, 0, 8, 0xFFFFFF, "0123456789", false);
        flapVolume = new FlapTextWidget(0, 0, 8, 0xFFFFFF, "0123456789KMG", false);
        // the localized labels flip in with the same ceremony as the digits;
        // recreated on resize exactly like the number counters
        flapDevicesLabel = FlapTextWidget.forLabel(flapLabel("devices"), 0xFFFFFF);
        flapVolumeLabel = FlapTextWidget.forLabel(flapLabel("volume"), 0xFFFFFF);
        flapDevicesLabel.setText(flapLabel("devices"), true);
        flapVolumeLabel.setText(flapLabel("volume"), true);
    }

    private static String flapLabel(String key) {
        return Component.translatable("gui.create_cultivation.greenhouse." + key).append(":").getString();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        flapDevices.tick();
        flapVolume.tick();
        flapDevicesLabel.tick();
        flapVolumeLabel.tick();

        GreenhouseSnapshotPayload snap = menu.getClientSnapshot();
        if (snap != null) {
            flapDevices.setText(String.valueOf(snap.deviceCount()), true);
            flapVolume.setText(formatVolume(snap.volume()), true);
            displayedTemp += (snap.tempC10() / 10f - displayedTemp) * 0.25f;
            displayedHum += (snap.humidity10() / 10f - displayedHum) * 0.25f;
        }
        itemScroll.tickChaser();
    }

    private static String formatVolume(int volume) {
        if (volume >= 1_000_000)
            return (volume / 1_000_000) + "M";
        if (volume >= 10_000)
            return (volume / 1000) + "K";
        return String.valueOf(volume);
    }

    private static String formatValue(float v) {
        return v == Math.floor(v) ? String.valueOf((int) v) : String.format("%.1f", v);
    }

    // --- setpoint dragging --------------------------------------------------

    private int barX(float value, float min, float max) {
        float t = (value - min) / (max - min);
        return leftPos + TEMP_BAR_X + Math.round(Mth.clamp(t, 0f, 1f) * BAR_LENGTH);
    }

    private float xToValue(int mouseX, float min, float max) {
        float t = (mouseX - leftPos - TEMP_BAR_X) / (float) BAR_LENGTH;
        return min + Mth.clamp(t, 0f, 1f) * (max - min);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isOverConfirm(mouseX, mouseY)) {
                confirmAndClose();
                return true;
            }
            if (isOverAuto(TEMP_BAR_Y, mouseX, mouseY)) {
                clickAutoToggle(TEMP_BAR_Y);
                return true;
            }
            if (isOverAuto(HUM_BAR_Y, mouseX, mouseY)) {
                clickAutoToggle(HUM_BAR_Y);
                return true;
            }
            GreenhouseSnapshotPayload snap = menu.getClientSnapshot();
            if (nearBar(mouseY, TEMP_BAR_Y)) {
                // auto axis: the controller owns this setpoint, no manual drag
                // (staged toggle counts: flipping on without saving already locks)
                if (snap == null || (autoModeStaged(snap) & 1) == 0) {
                    draggingTemp = true;
                    applyDragTemp(mouseX);
                }
                return true;
            }
            if (nearBar(mouseY, HUM_BAR_Y)) {
                if (snap == null || (autoModeStaged(snap) & 2) == 0) {
                    draggingHum = true;
                    applyDragHum(mouseX);
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean nearBar(double mouseY, int barY) {
        return mouseY >= topPos + barY - 10 && mouseY <= topPos + barY + 12
                && mouseXOverBar(mouseXDouble());
    }

    private double mouseXDouble() {
        return minecraft.mouseHandler.xpos() / minecraft.getWindow().getGuiScale();
    }

    private boolean mouseXOverBar(double mouseX) {
        return mouseX >= leftPos + TEMP_BAR_X - 4 && mouseX <= leftPos + TEMP_BAR_X + BAR_LENGTH + 4;
    }

    private boolean isOverConfirm(double mouseX, double mouseY) {
        return mouseX >= leftPos + CONFIRM_X && mouseX < leftPos + CONFIRM_X + CONFIRM_W
                && mouseY >= topPos + CONFIRM_Y && mouseY < topPos + CONFIRM_Y + CONFIRM_H;
    }

    private boolean isOverAuto(int barY, double mouseX, double mouseY) {
        int y = topPos + barY - 8;
        return mouseX >= leftPos + AUTO_BTN_X && mouseX < leftPos + AUTO_BTN_X + AUTO_BTN_W
                && mouseY >= y && mouseY < y + AUTO_BTN_H;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (draggingTemp) {
            applyDragTemp(mouseX);
            return true;
        }
        if (draggingHum) {
            applyDragHum(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingTemp = false;
        draggingHum = false;
        // the next drag starts a fresh detent run (engagement click on press)
        lastClickX = Double.NaN;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void applyDragTemp(double mouseX) {
        GreenhouseSnapshotPayload snap = menu.getClientSnapshot();
        if (snap == null)
            return;
        float v = Math.round(xToValue((int) mouseX, ClimateUnits.TEMP_C_MIN, ClimateUnits.TEMP_C_MAX) * 10) / 10f;
        // staged locally; the hook button submits both setpoints at once
        stagedTempC10 = Math.round(v * 10);
        playDetentClick(mouseX);
    }

    /**
     * Gear-detent click while dragging a setpoint: once when the drag engages
     * (NaN sentinel), then once per {@link #DETENT_STEP_PX} of travel, with a
     * slight pitch wobble so rapid ratcheting does not sound machine-gunned.
     */
    private void playDetentClick(double mouseX) {
        if (Double.isNaN(lastClickX) || Math.abs(mouseX - lastClickX) >= DETENT_STEP_PX) {
            lastClickX = mouseX;
            float pitch = 0.9f + RandomSource.create().nextFloat() * 0.2f;
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(CCSounds.UI_SETPOINT_CLICK.get(), pitch));
        }
    }

    private void applyDragHum(double mouseX) {
        GreenhouseSnapshotPayload snap = menu.getClientSnapshot();
        if (snap == null)
            return;
        float v = Math.round(xToValue((int) mouseX, ClimateUnits.HUMIDITY_MIN, ClimateUnits.HUMIDITY_MAX) * 10) / 10f;
        stagedHum10 = Math.round(v * 10);
        playDetentClick(mouseX);
    }

    private void confirmAndClose() {
        GreenhouseSnapshotPayload snap = menu.getClientSnapshot();
        if (snap == null) {
            onClose();
            return;
        }
        PacketDistributor.sendToServer(new GreenhouseSetpointsPayload(
                menu.getBlockPos(),
                stagedTempC10 != null ? stagedTempC10 : snap.setTempC10(),
                stagedHum10 != null ? stagedHum10 : snap.setHum10(),
                stagedAutoMode != null ? stagedAutoMode : snap.autoMode()));
        onClose();
    }

    /** Staged auto-mode bits (like the staged setpoints: applied on save only). */
    @Nullable
    private Integer stagedAutoMode;

    /** Staged auto bits for the GUI: snapshot value until the player toggles. */
    private int autoModeStaged(GreenhouseSnapshotPayload snap) {
        return stagedAutoMode != null ? stagedAutoMode : snap.autoMode();
    }

    /**
     * Draws the two auto-mode toggles from Create's widget sprites. The
     * toggles only stage locally - like the setpoint drags, the change is
     * applied server-side when the hook button confirms.
     */
    private void renderAutoButtons(GuiGraphics gui, Font font, int mouseX, int mouseY) {
        GreenhouseSnapshotPayload snap = menu.getClientSnapshot();
        if (snap == null)
            return;
        int mode = autoModeStaged(snap);
        for (int barY : new int[] {TEMP_BAR_Y, HUM_BAR_Y}) {
            boolean on = (barY == TEMP_BAR_Y ? mode & 1 : mode & 2) != 0;
            int x = leftPos + AUTO_BTN_X;
            int y = topPos + barY - 8;
            // Create's own enum handles binding its widgets sheet (the
            // sprites do not exist in this mod's texture folder)
            AllGuiTextures sprite = on ? AllGuiTextures.TRAINMAP_TOGGLE_ON : AllGuiTextures.TRAINMAP_TOGGLE_OFF;
            sprite.render(gui, x, y);
            if (isOverAuto(barY, mouseX, mouseY))
                gui.fill(x, y, x + AUTO_BTN_W, y + AUTO_BTN_H, 0x50FFDD55);
        }
    }

    /** Click on one auto toggle: stage the flip (applied on save, like a drag). */
    private void clickAutoToggle(int barY) {
        GreenhouseSnapshotPayload snap = menu.getClientSnapshot();
        if (snap == null)
            return;
        int mode = autoModeStaged(snap);
        boolean willBeOn = (barY == TEMP_BAR_Y ? mode & 1 : mode & 2) == 0;
        // Create's widget click: wooden button on/off sounds
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                willBeOn ? SoundEvents.WOODEN_BUTTON_CLICK_ON : SoundEvents.WOODEN_BUTTON_CLICK_OFF, 1.0f));
        stagedAutoMode = barY == TEMP_BAR_Y ? mode ^ 1 : mode ^ 2;
        if (willBeOn) {
            // switching an axis to auto discards any staged manual value so
            // the arrow snaps to the solver's optimum once saved
            if (barY == TEMP_BAR_Y)
                stagedTempC10 = null;
            else
                stagedHum10 = null;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double xScroll, double yScroll) {
        GreenhouseSnapshotPayload snap = menu.getClientSnapshot();
        int rows = snap != null ? snap.crops().length : 0;
        int max = getMaxScroll(rows);
        int direction = (int) (Math.ceil(Math.abs(yScroll)) * -Math.signum(yScroll));
        float newTarget = Mth.clamp(itemScroll.getChaseTarget() + direction, 0, max);
        itemScroll.chase(newTarget, 0.5, Chaser.EXP);
        return true;
    }

    /** How many rows can scroll out of view above the window. */
    private int getMaxScroll(int rows) {
        return Math.max(0, rows - VIEW_ROWS);
    }

    // --- rendering ----------------------------------------------------------

    @Override
    protected void renderBg(GuiGraphics gui, float partialTicks, int mouseX, int mouseY) {
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
        // vanilla draws the dark backdrop (and nothing else - no slots, blank
        // labels); our panel goes on top of it, undarkened
        super.render(gui, mouseX, mouseY, partialTicks);
        renderWindow(gui, mouseX, mouseY, partialTicks);
        // drag tooltips render after every layer so no opaque row art or bar
        // can cover the text
        renderGaugeTooltips(gui, mouseX, mouseY);
        renderTooltip(gui, mouseX, mouseY);
    }

    /** Live drag readouts, drawn last so nothing can paint over them. */
    private void renderGaugeTooltips(GuiGraphics gui, int mouseX, int mouseY) {
        if (draggingTemp) {
            float v = Math.round(xToValue(mouseX, ClimateUnits.TEMP_C_MIN, ClimateUnits.TEMP_C_MAX) * 10) / 10f;
            gui.renderTooltip(minecraft.font, Component.literal(formatValue(v) + "°C"), mouseX, mouseY);
        } else if (draggingHum) {
            float v = Math.round(xToValue(mouseX, ClimateUnits.HUMIDITY_MIN, ClimateUnits.HUMIDITY_MAX) * 10) / 10f;
            gui.renderTooltip(minecraft.font, Component.literal(formatValue(v) + "%RH"), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        // no title and no inventory label: the panel art carries everything
    }

    private void renderWindow(GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
        // panel art only: rows 221..255 of the texture hold the detached
        // crop-row template strip (y231..248) used as a clone source below -
        // it must not appear on the live panel
        gui.blit(BACKGROUND, leftPos, topPos, 0, 0, BG_WIDTH, PANEL_DRAW_HEIGHT, BG_WIDTH, BG_HEIGHT);
        // StockKeeper-style hover feedback: tint the hook button region
        if (isOverConfirm(mouseX, mouseY))
            gui.fill(leftPos + CONFIRM_X, topPos + CONFIRM_Y,
                    leftPos + CONFIRM_X + CONFIRM_W, topPos + CONFIRM_Y + CONFIRM_H, 0x80FFDD55);
        Font font = minecraft.font;
        GreenhouseSnapshotPayload snap = menu.getClientSnapshot();

        // layer 1: flap counters on one shared line - the digits flow right
        // of the measured label width, so translated labels (EN/中文) never
        // overlap the numbers; the volume group shifts right to keep clear
        // of the wider localized devices label
        int flapY = topPos + FLAP_TY0 + 5;
        int labelY = topPos + FLAP_TY0 + 4;
        flapDevicesLabel.renderAt(gui, font, leftPos + 70, labelY);
        int volumeX = leftPos + 152;
        flapVolumeLabel.renderAt(gui, font, volumeX, labelY);
        flapDevices.renderAt(gui, font, leftPos + 70 + flapDevicesLabel.width(font) + 4, flapY);
        flapVolume.renderAt(gui, font, volumeX + flapVolumeLabel.width(font) + 4, flapY);

        // layer 2: gauges
        float curTemp = snap != null ? snap.tempC10() / 10f : displayedTemp;
        float curHum = snap != null ? snap.humidity10() / 10f : displayedHum;
        // the arrows must show staged values while dragging, then the
        // snapshot (server-confirmed) once the hook button committed
        float setTemp = stagedTempC10 != null ? stagedTempC10 / 10f
                : snap != null ? snap.setTempC10() / 10f : 20f;
        float setHum = stagedHum10 != null ? stagedHum10 / 10f
                : snap != null ? snap.setHum10() / 10f : 50f;

        drawGauge(gui, font, TEMP_BAR_Y, TEMP_DISC_CY, curTemp, setTemp,
                ClimateUnits.TEMP_C_MIN, ClimateUnits.TEMP_C_MAX,
                W_DRAG_U, W_DRAG_V, W_CUR_U, W_CUR_V,
                snap != null && snap.tempShort());
        drawGauge(gui, font, HUM_BAR_Y, HUM_DISC_CY, curHum, setHum,
                ClimateUnits.HUMIDITY_MIN, ClimateUnits.HUMIDITY_MAX,
                W_DRAG_U, W_DRAG_HUM_V, W_CUR_U, W_CUR_HUM_V,
                snap != null && snap.humShort());

        // auto-setpoint buttons right of the two gauges
        renderAutoButtons(gui, font, mouseX, mouseY);

        // layer 3: crop rows
        renderCropRows(gui, font, snap, mouseX, mouseY, partialTicks);
    }

    private void drawGauge(GuiGraphics gui, Font font, int barY, int discCY,
            float current, float set, float min, float max,
            int dragU, int dragV, int curU, int curV, boolean shortOfDevices) {
        int dragX = barX(set, min, max) - W_DRAG_W / 2;
        // setpoint arrow: sits on the bar;
        // orange tint warns when the connected devices cannot reach it
        if (shortOfDevices)
            gui.fill(dragX - 1, topPos + barY + 2, dragX + W_DRAG_W + 1, topPos + barY + 9, 0x80D05D1F);
        gui.blit(WIDGETS, dragX, topPos + barY + 3, dragU, dragV, W_DRAG_W, W_DRAG_H, 128, 128);
        int curX = barX(current, min, max) - W_CUR_W / 2;
        // current-climate arrow moved 8px up, just above the bar
        gui.blit(WIDGETS, curX, topPos + barY - 2, curU, curV, W_CUR_W, W_CUR_H, 128, 128);

        // the round slot reads the LIVE greenhouse climate (not the setpoint)
        gui.drawCenteredString(font, formatValue(current), leftPos + DISC_CX, topPos + discCY, 0xFFFFFF);
    }

    private void renderCropRows(GuiGraphics gui, Font font,
            @Nullable GreenhouseSnapshotPayload snap, int mouseX, int mouseY, float partialTicks) {
        cropRowTooltip = null;
        if (snap == null)
            return;
        GreenhouseSnapshotPayload.CropRow[] rows = snap.crops();

        // StockKeeper-style list: scissor-clipped window, all rows rendered
        // at a continuous fractional offset chased by itemScroll; the window
        // bottom is the display limit (rows crop there)
        int winX = leftPos + ROW_TX;
        int winY = topPos + LIST_Y;
        int winH = LIST_CULL_Y - LIST_Y;
        gui.enableScissor(winX, winY, winX + ROW_W, winY + winH);

        float scroll = itemScroll.getValue(partialTicks);
        for (int index = 0; index < rows.length; index++) {
            float rowYf = winY + (index - scroll) * ROW_STRIDE;
            int rowY = (int) rowYf;
            if (rowY > winY + winH || rowY + ROW_H < winY)
                continue;
            GreenhouseSnapshotPayload.CropRow row = rows[index];

            // row background cloned from the template
            gui.blit(BACKGROUND, winX, rowY, ROW_TX, ROW_TY, ROW_W, ROW_H,
                    BG_WIDTH, BG_HEIGHT);

            // slot frame recolours with the crop's live climate state:
            // #f4c86a when a dimension left the optimal range but is still
            // inside survival, red #c30e0e when it left survival (stalled);
            // default texture frame while everything is optimal
            int frame = slotFrameColor(row, snap.tempC10(), snap.humidity10());
            if (frame != 0) {
                int sx = leftPos + SLOT_TX;
                gui.fill(sx, rowY, sx + 18, rowY + 1, frame);
                gui.fill(sx, rowY + 17, sx + 18, rowY + 18, frame);
                gui.fill(sx, rowY, sx + 1, rowY + 18, frame);
                gui.fill(sx + 17, rowY, sx + 18, rowY + 18, frame);
            }

            // item + bottom-right count. Blocks without an item form of their
            // own (rice, grapes, trellises - the planting item is separate)
            // fall back to the display_icon data map so the row is not blank
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(row.blockId()));
            Item item = block.asItem();
            if (item == Items.AIR) {
                CCDataMaps.CropDisplay display = block.builtInRegistryHolder().getData(CCDataMaps.CROP_DISPLAY_ICON);
                if (display != null) {
                    item = display.item();
                }
            }
            ItemStack stack = item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
            stack.setCount(Math.min(99, Math.max(1, row.count())));
            gui.renderItem(stack, leftPos + SLOT_TX + 1, rowY + 1);
            gui.renderItemDecorations(font, stack, leftPos + SLOT_TX + 1, rowY + 1, null);

            // mini bars + range arrows
            drawMiniBar(gui, leftPos + MINI_TX, rowY + (MINI_TY - ROW_TY),
                    row.tempOptMin(), row.tempOptMax(), row.tempSurMin(), row.tempSurMax(),
                    ClimateUnits.TEMP_C_MIN, ClimateUnits.TEMP_C_MAX);
            drawMiniBar(gui, leftPos + MINI_TX, rowY + (MINI_TY2 - ROW_TY),
                    row.humOptMin(), row.humOptMax(), row.humSurMin(), row.humSurMax(),
                    ClimateUnits.HUMIDITY_MIN, ClimateUnits.HUMIDITY_MAX);

            // hover tooltip - only while the cursor is inside the visible
            // scissor window: rows hidden above/below the list window must not
            // show a tooltip through the panel art. Range tooltips on the mini
            // bar spans take precedence over the crop name. Rendering happens
            // after the scissor closes (see below).
            if (mouseX >= winX && mouseX < winX + ROW_W
                    && mouseY >= winY && mouseY < winY + winH
                    && mouseY >= rowY && mouseY < rowY + ROW_H) {
                RowTooltip tip = miniBarTooltip(row, mouseX, mouseY, rowY);
                if (tip == null) {
                    // crop name, border hue matching the row's frame colour
                    tip = new RowTooltip(BuiltInRegistries.BLOCK.get(
                            ResourceLocation.parse(row.blockId())).getName(),
                            frame == FRAME_STALL ? BORDER_STALL[0]
                                    : frame == FRAME_SURV ? BORDER_SURVIVAL[0] : BORDER_OPTIMAL[0],
                            frame == FRAME_STALL ? BORDER_STALL[1]
                                    : frame == FRAME_SURV ? BORDER_SURVIVAL[1] : BORDER_OPTIMAL[1]);
                }
                cropRowTooltip = tip;
                cropRowTooltipX = mouseX;
                cropRowTooltipY = mouseY;
            }
        }
        gui.disableScissor();

        // render the row tooltip outside the scissor window: its box sits
        // above the cursor, so drawing it inside the window would clip the
        // part reaching past the window's top edge (the panel art there)
        if (cropRowTooltip != null) {
            pendingTooltipBorder = new int[] {cropRowTooltip.borderStart(), cropRowTooltip.borderEnd()};
            gui.renderTooltip(font, cropRowTooltip.text(), cropRowTooltipX, cropRowTooltipY);
            pendingTooltipBorder = null;
        }

        // StockKeeper style: the controller block rendered right of the hook
        // button. GuiGameElement's base unit is 1/16px, so a visible block
        // needs scale ~20-30 (Create's own categories use 20)
        gui.pose().pushPose();
        gui.pose().translate(leftPos + CONFIRM_X + CONFIRM_W + 30, topPos + CONFIRM_Y + 22, 100);
        gui.pose().mulPose(Axis.XP.rotationDegrees(-15.5f));
        gui.pose().mulPose(Axis.YP.rotationDegrees(22.5f));
        float modelScale = 30f;
        float hoverGrow = isOverConfirm(mouseX, mouseY) ? 1.08f : 1.0f;
        GuiGameElement.of(CCBlocks.GREENHOUSE_CONTROLLER.getDefaultState())
                .scale(modelScale * hoverGrow)
                .render(gui);
        gui.pose().popPose();
    }

    /**
     * Tooltip for the coloured range spans on a row's mini bars: hovering the
     * green optimal span reports the optimal bounds, the orange survival span
     * the survival bounds. The optimal canvas paints over the survival one, so
     * inside an overlap the optimal range wins - same as the visuals. Null
     * when the cursor is on neither span.
     */
    @Nullable
    private RowTooltip miniBarTooltip(GreenhouseSnapshotPayload.CropRow row,
            int mouseX, int mouseY, int rowY) {
        RowTooltip tempTip = barSpanTooltip(row, mouseX, mouseY, rowY, true);
        return tempTip != null ? tempTip : barSpanTooltip(row, mouseX, mouseY, rowY, false);
    }

    /** One bar's span hit-test; {@code temperature} picks the top (temp) or bottom (humidity) bar. */
    @Nullable
    private RowTooltip barSpanTooltip(GreenhouseSnapshotPayload.CropRow row,
            int mouseX, int mouseY, int rowY, boolean temperature) {
        int barY = rowY + (temperature ? MINI_TY : MINI_TY2) - ROW_TY;
        // forgiving 5px hit zone around the 3px canvas strip
        if (mouseY < barY - 2 || mouseY >= barY + 3)
            return null;
        int optMin10 = temperature ? row.tempOptMin() : row.humOptMin();
        int optMax10 = temperature ? row.tempOptMax() : row.humOptMax();
        int surMin10 = temperature ? row.tempSurMin() : row.humSurMin();
        int surMax10 = temperature ? row.tempSurMax() : row.humSurMax();
        boolean hasOpt = optMin10 != Integer.MIN_VALUE && optMax10 != Integer.MIN_VALUE;
        boolean hasSur = surMin10 != Integer.MIN_VALUE && surMax10 != Integer.MIN_VALUE;
        if (!hasOpt && !hasSur)
            return null;
        float scaleMin = temperature ? ClimateUnits.TEMP_C_MIN : ClimateUnits.HUMIDITY_MIN;
        float scaleMax = temperature ? ClimateUnits.TEMP_C_MAX : ClimateUnits.HUMIDITY_MAX;
        float span = scaleMax - scaleMin;
        int barX = leftPos + MINI_TX;

        // spans are hit-tested against the drawn (scale-clamped) extent;
        // the tooltip reports the true bounds even when they exceed the scale
        if (hasOpt) {
            int a = Mth.clamp(barX + Math.round((optMin10 / 10f - scaleMin) / span * MINI_LENGTH), barX, barX + MINI_LENGTH);
            int b = Mth.clamp(barX + Math.round((optMax10 / 10f - scaleMin) / span * MINI_LENGTH), barX, barX + MINI_LENGTH);
            if (mouseX >= a && mouseX < b)
                return new RowTooltip(Component.translatable(
                        temperature ? "gui.create_cultivation.greenhouse.range_optimal_temp"
                                : "gui.create_cultivation.greenhouse.range_optimal_humidity",
                        formatValue(optMin10 / 10f), formatValue(optMax10 / 10f)),
                        BORDER_OPTIMAL[0], BORDER_OPTIMAL[1]);
        }
        if (hasSur) {
            int a = Mth.clamp(barX + Math.round((surMin10 / 10f - scaleMin) / span * MINI_LENGTH), barX, barX + MINI_LENGTH);
            int b = Mth.clamp(barX + Math.round((surMax10 / 10f - scaleMin) / span * MINI_LENGTH), barX, barX + MINI_LENGTH);
            if (mouseX >= a && mouseX < b)
                return new RowTooltip(Component.translatable(
                        temperature ? "gui.create_cultivation.greenhouse.range_survival_temp"
                                : "gui.create_cultivation.greenhouse.range_survival_humidity",
                        formatValue(surMin10 / 10f), formatValue(surMax10 / 10f)),
                        BORDER_SURVIVAL[0], BORDER_SURVIVAL[1]);
        }
        return null;
    }

    /**
     * Colours the tooltip border while one of this screen's crop-list
     * tooltips renders: NeoForge fires the Color event synchronously inside
     * {@code GuiGraphics#renderTooltip}, and {@link #pendingTooltipBorder}
     * marks exactly that call. Other tooltips on this screen (the gauge drag
     * readouts) keep the vanilla border.
     */
    public static void onRenderTooltipColor(RenderTooltipEvent.Color event) {
        if (Minecraft.getInstance().screen instanceof GreenhouseControllerScreen screen
                && screen.pendingTooltipBorder != null) {
            event.setBorderStart(screen.pendingTooltipBorder[0]);
            event.setBorderEnd(screen.pendingTooltipBorder[1]);
            screen.pendingTooltipBorder = null;
        }
    }

    /**
     * Frame colour for one crop row's item slot: red when the live climate
     * left the survival range in either dimension (stalled), #f4c86a when it
     * left the optimal range but still survives, 0 for the default frame.
     */
    private static int slotFrameColor(GreenhouseSnapshotPayload.CropRow row, int tempC10, int hum10) {
        float t = tempC10 / 10f;
        float h = hum10 / 10f;
        boolean tempOk = inRange(row.tempSurMin(), row.tempSurMax(), t);
        boolean humOk = inRange(row.humSurMin(), row.humSurMax(), h);
        if (!tempOk || !humOk)
            return FRAME_STALL;
        boolean tempOpt = inRange(row.tempOptMin(), row.tempOptMax(), t);
        boolean humOpt = inRange(row.humOptMin(), row.humOptMax(), h);
        if (!tempOpt || !humOpt)
            return FRAME_SURV;
        return 0;
    }

    /** True if {@code v} lies inside [min, max] (MIN_VALUE bounds = no range). */
    private static boolean inRange(int min10, int max10, float v) {
        if (min10 == Integer.MIN_VALUE || max10 == Integer.MIN_VALUE)
            return true; // no configured range: no complaint
        return v >= min10 / 10f && v <= max10 / 10f;
    }

    /**
     * Four range arrows on a mini bar: green optimal bounds (30/50 in the
     * example), orange survival bounds (25/60). Survival arrows sit at the
     * outer limits, optimal arrows at the optimal interval; the max arrows
     * are horizontally mirrored so they point inward.
     */
    private void drawMiniBar(GuiGraphics gui, int barX, int barY,
            int optMin10, int optMax10, int surMin10, int surMax10,
            float scaleMin, float scaleMax) {
        float span = scaleMax - scaleMin;
        boolean hasOpt = optMin10 != Integer.MIN_VALUE && optMax10 != Integer.MIN_VALUE;
        boolean hasSur = surMin10 != Integer.MIN_VALUE && surMax10 != Integer.MIN_VALUE;
        if (!hasOpt && !hasSur)
            return;
        int xOptMin = hasOpt ? barX + Math.round((optMin10 / 10f - scaleMin) / span * MINI_LENGTH) : 0;
        int xOptMax = hasOpt ? barX + Math.round((optMax10 / 10f - scaleMin) / span * MINI_LENGTH) : 0;
        int xSurMin = hasSur ? barX + Math.round((surMin10 / 10f - scaleMin) / span * MINI_LENGTH) : 0;
        int xSurMax = hasSur ? barX + Math.round((surMax10 / 10f - scaleMin) / span * MINI_LENGTH) : 0;

        // survival bounds first (behind), then optimal bounds (in front)
        if (hasSur) {
            // 50% orange canvas over the survival span, arrows on top
            int a = Mth.clamp(xSurMin, barX, barX + MINI_LENGTH);
            int b = Mth.clamp(xSurMax, barX, barX + MINI_LENGTH);
            if (b > a)
                gui.fill(a, barY - 1, b, barY + 2, CANVAS_SURV);
            gui.blit(WIDGETS, xSurMin - 1, barY - 2, W_SURV_U, W_SURV_V, W_OPT_W, W_OPT_H, 128, 128);
            gui.blit(WIDGETS, xSurMax - W_OPT_W + 1, barY - 2, W_SURV_MAX_U, W_SURV_V, W_OPT_W, W_OPT_H, 128, 128);
        }
        if (hasOpt) {
            // 50% green canvas over the optimal span (paints over orange
            // where the two intervals overlap), arrows on top
            int a = Mth.clamp(xOptMin, barX, barX + MINI_LENGTH);
            int b = Mth.clamp(xOptMax, barX, barX + MINI_LENGTH);
            if (b > a)
                gui.fill(a, barY - 1, b, barY + 2, CANVAS_OPT);
            gui.blit(WIDGETS, xOptMin - 1, barY - 2, W_OPT_U, W_OPT_V, W_OPT_W, W_OPT_H, 128, 128);
            gui.blit(WIDGETS, xOptMax - W_OPT_W + 1, barY - 2, W_OPT_MAX_U, W_OPT_V, W_OPT_W, W_OPT_H, 128, 128);
        }
    }
}
