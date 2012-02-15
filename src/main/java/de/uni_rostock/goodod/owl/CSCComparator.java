/**
  Copyright (C) 2011 The University of Rostock.
 
  Written by:  Niels Grewe <niels.grewe@uni-rostock.de>
  Created: 15.12.2011
  
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
 * Comparator that implements the common semantic cotopy algorithm from
 * Dellschaft/Staab 2006.
 * 
 * @author Niels Grewe <niels.grewe@uni-rostock.de>
 *
 */
public class CSCComparator extends SCComparator {

	public CSCComparator(OntologyPair thePair, boolean doIncludeImports)
	{
		super(thePair, doIncludeImports);
	}
	
	
	/**
	 * Gets a characteristic extract, taking only classes that appear in both
	 * ontologies into account.
	 * @param c The class to create an extract for.
	 * @param ontA The first ontology. 
	 * @param ontB The second ontology.
	 * @return The set of all sub- and superclasses that appear in both ontologies.
	 */
	private Set<OWLClass> commonSemanticCotopy(OWLClass c, OWLOntology ontA, OWLOntology ontB)
	{
		
		
		Set<OWLClass> candidates = transitiveSuperClasses(c, ontA);
		candidates.addAll(transitiveSubClasses(c, ontB));
		
		Set<OWLClass> extract = new HashSet<OWLClass>();
		for (OWLClass candidate : candidates)
		{
		
			if (null != findClass(candidate, ontB))
			{
				extract.add(candidate);
			}
		}
		/*
		 *  The class itself belongs to the extract as well and prevents us
		 *  from doing divisions by zero.
		 */
		if (null != findClass(c, ontB))
		{
			extract.add(c);
		}
		return extract;
	}
	
	@Override
	protected String getComparisonMethod()
	{
		return "Common Semantic Cotopy Comparison";
	}
	
	@Override
	protected double getTaxonomicPrecision(OWLClass classA, OWLClass classB, OWLOntology ontologyA, OWLOntology ontologyB)
	{
		boolean oneIsNull = ((null == classA) || (null == classB));
		
		if ((null == classA) && (null == classB))
		{
			//Garbage, might as well raise an exception here.
			return 0;
		}
		
		if (oneIsNull)
		{
			/*
			 * Precision estimation: We traverse all classes from the other
			 * ontology that is not also contained in the first one and use
			 * the maximum precision we can achieve this way.
			 */
			double maxPrecision = 0;
			if (null == classA)
			{
				for (OWLClass c : ontologyA.getClassesInSignature())
				{
					//Find all classes that are not also in B.
					if (null == findClass(c,ontologyB))
					{
						//Test how well they match our concept and use the maximum
						double newPrecision = computeTaxonomicPrecision(c, classB, ontologyA, ontologyB);
						maxPrecision = Math.max(maxPrecision, newPrecision);
					}
				}
		
			}
			else if (null == classB)
			{
				for (OWLClass c : ontologyB.getClassesInSignature())
				{
					//Find all classes that are not also in A.
					if (null == findClass(c,ontologyA))
					{
						// Test how well they match our concept and use the maximum.
						double newPrecision = computeTaxonomicPrecision(classA, c, ontologyA, ontologyB);
						maxPrecision = Math.max(maxPrecision, newPrecision);
					}
				}
			}
			
			/*
			 * So we got we got an estimation of maximum precision achievable 
			 * with the ontology. We return that.
			 */
			return maxPrecision;
		}
		
		/*
		 * Otherwise, if we got both concepts, we can do a plain ol' precision calculation.
		 */
		return computeTaxonomicPrecision(classA, classB, ontologyA, ontologyB);
	}
	

	@Override
	protected double computeTaxonomicPrecision(OWLClass classA, OWLClass classB, OWLOntology ontA, OWLOntology ontB)
	{
		if ((null == classA) || (null == classB))
		{
			//If one of the classes is null, we just return zero.
			return 0;
		}
		Set<OWLClass> extractA = commonSemanticCotopy(classA, ontA, ontB);
		Set<OWLClass> extractB = commonSemanticCotopy(classB, ontB, ontA);
		Set<OWLClass> commonExtract = commonClasses(extractA,extractB);
		
		/*
		 * With common semantic cotopy, it is possible for the extract from
		 * ontologyA to be empty, if the target class is not also in ontology 
		 * and no other common classes can be found. So we need to guard
		 * against division by zero here.
		 */
		if (0 == extractA.size())
		{
			return 0;
		}
		return (((double)commonExtract.size())/(double)extractA.size());
	}
	
}
