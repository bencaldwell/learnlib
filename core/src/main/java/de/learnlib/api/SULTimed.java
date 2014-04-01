/* Copyright (C) 2013 TU Dortmund
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
package de.learnlib.api;

import java.io.IOException;

/**
 * Interface for a system under learning (SUL) that can make single steps.
 *
 * @param <I> input symbols
 * @param <O> output symbols
 *
 * @author falkhowar
 */
public interface SULTimed<I, O> extends SUL<I,O> {

    /**
     * make one step on the SUL.
     *
     * @param input the input symbol to the SUL
     * @param stepClockLimit the clock limit to wait for an output symbol this step
     * @return output of SUL
     */
    O step(I input, long stepClockLimit) throws IOException;
    
    long getClockLimit();

}
