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

package ch.njol.skript.lang.util;

import java.lang.reflect.Array;
import java.util.Iterator;

import org.bukkit.event.Event;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Converter;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Utils;
import ch.njol.util.Checker;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import ch.njol.util.coll.iterator.ArrayIterator;

/**
 * An implementation of the {@link Expression} interface. You should usually extend this class to make a new expression.
 * 
 * @see Skript#registerExpression(Class, Class, ExpressionType, String...)
 * @author Peter Güttinger
 */
@SuppressWarnings("serial")
public abstract class SimpleExpression<T> implements Expression<T> {
	
	private int time = 0;
	
	protected SimpleExpression() {}
	
	@Override
	public final T getSingle(final Event e) {
		final T[] all = getArray(e);
		if (all.length == 0)
			return null;
		if (all.length > 1)
			throw new SkriptAPIException("Call to getSingle() on a non-single expression");
		return all[0];
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * Unlike {@link #get(Event)} you have to make sure that the this method's returned array is neither null nor contains null elements.
	 */
	@Override
	public T[] getAll(final Event e) {
		final T[] all = get(e);
		if (all == null)
			return (T[]) Array.newInstance(getReturnType(), 0);
		if (all.length == 0)
			return all;
		int numNonNull = 0;
		for (final T t : all)
			if (t != null)
				numNonNull++;
		if (numNonNull == all.length)
			return all;
		final T[] r = (T[]) Array.newInstance(getReturnType(), numNonNull);
		int i = 0;
		for (final T t : all)
			if (t != null)
				r[i++] = t;
		return r;
	}
	
	@Override
	public final T[] getArray(final Event e) {
		final T[] all = get(e);
		if (all == null)
			return (T[]) Array.newInstance(getReturnType(), 0);
		if (all.length == 0)
			return all;
		
		int numNonNull = 0;
		for (final T t : all)
			if (t != null)
				numNonNull++;
		
		if (!getAnd()) {
			if (all.length == 1 && all[0] != null)
				return all;
			int rand = Utils.random(0, numNonNull);
			final T[] one = (T[]) Array.newInstance(getReturnType(), 1);
			for (final T t : all) {
				if (t != null) {
					if (rand == 0) {
						one[0] = t;
						return one;
					}
					rand--;
				}
			}
			assert false;
		}
		
		if (numNonNull == all.length)
			return all;
		final T[] r = (T[]) Array.newInstance(getReturnType(), numNonNull);
		int i = 0;
		for (final T t : all)
			if (t != null)
				r[i++] = t;
		return r;
	}
	
	/**
	 * This is the internal method to get an expression's values.<br>
	 * To get the expression's value from the outside use {@link #getSingle(Event)} or {@link #getArray(Event)}.
	 * 
	 * @param e The event
	 * @return An array of values for this event. May not contain nulls.
	 */
	protected abstract T[] get(Event e);
	
	@Override
	public final boolean check(final Event e, final Checker<? super T> c) {
		return check(e, c, false);
	}
	
	@Override
	public final boolean check(final Event e, final Checker<? super T> c, final boolean negated) {
		return check(get(e), c, negated, getAnd());
	}
	
	public final static <T> boolean check(final T[] all, final Checker<? super T> c, final boolean invert, final boolean and) {
		if (all == null)
			return false;
		boolean hasElement = false;
		for (final T t : all) {
			if (t == null)
				continue;
			hasElement = true;
			final boolean b = invert ^ c.check(t);
			if (and && !b)
				return false;
			if (!and && b)
				return true;
		}
		if (!hasElement)
			return false;
		return and;
	}
	
	/**
	 * Converts this expression to another type. Unless the expression is special, the default implementation is sufficient.
	 * <p>
	 * This method is guaranteed to never being called with a supertype of the return type of this expression, or the return type itself.
	 * 
	 * @param to The desired return type of the returned expression
	 * @return Expression with the desired return type or null if it can't be converted to the given type
	 * @see Expression#getConvertedExpression(Class...)
	 * @see ConvertedExpression#newInstance(Expression, Class...)
	 * @see Converter
	 */
	protected <R> ConvertedExpression<T, ? extends R> getConvertedExpr(final Class<R>... to) {
		assert !CollectionUtils.containsSuperclass(to, getReturnType());
		return ConvertedExpression.newInstance(this, to);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public final <R> Expression<? extends R> getConvertedExpression(final Class<R>... to) {
		if (CollectionUtils.containsSuperclass(to, getReturnType()))
			return (Expression<? extends R>) this;
		return this.getConvertedExpr(to);
	}
	
	private ClassInfo<?> returnTypeInfo;
	
	@Override
	public Class<?>[] acceptChange(final ChangeMode mode) {
		if (returnTypeInfo == null)
			returnTypeInfo = Classes.getSuperClassInfo(getReturnType());
		if (returnTypeInfo.getChanger() == null)
			return null;
		return returnTypeInfo.getChanger().acceptChange(mode);
	}
	
	@Override
	public void change(final Event e, final Object[] delta, final ChangeMode mode) {
		((Changer<T>) returnTypeInfo.getChanger()).change(getArray(e), delta, mode);
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * This implementation sets the time but returns false.
	 * 
	 * @see #setTime(int, Class, Expression...)
	 * @see #setTime(int, Expression, Class...)
	 */
	@Override
	public boolean setTime(final int time) {
		if (ScriptLoader.hasDelayBefore == Kleenean.TRUE && time != 0) {
			Skript.error("Can't use time states after the event has already passed");
			return false;
		}
		this.time = time;
		return false;
	}
	
	protected final boolean setTime(final int time, final Class<? extends Event> applicableEvent, final Expression<?>... mustbeDefaultVars) {
		if (ScriptLoader.hasDelayBefore == Kleenean.TRUE && time != 0) {
			Skript.error("Can't use time states after the event has already passed");
			return false;
		}
		if (!ScriptLoader.isCurrentEvent(applicableEvent))
			return false;
		for (final Expression<?> var : mustbeDefaultVars) {
			if (!var.isDefault()) {
				return false;
			}
		}
		this.time = time;
		return true;
	}
	
	protected final boolean setTime(final int time, final Expression<?> mustbeDefaultVar, final Class<? extends Event>... applicableEvents) {
		if (ScriptLoader.hasDelayBefore == Kleenean.TRUE && time != 0) {
			Skript.error("Can't use time states after the event has already passed");
			return false;
		}
		if (!mustbeDefaultVar.isDefault())
			return false;
		for (final Class<? extends Event> e : applicableEvents) {
			if (ScriptLoader.isCurrentEvent(e)) {
				this.time = time;
				return true;
			}
		}
		return false;
	}
	
	@Override
	public int getTime() {
		return time;
	}
	
	@Override
	public boolean isDefault() {
		return false;
	}
	
	@Override
	public boolean isLoopOf(final String s) {
		return false;
	}
	
	@Override
	public Iterator<? extends T> iterator(final Event e) {
		return new ArrayIterator<T>(getArray(e));
	}
	
	@Override
	public String toString() {
		return toString(null, false);
	}
	
	@Override
	public Expression<?> getSource() {
		return this;
	}
	
	@Override
	public Expression<? extends T> simplify() {
		return this;
	}
	
	@Override
	public boolean getAnd() {
		return true;
	}
	
}
