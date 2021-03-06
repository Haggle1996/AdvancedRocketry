package zmaster587.advancedRocketry.api;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityEvent;

/**
 * Events related to rockets here
 *
 */
public class RocketEvent extends EntityEvent {
	public final World world;
	public RocketEvent(Entity entity) {
		super(entity);
		world = entity.worldObj;
	}

	/**
	 * Fired when the rocket is launched on the Minecraft Forge EVENT_BUS
	 */
	public static class RocketLaunchEvent extends RocketEvent {
		public RocketLaunchEvent(Entity entity) {
			super(entity);
		}
	}
	
	/**
	 * Fired before the rocket is finished teleporting to the destination world on the Minecraft Forge EVENT_BUS
	 */
	public static class RocketDeOrbitingEvent extends RocketEvent {
		public RocketDeOrbitingEvent(Entity entity) {
			super(entity);
		}
	}
	
	/**
	 * Fired when the rocket touches the ground on the Minecraft Forge EVENT_BUS
	 */
	public static class RocketLandedEvent extends RocketEvent {
		public RocketLandedEvent(Entity entity) {
			super(entity);
		}
	}
	
	/**
	 * Fired when the rocket is about to be teleported to its destination on the Minecraft Forge EVENT_BUS
	 */
	public static class RocketReachesOrbitEvent extends RocketEvent {
		public RocketReachesOrbitEvent(Entity entity) {
			super(entity);
		}
	}
}
