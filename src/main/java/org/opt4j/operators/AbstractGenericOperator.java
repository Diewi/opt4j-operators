/*******************************************************************************
 * Copyright (c) 2014 Opt4J
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
 

package org.opt4j.operators;

import static com.google.common.collect.MultimapBuilder.hashKeys;
import static com.google.common.collect.MultimapBuilder.treeKeys;

import java.lang.reflect.Type;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.opt4j.core.Genotype;
import org.opt4j.core.Individual;
import org.opt4j.core.genotype.CompositeGenotype;
import org.opt4j.core.optimizer.IncompatibilityException;
import org.opt4j.core.optimizer.Operator;
import org.opt4j.core.start.Opt4JTask;
import org.opt4j.core.start.Parameters;
import org.opt4j.operators.selection.IOperatorSelector;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;

/**
 * Superclass for {@link GenericOperator}s.
 * 
 * @author lukasiewycz
 * 
 * @param <O>
 *            The specified {@link Operator}.
 * @param <Q>
 *            The specified {@link Operator} with a wildcard (?).
 */
public abstract class AbstractGenericOperator<O extends Operator<?>, Q extends Operator<?>> implements
		GenericOperator<O> {

	/**
	 * Comparator for a specific order: Superclasses always are sorted after
	 * subclasses.
	 * 
	 * @author lukasiewycz
	 * 
	 */
	protected static class ClassComparator implements Comparator<Class<? extends Genotype>> {

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(Class<? extends Genotype> arg0, Class<? extends Genotype> arg1) {
			if (arg0.equals(arg1)) {
				return 0;
			} else if (arg0.isAssignableFrom(arg1)) {
				// arg0 is superclass of arg1
				return 1;
			} else if (arg1.isAssignableFrom(arg0)) {
				// arg1 is superclass of arg0
				return -1;
			} else {
				return arg0.getCanonicalName().compareTo(arg1.getCanonicalName());
			}
		}

	}

	protected Map<SimpleEntry<Class<? extends Genotype>, Class<? extends Operator<?>>>, IOperatorSelector>
			operatorSelectors;
	protected Multimap<Class<? extends Genotype>, O> classOperators =
			treeKeys(new ClassComparator()).arrayListValues().build();
	protected Multimap<OperatorPredicate, O> genericOperators =
			hashKeys().arrayListValues().build();

	protected List<Class<? extends Q>> cldef = new ArrayList<Class<? extends Q>>();

	/**
	 * Constructs an {@link AbstractGenericOperator} class with the given
	 * clazzes of default operators.
	 * 
	 * @param clazzes
	 *            the default operators
	 */
	public AbstractGenericOperator(Class<? extends Q>... clazzes) {
		for (Class<? extends Q> cl : clazzes) {
			cldef.add(cl);
		}
	}

	/**
	 * Inject and organize the operators.
	 * 
	 * @param holder
	 *            the operator holder
	 */
	@SuppressWarnings("unchecked")
	@Inject
	protected synchronized void inject(OperatorHolder<Q> holder) {
		if (classOperators.isEmpty()) {
			classOperators.put(CompositeGenotype.class, null);
			holder.add(cldef);

			for (Entry<OperatorPredicate, Collection<Q>> entry : holder.getMap().asMap().entrySet()) {
				entry.getValue().forEach( v -> addOperator(entry.getKey(), (O)v));
			}
		}
		this.operatorSelectors = holder.getSelectors();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opt4j.operator.GenericOperator#addOperator(org.opt4j.operator.
	 * AbstractGenericOperator.OperatorPredicate,
	 * org.opt4j.core.optimizer.Operator)
	 */
	@Override
	public void addOperator(OperatorPredicate predicate, O operator) {
		if (predicate instanceof OperatorClassPredicate) {
			Class<? extends Genotype> clazz = ((OperatorClassPredicate) predicate).getClazz();
			classOperators.put(clazz, operator);
		} else {
			genericOperators.put(predicate, operator);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opt4j.operator.GenericOperator#addOperatorSelectororg.opt4j.core.problem
	 * .Genotype, org.opt4j.operator.selection.IOperatorSelector)
	 */
	@Override
	public void addOperatorSelector(SimpleEntry<Class<? extends Genotype>, Class<? extends Operator<?>>>
			selectorKey, IOperatorSelector selector) {
		operatorSelectors.put(selectorKey, selector);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.opt4j.operator.GenericOperator#getOperator(org.opt4j.core.problem
	 * .Genotype)
	 */
	@Override
	public O getOperator(Genotype genotype) {
		// Exit early for CompositeGenotypes: Defining operators on them is invalid.
		if(genotype instanceof CompositeGenotype) {
			return null;
		}
		
		List<O> applicableOperators = new ArrayList<>();
		if (classOperators.containsKey(genotype.getClass())) {
			applicableOperators.addAll(classOperators.get(genotype.getClass()));
		} else {
			// Search for a predicate that satisfies the genotype.
			for (Entry<OperatorPredicate, O> predicate : genericOperators.entries()) {
				if (predicate.getKey().evaluate(genotype)) {
					applicableOperators.add(predicate.getValue());
				}
			}

			// Searches for a superclass that is registered as an operator.
			Collection<Entry<Class<? extends Genotype>, O>> newOperatorsDefs = null;
			for (Entry<Class<? extends Genotype>, O> entry : classOperators.entries()) {
				if(entry.getValue() == null) {
					continue;
				}
				Class<? extends Genotype> c = entry.getKey();
				if (c.isAssignableFrom(genotype.getClass())) {
					if(newOperatorsDefs == null) {
						newOperatorsDefs = new ArrayList<>();
					}
					O operator = entry.getValue();
					SimpleEntry<Class<? extends Genotype>, O> newEntry =
							new SimpleEntry<>(genotype.getClass(), operator);
					newOperatorsDefs.add(newEntry);
					applicableOperators.add(operator);
				}
			}
			newOperatorsDefs.forEach(e -> classOperators.put(e.getKey(), e.getValue()));
		}
		if(applicableOperators.isEmpty()) {
			throw new IncompatibilityException("No handler found for " + genotype.getClass() +
					" in " + this.getClass());
		}
		
		return selectApplicableOperator(applicableOperators, genotype);
	}
	
	/**
	 * Returns an {@link Operator} from the given list of {@link Operator}s that are applicable to
	 * the given {@link Genotype}. If multiple {@link Operator}s of the same type, such as
	 * mutation, an {@link IOperatorSelector} must be bound that defines a strategy to select one
	 * operator per round and {@link Individual}.
	 * 
	 * The selection logic is as follows:
	 * * Get the registered {@link IOperatorSelector} if available.
	 * * If no selector is defined and only one {@link Operator} is applicable select this
	 *   {@link Operator}.
	 * * If multiple {@link Operator}s are given, ensure the existence of an
	 *   {@link IOperatorSelector} or throw an {@link IncompatibilityException}.
	 * * Return the {@link Operator} selected by the registered {@link IOperatorSelector}.
	 */
	private O selectApplicableOperator(List<O> applicableOperators, Genotype genotype) {
		Class<? extends Operator<?>> operatorType = applicableOperators.get(0).getOperatorType();
		SimpleEntry<Class<? extends Genotype>, Class<? extends Operator<?>>> selectorKey =
				new SimpleEntry<>(genotype.getClass(), operatorType);
		IOperatorSelector operatorSelector = operatorSelectors.get(selectorKey);
		// Do not require an IOperatorSelector for 1:1 associations of Genotypes and Operators.
		if(operatorSelector == null && applicableOperators.size() == 1) {
			return applicableOperators.get(0);
		}
		if(operatorSelector == null) {
			throw new IncompatibilityException("Multiple Operators of the same kind defined for " +
					genotype.getClass() + " but no " + IOperatorSelector.class.getSimpleName() + 
					"is provided (required).");
		}
		return operatorSelector.select(applicableOperators, genotype);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opt4j.operator.common.GenericOperator#getHandlers()
	 */
	@Override
	public Collection<O> getOperators() {
		Set<O> set = new HashSet<O>();
		set.addAll(classOperators.values());
		set.addAll(genericOperators.values());
		return set;
	}

	/**
	 * Returns the target {@link Genotype} for an operator based on the
	 * {@link Apply} annotation.
	 * 
	 * @param <O>
	 *            the type of operator
	 * @param operator
	 *            the operator
	 * @return the target genotype
	 */
	protected static <O> Class<? extends Genotype> getTarget(O operator) {
		Apply apply = operator.getClass().getAnnotation(Apply.class);

		if (apply != null) {
			return apply.value();
		}

		Type type = Parameters.getType(Operator.class, operator, "G");
		if (type != null) {
			Class<? extends Genotype> target = Parameters.getClass(type).asSubclass(Genotype.class);
			return target;
		}

		throw new IllegalArgumentException("No target specified for the operator " + operator.getClass().getName()
				+ ". Either parameterize the Operator or use the " + Apply.class.getName()
				+ " annotation to specify a target.");
	}

	protected static class OperatorHolder<P> {

		@Inject(optional = true)
		protected Map<OperatorPredicate, P> map;
		
		@Inject(optional = true)
		protected Map<OperatorPredicate, Set<P>> multimap;
		
		@Inject(optional = true)
		Map<SimpleEntry<Class<? extends Genotype>, Class<? extends Operator<?>>>,
			IOperatorSelector> operatorSelectors;
		
		@Inject(optional = true)
		SimpleEntry<Class<? extends Genotype>, Class<? extends Operator<?>>> test;

		@Inject
		protected Opt4JTask opt4JTask;

		protected Collection<Class<? extends P>> clazzes = new ArrayList<Class<? extends P>>();

		public void add(Collection<Class<? extends P>> clazzes) {
			this.clazzes.addAll(clazzes);
		}

		public Multimap<OperatorPredicate, P> getMap() {
			Multimap<OperatorPredicate, P> multimap = HashMultimap.create();
			if(this.multimap != null) {
				for(Entry<OperatorPredicate, Set<P>> operators : this.multimap.entrySet()) {
					multimap.putAll(operators.getKey(), operators.getValue());
				}
			}
			for(Entry<OperatorPredicate, P> entry : this.map.entrySet()) {
				if(!multimap.containsKey(entry.getKey())) {
					multimap.put(entry.getKey(), entry.getValue());
				}
			}

			for (Class<? extends P> clazz : clazzes) {
				P p = opt4JTask.getInstance(clazz);
				multimap.put(new OperatorClassPredicate(getTarget((Operator<?>) p)), p);
			}
			
			Set<OperatorPredicate> replaceSet = new HashSet<>();
			for (Entry<OperatorPredicate, P> entry : multimap.entries()) {
				OperatorPredicate predicate = entry.getKey();
				if (predicate instanceof OperatorVoidPredicate) {
					predicate = new OperatorClassPredicate(getTarget((Operator<?>)entry.getValue()));
					replaceSet.add(predicate);
				}
			}
			
			for(OperatorPredicate key : replaceSet) {
				Collection<P> operators = multimap.get(key);
				multimap.removeAll(key);
				multimap.putAll(key, operators);
			}

			return multimap;
		}
		
		public Map<SimpleEntry<Class<? extends Genotype>, Class<? extends Operator<?>>>, IOperatorSelector> getSelectors() {
			if(operatorSelectors != null) {
				return operatorSelectors;
			}
			return Collections.emptyMap();
		}
	}

	/**
	 * The {@link OperatorPredicate} interface.
	 * 
	 * @author lukasiewycz
	 * 
	 */
	public interface OperatorPredicate {

		/**
		 * Checks whether a {@link Genotype} satisfies the predicate.
		 * 
		 * @param genotype
		 *            the genotype
		 * @return {@code true} if the predicate is satisfied
		 */
		public boolean evaluate(Genotype genotype);
	}

	/**
	 * The {@link OperatorVoidPredicate} interface is used as marker for
	 * {@link Operator}s for which the predicate is not explicitly defined.
	 * 
	 * @author lukasiewycz
	 * 
	 */
	public static class OperatorVoidPredicate implements OperatorPredicate {
		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.opt4j.operator.AbstractGenericOperator.OperatorPredicate#evaluate
		 * (org.opt4j.core.problem.Genotype)
		 */
		@Override
		public boolean evaluate(Genotype genotype) {
			return false;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "Predicate Void";
		}

	}

	/**
	 * The {@link OperatorClassPredicate} returns {@code true} for a given
	 * specific class.
	 * 
	 * @author lukasiewycz
	 * 
	 */
	public static class OperatorClassPredicate implements OperatorPredicate {

		protected final Class<? extends Genotype> clazz;

		/**
		 * Creates a new {@link OperatorClassPredicate} for the given
		 * {@link Genotype} class.
		 * 
		 * @param clazz
		 *            the class of the genotype
		 */
		public OperatorClassPredicate(Class<? extends Genotype> clazz) {
			this.clazz = clazz;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.opt4j.operator.AbstractGenericOperator.OperatorPredicate#evaluate
		 * (org.opt4j.core.problem.Genotype)
		 */
		@Override
		public boolean evaluate(Genotype genotype) {
			return clazz.equals(genotype.getClass());
		}

		/**
		 * Returns the genotype class for the operator.
		 * 
		 * @return the genotype class
		 */
		public Class<? extends Genotype> getClazz() {
			return clazz;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "Predicate [clazz=" + clazz.getSimpleName() + "]";
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
			return result;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			OperatorClassPredicate other = (OperatorClassPredicate) obj;
			if (clazz == null) {
				if (other.clazz != null) {
					return false;
				}
			} else if (!clazz.equals(other.clazz)) {
				return false;
			}
			return true;
		}
	}

}
