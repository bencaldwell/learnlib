/* Copyright (C) 2013-2014 TU Dortmund
 * This file is part of LearnLib, http://www.learnlib.de/.
 * 
 * LearnLib is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License version 3.0 as published by the Free Software Foundation.
 * 
 * LearnLib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with LearnLib; if not, see
 * http://www.gnu.de/documents/lgpl.en.html.
 */
package de.learnlib.oracles;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import net.automatalib.words.Word;
import de.learnlib.api.Query;

@ParametersAreNonnullByDefault
public abstract class AbstractQuery<I, O> extends Query<I, O> {

	protected final Word<I> prefix;
	protected final Word<I> suffix;
	
	public AbstractQuery(Word<I> prefix, Word<I> suffix) {
		this.prefix = prefix;
		this.suffix = suffix;
	}
	
	public AbstractQuery(Word<I> queryWord) {
		this(Word.<I>epsilon(), queryWord);
	}
	
	public AbstractQuery(Query<I,?> query) {
		this(query.getPrefix(), query.getSuffix());
	}

	@Override
	@Nonnull
	public Word<I> getPrefix() {
		return prefix;
	}

	@Override
	@Nonnull
	public Word<I> getSuffix() {
		return suffix;
	}

	/**
	 * Returns the string representation of this query, including a possible answer.
	 * This method should be used by classes extending {@link AbstractQuery} for their
	 * toString method to ensure output consistency.
	 *
	 * @return A string of the form "Query[<prefix>|<suffix> / <answer>]". If the query
	 * has not been answered yet, <answer> will be null.
	 */
	public String toStringWithAnswer(O answer) {
		return "Query[" + prefix + '|' + suffix + " / " + answer + ']';
	}
}
