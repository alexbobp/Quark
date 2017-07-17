package vazkii.quark.automation;

import com.mojang.authlib.GameProfile;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;

import java.util.UUID;

public class FakerPlayer extends FakePlayer {
    private static final GameProfile QUARK_PROFILE = new GameProfile(UUID.randomUUID(), "Quark Mod");

    public FakerPlayer(World world) {
        super((WorldServer)world, QUARK_PROFILE);

    }

    ItemStack held;
    public FakerPlayer(WorldServer world, BlockPos loc, EnumFacing facing, ItemStack held) {
        super(world, QUARK_PROFILE);

        setPosition(loc.getX()+.5, loc.getY()+.5, loc.getZ()+.5);
        rotationYawHead = rand.nextFloat() * 360;
        rotationYaw = rotationYawHead;
        rotationPitch = facing.getHorizontalAngle();
        this.held = held;
    }

    @Override public ItemStack getHeldItemMainhand() {return held;}

    @Override public ItemStack getHeldItem(EnumHand hand) {
        if (hand == EnumHand.MAIN_HAND)
            return getHeldItemMainhand();
        return super.getHeldItem(hand);
    }

    @Override public boolean isSneaking() {return true;}

}
