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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

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
	public FMeasureComparisonResult compare() throws InterruptedException, ExecutionException {
		Set<IRI> noIRIs = Collections.emptySet();
		return compareClasses(pair.getOntologyA().getClassesInSignature(includeImports), noIRIs);
	}

	/* (non-Javadoc)
	 * @see de.uni_rostock.goodod.owl.Comparator#compare(java.util.Set)
	 */
	public FMeasureComparisonResult compare(Set<IRI> classIRIs) throws InterruptedException, ExecutionException {
		Set<OWLClass> classes = new HashSet<OWLClass>();
		Set<IRI> found = new HashSet<IRI>();
		Set<IRI> notFound = new HashSet<IRI>(classIRIs);
		for (OWLClass c : pair.getOntologyA().getClassesInSignature(includeImports))
		{
					if(classIRIs.contains(c.getIRI()))
					{
						classes.add(c);
						found.add(c.getIRI());
					}
		}
		notFound.removeAll(found);
		return compareClasses(classes, notFound);
	}

	private static boolean notNaN(double d)
	{
		Double o = new Double(d);
		return (false == o.isNaN());
	}

	/**
	 * Primary method that performs the actual computation.
	 * @param classes The classes from ontology A to use for the comparison.
	 * @param notFound IRIs of classes that could not be found in the ontology
	 * @return The result of the comparison.
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	protected FMeasureComparisonResult compareClasses(Set<OWLClass>classes, Set<IRI> notFound) throws InterruptedException, ExecutionException
	{
		int classCount = classes.size() + notFound.size();
		// Classes that are not available in the computed ontology by definition achieve maximum precision
		// because they don't return any irrelevant concept.
		double precisionAccumulator = 0;
		double recallAccumulator = 0;
		for (OWLClass classA : classes)
		{
			OWLClass classB = findClass(classA, pair.getOntologyB());
			double newPrec = getTaxonomicPrecision(classA, classB, pair.getOntologyA(), pair.getOntologyB());
			// Lucky fact: precision of A vs. B is the same thing as recall B vs. A.
			double newRec= getTaxonomicPrecision(classB, classA, pair.getOntologyB(), pair.getOntologyA());
			if (notNaN(newPrec))
			{
				precisionAccumulator += newPrec;
			}
			if (notNaN(newRec))
			{
				recallAccumulator += newRec;
			}
		}
		for (OWLClass classB : pair.getOntologyB().getClassesInSignature(includeImports))
		{
			if (notFound.contains(classB.getIRI()))
			{
				OWLClass classA = findClass(classB, pair.getOntologyA());
				double newPrec = getTaxonomicPrecision(classA, classB, pair.getOntologyA(), pair.getOntologyB());
				// Lucky fact: precision of A vs. B is the same thing as recall B vs. A.
				double newRec= getTaxonomicPrecision(classB, classA, pair.getOntologyB(), pair.getOntologyA());
				if (notNaN(newPrec))
				{
					precisionAccumulator += newPrec;
				}
				if (notNaN(newRec))
				{
					recallAccumulator += newRec;
				}

			}	

		}
		double precision = precisionAccumulator / (double)classCount;
		double recall = recallAccumulator / (double)classCount;
	
		return new FMeasureComparisonResult(getComparisonMethod(), pair, precision, recall);
		
	}
	
	protected String getComparisonMethod()
	{
		return "Semantic Cotopy Comparison";
	}
	
	protected Set<OWLClass>transitiveSubClasses(OWLClass c, OWLOntology o)
	{
		Set<OWLOntology> ontologies = null;
		if (includeImports)
		{
			ontologies = o.getImportsClosure();
		}
		else
		{
			ontologies = Collections.singleton(o);
		}
		return new SubClassCollector(c, ontologies).collect();
	}
	
	protected Set<OWLClass>transitiveSuperClasses(OWLClass c, OWLOntology o)
	{
		Set<OWLOntology> ontologies = null;
		if (includeImports)
		{
			ontologies = o.getImportsClosure();
		}
		else
		{
			ontologies = Collections.singleton(o);
		}
		return new SuperClassCollector(c, ontologies).collect();
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
		Set<OWLClass> extract = new HashSet<OWLClass>();
		
		extract.addAll(transitiveSuperClasses(c, o));
		extract.addAll(transitiveSubClasses(c, o));
		
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

