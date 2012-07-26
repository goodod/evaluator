/**
  Copyright (C) 2012 The University of Rostock.
 
  Written by:  Niels Grewe <niels.grewe@uni-rostock.de>
  Created: 26.07.2012
  
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

import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * @author Niels Grewe
 *
 */
public class DefaultComparatorDelegate implements ComparatorDelegate {

	private final boolean includeImports;
	
	public DefaultComparatorDelegate(boolean includeImp)
	{
		includeImports = includeImp;
	}
	
	/* (non-Javadoc)
	 * @see de.uni_rostock.goodod.owl.comparison.ComparatorDelegate#findClass(org.semanticweb.owlapi.model.OWLClass, org.semanticweb.owlapi.model.OWLOntology, org.semanticweb.owlapi.model.OWLOntology)
	 */
	public OWLClass findClass(OWLClass referenceClass,
			OWLOntology referenceOntology, OWLOntology searchOntology) {
		
		IRI iriA = referenceClass.getIRI();
		Set<OWLEntity> entities = searchOntology.getEntitiesInSignature(iriA, includeImports);
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
		Set<OWLClass>classes = searchOntology.getClassesInSignature(includeImports);
		for (OWLClass c : classes)
		{
			if (classesConsideredEqual(referenceClass, referenceOntology, c, searchOntology))
			{
				return c;
			}
		}
		
		return null;
	}

	/* (non-Javadoc)
	 * @see de.uni_rostock.goodod.owl.comparison.ComparatorDelegate#getClassWeight(org.semanticweb.owlapi.model.OWLClass, org.semanticweb.owlapi.model.OWLOntology, org.semanticweb.owlapi.model.OWLOntology)
	 */
	public double getClassWeight(OWLClass targetClass,
			OWLOntology referenceOntology, OWLOntology searchOntology) {
		return 1;
	}

	
	public boolean classesConsideredEqual(OWLClass classA, OWLOntology a, OWLClass classB, OWLOntology b)
	{
		IRI iriA = classA.getIRI();
		IRI iriB = classB.getIRI();
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
}
