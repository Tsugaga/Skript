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

package ch.njol.skript.expressions;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;

/**
 * @author Peter Güttinger
 */
@SuppressWarnings("serial")
@Name("Bed")
@Description("The bed location of a player, i.e. the spawn point of a player if he ever slept in a bed and the bed still exists and is unobstructed.")
@Examples({"bed of player exists:",
		"	teleport player the the player's bed",
		"else:",
		"	teleport the player to the world's spawn point"})
@Since("2.0")
public class ExprBed extends SimplePropertyExpression<Player, Location> {
	static {
		register(ExprBed.class, Location.class, "bed[s] [location[s]]", "players");
	}
	
	@Override
	public Location convert(final Player p) {
		return p.getBedSpawnLocation();
	}
	
	@Override
	protected String getPropertyName() {
		return "bed";
	}
	
	@Override
	public Class<? extends Location> getReturnType() {
		return Location.class;
	}
	
}
