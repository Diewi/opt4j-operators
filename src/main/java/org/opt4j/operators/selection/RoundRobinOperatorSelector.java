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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opt4j.core.Genotype;
import org.opt4j.core.optimizer.Operator;

/**
 * Selector that selects one {@link Operator} out of the given {@link Operator}s by a round robin
 * principle. The list of passed applicable {@link Operator}s is assumed to have a fixed order:
 * the selection index is bound to the {@link Genotype} to be modified.
 * 
 * @author diewald
 */
public class RoundRobinOperatorSelector implements IOperatorSelector {
	
	/** Remembers the selection index bound to {@link Genotype}s. */
	private Map<Genotype, Integer> selectionHolder = new HashMap<>();

	/* (non-Javadoc)
	 * @see org.opt4j.operators.selection.IOperatorSelector#select(java.util.List, org.opt4j.core.Genotype)
	 */
	@Override
	public <O extends Operator<?>> O select(List<O> applicableOperators, Genotype genotype) {
		if(applicableOperators.isEmpty()) {
			return null;
		}
		
		int selIdx = selectionHolder.get(genotype);
		selIdx = (selIdx < applicableOperators.size()) ? selIdx : 0;
		selectionHolder.put(genotype, selIdx);
		
		return applicableOperators.get(selIdx);
	}
}
