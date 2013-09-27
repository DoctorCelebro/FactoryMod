package com.github.igotyou.FactoryMod.Factorys;

import com.github.igotyou.FactoryMod.FactoryModPlugin;
import com.github.igotyou.FactoryMod.Factorys.BaseFactory.FactoryCategory;
import com.github.igotyou.FactoryMod.interfaces.AreaEffect;
import com.github.igotyou.FactoryMod.properties.ContinousFactoryProperties;
import com.github.igotyou.FactoryMod.utility.Anchor;
import com.github.igotyou.FactoryMod.utility.ItemList;
import com.github.igotyou.FactoryMod.utility.NamedItemStack;
import com.untamedears.citadel.Citadel;
import com.untamedears.citadel.access.AccessDelegate;
import com.untamedears.citadel.entity.Faction;
import com.untamedears.citadel.entity.FactionMember;
import com.untamedears.citadel.entity.IReinforcement;
import com.untamedears.citadel.entity.PlayerReinforcement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

public class ContinousFactory extends BaseFactory {

	private int currentEnergyTime;

	public ContinousFactory(Anchor anchor, String factoryType, FactoryCategory factoryCategory) {
		this(anchor,
			factoryType,
			factoryCategory,
			((ContinousFactoryProperties) FactoryModPlugin.getManager().getManager(factoryCategory).getProperties(factoryType)).getEnergyTime());
	}

	public ContinousFactory(Anchor anchor, String factoryType,FactoryCategory factoryCategory, int currentEnergyTime) {
		super(anchor, factoryCategory, factoryType);
		this.currentEnergyTime = currentEnergyTime;
		indicatePowerOn();
	}
	/*
	 * Performs an update of the factory
	 */

	@Override
	public void update() {
		if (!isWhole()) {
			powerOff();
			return;
		}
		if (!updateEnergy()) {
			powerOff();
			return;
		}
		updateSpecifics();
		currentEnergyTime += FactoryModPlugin.getManager().getManager(factoryCategory).getUpdatePeriod();
	}

	/*
	 * Hook for subclasses
	 */
	public void updateSpecifics() {
	}
	
	/*
	 * Updates the energy state of the factory, returns false if fuel is needed
	 * and is not present
	 */

	protected boolean updateEnergy() {
		if (currentEnergyTime >= getEnergyTime()) {
			if (consumeFuel()) {
				currentEnergyTime = 0;
			} else {
				return false;
			}
		}
		return true;
	}

	/*
	 * Attempts to consume fuel from the powerSource
	 */
	protected boolean consumeFuel() {
		Inventory powerSourceInventory = getInventory();
		ItemList<NamedItemStack> fuel = getFactoryProperties().getFuel();
		if (fuel.allIn(powerSourceInventory)) {
			fuel.removeFrom(powerSourceInventory);
			return true;
		} else {
			return false;
		}

	}

	/*
	 * Power off the ContinousFactory, completely deleting it
	 */

	protected void powerOff() {
		indicatePowerOff();
		for (AreaEffect areaEffect : getFactoryProperties().getAllAreaEffects()) {
			areaEffect.disable(this);
		}
		FactoryModPlugin.getManager().getManager(factoryCategory).removeFactory(this);
	}

	/*
	 * Returns the time one unit of fuel lasts in ticks
	 */
	public int getEnergyTime() {
		return getFactoryProperties().getEnergyTime();
	}



	/*
	 * Gets the inventory of the powersource used for this factory
	 */
	protected Inventory getInventory() {
		return ((InventoryHolder) getInventoryLocation().getBlock().getState()).getInventory();
	}

	/*
	 * gets the location of the power source of the factory
	 */
	protected Location getInventoryLocation() {
		return anchor.getLocationOfOffset(getFactoryProperties().getInventory());
	}

	/*
	 * Gets a correctly caste properties file
	 */
	@Override
	protected ContinousFactoryProperties getFactoryProperties() {
		return (ContinousFactoryProperties) super.getFactoryProperties();
	}

	/*
	 * Gets the set of allowed playersgroup which owns the factory
	 */
	public Set<Player> getGroup() {
		Set<Player> players = new HashSet<Player>();
		if (FactoryModPlugin.CITADEL_ENABLED) {
			IReinforcement rein = AccessDelegate.getDelegate(getInventoryLocation().getBlock()).getReinforcement();
			if (!(rein instanceof PlayerReinforcement)) {
				PlayerReinforcement prein = (PlayerReinforcement) rein;
				Faction group = prein.getOwner();
				Set<FactionMember> factionMembers = Citadel.getGroupManager().getMembersOfGroup(group.getName());
				for (FactionMember factionMemeber : factionMembers) {
					players.add(Bukkit.getServer().getPlayer(factionMemeber.getMemberName()));
				}
			}
		}
		return players;

	}

	/*
	 * Generates the outputs produced by this factory
	 */
	protected void generateProducts() {
		getFactoryProperties().getOutputs().putIn(getInventory());
	}


	@Override
	public void blockBreakResponse() {
		powerOff();
	}

	/*
	 * Changes the power indicator block to indicate the factory is on
	 */
	public void indicatePowerOn() {
	}

	/*
	 * Changes the power indicator block to indicate the factory is off
	 */
	public void indicatePowerOff() {
	}
}
