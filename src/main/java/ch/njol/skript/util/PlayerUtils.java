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
 * Copyright 2011, 2012 Peter Güttinger
 * 
 */

package ch.njol.skript.util;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.entity.Player;

import ch.njol.skript.Skript;

/**
 * TODO check all updates and find out which ones are not required
 * 
 * @author Peter Güttinger
 */
public abstract class PlayerUtils {
	private PlayerUtils() {}
	
	final static Set<Player> inviUpdate = new HashSet<Player>();
	
	public final static void updateInventory(final Player p) {
		inviUpdate.add(p);
	}
	
	// created when first used
	@SuppressWarnings("unused")
	private final static Task task = new Task(Skript.getInstance(), 1, 1) {
		@SuppressWarnings("deprecation")
		@Override
		public void run() {
			for (final Player p : inviUpdate)
				p.updateInventory();
			inviUpdate.clear();
		}
	};
	
}
