package zmaster587.advancedRocketry.event;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;
import net.minecraftforge.event.entity.player.PlayerOpenContainerEvent;
import net.minecraftforge.event.world.WorldEvent;
import zmaster587.advancedRocketry.api.AdvancedRocketryBlocks;
import zmaster587.advancedRocketry.api.IPlanetaryProvider;
import zmaster587.advancedRocketry.api.stations.ISpaceObject;
import zmaster587.advancedRocketry.api.stations.SpaceObjectManager;
import zmaster587.advancedRocketry.atmosphere.AtmosphereHandler;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.advancedRocketry.network.PacketDimInfo;
import zmaster587.advancedRocketry.network.PacketHandler;
import zmaster587.advancedRocketry.network.PacketSpaceStationInfo;
import zmaster587.advancedRocketry.network.PacketStellarInfo;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.eventhandler.Event.Result;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class PlanetEventHandler {

	public static long time = 0;
	private static long endTime, duration;

	//Handle gravity
	@SubscribeEvent
	public void playerTick(LivingUpdateEvent event) {

		if(event.entity.worldObj.isRemote && event.entity.posY > 260 && event.entity.posY < 270 && event.entity.motionY < -.1) {
			RocketEventHandler.destroyOrbitalTextures(event.entity.worldObj);
		}
		if(event.entity.worldObj.provider instanceof IPlanetaryProvider && !event.entity.isInWater()) {
			IPlanetaryProvider planet = (IPlanetaryProvider)event.entity.worldObj.provider;
			if(!(event.entity instanceof EntityPlayer) || !((EntityPlayer)event.entity).capabilities.isFlying) {
				//event.entity.motionY += 0.075f - planet.getGravitationalMultiplier((int)event.entity.posX, (int)event.entity.posZ)*0.075f;
			}
		}
		else if(event.entity.worldObj.provider.dimensionId == 0) {
			if(!(event.entity instanceof EntityPlayer) || !((EntityPlayer)event.entity).capabilities.isFlying) {
				//event.entity.motionY += 0.075f - DimensionManager.overworldProperties.gravitationalMultiplier*0.075f;
			}
		}
	}

	@SubscribeEvent
	public void blockPlaceEvent(PlayerInteractEvent event) {
		ForgeDirection direction = ForgeDirection.getOrientation(event.face);
		if(!event.world.isRemote && Action.RIGHT_CLICK_BLOCK == event.action && event.entityPlayer != null  && AtmosphereHandler.getOxygenHandler(event.world.provider.dimensionId) != null &&
				!AtmosphereHandler.getOxygenHandler(event.world.provider.dimensionId).getAtmosphereType(event.x + direction.offsetX, event.y + direction.offsetY, event.z + direction.offsetZ).allowsCombustion()) {

			if(event.entityPlayer.getCurrentEquippedItem() != null) {
				if(event.entityPlayer.getCurrentEquippedItem().getItem() == Item.getItemFromBlock(Blocks.torch) && 
						event.world.getBlock(event.x, event.y, event.z).isSideSolid(event.world, event.x, event.y, event.z, direction)) {
					event.setCanceled(true);
					event.world.setBlock(event.x + direction.offsetX, event.y + direction.offsetY, event.z + direction.offsetZ, AdvancedRocketryBlocks.blockUnlitTorch);
				}
				else if(event.entityPlayer.getCurrentEquippedItem().getItem() == Items.flint_and_steel || event.entityPlayer.getCurrentEquippedItem().getItem() == Items.fire_charge)
					event.setCanceled(true);
			}
		}
	}

	@EventHandler
	public void disconnected(ClientDisconnectionFromServerEvent event) {
		zmaster587.advancedRocketry.dimension.DimensionManager.getInstance().unregisterAllDimensions();
	}

	/*@SubscribeEvent
	public void entityRegister(EntityConstructing event) {
		if(event.entity instanceof EntityPlayer) {
			event.entity.registerExtendedProperties(PlayerDataHandler.IDENTIFIER, new PlayerDataHandler());
		}
	}*/

	//TODO move
	//Has weak refs so if the player gets killed/logsout etc the entry doesnt stay trapped in RAM
	private static HashSet<WeakReference<EntityPlayer>> inventoryCheckPlayerBypassMap = new HashSet<WeakReference<EntityPlayer>>();

	public static void addPlayerToInventoryBypass(EntityPlayer player) {
		inventoryCheckPlayerBypassMap.add(new WeakReference<>(player));
	}

	public static void removePlayerFromInventoryBypass(EntityPlayer player) {
		Iterator<WeakReference<EntityPlayer>> iter = inventoryCheckPlayerBypassMap.iterator();

		while(iter.hasNext()) {
			WeakReference<EntityPlayer> player2 = iter.next();
			if(player2.get() == player || player2.get() == null)
				iter.remove();
		}
	}

	public static boolean canPlayerBypassInvChecks(EntityPlayer player) {
		Iterator<WeakReference<EntityPlayer>> iter = inventoryCheckPlayerBypassMap.iterator();
		while(iter.hasNext()) {
			WeakReference<EntityPlayer> player2 = iter.next();
			if(player2.get() == player)
				return true;
		}
		return false;
	}

	@SubscribeEvent
	public void containerOpen(PlayerOpenContainerEvent event) {
		//event.entityPlayer.openContainer
		if(canPlayerBypassInvChecks(event.entityPlayer))
			if(event.entityPlayer.openContainer.windowId == 0)
				removePlayerFromInventoryBypass(event.entityPlayer);
			else
				event.setResult(Result.ALLOW);
	}

	//Tick dimensions, needed for satellites, and guis
	@SubscribeEvent
	public void tick(TickEvent.ServerTickEvent event) {
		//Tick satellites
		DimensionManager.getInstance().tickDimensions();
		time++;
	}


	//Make sure the player receives data about the dimensions
	@SubscribeEvent
	public void playerLoggedInEvent(FMLNetworkEvent.ServerConnectionFromClientEvent event) {
		
		//Make sure stars are sent first
		for(int i : DimensionManager.getInstance().getStars()) {
			PacketHandler.sendToDispatcher(new PacketStellarInfo(i, DimensionManager.getInstance().getStar(i)), event.manager);
		}

		for(int i : DimensionManager.getInstance().getregisteredDimensions()) {
			PacketHandler.sendToDispatcher(new PacketDimInfo(i, DimensionManager.getInstance().getDimensionProperties(i)), event.manager);
		}

		for(ISpaceObject obj : SpaceObjectManager.getSpaceManager().getSpaceObjects()) {
			PacketHandler.sendToDispatcher(new PacketSpaceStationInfo(obj.getId(), obj), event.manager);
		}

		PacketHandler.sendToDispatcher(new PacketDimInfo(0, DimensionManager.getInstance().getDimensionProperties(0)), event.manager);

	}


	// Used to save extra biome data
	/*@SubscribeEvent
	public void worldLoadEvent(WorldEvent.Load event) {
		if(event.world.provider instanceof ProviderPlanet && DimensionManager.getInstance().getDimensionProperties(event.world.provider.dimensionId).biomeProperties == null) {
			DimensionManager.getInstance().getDimensionProperties(event.world.provider.dimensionId).biomeProperties = new ExtendedBiomeProperties(event.world);
		}
	}

	// Used to load extra biome data
	@SubscribeEvent
	public void saveExtraData(ChunkDataEvent.Save event) {
		if(event.world.provider instanceof ProviderPlanet) {
			NBTTagCompound nbt = event.getData();

			int xPos = event.getChunk().xPosition;//nbt.getInteger("xPos");
			int zPos = event.getChunk().zPosition;//nbt.getInteger("zPos");

			ChunkProperties properties = DimensionManager.getInstance().getDimensionProperties(event.world.provider.dimensionId).biomeProperties.getChunkPropertiesFromChunkCoords(xPos, zPos);

			nbt.setIntArray("ExtendedBiomeArray", properties.getBlockBiomeArray());
		}
	}

	@SubscribeEvent
	public void loadExtraData(ChunkDataEvent.Load event) {
		if(event.world.provider instanceof ProviderPlanet)  {
			NBTTagCompound nbt = event.getData();


			int xPos = event.getChunk().xPosition;//nbt.getInteger("xPos");
			int zPos = event.getChunk().zPosition;//nbt.getInteger("zPos");
			ChunkProperties properties = DimensionManager.getInstance().getDimensionProperties(event.world.provider.dimensionId).biomeProperties.getChunkPropertiesFromChunkCoords(xPos, zPos);

			properties.setBlockBiomeArray(event.getData().getIntArray("ExtendedBiomeArray"));
		}
	}
	 */

	@SubscribeEvent
	public void worldLoadEvent(WorldEvent.Load event) {
		if(!event.world.isRemote)
			AtmosphereHandler.registerWorld(event.world.provider.dimensionId);
	}

	@SubscribeEvent
	public void worldUnloadEvent(WorldEvent.Unload event) {
		if(!event.world.isRemote)
			AtmosphereHandler.unregisterWorld(event.world.provider.dimensionId);
	}

	/**
	 * Starts a burst, used for move to warp effect
	 * @param endTime
	 * @param duration
	 */
	@SideOnly(Side.CLIENT)
	public static void runBurst(long endTime, long duration) {
		PlanetEventHandler.endTime = endTime;
		PlanetEventHandler.duration = duration;
	}

	//Handle fog density and color
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void fogColor(net.minecraftforge.client.event.EntityViewRenderEvent.FogColors event) {
		Block block = ActiveRenderInfo.getBlockAtEntityViewpoint(event.entity.worldObj, event.entity, (float)event.renderPartialTicks);
		if(block.getMaterial() == Material.water)
			return;

		DimensionProperties properties = DimensionManager.getInstance().getDimensionProperties(event.entity.dimension);
		if(properties != null) {
			float fog = properties.getAtmosphereDensityAtHeight(event.entity.posY);

			if(event.entity.worldObj.provider instanceof IPlanetaryProvider) {
				Vec3 color = event.entity.worldObj.provider.getSkyColor(event.entity, 0f);
				event.red = (float) Math.min(color.xCoord*1.4f,1f);
				event.green = (float) Math.min(color.yCoord*1.4f, 1f);
				event.blue = (float) Math.min(color.zCoord*1.4f, 1f);
			}

			if(endTime > 0) {
				double amt = (endTime - Minecraft.getMinecraft().theWorld.getTotalWorldTime()) / (double)duration;
				if(amt < 0) {
					endTime = 0;
				}
				else
					event.green = event.blue = event.red = (float)amt;

			} else {
				event.red *= fog;
				event.green *= fog;
				event.blue *= fog;
			}
		}
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void fogColor(net.minecraftforge.client.event.EntityViewRenderEvent.FogDensity event) {
		DimensionProperties properties = DimensionManager.getInstance().getDimensionProperties(event.entity.dimension);
		if(properties != null && properties.atmosphereDensity > 125) {
			float fog = properties.getAtmosphereDensityAtHeight(event.entity.posY);

			event.density = fog/128f;
			event.setCanceled(true);
		}
	}



	//Saves NBT data
	@SubscribeEvent
	public void worldSaveEvent(WorldEvent.Save event) {
		//TODO: save only the one dimension
		if(event.world.provider.dimensionId == 0)
			//DimensionManager.getInstance().getDimensionProperties(event.world.provider.dimensionId).
			DimensionManager.getInstance().saveDimensions(DimensionManager.filePath);
	}


	//Make sure the player doesnt die on low gravity worlds
	@SubscribeEvent
	public void fallEvent(LivingFallEvent event) {
		if(event.entity.worldObj.provider instanceof IPlanetaryProvider) {
			IPlanetaryProvider planet = (IPlanetaryProvider)event.entity.worldObj.provider;
			event.distance *= planet.getGravitationalMultiplier((int)event.entity.posX, (int)event.entity.posZ);
		}
	}
}
