package euphy.upo.create_cultivation.infrastructure.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import euphy.upo.create_cultivation.CreateCultivationCraft;

/**
 * Client -> server: the player confirmed the greenhouse controller GUI with
 * the hook button. Carries the staged manual setpoints plus the staged
 * auto-mode bits (bit 1 = temperature, bit 2 = humidity); an auto axis'
 * setpoint is ignored server-side, the solver owns it.
 *
 * @param pos        controller position (which machine this commit belongs to)
 * @param setTempC10 staged target temperature, deg C * 10
 * @param setHum10   staged target humidity, %RH * 10
 * @param autoMode   staged auto-mode bitmask (0..3)
 */
public record GreenhouseSetpointsPayload(BlockPos pos, int setTempC10, int setHum10, int autoMode)
        implements CustomPacketPayload {

    public static final Type<GreenhouseSetpointsPayload> TYPE = new Type<>(
            CreateCultivationCraft.asResource("greenhouse_setpoints"));

    public static final StreamCodec<ByteBuf, GreenhouseSetpointsPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, GreenhouseSetpointsPayload::pos,
            ByteBufCodecs.VAR_INT, GreenhouseSetpointsPayload::setTempC10,
            ByteBufCodecs.VAR_INT, GreenhouseSetpointsPayload::setHum10,
            ByteBufCodecs.VAR_INT, GreenhouseSetpointsPayload::autoMode,
            GreenhouseSetpointsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
