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

package ch.njol.skript.hooks.regions;

import java.io.NotSerializableException;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import ch.njol.skript.hooks.regions.classes.Region;
import ch.njol.skript.variables.Variables;
import ch.njol.util.coll.iterator.EmptyIterator;
import ch.njol.yggdrasil.Fields;

import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

/**
 * @author Peter Güttinger
 */
public class WorldGuardHook extends RegionsPlugin<WorldGuardPlugin> {
	
	@Override
	public String getName() {
		return "WorldGuard";
	}
	
	@Override
	public boolean canBuild_i(final Player p, final Location l) {
		return plugin.canBuild(p, l);
	}
	
	public final class WorldGuardRegion extends Region {
		
		final World world;
		private transient ProtectedRegion region;
		
		public WorldGuardRegion(final World w, final ProtectedRegion r) {
			world = w;
			region = r;
		}
		
		@Override
		public boolean contains(final Location l) {
			return l.getWorld().equals(world) && region.contains(l.getBlockX(), l.getBlockY(), l.getBlockZ());
		}
		
		@Override
		public boolean isMember(final OfflinePlayer p) {
			return region.isMember(p.getName());
		}
		
		@Override
		public Collection<OfflinePlayer> getMembers() {
			final Collection<String> ps = region.getMembers().getPlayers();
			final Collection<OfflinePlayer> r = new ArrayList<OfflinePlayer>(ps.size());
			for (final String p : ps)
				r.add(Bukkit.getOfflinePlayer(p));
			return r;
		}
		
		@Override
		public boolean isOwner(final OfflinePlayer p) {
			return region.isOwner(p.getName());
		}
		
		@Override
		public Collection<OfflinePlayer> getOwners() {
			final Collection<String> ps = region.getOwners().getPlayers();
			final Collection<OfflinePlayer> r = new ArrayList<OfflinePlayer>(ps.size());
			for (final String p : ps)
				r.add(Bukkit.getOfflinePlayer(p));
			return r;
		}
		
		@Override
		public Iterator<Block> getBlocks() {
			final Iterator<BlockVector2D> iter = region.getPoints().iterator();
			if (!iter.hasNext())
				return EmptyIterator.get();
			return new Iterator<Block>() {
				BlockVector2D current = iter.next();
				int height = 0;
				final int maxHeight = world.getMaxHeight();
				
				@Override
				public boolean hasNext() {
					if (height >= maxHeight && iter.hasNext()) {
						height = 0;
						current = iter.next();
					}
					return height < maxHeight;
				}
				
				@Override
				public Block next() {
					if (!hasNext())
						throw new NoSuchElementException();
					return world.getBlockAt(current.getBlockX(), height++, current.getBlockZ());
				}
				
				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}
		
		@Override
		public Fields serialize() throws NotSerializableException {
			final Fields f = new Fields(this);
			f.putObject("region", region.getId());
			return f;
		}
		
		@Override
		public void deserialize(final Fields fields) throws StreamCorruptedException, NotSerializableException {
			final String r = fields.getAndRemoveObject("region", String.class);
			fields.setFields(this, Variables.yggdrasil);
			region = plugin.getRegionManager(world).getRegion(r);
			if (region == null)
				throw new StreamCorruptedException("Invalid region " + r + " in world " + world);
		}
		
		@Override
		public String toString() {
			return region.getId() + " in world " + world.getName();
		}
		
		@Override
		public RegionsPlugin<?> getPlugin() {
			return WorldGuardHook.this;
		}
		
		@Override
		public boolean equals(final Object o) {
			if (o == this)
				return true;
			if (o == null)
				return false;
			if (!(o instanceof WorldGuardRegion))
				return false;
			return world.equals(((WorldGuardRegion) o).world) && region.equals(((WorldGuardRegion) o).region);
		}
		
		@Override
		public int hashCode() {
			return world.hashCode() * 31 + region.hashCode();
		}
		
	}
	
	@Override
	public Collection<? extends Region> getRegionsAt_i(final Location l) {
		final Iterator<ProtectedRegion> i = plugin.getRegionManager(l.getWorld()).getApplicableRegions(l).iterator();
		final ArrayList<Region> r = new ArrayList<Region>();
		while (i.hasNext())
			r.add(new WorldGuardRegion(l.getWorld(), i.next()));
		return r;
	}
	
	@Override
	public Region getRegion_i(final World world, final String name) {
		final ProtectedRegion r = plugin.getRegionManager(world).getRegion(name);
		if (r != null)
			return new WorldGuardRegion(world, r);
		return null;
	}
	
	@Override
	public boolean hasMultipleOwners_i() {
		return true;
	}
	
	@Override
	protected Class<? extends Region> getRegionClass() {
		return WorldGuardRegion.class;
	}
	
}
