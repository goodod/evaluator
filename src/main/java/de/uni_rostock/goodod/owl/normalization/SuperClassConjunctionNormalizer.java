/**
  Copyright (C) 2012 The University of Rostock.
 
  Written by:  Niels Grewe
  Created: 01.03.2012
  
  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  as published by the Free Software Foundation; either
  version 3 of the License, or (at your option) any later version.

  You should have received a copy of the GNU General Public
  License along with this program; see the file COPYING.
  If not, write to the Free Software Foundation,
  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 */
package de.uni_rostock.goodod.owl.normalization;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.*;

/**
 * @author Niels Grewe
 * For every class, this normalizer compresses the superclasses mentioned in
 * SubClassOf axioms into a single conjunctive superclass 
 */
public class SuperClassConjunctionNormalizer extends AbstractNormalizer {

	SuperClassConjunctionNormalizer(OWLOntology ont)
	{
		super(ont);
	}


	/* (non-Javadoc)
	 * @see de.uni_rostock.goodod.owl.Normalizer#normalize(java.util.Set)
	 */
	public void normalize(Set<IRI> IRIs) throws OWLOntologyCreationException {
		for (IRI i: IRIs)
		{
			Set<OWLSubClassOfAxiom> axioms = ontology.getSubClassAxiomsForSubClass(factory.getOWLClass(i));
			if ((null != axioms) && (axioms.size() > 1))
			{
				rewrite(axioms);
			}
		}
		manager.applyChanges(new ArrayList<OWLOntologyChange>(changes));
	}

	private void rewrite(Set<OWLSubClassOfAxiom> axioms)
	{
		Set<OWLClassExpression> superClasses = new HashSet<OWLClassExpression>();
		OWLClassExpression subClass = axioms.iterator().next().getSubClass();
		for (OWLSubClassOfAxiom a : axioms)
		{
			superClasses.add(a.getSuperClass());
			changes.add(new RemoveAxiom(ontology, a));
		}
		OWLClassExpression newSuperClass = factory.getOWLObjectIntersectionOf(superClasses);
		changes.add(new AddAxiom(ontology, factory.getOWLSubClassOfAxiom(subClass, newSuperClass)));
	}
}
