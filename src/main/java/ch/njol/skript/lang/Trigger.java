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

package ch.njol.skript.lang;

import java.io.File;
import java.util.List;

import org.bukkit.event.Event;

/**
 * @author Peter Güttinger
 */
@SuppressWarnings("serial")
public class Trigger extends TriggerSection {
	
	private final String name;
	private final SkriptEvent event;
	
	private final File script;
	
	public Trigger(final File script, final String name, final SkriptEvent event, final List<TriggerItem> items) {
		super(items);
		this.script = script;
		this.name = name;
		this.event = event;
	}
	
	/**
	 * @param e
	 * @return false iff an exception occurred
	 */
	public boolean execute(final Event e) {
		return TriggerItem.walk(this, e);
	}
	
	@Override
	protected TriggerItem walk(final Event e) {
		return walk(e, true);
	}
	
	@Override
	public String toString(final Event e, final boolean debug) {
		return name + " (" + event.toString(e, debug) + ")";
	}
	
	public String getName() {
		return name;
	}
	
	public SkriptEvent getEvent() {
		return event;
	}
	
	public File getScript() {
		return script;
	}
	
}
