/**
  Copyright (C) 2011 The University of Rostock.
 
  Written by:  Niels Grewe <niels.grewe@uni-rostock.de>
  Created: 27.12.2011
  
  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  as published by the Free Software Foundation; either
  version 3 of the License, or (at your option) any later version.

  You should have received a copy of the GNU General Public
  License along with this program; see the file COPYING.
  If not, write to the Free Software Foundation,
  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 */
package de.uni_rostock.goodod.owl;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

/**
 * @author Niels Grewe <niels.grewe@uni-rostock.de>
 *
 */
public class TaxonomicDecompositionCollector implements OWLClassExpressionVisitor {

	private final OWLClassExpression sourceExpression;
	private final Set<OWLClassExpression> decompositionSet;
	private Stack<OWLClassExpression> activeStack;
	private final Map<Stack<OWLClassExpression>,Set<OWLClassExpression>> queueMap;
	private final OWLDataFactory factory;
	

	public TaxonomicDecompositionCollector(OWLClassExpression e)
	{
		sourceExpression = e;
		factory = OWLManager.getOWLDataFactory();
		queueMap = new HashMap<Stack<OWLClassExpression>,Set<OWLClassExpression>>();
		decompositionSet = new HashSet<OWLClassExpression>();
		activeStack = new Stack<OWLClassExpression>();
	}
	
	public Set<OWLClassExpression> collect()
	{
		if (sourceExpression instanceof OWLClass)
		{
			// Trying to decompose named classes is very much dull.
			return Collections.singleton(sourceExpression);
		}
		
		/*
		 * We produce a primary decomposition of the source expression to fill
		 * the queue initially.
		 */
		sourceExpression.accept(this);
		
		Map<Stack<OWLClassExpression>,Set<OWLClassExpression>> newQueue = new HashMap<Stack<OWLClassExpression>,Set<OWLClassExpression>>();
		while(0 < queueMap.size())
		{
			/*
			 * We do not iterate over the queueMap itself because the nodes we
			 * visit need to modify it.
			 */
			newQueue.putAll(queueMap);
			queueMap.clear();
			for (Map.Entry<Stack<OWLClassExpression>,Set<OWLClassExpression>> e: newQueue.entrySet() )
			{
				// Set the currently active stack:
				activeStack = e.getKey();
				for (OWLClassExpression expr : e.getValue())
				{
					if (0 == activeStack.size())
					{
						/*
						 *  If the expression is not masked by anything on the
						 *  stack, we add the expression itself. This leads to
						 *  some redundant operations, though.
						 *  E.g. if we are evaluating "ObjectIntersectionOf(A,B)",
						 *  the first queue run will add "ObjectIntersectionOf(A,B)"
						 *  to the decomposition set and schedule "A" and "B" on the
						 *  queue (which is just what our TDS algorithm prescribes).
						 *  The accept call on A will then unwind the (empty) stack
						 *  and put "A" on the decomposition set, while the call below
						 *  does just the same thing. But since we are operating on sets,
						 *  it's a no-op anyways. 
						 */
						decompositionSet.add(expr);
					}
					/*
					 *  Analyse the subexpressions at present stack depth.
					 *  Subexpression evaluation will either refill the queue
					 *  or (if they are terminal) unwind the present stack,
					 *  placing the result in the decomposition set.
					 */
					expr.accept(this);
				}
				
			}
			newQueue.clear();
		}
		decompositionSet.add(sourceExpression);
		return decompositionSet;
	}
	
	
	
	public static Set<OWLClassExpression> collect(OWLClassExpression e)
	{
		return new TaxonomicDecompositionCollector(e).collect();
	}
	
	/*
	 * Visting deals with decomposing and reassembling expressions. The
	 * following three types of entities exists.
	 * 1. Terminals (Data properties, ObjectHasValue, ObjectHasSelf, ObjectOneOf, Classes):
	 *    Trigger needed conversions and unwinding of the stack.
	 * 2. Decomposables (ObjectUnionOf, ObjectIntersectionOf):
	 *    Are decomposed and the decompositions inserted a present stack depth.
	 * 3. Stack masking (Object(All|Some)ValuesFrom, Object(Min|Exact|Max)Cardinality, ObjectComplementOf):
	 *    Are put on the stack while waiting for the entities on lower syntactical levels to be analysed.
	 */

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLClassExpressionVisitor#visit(org.semanticweb.owlapi.model.OWLClass)
	 */
	public void visit(OWLClass arg0) {
		//Terminal:
		unwind(activeStack, arg0);
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLClassExpressionVisitor#visit(org.semanticweb.owlapi.model.OWLObjectIntersectionOf)
	 */
	public void visit(OWLObjectIntersectionOf arg0) {
		Set<OWLClassExpression> existing = queueMap.get(activeStack);
		if (null == existing)
		{
			existing = new HashSet<OWLClassExpression>();
			queueMap.put(activeStack,existing);
		}
		
		existing.addAll(arg0.getOperands());
	
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLClassExpressionVisitor#visit(org.semanticweb.owlapi.model.OWLObjectUnionOf)
	 */
	public void visit(OWLObjectUnionOf arg0) {
		Set<OWLClassExpression> existing = queueMap.get(activeStack);
		if (null == existing)
		{
			existing = new HashSet<OWLClassExpression>();
			queueMap.put(activeStack, existing);
		}
		existing.addAll(arg0.getOperands());
		
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLClassExpressionVisitor#visit(org.semanticweb.owlapi.model.OWLObjectComplementOf)
	 */
	public void visit(OWLObjectComplementOf arg0) {
		queueMap.put(nextStack(arg0),new HashSet<OWLClassExpression>(Collections.singleton(arg0.getOperand())));
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLClassExpressionVisitor#visit(org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom)
	 */
	public void visit(OWLObjectSomeValuesFrom arg0) {
		queueMap.put(nextStack(arg0), new HashSet<OWLClassExpression>(Collections.singleton(arg0.getFiller())));
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLClassExpressionVisitor#visit(org.semanticweb.owlapi.model.OWLObjectAllValuesFrom)
	 */
	public void visit(OWLObjectAllValuesFrom arg0) {
		queueMap.put(nextStack(arg0), new HashSet<OWLClassExpression>(Collections.singleton(arg0.getFiller())));		
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLClassExpressionVisitor#visit(org.semanticweb.owlapi.model.OWLObjectHasValue)
	 */
	public void visit(OWLObjectHasValue arg0) {
		//Terminal:
		unwind(activeStack, arg0);
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLClassExpressionVisitor#visit(org.semanticweb.owlapi.model.OWLObjectMinCardinality)
	 */
	public void visit(OWLObjectMinCardinality arg0) {
		queueMap.put(nextStack(arg0), new HashSet<OWLClassExpression>(Collections.singleton(arg0.getFiller())));	
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLClassExpressionVisitor#visit(org.semanticweb.owlapi.model.OWLObjectExactCardinality)
	 */
	public void visit(OWLObjectExactCardinality arg0) {
		queueMap.put(nextStack(arg0), new HashSet<OWLClassExpression>(Collections.singleton(arg0.getFiller())));
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLClassExpressionVisitor#visit(org.semanticweb.owlapi.model.OWLObjectMaxCardinality)
	 */
	public void visit(OWLObjectMaxCardinality arg0) {
		queueMap.put(nextStack(arg0), new HashSet<OWLClassExpression>(Collections.singleton(arg0.getFiller())));
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLClassExpressionVisitor#visit(org.semanticweb.owlapi.model.OWLObjectHasSelf)
	 */
	public void visit(OWLObjectHasSelf arg0) {	
		//Terminal:
		unwind(activeStack, arg0);
		}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLClassExpressionVisitor#visit(org.semanticweb.owlapi.model.OWLObjectOneOf)
	 */
	public void visit(OWLObjectOneOf arg0) {
		// Terminal for each element of the power set:
		Set<OWLIndividual> individuals = arg0.getIndividuals();
		for (Set<OWLIndividual> indSet : powerSet(individuals))
		{
			if (0 < indSet.size())
			{
				unwind(activeStack, factory.getOWLObjectOneOf(indSet));
			}
		}

	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLClassExpressionVisitor#visit(org.semanticweb.owlapi.model.OWLDataSomeValuesFrom)
	 */
	public void visit(OWLDataSomeValuesFrom arg0) {
		//Terminal:
		unwind(activeStack, arg0);
		
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLClassExpressionVisitor#visit(org.semanticweb.owlapi.model.OWLDataAllValuesFrom)
	 */
	public void visit(OWLDataAllValuesFrom arg0) {
		//Terminal:
		unwind(activeStack, arg0);
		
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLClassExpressionVisitor#visit(org.semanticweb.owlapi.model.OWLDataHasValue)
	 */
	public void visit(OWLDataHasValue arg0) {
		//Terminal:
		unwind(activeStack, arg0);
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLClassExpressionVisitor#visit(org.semanticweb.owlapi.model.OWLDataMinCardinality)
	 */
	public void visit(OWLDataMinCardinality arg0) {
		//Terminal:
		unwind(activeStack, arg0);	
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLClassExpressionVisitor#visit(org.semanticweb.owlapi.model.OWLDataExactCardinality)
	 */
	public void visit(OWLDataExactCardinality arg0) {
		//Terminal:
		unwind(activeStack, arg0);
		}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLClassExpressionVisitor#visit(org.semanticweb.owlapi.model.OWLDataMaxCardinality)
	 */
	public void visit(OWLDataMaxCardinality arg0) {
		//Terminal:
		unwind(activeStack, arg0);
	}
	
	private static <T> Set<Set<T>> powerSet(Set<T> originalSet) {
	    Set<Set<T>> sets = new HashSet<Set<T>>();
	    if (originalSet.isEmpty()) {
	    	sets.add(new HashSet<T>());
	    	return sets;
	    }
	    List<T> list = new ArrayList<T>(originalSet);
	    T head = list.get(0);
	    Set<T> rest = new HashSet<T>(list.subList(1, list.size())); 
	    for (Set<T> set : powerSet(rest)) {
	    	Set<T> newSet = new HashSet<T>();
	    	newSet.add(head);
	    	newSet.addAll(set);
	    	sets.add(newSet);
	    	sets.add(set);
	    }		
	    return sets;
	}
	
	private Stack<OWLClassExpression> nextStack(OWLClassExpression expr)
	{
		@SuppressWarnings("unchecked")
		Stack<OWLClassExpression> nextStack = (Stack<OWLClassExpression>) activeStack.clone();
		nextStack.push(expr);
		return nextStack;
	}
	
	private void unwind(Stack<OWLClassExpression> stack, OWLClassExpression terminal)
	{
		OWLClassExpression ancestor = null;
		OWLClassExpression descendant = terminal;
		/*
		 * We cannot pop the stack because subsequenent iterations of the
		 * decomposition loop might still need it. So we make a copy.
		 */
		@SuppressWarnings("unchecked")
		Stack<OWLClassExpression> stackClone = (Stack<OWLClassExpression>) stack.clone();
		while (0 < stackClone.size())
		{
			ancestor = stackClone.pop();
			descendant = recreateClassExpression(ancestor, descendant);
		}
		stackClone = null;
		decompositionSet.add(descendant);
	}
	
	private OWLClassExpression recreateClassExpression(OWLObjectComplementOf ancestor, OWLClassExpression descendant)
	{
		return factory.getOWLObjectComplementOf(descendant);
	}
	
	private OWLClassExpression recreateClassExpression(OWLObjectSomeValuesFrom ancestor, OWLClassExpression descendant)
	{
		OWLObjectPropertyExpression prop = ancestor.getProperty();
		return factory.getOWLObjectSomeValuesFrom(prop, descendant);
	}
	
	private OWLClassExpression recreateClassExpression(OWLObjectAllValuesFrom ancestor, OWLClassExpression descendant)
	{
		OWLObjectPropertyExpression prop = ancestor.getProperty();
		return factory.getOWLObjectAllValuesFrom(prop, descendant);
	}
	
	private OWLClassExpression recreateClassExpression(OWLObjectMinCardinality ancestor, OWLClassExpression descendant)
	{
		OWLObjectPropertyExpression prop = ancestor.getProperty();
		int cardinality = ancestor.getCardinality();
		return factory.getOWLObjectMinCardinality(cardinality, prop, descendant);
	}
	
	private OWLClassExpression recreateClassExpression(OWLObjectMaxCardinality ancestor, OWLClassExpression descendant)
	{
		OWLObjectPropertyExpression prop = ancestor.getProperty();
		int cardinality = ancestor.getCardinality();
		return factory.getOWLObjectMaxCardinality(cardinality, prop, descendant);
	}
	
	
	private OWLClassExpression recreateClassExpression(OWLObjectExactCardinality ancestor, OWLClassExpression descendant)
	{
		OWLObjectPropertyExpression prop = ancestor.getProperty();
		int cardinality = ancestor.getCardinality();
		return factory.getOWLObjectExactCardinality(cardinality, prop, descendant);
	}
	
	
	private OWLClassExpression recreateClassExpression(OWLClassExpression ancestor, OWLClassExpression descendant)
	{
		ClassExpressionType theType = ancestor.getClassExpressionType();
		/* 
		 * How tedious. We could have as well used a separate visitor object
		 * for this, but it's not really worth the effort.
		 */
		switch (theType)
		{
		case OBJECT_ALL_VALUES_FROM:
			return recreateClassExpression((OWLObjectAllValuesFrom)ancestor, descendant);
		case OBJECT_COMPLEMENT_OF:
			return recreateClassExpression((OWLObjectComplementOf)ancestor, descendant);
		case OBJECT_EXACT_CARDINALITY:
			return recreateClassExpression((OWLObjectExactCardinality)ancestor, descendant);
		case OBJECT_MAX_CARDINALITY:
			return recreateClassExpression((OWLObjectMaxCardinality)ancestor, descendant);
		case OBJECT_MIN_CARDINALITY:
			return recreateClassExpression((OWLObjectMinCardinality)ancestor, descendant);
		case OBJECT_SOME_VALUES_FROM:
			return recreateClassExpression((OWLObjectSomeValuesFrom)ancestor, descendant);
		}
		return null;
	}
}

