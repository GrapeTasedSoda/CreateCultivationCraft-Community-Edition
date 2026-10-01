package euphy.upo.create_cultivation.infrastructure.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import euphy.upo.create_cultivation.CreateCultivationCraft;

/**
 * Server -> client snapshot of everything the greenhouse controller GUI
 * paints. Sent when the menu opens and whenever the scan or climate changes.
 *
 * @param pos          controller position (which machine this snapshot belongs to)
 * @param valid        whether the enclosure scan succeeded
 * @param volume       enclosure volume (cells)
 * @param deviceCount  connected greenhouse devices (all types)
 * @param cropCount    distinct crop types inside the enclosure
 * @param tempC10      live greenhouse temperature, deg C * 10
 * @param humidity10   live greenhouse humidity, %RH * 10
 * @param setTempC10   player-set target temperature, deg C * 10
 * @param setHum10     player-set target humidity, %RH * 10
 * @param tempShort    true when no amount of connected devices can reach the set temperature
 * @param humShort     same for humidity
 * @param reachTempC10 reachable temperature given the connected capacity, deg C * 10
 * @param reachHum10   reachable humidity given the connected capacity, %RH * 10
 * @param acCount      connected air conditioners
 * @param humCount     connected humidifiers
 * @param dehumCount   connected dehumidifiers
 * @param autoMode     auto-setpoint switches (bit 1 = temperature, 2 = humidity)
 * @param crops        one row per distinct crop type (see {@link CropRow})
 * @param boosts       per-crop climate boosts for the Jade client tooltip
 */
public record GreenhouseSnapshotPayload(BlockPos pos, boolean valid, int volume, int deviceCount, int cropCount,
        int tempC10, int humidity10, int setTempC10, int setHum10,
        boolean tempShort, boolean humShort, int reachTempC10, int reachHum10,
        int acCount, int humCount, int dehumCount, int autoMode,
        CropRow[] crops, CropBoost[] boosts) implements CustomPacketPayload {

    public static final Type<GreenhouseSnapshotPayload> TYPE = new Type<>(
            CreateCultivationCraft.asResource("greenhouse_snapshot"));
    /** One scroll-list row: a crop type, its climate ranges and plant count. */
    public record CropRow(String blockId, int tempOptMin, int tempOptMax, int tempSurMin, int tempSurMax,
            int humOptMin, int humOptMax, int humSurMin, int humSurMax, int count) {
    }

    /**
     * One soil crop's climate boost, piggybacking on the controller's periodic
     * broadcast for the client-side Jade tooltip.
     *
     * @param packedPos the crop block position packed as a long
     * @param growth    growth multiplier x1000 (int fixed point)
     * @param yield     yield multiplier x1000 (int fixed point)
     * @param stalled   whether the crop is outside its survival ranges
     */
    public record CropBoost(long packedPos, int growth, int yield, boolean stalled) {
    }

    public static final StreamCodec<FriendlyByteBuf, GreenhouseSnapshotPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public GreenhouseSnapshotPayload decode(FriendlyByteBuf buf) {
            // field order MUST mirror encode(): pos first
            BlockPos pos = buf.readBlockPos();
            boolean valid = buf.readBoolean();
            int volume = buf.readInt();
            int deviceCount = buf.readInt();
            int cropCount = buf.readInt();
            int tempC10 = buf.readInt();
            int humidity10 = buf.readInt();
            int setTempC10 = buf.readInt();
            int setHum10 = buf.readInt();
            boolean tempShort = buf.readBoolean();
            boolean humShort = buf.readBoolean();
            int reachTempC10 = buf.readInt();
            int reachHum10 = buf.readInt();
            int acCount = buf.readInt();
            int humCount = buf.readInt();
            int dehumCount = buf.readInt();
            int autoMode = buf.readInt();
            int rows = buf.readInt();
            CropRow[] crops = new CropRow[rows];
            for (int i = 0; i < rows; i++) {
                crops[i] = new CropRow(buf.readUtf(256), buf.readInt(), buf.readInt(), buf.readInt(),
                        buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
            }
            int boostCount = buf.readVarInt();
            CropBoost[] boosts = new CropBoost[boostCount];
            for (int i = 0; i < boostCount; i++) {
                boosts[i] = new CropBoost(buf.readLong(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean());
            }
            return new GreenhouseSnapshotPayload(pos, valid, volume, deviceCount, cropCount,
                    tempC10, humidity10, setTempC10, setHum10, tempShort, humShort,
                    reachTempC10, reachHum10, acCount, humCount, dehumCount, autoMode, crops, boosts);
        }

        @Override
        public void encode(FriendlyByteBuf buf, GreenhouseSnapshotPayload p) {
            buf.writeBlockPos(p.pos);
            buf.writeBoolean(p.valid);
            buf.writeInt(p.volume);
            buf.writeInt(p.deviceCount);
            buf.writeInt(p.cropCount);
            buf.writeInt(p.tempC10);
            buf.writeInt(p.humidity10);
            buf.writeInt(p.setTempC10);
            buf.writeInt(p.setHum10);
            buf.writeBoolean(p.tempShort);
            buf.writeBoolean(p.humShort);
            buf.writeInt(p.reachTempC10);
            buf.writeInt(p.reachHum10);
            buf.writeInt(p.acCount);
            buf.writeInt(p.humCount);
            buf.writeInt(p.dehumCount);
            buf.writeInt(p.autoMode);
            buf.writeInt(p.crops.length);
            for (CropRow row : p.crops) {
                buf.writeUtf(row.blockId(), 256);
                buf.writeInt(row.tempOptMin());
                buf.writeInt(row.tempOptMax());
                buf.writeInt(row.tempSurMin());
                buf.writeInt(row.tempSurMax());
                buf.writeInt(row.humOptMin());
                buf.writeInt(row.humOptMax());
                buf.writeInt(row.humSurMin());
                buf.writeInt(row.humSurMax());
                buf.writeInt(row.count());
            }
            buf.writeVarInt(p.boosts.length);
            for (CropBoost b : p.boosts) {
                buf.writeLong(b.packedPos());
                buf.writeVarInt(b.growth());
                buf.writeVarInt(b.yield());
                buf.writeBoolean(b.stalled());
            }
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
