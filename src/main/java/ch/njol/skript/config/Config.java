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

package ch.njol.skript.config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;

import ch.njol.skript.Skript;
import ch.njol.skript.config.validate.SectionValidator;

/**
 * Represents a config file.
 * 
 * @author Peter Güttinger
 */
public class Config {
	
	boolean simple = false;
	
	/**
	 * One level of the indentation, e.g. a tab or 4 spaces.
	 */
	private String indentation = "\t";
	/**
	 * The indentation's name, i.e. 'tab' or 'space'.
	 */
	private String indentationName = "tab";
	
	final String defaultSeparator;
	String separator;
	
	String line = "";
	
	int level = 0;
	
	private final SectionNode main;
	
	int errors = 0;
	
	final boolean allowEmptySections;
	
	String fileName;
	File file = null;
	
	public Config(final InputStream source, final String fileName, final boolean simple, final boolean allowEmptySections, final String defaultSeparator) throws IOException {
		try {
			this.fileName = fileName;
			this.simple = simple;
			this.allowEmptySections = allowEmptySections;
			this.defaultSeparator = defaultSeparator;
			separator = defaultSeparator;
			
			if (source.available() == 0) {
				main = new SectionNode(this);
				Skript.warning("'" + getFileName() + "' is empty");
				return;
			}
			
			if (Skript.logVeryHigh())
				Skript.info("loading '" + fileName + "'");
			
			final ConfigReader r = new ConfigReader(source);
			try {
				main = SectionNode.load(this, r);
			} finally {
				r.close();
			}
		} finally {
			source.close();
		}
	}
	
	@SuppressWarnings("resource")
	public Config(final File file, final boolean simple, final boolean allowEmptySections, final String defaultSeparator) throws IOException {
		this(new FileInputStream(file), file.getName(), simple, allowEmptySections, defaultSeparator);
		this.file = file;
	}
	
	/**
	 * For testing
	 * 
	 * @param s
	 * @param fileName
	 * @param simple
	 * @param allowEmptySections
	 * @param defaultSeparator
	 * @throws IOException
	 */
	public Config(final String s, final String fileName, final boolean simple, final boolean allowEmptySections, final String defaultSeparator) throws IOException {
		this(new ByteArrayInputStream(s.getBytes("UTF-8")), fileName, simple, allowEmptySections, defaultSeparator);
	}
	
	void setIndentation(final String indent) {
		assert indentation.length() > 0;
		indentation = indent;
		indentationName = (indent.charAt(0) == ' ' ? "space" : "tab");
	}
	
	String getIndentation() {
		assert indentation.length() > 0;
		return indentation;
	}
	
	String getIndentationName() {
		return indentationName;
	}
	
	public SectionNode getMainNode() {
		return main;
	}
	
	public String getFileName() {
		return fileName;
	}
	
	/**
	 * Saves the config to a file.
	 * 
	 * @param f The file to save to
	 * @throws IOException If the file could not be written to.
	 */
	public void save(final File f) throws IOException {
		separator = defaultSeparator;
		final PrintWriter w = new PrintWriter(f, "UTF-8");
		try {
			main.save(w);
		} finally {
			w.flush();
			w.close();
		}
	}
	
	/**
	 * Sets this config's values to those in the given config.
	 * <p>
	 * Used by Skript to import old settings into the updated config. The return value is used to not modify the config if no new options were added.
	 * 
	 * @param other
	 * @return Whether the configs' keys differ, i.e. false == configs only differ in values, not keys.
	 */
	public boolean setValues(final Config other) {
		return getMainNode().setValues(other.getMainNode());
	}
	
	public File getFile() {
		return file;
	}
	
	/**
	 * @return The most recent separator. Only useful while the file is loading.
	 */
	public String getSeparator() {
		return separator;
	}
	
	/**
	 * @return A separator string useful for saving, e.g. ": " or " = ".
	 */
	public String getSaveSeparator() {
		if (separator.equals(":"))
			return ": ";
		if (separator.equals("="))
			return " = ";
		return " " + separator + " ";
	}
	
	/**
	 * Splits the given path at the dot character and passes the result to {@link #get(String...)}.
	 * 
	 * @param path
	 * @return <tt>get(path.split("\\."))</tt>
	 */
	public String getByPath(final String path) {
		return get(path.split("\\."));
	}
	
	/**
	 * Gets an entry node's value at the designated path
	 * 
	 * @param path
	 * @return The entry node's value at the location defined by path or null if it either doesn't exist or is not an entry.
	 */
	public String get(final String... path) {
		SectionNode section = main;
		for (int i = 0; i < path.length; i++) {
			final Node n = section.get(path[i]);
			if (n == null)
				return null;
			if (n instanceof SectionNode) {
				if (i == path.length - 1)
					return null;
				section = (SectionNode) n;
			} else {
				if (n instanceof EntryNode && i == path.length - 1)
					return ((EntryNode) n).getValue();
				else
					return null;
			}
		}
		return null;
	}
	
	public boolean isEmpty() {
		return main.isEmpty();
	}
	
	public HashMap<String, String> toMap(final String separator) {
		return main.toMap("", separator);
	}
	
	public boolean validate(final SectionValidator validator) {
		return validator.validate(getMainNode());
	}
	
	private void load(final Class<?> c, final Object o, final String path) {
		for (final Field f : c.getDeclaredFields()) {
			f.setAccessible(true);
			if (o != null || Modifier.isStatic(f.getModifiers())) {
				try {
					if (OptionSection.class.isAssignableFrom(f.getType())) {
						load(f.get(o).getClass(), f.get(o), path + ((OptionSection) f.get(o)).name + ".");
					} else if (Option.class.isAssignableFrom(f.getType())) {
						((Option<?>) f.get(o)).set(this, path);
					}
				} catch (final IllegalArgumentException e) {
					assert false;
				} catch (final IllegalAccessException e) {
					assert false;
				}
			}
		}
	}
	
	public void load(final Object o) {
		load(o.getClass(), o, "");
	}
	
	public void load(final Class<?> c) {
		load(c, null, "");
	}
	
}
