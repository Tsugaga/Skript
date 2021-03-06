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

package ch.njol.skript.command;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.help.HelpMap;
import org.bukkit.help.HelpTopic;
import org.bukkit.plugin.SimplePluginManager;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.config.validate.SectionValidator;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.localization.ArgsMessage;
import ch.njol.skript.localization.Language;
import ch.njol.skript.localization.Message;
import ch.njol.skript.log.BukkitLoggerFilter;
import ch.njol.skript.log.RetainingLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Utils;
import ch.njol.util.Callback;
import ch.njol.util.Pair;
import ch.njol.util.StringUtils;

/**
 * @author Peter Güttinger
 */
@SuppressWarnings("deprecation")
public abstract class Commands {
	
	public final static ArgsMessage m_too_many_arguments = new ArgsMessage("commands.too many arguments");
	public final static Message m_correct_usage = new Message("commands.correct usage");
	public final static Message m_internal_error = new Message("commands.internal error");
	
	private final static Map<String, ScriptCommand> commands = new HashMap<String, ScriptCommand>();
	
	private static SimpleCommandMap commandMap = null;
	private static Map<String, Command> cmKnownCommands;
	private static Set<String> cmAliases;
	static {
		try {
			if (Bukkit.getPluginManager() instanceof SimplePluginManager) {
				final Field commandMapField = SimplePluginManager.class.getDeclaredField("commandMap");
				commandMapField.setAccessible(true);
				commandMap = (SimpleCommandMap) commandMapField.get(Bukkit.getPluginManager());
				
				final Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
				knownCommandsField.setAccessible(true);
				cmKnownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);
				
				final Field aliasesField = SimpleCommandMap.class.getDeclaredField("aliases");
				aliasesField.setAccessible(true);
				cmAliases = (Set<String>) aliasesField.get(commandMap);
			}
		} catch (final SecurityException e) {
			Skript.error("Please disable the security manager");
			commandMap = null;
		} catch (final Exception e) {
			Skript.outdatedError(e);
			commandMap = null;
		}
	}
	
	private final static SectionValidator commandStructure = new SectionValidator()
			.addEntry("usage", true)
			.addEntry("description", true)
			.addEntry("permission", true)
			.addEntry("permission message", true)
			.addEntry("aliases", true)
			.addEntry("executable by", true)
			.addSection("trigger", false);
	
	public static List<Argument<?>> currentArguments = null;
	
	private final static Pattern escape = Pattern.compile("[" + Pattern.quote("(|)<>%\\") + "]");
	private final static Pattern unescape = Pattern.compile("\\\\[" + Pattern.quote("(|)<>%\\") + "]");
	
	private final static String escape(final String s) {
		return escape.matcher(s).replaceAll("\\\\$0");
	}
	
	private final static String unescape(final String s) {
		return unescape.matcher(s).replaceAll("$0");
	}
	
	private final static Listener commandListener = new Listener() {
		@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
		public void onPlayerCommand(final PlayerCommandPreprocessEvent e) {
			if (handleCommand(e.getPlayer(), e.getMessage().substring(1)))
				e.setCancelled(true);
		}
		
		@EventHandler(priority = EventPriority.HIGHEST)
		public void onServerCommand(final ServerCommandEvent e) {
			if (e.getCommand() == null || e.getCommand().isEmpty())
				return;
			if (SkriptConfig.enableEffectCommands.value() && e.getCommand().startsWith(SkriptConfig.effectCommandToken.value())) {
				if (handleEffectCommand(e.getSender(), e.getCommand())) {
					e.setCommand("");
					suppressUnknownCommandMessage = true;
				}
				return;
			}
			if (handleCommand(e.getSender(), e.getCommand())) {
				e.setCommand("");
				suppressUnknownCommandMessage = true;
			}
		}
	};
	
	static boolean suppressUnknownCommandMessage = false;
	static {
		BukkitLoggerFilter.addFilter(new Filter() {
			@Override
			public boolean isLoggable(final LogRecord record) {
				if (suppressUnknownCommandMessage && record.getMessage() != null && record.getMessage().toLowerCase().startsWith("unknown command.")) {
					suppressUnknownCommandMessage = false;
					return false;
				}
				return true;
			}
		});
	}
	
	private final static Listener pre1_3chatListener = Skript.isRunningMinecraft(1, 3) ? null : new Listener() {
		@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
		public void onPlayerChat(final PlayerChatEvent e) {
			if (!SkriptConfig.enableEffectCommands.value() || !e.getMessage().startsWith(SkriptConfig.effectCommandToken.value()))
				return;
			if (handleEffectCommand(e.getPlayer(), e.getMessage()))
				e.setCancelled(true);
		}
	};
	private final static Listener post1_3chatListener = !Skript.isRunningMinecraft(1, 3) ? null : new Listener() {
		@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
		public void onPlayerChat(final AsyncPlayerChatEvent e) {
			if (!SkriptConfig.enableEffectCommands.value() || !e.getMessage().startsWith(SkriptConfig.effectCommandToken.value()))
				return;
			if (!e.isAsynchronous()) {
				if (handleEffectCommand(e.getPlayer(), e.getMessage()))
					e.setCancelled(true);
			} else {
				final Future<Boolean> f = Bukkit.getScheduler().callSyncMethod(Skript.getInstance(), new Callable<Boolean>() {
					@Override
					public Boolean call() throws Exception {
						return handleEffectCommand(e.getPlayer(), e.getMessage());
					}
				});
				try {
					while (true) {
						try {
							if (f.get())
								e.setCancelled(true);
							break;
						} catch (final InterruptedException e1) {}
					}
				} catch (final ExecutionException e1) {
					Skript.exception(e1);
				}
			}
		}
	};
	
	/**
	 * @param sender
	 * @param command full command string without the slash
	 * @return whether to cancel the event
	 */
	final static boolean handleCommand(final CommandSender sender, final String command) {
		final String[] cmd = command.split("\\s+", 2);
		cmd[0] = cmd[0].toLowerCase();
		if (cmd[0].endsWith("?")) {
			final ScriptCommand c = commands.get(cmd[0].substring(0, cmd[0].length() - 1));
			if (c != null) {
				c.sendHelp(sender);
				return true;
			}
		}
		final ScriptCommand c = commands.get(cmd[0]);
		if (c != null) {
//			if (cmd.length == 2 && cmd[1].equals("?")) {
//				c.sendHelp(sender);
//				return true;
//			}
			if (SkriptConfig.logPlayerCommands.value() && !(sender instanceof ConsoleCommandSender))
				SkriptLogger.LOGGER.info(sender.getName() + ": /" + command);
			c.execute(sender, cmd[0], cmd.length == 1 ? "" : cmd[1]);
			return true;
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	final static boolean handleEffectCommand(final CommandSender sender, String command) {
		if (!(sender.hasPermission("skript.effectcommands") || SkriptConfig.allowOpsToUseEffectCommands.value() && sender.isOp()))
			return false;
		try {
			command = command.substring(SkriptConfig.effectCommandToken.value().length()).trim();
			final RetainingLogHandler log = SkriptLogger.startRetainingLog();
			final Effect e;
			try {
				ScriptLoader.setCurrentEvents(CommandEvent.class);
				e = Effect.parse(command, null);
				ScriptLoader.setCurrentEvents((Class[]) null);
			} finally {
				log.stop();
			}
			if (e != null) {
				sender.sendMessage(ChatColor.GRAY + "executing '" + ChatColor.stripColor(command) + "'");
				if (SkriptConfig.logPlayerCommands.value() && !(sender instanceof ConsoleCommandSender))
					Skript.info(sender.getName() + " issued effect command: " + command);
				e.run(new CommandEvent(sender, "effectcommand", new String[0]));
			} else {
				sender.sendMessage(ChatColor.RED + "Error in: " + ChatColor.GRAY + ChatColor.stripColor(command));
				log.printErrors(sender, "(No specific information is available)");
			}
			return true;
		} catch (final Exception e) {
			Skript.exception(e, "Unexpected error while executing effect command '" + command + "' by '" + sender.getName() + "'");
			sender.sendMessage(ChatColor.RED + "An internal error occurred while executing this effect. Please refer to the server log for details.");
			return true;
		}
	}
	
	public final static ScriptCommand loadCommand(final SectionNode node) {
		
		final String s = ScriptLoader.replaceOptions(node.getKey());
		
		int level = 0;
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == '[') {
				level++;
			} else if (s.charAt(i) == ']') {
				if (level == 0) {
					Skript.error("Invalid placement of [optional brackets]");
					return null;
				}
				level--;
			}
		}
		if (level > 0) {
			Skript.error("Invalid amount of [optional brackets]");
			return null;
		}
		
		Matcher m = Pattern.compile("(?i)^command /?(\\S+)(\\s+(.+))?$").matcher(s);
		final boolean a = m.matches();
		assert a;
		
		final String command = m.group(1).toLowerCase();
		final ScriptCommand existingCommand = commands.get(command);
		if (existingCommand != null && existingCommand.getLabel().equals(command)) {
			Skript.error("A command with the name /" + command + " is already defined in " + existingCommand.getScript().getName());
			return null;
		}
		
		final String arguments = m.group(3) == null ? "" : m.group(3);
		final StringBuilder pattern = new StringBuilder();
		
		currentArguments = new ArrayList<Argument<?>>();
		m = Pattern.compile("<([a-zA-Z -]+?)\\s*(=\\s*(" + SkriptParser.wildcard + "))?>").matcher(arguments);
		int lastEnd = 0;
		int optionals = 0;
		for (int i = 0; m.find(); i++) {
			pattern.append(escape(arguments.substring(lastEnd, m.start())));
			optionals += StringUtils.count(arguments, '[', lastEnd, m.start());
			optionals -= StringUtils.count(arguments, ']', lastEnd, m.start());
			
			lastEnd = m.end();
			
			ClassInfo<?> c;
			c = Classes.getClassInfoFromUserInput(m.group(1));
			final Pair<String, Boolean> p = Utils.getEnglishPlural(m.group(1));
			if (c == null)
				c = Classes.getClassInfoFromUserInput(p.first);
			if (c == null) {
				Skript.error("unknown type '" + m.group(1) + "'");
				return null;
			}
			if (c.getParser() == null || !c.getParser().canParse(ParseContext.COMMAND)) {
				Skript.error("can't use " + m.group(1) + " as argument of a command");
				return null;
			}
			
			final Argument<?> arg = Argument.newInstance(c.getC(), m.group(3), i, !p.second, optionals > 0);
			if (arg == null)
				return null;
			currentArguments.add(arg);
			
			if (arg.isOptional() && optionals == 0) {
				pattern.append('[');
				optionals++;
			}
			pattern.append("%" + (arg.isOptional() ? "-" : "") + Utils.toEnglishPlural(c.getCodeName(), p.second) + "%");
		}
		
		pattern.append(escape(arguments.substring(lastEnd)));
		optionals += StringUtils.count(arguments, '[', lastEnd);
		optionals -= StringUtils.count(arguments, ']', lastEnd);
		for (int i = 0; i < optionals; i++)
			pattern.append(']');
		
		String desc = "/" + command + " ";
		final boolean wasLocal = Language.setUseLocal(true); // use localized class names in desc
		try {
			desc += StringUtils.replaceAll(pattern, "(?<!\\\\)%-?(.+?)%", new Callback<String, Matcher>() {
				@Override
				public String run(final Matcher m) {
					final Pair<String, Boolean> p = Utils.getEnglishPlural(m.group(1));
					final String s = p.first;
					return "<" + Classes.getClassInfo(s).getName().toString(p.second) + ">";
				}
			});
		} finally {
			Language.setUseLocal(wasLocal);
		}
		desc = unescape(desc);
		desc = desc.trim();
		
		node.convertToEntries(0);
		commandStructure.validate(node);
		if (!(node.get("trigger") instanceof SectionNode))
			return null;
		
		final String usage = ScriptLoader.replaceOptions(node.get("usage", desc));
		final String description = ScriptLoader.replaceOptions(node.get("description", ""));
		List<String> aliases = Arrays.asList(ScriptLoader.replaceOptions(node.get("aliases", "")).split("\\s*,\\s*/?"));
		if (aliases.get(0).startsWith("/"))
			aliases.set(0, aliases.get(0).substring(1));
		else if (aliases.get(0).isEmpty())
			aliases = new ArrayList<String>(0);
		final String permission = ScriptLoader.replaceOptions(node.get("permission", null));
		final String permissionMessage = ScriptLoader.replaceOptions(node.get("permission message", null));
		final SectionNode trigger = (SectionNode) node.get("trigger");
		final String[] by = ScriptLoader.replaceOptions(node.get("executable by", "console,players")).split("\\s*,\\s*|\\s+(and|or)\\s+");
		int executableBy = 0;
		for (final String b : by) {
			if (b.equalsIgnoreCase("console") || b.equalsIgnoreCase("the console")) {
				executableBy |= ScriptCommand.CONSOLE;
			} else if (b.equalsIgnoreCase("players") || b.equalsIgnoreCase("player")) {
				executableBy |= ScriptCommand.PLAYERS;
			} else {
				Skript.warning("'executable by' should be either be 'players', 'console', or both, but found '" + b + "'");
			}
		}
		
		if (permissionMessage != null && permission == null) {
			Skript.warning("command /" + command + " has a permission message set, but not a permission");
		}
		
		if (Skript.debug() || node.debug())
			Skript.debug("command " + desc + ":");
		
		final ScriptCommand c = new ScriptCommand(node.getConfig().getFile(), command, pattern.toString(), currentArguments, description, usage, aliases, permission, permissionMessage, executableBy, ScriptLoader.loadItems(trigger));
		registerCommand(c);
		
		if (Skript.logVeryHigh() && !Skript.debug())
			Skript.info("registered command " + desc);
		currentArguments = null;
		return c;
	}
	
//	public static boolean skriptCommandExists(final String command) {
//		final ScriptCommand c = commands.get(command);
//		return c != null && c.getName().equals(command);
//	}
	
	public static void registerCommand(final ScriptCommand command) {
		if (commandMap != null)
			command.register(commandMap, cmKnownCommands, cmAliases);
		commands.put(command.getLabel(), command);
		for (final String alias : command.getActiveAliases()) {
			commands.put(alias.toLowerCase(), command);
		}
		command.registerHelp();
	}
	
	public static int unregisterCommands(final File script) {
		int numCommands = 0;
		final Iterator<ScriptCommand> commandsIter = commands.values().iterator();
		while (commandsIter.hasNext()) {
			final ScriptCommand c = commandsIter.next();
			if (c.getScript().equals(script)) {
				numCommands++;
				c.unregisterHelp();
				if (commandMap != null)
					c.unregister(commandMap, cmKnownCommands, cmAliases);
				commandsIter.remove();
			}
		}
		return numCommands;
	}
	
	private static boolean registeredListeners = false;
	
	public final static void registerListeners() {
		if (!registeredListeners) {
			Bukkit.getPluginManager().registerEvents(commandListener, Skript.getInstance());
			Bukkit.getPluginManager().registerEvents(post1_3chatListener != null ? post1_3chatListener : pre1_3chatListener, Skript.getInstance());
			registeredListeners = true;
		}
	}
	
	public final static void clearCommands() {
		if (commandMap != null) {
			for (final ScriptCommand c : commands.values())
				c.unregister(commandMap, cmKnownCommands, cmAliases);
		}
		for (final ScriptCommand c : commands.values()) {
			c.unregisterHelp();
		}
		commands.clear();
	}
	
	/**
	 * copied from CraftBukkit (org.bukkit.craftbukkit.help.CommandAliasHelpTopic)
	 */
	public final static class CommandAliasHelpTopic extends HelpTopic {
		
		private final String aliasFor;
		private final HelpMap helpMap;
		
		public CommandAliasHelpTopic(final String alias, final String aliasFor, final HelpMap helpMap) {
			this.aliasFor = aliasFor.startsWith("/") ? aliasFor : "/" + aliasFor;
			this.helpMap = helpMap;
			name = alias.startsWith("/") ? alias : "/" + alias;
			Validate.isTrue(!name.equals(this.aliasFor), "Command " + name + " cannot be alias for itself");
			shortText = ChatColor.YELLOW + "Alias for " + ChatColor.WHITE + this.aliasFor;
		}
		
		@Override
		public String getFullText(final CommandSender forWho) {
			final StringBuilder sb = new StringBuilder(shortText);
			final HelpTopic aliasForTopic = helpMap.getHelpTopic(aliasFor);
			if (aliasForTopic != null) {
				sb.append("\n");
				sb.append(aliasForTopic.getFullText(forWho));
			}
			return sb.toString();
		}
		
		@Override
		public boolean canSee(final CommandSender commandSender) {
			if (amendedPermission == null) {
				final HelpTopic aliasForTopic = helpMap.getHelpTopic(aliasFor);
				if (aliasForTopic != null) {
					return aliasForTopic.canSee(commandSender);
				} else {
					return false;
				}
			} else {
				return commandSender.hasPermission(amendedPermission);
			}
		}
	}
	
}
