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

package ch.njol.skript.effects;

import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.classes.Changer.ChangerUtils;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.util.Color;
import ch.njol.skript.util.Slot;
import ch.njol.util.Kleenean;
import ch.njol.util.Math2;

/**
 * @author joeuguce99
 */
@SuppressWarnings("serial")
@Name("Colour Armour")
@Description("Colours leather armour in a given <a href='../classes/#color'>colour</a>. " +
		"You can also use RGB codes if you feel limited with the 16 default colours. " +
		"RGB codes are three numbers from 0 to 255 in the order <code>(red, green, blue)</code>, where <code>(0,0,0)</code> is black and <code>(255,255,255)</code> is white.")
@Examples({"dye player's helmet blue",
		"colour the player's tool red"})
@Since("2.0")
public class EffColorArmor extends Effect {
	static {
		Skript.registerEffect(EffColorArmor.class,
				"(dye|colo[u]r|paint) %slots/itemstack% %color%",
				"(dye|colo[u]r|paint) %slots/itemstack% \\(%number%, %number%, %number%\\)");
	}
	
	private Expression<?> items;
	private Expression<Color> color;
	private Expression<Number> red;
	private Expression<Number> green;
	private Expression<Number> blue;
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean init(final Expression<?>[] exprs, final int matchedPattern, final Kleenean isDelayed, final ParseResult parser) {
		items = exprs[0];
		if (!Slot.class.isAssignableFrom(items.getReturnType()) && !ChangerUtils.acceptsChange(items, ChangeMode.SET, ItemStack.class)) {
			Skript.error(items + " cannot be coloured as it cannot be changed at all.");
			return false;
		}
		if (matchedPattern == 0) {
			color = (Expression<Color>) exprs[1];
		} else {
			red = (Expression<Number>) exprs[1];
			green = (Expression<Number>) exprs[2];
			blue = (Expression<Number>) exprs[3];
		}
		return true;
	}
	
	@Override
	public String toString(final Event e, final boolean debug) {
		return "dye " + items.toString(e, debug) + " " + (color != null ? color.toString(e, debug) : "(" + red.toString(e, debug) + "," + green.toString(e, debug) + "," + blue.toString(e, debug) + ")");
	}
	
	@Override
	protected void execute(final Event e) {
		final org.bukkit.Color c;
		if (color != null) {
			final Color cl = color.getSingle(e);
			if (cl == null)
				return;
			c = cl.getBukkitColor();
		} else {
			final Number r = red.getSingle(e), g = green.getSingle(e), b = blue.getSingle(e);
			if (r == null || g == null || b == null)
				return;
			c = org.bukkit.Color.fromRGB(Math2.fit(0, r.intValue(), 255), Math2.fit(0, g.intValue(), 255), Math2.fit(0, b.intValue(), 255));
		}
		
		for (final Object o : items.getArray(e)) {
			final ItemStack i = o instanceof Slot ? ((Slot) o).getItem() : (ItemStack) o;
			if (i == null)
				continue;
			if (i.getType() == Material.LEATHER_BOOTS || i.getType() == Material.LEATHER_CHESTPLATE || i.getType() == Material.LEATHER_HELMET || i.getType() == Material.LEATHER_LEGGINGS) {
				final LeatherArmorMeta m = (LeatherArmorMeta) i.getItemMeta();
				m.setColor(c);
				i.setItemMeta(m);
			}
			if (o instanceof Slot) {
				((Slot) o).setItem(i);
			} else {
				items.change(e, new ItemStack[] {i}, ChangeMode.SET);
				return;
			}
		}
	}
}
