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

package ch.njol.skript.entity;

import java.util.Arrays;

import org.bukkit.entity.Item;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.localization.Adjective;
import ch.njol.skript.localization.Language;
import ch.njol.skript.localization.Noun;
import ch.njol.skript.registrations.Classes;
import ch.njol.util.coll.CollectionUtils;

/**
 * @author Peter Güttinger
 */
@SuppressWarnings("serial")
public class DroppedItemData extends EntityData<Item> {
	static {
		register(DroppedItemData.class, "dropped item", Item.class, "dropped item");
	}
	
	private final static Adjective m_adjective = new Adjective("entities.dropped item.adjective");
	
	private ItemType[] types;
	
	@Override
	protected boolean init(final Literal<?>[] exprs, final int matchedPattern, final ParseResult parseResult) {
		if (exprs.length > 0 && exprs[0] != null)
			types = (ItemType[]) exprs[0].getAll();
		return true;
	}
	
	@Override
	protected boolean init(final Class<? extends Item> c, final Item e) {
		if (e != null)
			types = new ItemType[] {new ItemType(e.getItemStack())};
		return true;
	}
	
	@Override
	protected boolean match(final Item entity) {
		if (types == null)
			return true;
		for (final ItemType t : types) {
			if (t.isOfType(entity.getItemStack()))
				return true;
		}
		return false;
	}
	
	@Override
	public void set(final Item entity) {
		entity.setItemStack(CollectionUtils.getRandom(types).getItem().getRandom());
	}
	
	@Override
	public boolean isSupertypeOf(final EntityData<?> e) {
		if (!(e instanceof DroppedItemData))
			return false;
		if (types == null)
			return true;
		final DroppedItemData d = (DroppedItemData) e;
		return d.types != null && ItemType.isSubset(types, d.types);
	}
	
	@Override
	public Class<? extends Item> getType() {
		return Item.class;
	}
	
	@Override
	public EntityData getSuperType() {
		return new DroppedItemData();
	}
	
	@Override
	public String toString(final int flags) {
		if (types == null)
			return super.toString(flags);
		final StringBuilder b = new StringBuilder();
		b.append(Noun.getArticleWithSpace(types[0].getTypes().get(0).getGender(), flags));
		b.append(m_adjective.toString(types[0].getTypes().get(0).getGender(), flags));
		b.append(" ");
		b.append(Classes.toString(types, flags & Language.NO_ARTICLE_MASK, false));
		return b.toString();
	}
	
//		return ItemType.serialize(types);
	@Override
	@Deprecated
	protected boolean deserialize(final String s) {
		if (s.isEmpty())
			return true;
		types = ItemType.deserialize(s);
		return types != null;
	}
	
	@Override
	protected boolean equals_i(final EntityData<?> obj) {
		if (!(obj instanceof DroppedItemData))
			return false;
		return Arrays.equals(types, ((DroppedItemData) obj).types);
	}
	
	@Override
	protected int hashCode_i() {
		return Arrays.hashCode(types);
	}
	
}
