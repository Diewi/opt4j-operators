/*******************************************************************************
 * Copyright (c) 2018 Opt4J
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package org.opt4j.operators.selection;

import java.util.List;
import java.util.Random;

import org.opt4j.core.Genotype;
import org.opt4j.core.optimizer.Operator;

import com.google.inject.Inject;

/**
 * Selection strategy that randomly selects one {@link Operator} out of the given {@link Operator}s.
 * 
 * @author diewald
 */
public class RandomOperatorSelector implements IOperatorSelector {
	
	/** Utilized random number generator. */
	private Random randomGenerator;
	
	/**
	 * Constructor.
	 * 
	 * @param randomGenerator {@link Random} generator to use for the selection.
	 */
	@Inject
	public RandomOperatorSelector(Random randomGenerator) {
		this.randomGenerator = randomGenerator;
	}

	/* (non-Javadoc)
	 * @see org.opt4j.operators.selection.IOperatorSelector#select(java.util.List)
	 */
	@Override
	public <O extends Operator<?>> O select(List<O> applicableOperators, Genotype genotype) {
		int selIdx = randomGenerator.nextInt(applicableOperators.size());
		return applicableOperators.get(selIdx);
	}
}
