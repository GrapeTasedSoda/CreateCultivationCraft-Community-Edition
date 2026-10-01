package euphy.upo.create_cultivation.content.greenhouse;

import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlock;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlockEntity;

import euphy.upo.create_cultivation.registry.CCBlockEntities;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Greenhouse sliding door: functionally identical to Create's framed glass
 * door (sliding double-height door with contraption support), only with our
 * own textures. Subclassing is required because {@link SlidingDoorBlock}
 * hard-codes Create's own block entity type; ours must point at the mod's
 * registered type so the door animates on contraptions and in-world alike.
 */
public class GreenhouseSlidingDoorBlock extends SlidingDoorBlock {

    public GreenhouseSlidingDoorBlock(Properties properties) {
        super(properties, GLASS_SET_TYPE.get(), false);
    }

    @Override
    public Class<SlidingDoorBlockEntity> getBlockEntityClass() {
        return SlidingDoorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SlidingDoorBlockEntity> getBlockEntityType() {
        return CCBlockEntities.GREENHOUSE_SLIDING_DOOR.get();
    }
}
