/* Copyright (C) 2014 TU Dortmund
 * This file is part of AutomataLib, http://www.automatalib.net/.
 * 
 * AutomataLib is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License version 3.0 as published by the Free Software Foundation.
 * 
 * AutomataLib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with AutomataLib; if not, see
 * <http://www.gnu.de/documents/lgpl.en.html>.
 */
package de.learnlib.testsupport.queries;

import net.automatalib.words.Word;
import de.learnlib.api.Query;
import de.learnlib.oracles.AbstractQuery;

public class NoopQuery<I, O> extends AbstractQuery<I, O> {

	public NoopQuery(Query<I, ?> query) {
		super(query);
	}

	public NoopQuery(Word<I> prefix, Word<I> suffix) {
		super(prefix, suffix);
	}

	public NoopQuery(Word<I> queryWord) {
		super(queryWord);
	}

	@Override
	public void answer(O output) {
	}

	/**
	 * @see de.learnlib.oracles.AbstractQuery#toStringWithAnswer(Object)
	 */
	@Override
	public String toString() {
		return toStringWithAnswer(null);
	}

}
