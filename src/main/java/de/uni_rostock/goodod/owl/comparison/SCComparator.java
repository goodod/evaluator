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
package de.uni_rostock.goodod.owl.comparison;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.*;

import de.uni_rostock.goodod.owl.OntologyCache;
import de.uni_rostock.goodod.owl.OntologyPair;
import de.uni_rostock.goodod.owl.SubClassCollector;
import de.uni_rostock.goodod.owl.SuperClassCollector;

/**
 * Comparator for ontology pairs using semantic cotopy (cf. Dellschaft/Staab
 * 2006) without any special provisions.
 * @author Niels Grewe <niels.grewe@uni-rostock.de>
 *
 */
public class SCComparator implements Comparator {

	private OntologyPair pair;
	boolean includeImports;
	protected ComparatorDelegate delegate;
	
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
		delegate = new DefaultComparatorDelegate(includeImports);
	}
	
	public void setDelegate(ComparatorDelegate del)
	{
		if (null != del)
		{
			delegate = del;
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uni_rostock.goodod.owl.Comparator#compare()
	 */
	public FMeasureComparisonResult compare() throws InterruptedException, ExecutionException {
		Set<IRI> noIRIs = Collections.emptySet();
		Set<OWLClass> classSet = new HashSet<OWLClass>();
		OWLOntology ontologyA = pair.getOntologyA();
		OWLAnnotationValue theTrue = ontologyA.getOWLOntologyManager().getOWLDataFactory().getOWLLiteral(true);
		for (OWLClass c: ontologyA.getClassesInSignature(includeImports))
		{
			if (false == includeImports)
			{
				Set<OWLAnnotationAssertionAxiom> annotations = ontologyA.getAnnotationAssertionAxioms(c.getIRI());
				for (OWLAnnotationAssertionAxiom ax : annotations)
				{
					if ((ax.getProperty().getIRI().equals(OntologyCache.originallyDefinedIRI))
							&& (ax.getValue().equals(theTrue)))	
					{
						classSet.add(c);
					}
				}
			}
			else
			{
				classSet.add(c);
			}
		}
		return compareClasses(classSet, noIRIs);
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
		return (false == Double.isNaN(d));
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
		double overallWeight = 0;
		// Classes that are not available in the computed ontology by definition achieve maximum precision
		// because they don't return any irrelevant concept.
		double precisionAccumulator = 0;
		double recallAccumulator = 0;
		OWLOntology ontologyA = pair.getOntologyA();
		OWLOntology ontologyB = pair.getOntologyB();
		for (OWLClass classA : classes)
		{
			OWLClass classB = delegate.findClass(classA, ontologyA, ontologyB);
			overallWeight += delegate.getClassWeight(classB, ontologyA, ontologyB);
			double newPrec = getTaxonomicPrecision(classA, classB, ontologyA, pair.getOntologyB());
			// Lucky fact: precision of A vs. B is the same thing as recall B vs. A.
			double newRec= getTaxonomicPrecision(classB, classA, ontologyB, ontologyA);
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
				OWLClass classA = delegate.findClass(classB, ontologyA, ontologyB);
				overallWeight += delegate.getClassWeight(classB, ontologyA, ontologyB);
				double newPrec = getTaxonomicPrecision(classA, classB, ontologyA, ontologyB);
				// Lucky fact: precision of A vs. B is the same thing as recall B vs. A.
				double newRec= getTaxonomicPrecision(classB, classA, ontologyB, ontologyA);
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
		double precision = precisionAccumulator / overallWeight;
		double recall = recallAccumulator / overallWeight;
	
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
	

	
	
	
	protected Set<OWLClass> commonClasses(Set<OWLClass> extractA, Set<OWLClass> extractB)
	{
		Set<OWLClass> commonClasses = new HashSet<OWLClass>();
		// TODO: Nested loop. There is probably a smarter way.
		for (OWLClass classA : extractA)
		{
			for (OWLClass classB : extractB)
			{
				try
				{
					if (delegate.classesConsideredEqual(classA,pair.getOntologyA(),classB, pair.getOntologyB()))
					{
						commonClasses.add(classA);
					}
				}
				catch (Throwable e)
				{
					Logger.getLogger(this.getClass()).error("Could not get ontologies.", e);
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

