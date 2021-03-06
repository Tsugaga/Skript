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

package ch.njol.skript.entity;

import org.bukkit.entity.Minecart;
import org.bukkit.entity.PoweredMinecart;
import org.bukkit.entity.StorageMinecart;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.RideableMinecart;
import org.bukkit.entity.minecart.SpawnerMinecart;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.variables.Variables;

/**
 * @author Peter Güttinger
 */
@SuppressWarnings({"serial", "deprecation"})
public class MinecartData extends EntityData<Minecart> {
	private static enum MinecartType {
		ANY(Minecart.class, "minecart"),
		NORMAL(Skript.isRunningMinecraft(1, 5) ? RideableMinecart.class : Minecart.class, "regular minecart"),
		STORAGE(Skript.isRunningMinecraft(1, 5) ? org.bukkit.entity.minecart.StorageMinecart.class : StorageMinecart.class, "storage minecart"),
		POWERED(Skript.isRunningMinecraft(1, 5) ? org.bukkit.entity.minecart.PoweredMinecart.class : PoweredMinecart.class, "powered minecart"),
		// 1.5
		HOPPER(Skript.isRunningMinecraft(1, 5) ? HopperMinecart.class : null, "hopper minecart"),
		EXPLOSIVE(Skript.isRunningMinecraft(1, 5) ? ExplosiveMinecart.class : null, "explosive minecart"),
		SPAWNER(Skript.isRunningMinecraft(1, 5) ? SpawnerMinecart.class : null, "spawner minecart");
		
		final Class<? extends Minecart> c;
		private final String codeName;
		
		MinecartType(final Class<? extends Minecart> c, final String codeName) {
			this.c = c;
			this.codeName = codeName;
		}
		
		@Override
		public String toString() {
			return codeName;
		}
		
		public static String[] codeNames;
		static {
			codeNames = new String[values().length - (Skript.isRunningMinecraft(1, 5) ? 0 : 3)];
			for (int i = 0; i < codeNames.length; i++) {
				codeNames[i] = values()[i].codeName;
			}
		}
	}
	
	static {
		register(MinecartData.class, "minecart", Minecart.class, 0, MinecartType.codeNames);
		
		Variables.yggdrasil.registerSingleClass(MinecartType.class, "MinecartType");
	}
	
	private MinecartType type = MinecartType.ANY;
	
	public MinecartData() {}
	
	public MinecartData(final MinecartType type) {
		this.type = type;
	}
	
	@Override
	protected boolean init(final Literal<?>[] exprs, final int matchedPattern, final ParseResult parseResult) {
		type = MinecartType.values()[matchedPattern];
		return true;
	}
	
	@Override
	protected boolean init(final Class<? extends Minecart> c, final Minecart e) {
		final MinecartType[] ts = MinecartType.values();
		for (int i = ts.length - 1; i >= 0; i--) {
			if (e == null ? ts[i].c.isAssignableFrom(c) : ts[i].c.isInstance(e)) {
				type = ts[i];
				return true;
			}
		}
		assert false;
		return false;
	}
	
	@Override
	public void set(final Minecart entity) {}
	
	@Override
	public boolean match(final Minecart entity) {
		if (type == MinecartType.NORMAL && type.c == Minecart.class) // pre-1.5
			return !(entity instanceof PoweredMinecart || entity instanceof StorageMinecart);
		return type.c.isInstance(entity);
	}
	
	@Override
	public Class<? extends Minecart> getType() {
		return type.c;
	}
	
	@Override
	protected int hashCode_i() {
		return type.hashCode();
	}
	
	@Override
	protected boolean equals_i(final EntityData<?> obj) {
		if (!(obj instanceof MinecartData))
			return false;
		final MinecartData other = (MinecartData) obj;
		return type == other.type;
	}
	
//		return type.name();
	@Override
	protected boolean deserialize(final String s) {
		try {
			type = MinecartType.valueOf(s);
			return true;
		} catch (final IllegalArgumentException e) {
			return false;
		}
	}
	
	@Override
	public boolean isSupertypeOf(final EntityData<?> e) {
		if (e instanceof MinecartData)
			return type == MinecartType.ANY || ((MinecartData) e).type == type;
		return false;
	}
	
	@Override
	public EntityData getSuperType() {
		return new MinecartData(type);
	}
	
}
