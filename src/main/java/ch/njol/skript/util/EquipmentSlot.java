/*
 *   This file is part of Skript.
 *
 *  Skript is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Skript is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * 
 * Copyright 2011-2013 Peter Güttinger
 * 
 */

package ch.njol.skript.util;

import java.util.Locale;

import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import ch.njol.skript.registrations.Classes;

/**
 * @author Peter Güttinger
 */
public class EquipmentSlot extends Slot {
	
	// according to mcstats there are only 2 servers running 1.2.5 or 1.3.1 respectively
//	public final static Version EQUIPMENT_VERSION = new Version(1,4,5);
//	public final static boolean SUPPORTS_EQUIPMENT = Skript.isRunningMinecraft(EQUIPMENT_VERSION);
	
	public static enum EquipSlot {
		TOOL {
			@Override
			public ItemStack get(final EntityEquipment e) {
				return e.getItemInHand();
			};
			
			@Override
			public void set(final EntityEquipment e, final ItemStack item) {
				e.setItemInHand(item);
			};
		},
		HELMET {
			@Override
			public ItemStack get(final EntityEquipment e) {
				return e.getHelmet();
			}
			
			@Override
			public void set(final EntityEquipment e, final ItemStack item) {
				e.setHelmet(item);
			}
		},
		CHESTPLATE {
			@Override
			public ItemStack get(final EntityEquipment e) {
				return e.getChestplate();
			}
			
			@Override
			public void set(final EntityEquipment e, final ItemStack item) {
				e.setChestplate(item);
			}
		},
		LEGGINGS {
			@Override
			public ItemStack get(final EntityEquipment e) {
				return e.getLeggings();
			}
			
			@Override
			public void set(final EntityEquipment e, final ItemStack item) {
				e.setLeggings(item);
			}
		},
		BOOTS {
			@Override
			public ItemStack get(final EntityEquipment e) {
				return e.getBoots();
			}
			
			@Override
			public void set(final EntityEquipment e, final ItemStack item) {
				e.setBoots(item);
			}
		};
		
		public abstract ItemStack get(EntityEquipment e);
		
		public abstract void set(EntityEquipment e, ItemStack item);
		
	}
	
	private final EntityEquipment e;
	private final EquipSlot slot;
	
	public EquipmentSlot(final EntityEquipment e, final EquipSlot slot) {
		this.e = e;
		this.slot = slot;
	}
	
	@Override
	public ItemStack getItem() {
		return slot.get(e);
	}
	
	@Override
	public void setItem(final ItemStack item) {
		slot.set(e, item);
	}
	
	@Override
	public String toString_i() {
		return "the " + slot.name().toLowerCase(Locale.ENGLISH) + " of " + Classes.toString(e.getHolder());
	}
	
}
