package ch.njol.skript.util;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import ch.njol.skript.Skript;

/**
 * @author Peter Güttinger
 */
public abstract class Task implements Runnable {
	
	private final Plugin plugin;
	private final boolean async;
	private long period = -1;
	
	private int taskID;
	
	public Task(final Plugin plugin, final long delay, final long period) {
		this(plugin, delay, period, false);
	}
	
	public Task(final Plugin plugin, final long delay, final long period, final boolean async) {
		this.plugin = plugin;
		this.period = period;
		this.async = async;
		schedule(delay);
	}
	
	public Task(final Plugin plugin, final long delay) {
		this(plugin, delay, false);
	}
	
	public Task(final Plugin plugin, final long delay, final boolean async) {
		this.plugin = plugin;
		this.async = async;
		schedule(delay);
	}
	
	@SuppressWarnings("deprecation")
	private void schedule(final long delay) {
		if (period == -1) {
			if (async) {
				taskID = Skript.isRunningMinecraft(1, 4, 6) ?
						Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, this, delay).getTaskId() :
						Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, this, delay);
			} else {
				taskID = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, this, delay);
			}
		} else {
			if (async) {
				taskID = Skript.isRunningMinecraft(1, 4, 6) ?
						Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this, delay, period).getTaskId() :
						Bukkit.getScheduler().scheduleAsyncRepeatingTask(plugin, this, delay, period);
			} else {
				taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this, delay, period);
			}
		}
	}
	
	/**
	 * @return Whether this task is still running, i.e. whether it will run later or is currently running.
	 */
	public final boolean isAlive() {
		return Bukkit.getScheduler().isQueued(taskID) || Bukkit.getScheduler().isCurrentlyRunning(taskID);
	}
	
	/**
	 * Cancels this task.
	 */
	public final void cancel() {
		Bukkit.getScheduler().cancelTask(taskID);
	}
	
	/**
	 * Re-schedules the task to run next after the given delay.
	 * 
	 * @param delay
	 */
	public void setNextExecution(final long delay) {
		assert delay >= 0;
		Bukkit.getScheduler().cancelTask(taskID);
		schedule(delay);
	}
	
	/**
	 * Sets the period of this task. This will re-schedule the task to be run next after the given period if the task is still running.
	 * 
	 * @param period Period in ticks or -1 to cancel the task and make it non-repeating
	 */
	public void setPeriod(final long period) {
		assert period == -1 || period > 0;
		if (period == this.period)
			return;
		this.period = period;
		if (isAlive()) {
			Bukkit.getScheduler().cancelTask(taskID);
			if (period != -1)
				schedule(period);
		}
	}
	
	/**
	 * Calls a method on Bukkit's main thread.
	 * <p>
	 * Hint: Use a Callable&lt;Void&gt; to make a task which blocks your current thread until it is completed.
	 * 
	 * @param c The method
	 * @return What the method returned or null if it threw an error or was stopped (usually due to the server shutting down)
	 */
	public final static <T> T callSync(final Callable<T> c) {
		if (Bukkit.isPrimaryThread()) {
			try {
				return c.call();
			} catch (final Exception e) {
				Skript.exception(e);
			}
		}
		final Future<T> f = Bukkit.getScheduler().callSyncMethod(Skript.getInstance(), c);
		try {
			while (true) {
				try {
					return f.get();
				} catch (final InterruptedException e) {}
			}
		} catch (final ExecutionException e) {
			Skript.exception(e);
		} catch (final CancellationException e) {} catch (final ThreadDeath e) {}// server shutting down
		return null;
	}
	
}
