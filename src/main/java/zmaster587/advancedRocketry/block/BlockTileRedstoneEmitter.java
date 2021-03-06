package zmaster587.advancedRocketry.block;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockTileRedstoneEmitter extends BlockTile {

	public BlockTileRedstoneEmitter(Class<? extends TileEntity> tileClass,
			int guiId) {
		super(tileClass, guiId);
	}
	
	@Override
	public int isProvidingWeakPower(IBlockAccess world,
			int x, int y, int z, int direction) {
		return (world.getBlockMetadata(x, y, z) & 8) != 0 ? 15 : 0;
	}
	
	@Override
	public boolean canProvidePower() {
		return true;
	}
	
	public void setRedstoneState(World world, int x, int y, int z, boolean state) {
		if(state && (world.getBlockMetadata(x, y, z) & 8) == 0) {
			world.setBlockMetadataWithNotify(x, y, z, world.getBlockMetadata(x, y, z) | 8, 3);
		}
		else if(!state && (world.getBlockMetadata(x, y, z) & 8) != 0) {
			world.setBlockMetadataWithNotify(x, y, z, world.getBlockMetadata(x, y, z) & 7, 3);
		}
	}
}
