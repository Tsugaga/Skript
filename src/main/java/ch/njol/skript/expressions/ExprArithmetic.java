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

package ch.njol.skript.expressions;

import java.lang.reflect.Array;

import org.bukkit.event.Event;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Patterns;
import ch.njol.util.Kleenean;

/**
 * @author Peter Güttinger
 */
@SuppressWarnings("serial")
@Name("Arithmetic")
@Description("Arithmetic expressions, e.g. 1+2, (2 - health of player)/3, etc.")
@Examples({"set the player's health to 10 - the player's health",
		"loop (argument + 2)/5 times:",
		"	message \"Two useless numbers: %loop-num*2 - 5%, %2^loop-num - 1%\"",
		"message \"You have %health of player * 2% half hearts of HP!\""})
@Since("1.4.2")
public class ExprArithmetic extends SimpleExpression<Number> {
	
	private static enum Operator {
		PLUS('+') {
			@Override
			public Number calculate(final Number n1, final Number n2, final boolean integer) {
				if (integer)
					return Integer.valueOf(n1.intValue() + n2.intValue());
				return Double.valueOf(n1.doubleValue() + n2.doubleValue());
			}
		},
		MINUS('-') {
			@Override
			public Number calculate(final Number n1, final Number n2, final boolean integer) {
				if (integer)
					return Integer.valueOf(n1.intValue() - n2.intValue());
				return Double.valueOf(n1.doubleValue() - n2.doubleValue());
			}
		},
		MULT('*') {
			@Override
			public Number calculate(final Number n1, final Number n2, final boolean integer) {
				if (integer)
					return Integer.valueOf(n1.intValue() * n2.intValue());
				return Double.valueOf(n1.doubleValue() * n2.doubleValue());
			}
		},
		DIV('/') {
			@Override
			public Number calculate(final Number n1, final Number n2, final boolean integer) {
				if (integer)
					return Integer.valueOf(n1.intValue() / n2.intValue());
				return Double.valueOf(n1.doubleValue() / n2.doubleValue());
			}
		},
		EXP('^') {
			@Override
			public Number calculate(final Number n1, final Number n2, final boolean integer) {
				if (integer)
					return Integer.valueOf((int) Math.pow(n1.intValue(), n2.intValue()));
				return Double.valueOf(Math.pow(n1.doubleValue(), n2.doubleValue()));
			}
		};
		
		public final char sign;
		
		private Operator(final char sign) {
			this.sign = sign;
		}
		
		public abstract Number calculate(Number n1, Number n2, boolean integer);
		
		@Override
		public String toString() {
			return "" + sign;
		}
	}
	
	private final static Patterns<Operator> patterns = new Patterns<Operator>(new Object[][] {
			
			{"%number%[ ]+[ ]%number%", Operator.PLUS},
			{"%number%[ ]-[ ]%number%", Operator.MINUS},
			
			{"%number%[ ]*[ ]%number%", Operator.MULT},
			{"%number%[ ]/[ ]%number%", Operator.DIV},
			
			{"%number%[ ]^[ ]%number%", Operator.EXP},
			
			// more general in SkriptParser now
//			{"\\(%number%\\)", null}
	
	});
	
	static {
		Skript.registerExpression(ExprArithmetic.class, Number.class, ExpressionType.PATTERN_MATCHES_EVERYTHING, patterns.getPatterns());
	}
	
	private Expression<? extends Number> first, second;
	private Operator op;
	
	private Class<? extends Number> returnType;
	private boolean integer;
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean init(final Expression<?>[] exprs, final int matchedPattern, final Kleenean isDelayed, final ParseResult parseResult) {
		first = (Expression<? extends Number>) exprs[0];
		second = exprs.length == 1 ? null : (Expression<? extends Number>) exprs[1];
		op = patterns.getInfo(matchedPattern);
		if (op == null) {
			returnType = first.getReturnType();
		} else if (op == Operator.DIV || op == Operator.EXP) {
			returnType = Double.class;
		} else {
			final Class<?> f = first.getReturnType(), s = second.getReturnType();
			final Class<?>[] integers = {Long.class, Integer.class, Short.class, Byte.class};
			boolean firstIsInt = false, secondIsInt = false;
			for (final Class<?> i : integers) {
				firstIsInt |= i.isAssignableFrom(f);
				secondIsInt |= i.isAssignableFrom(s);
			}
			if (firstIsInt && secondIsInt)
				returnType = Integer.class;
			else
				returnType = Double.class;
		}
		integer = returnType == Integer.class;
		return true;
	}
	
	@Override
	protected Number[] get(final Event e) {
		if (op == null)
			return first.getArray(e);
		final Number[] one = (Number[]) Array.newInstance(returnType, 1);
		Number n1 = first.getSingle(e), n2 = second.getSingle(e);
		if (n1 == null)
			n1 = Integer.valueOf(0);
		if (n2 == null)
			n2 = Integer.valueOf(0);
		one[0] = op.calculate(n1, n2, integer);
		return one;
	}
	
	@Override
	public Class<? extends Number> getReturnType() {
		return returnType;
	}
	
	@Override
	public boolean isSingle() {
		return true;
	}
	
	@Override
	public String toString(final Event e, final boolean debug) {
		if (op == null) {
			return "(" + first.toString(e, debug) + ")";
		}
		return first.toString(e, debug) + " " + op + " " + second.toString(e, debug);
	}
	
	@Override
	public Expression<? extends Number> simplify() {
		if (op == null)
			return first;
		return this;
	}
	
}
