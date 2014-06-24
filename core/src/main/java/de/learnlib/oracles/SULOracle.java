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
 * <http://www.gnu.de/documents/lgpl.en.html>.
 */
package de.learnlib.oracles;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import de.learnlib.api.MembershipOracle.MealyMembershipOracle;
import de.learnlib.api.SUL;
import de.learnlib.api.SULException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;

/**
 * A wrapper around a system under learning (SUL).
 * 
 * @author falkhowar
 */
@ParametersAreNonnullByDefault
public class SULOracle<I, O> extends AbstractSingleQueryOracle<I, Word<O>> implements MealyMembershipOracle<I,O> {

	private final SUL<I, O> sul;
        private final static int RETRY_ATTEMPTS = 20;
        private final static Logger LOGGER = Logger.getGlobal();
        
	public SULOracle(SUL<I, O> sul) {
		this.sul = sul;
	}

	@Override
	@Nonnull
	public Word<O> answerQuery(Word<I> prefix, Word<I> suffix) throws SULException {
            
            // if the step fails retry up to a limit
            int retries = 0;
            while (retries < RETRY_ATTEMPTS) {
                try {    
                    sul.pre();
                    // Prefix: Execute symbols, don't record output
                    for(I sym : prefix) {
                            sul.step(sym);
                    }

                    // Suffix: Execute symbols, outputs constitute output word
                    WordBuilder<O> wb = new WordBuilder<>(suffix.length());
                    for(I sym : suffix) {
                            wb.add(sul.step(sym));
                    }
                    sul.post();
                    return wb.toWord();
                } catch (IOException e) {
                    try {
                        sul.post();
                    } catch (IOException ex) {
                        Logger.getLogger(SULOracle.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    retries++;
                }
            }
            // if we get to here we've tried too many times with this query
            LOGGER.severe("SUL failed to answer a query");
            System.exit(-1);
            return null;
	}

}
