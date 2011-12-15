/**
  Copyright (C) 2011 The University of Rostock.
 
  Written by:  Niels Grewe <niels.grewe@uni-rostock.de>
  Created: 14.12.2011
  
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

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.*;

/**
 * Comparator for ontology pairs using semantic cotopy (cf. Dellschaft/Staab
 * 2006) without any special provisions.
 * @author Niels Grewe <niels.grewe@uni-rostock.de>
 *
 */
public class SCComparator implements Comparator {

	private OntologyPair pair;
	boolean includeImports;
	
	/**
	 * 
	 * @param thePair The ontology pair to compare.
	 * @param doIncludeImports Whether the imports closure of the ontologies
	 * should be taken into account for comparisons.
	 */
	public SCComparator(OntologyPair thePair, boolean doIncludeImports)
	{
		super();
		pair = thePair;
		includeImports = doIncludeImports;
	}
	
	/* (non-Javadoc)
	 * @see de.uni_rostock.goodod.owl.Comparator#compare()
	 */
	public ComparisonResult compare() {
		return compareClasses(pair.getOntologyA().getClassesInSignature(includeImports));
	}

	/* (non-Javadoc)
	 * @see de.uni_rostock.goodod.owl.Comparator#compare(java.util.Set)
	 */
	public ComparisonResult compare(Set<IRI> classIRIs) {
		Set<OWLClass> classes = new HashSet<OWLClass>();
		for (OWLClass c : pair.getOntologyA().getClassesInSignature(includeImports))
		{
					if(classIRIs.contains(c.getIRI()))
					{
						classes.add(c);
					}
		}
		return compareClasses(classes);
	}

	/**
	 * Primary method that performs the actual computation.
	 * @param classes The classes from ontology A to use for the comparison.
	 * @return The result of the comparison.
	 */
	protected ComparisonResult compareClasses(Set<OWLClass>classes)
	{
		double classCount = classes.size();
		double precisionAccumulator = 0;
		double recallAccumulator = 0;
		for (OWLClass classA : classes)
		{
			OWLClass classB = findClass(classA, pair.getOntologyB());
			precisionAccumulator += getTaxonomicPrecision(classA, classB, pair.getOntologyA(), pair.getOntologyB());
			// Lucky fact: precision of A vs. B is the same thing as recall B vs. A.
			recallAccumulator += getTaxonomicPrecision(classB, classA, pair.getOntologyB(), pair.getOntologyA());
		}
		
		double precision = precisionAccumulator / classCount;
		double recall = recallAccumulator / classCount;
		
		return new FMeasureComparisonResult(getComparisonMethod(), pair, precision, recall);
		
	}
	
	protected String getComparisonMethod()
	{
		return "Semantic Cotopy Comparison";
	}
	
	/**
	 * Computes a characteristic extract from the ontology, consisting of all
	 * sub- and superclasses of the given class.
	 * 
	 * @param o The ontology from which to get the classes.
	 * @param c The class for which to fetch the extract.
	 * @return The set of all sub- and superclasses of the given class.
	 */
	private Set<OWLClass> semanticCotopy(OWLClass c, OWLOntology o)
	{
		Set<OWLClassExpression> expressions = c.getSubClasses(o);
		expressions.addAll(c.getSuperClasses(o));
		
		
		/*
		 *  Since we got a set of class expressions back, we prune so that it
		 *  only contains asserted classes, for which we can check IRI equality.
		 */
		Set<OWLClass> extract = new HashSet<OWLClass>();
		for (OWLClassExpression ce : expressions)
		{
			if (ce instanceof OWLClass)
			{
				extract.add(ce.asOWLClass());
			}
		}
		/*
		 *  The class itself belongs to the extract as well and prevents us
		 *  from doing divisions by zero.
		 */
		extract.add(c);
		return extract;
	}
	
	/**
	 * Finds a class with the same name in the named ontology.
	 * @param classA The class for which to find a twin.
	 * @param o The ontology to consider.
	 * @return The class from the other ontology.
	 */
	protected OWLClass findClass(OWLClass classA, OWLOntology o)
	{
		IRI iriA = classA.getIRI();
		Set<OWLEntity> entities = o.getEntitiesInSignature(iriA, includeImports);
		if ((null != entities) && (0 != entities.size()))
		{
			for (OWLEntity e : entities)
			{
				if (e instanceof OWLClass)
				{
					return e.asOWLClass();
				}
			}
		}
		
		/* 
		 * If we got thus far, we have no exact match and need to search for a
		 * fragment-wise on.
		 */
		Set<OWLClass>classes = o.getClassesInSignature(includeImports);
		for (OWLClass c : classes)
		{
			if (equalIRIsOrFragments(iriA, c.getIRI()))
			{
				return c;
			}
		}
		
		return null;
	}
	
	protected boolean equalIRIsOrFragments(IRI iriA, IRI iriB)
	{
		if (iriA.equals(iriB))
		{
			return true;
		}
		if (iriA.getFragment().equals(iriB.getFragment()))
		{
			return true;
		}
		
		return false;
	}
	
	protected Set<OWLClass> commonClasses(Set<OWLClass> extractA, Set<OWLClass> extractB)
	{
		Set<OWLClass> commonClasses = new HashSet<OWLClass>();
		// TODO: Nested loop. There is probably a smarter way.
		for (OWLClass classA : extractA)
		{
			/*
			 *  But at least we can extract the loop invariant, just in case
			 *  the JVM is as stupid as we all think it is.
			 */
			IRI iriA = classA.getIRI();
			for (OWLClass classB : extractB)
			{
				if (equalIRIsOrFragments(iriA,classB.getIRI()))
				{
					commonClasses.add(classA);
				}
			}
			
		}
		return commonClasses;
	}


	protected double getTaxonomicPrecision(OWLClass classA, OWLClass classB, OWLOntology ontA, OWLOntology ontB)
	{
		return computeTaxonomicPrecision(classA, classB, ontA, ontB);
	}
	
	protected double computeTaxonomicPrecision(OWLClass classA, OWLClass classB, OWLOntology ontA, OWLOntology ontB)
	{
		if ((null == classA) || (null == classB))
		{
			//If one of the classes is null, we just return zero.
			return 0;
		}
		Set<OWLClass> extractA = semanticCotopy(classA, ontA);
		Set<OWLClass> extractB = semanticCotopy(classB, ontB);
		Set<OWLClass> commonExtract = commonClasses(extractA,extractB);
		

		return (((double)commonExtract.size())/(double)extractA.size());
	}
}

