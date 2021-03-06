package zmaster587.advancedRocketry.inventory.modules;

import java.util.List;

import zmaster587.advancedRocketry.inventory.GuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

public interface IModularInventory {
	
	/**
	 * @return a list of modules to add to the inventory
	 */
	public List<ModuleBase> getModules(int id, EntityPlayer player);
	
	public String getModularInventoryName();
	
	public boolean canInteractWithContainer(EntityPlayer entity);
	
}
