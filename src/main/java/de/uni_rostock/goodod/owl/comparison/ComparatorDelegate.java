/**
  Copyright (C) 2012 The University of Rostock.
 
  Written by:  thebeing
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

import org.semanticweb.owlapi.model.*;

/**
 * @author Niels Grewe
 *
 * Objects adopting the comparator delegate protocol can be used to implement
 * various ways of obtaining classes for comparison. This is useful when, e.g.
 * lexical matching is desired.
 */
public interface ComparatorDelegate {

	/**
	 * Searches for a class in the search ontology that corresponds to the
	 * referenceClass from the reference ontology.
	 * @param referenceClass The reference class for which to find a corresponding class.
	 * @param referenceOntology The reference ontology from which the class stems.
	 * @param searchOntology The ontology in which to search.
	 * @return The class that is judged to correspond to the referenceClass
	 */
	public OWLClass findClass(OWLClass referenceClass, OWLOntology referenceOntology, OWLOntology searchOntology);

	/**
	 * Provides a weight (importance) from the <0,1> interval for the class.
	 * @param targetClass The class which to weigh
	 * @param referenceOntology The reference ontology
	 * @param searchOntology The ontology from which the targetClass is taken
	 * @return The weight of the class.
	 */
	public double getClassWeight(OWLClass targetClass, OWLOntology referenceOntology, OWLOntology searchOntology);

	/**
	 * Determines whether the delegate judges the two classes to be equal.
	 * @param classA The first class
	 * @param ontologyA The ontology from which the first class stems
	 * @param classB The second class
	 * @param ontologyB The ontology from which the second class stems
	 * @return Whether the two classes can be considered as equal.
	 */
	public boolean classesConsideredEqual(OWLClass classA, OWLOntology ontologyA, OWLClass classB, OWLOntology ontologyB);
}
