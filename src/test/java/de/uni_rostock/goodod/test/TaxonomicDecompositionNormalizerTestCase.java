/**
  Copyright (C) 2012 The University of Rostock.
 
  Written by:  Niels Grewe
  Created: 17.02.2012
  
  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  as published by the Free Software Foundation; either
  version 3 of the License, or (at your option) any later version.

  You should have received a copy of the GNU General Public
  License along with this program; see the file COPYING.
  If not, write to the Free Software Foundation,
  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 */
package de.uni_rostock.goodod.test;

import org.junit.*;
import org.semanticweb.owlapi.model.*;

import de.uni_rostock.goodod.owl.normalization.TaxonomicDecompositionNormalizerFactory;

/**
 * @author Niels Grewe
 *
 */
public class TaxonomicDecompositionNormalizerTestCase extends
		AbstractNormalizerTestCase {
	@Override
	@Before public void setUp() throws OWLOntologyCreationException
	{
		super.setUp();
		normalizer = new TaxonomicDecompositionNormalizerFactory();
	}
	
	// We only need one test because we know check taxonomic decomposition works in another test.
	@Test public void testTaxonomicDecomposition() throws OWLOntologyCreationException
	{
		OWLClass A = addClass("A");
		OWLClass B = addClass("B");
		OWLClass C = addClass("C");
		OWLObjectProperty P = factory.getOWLObjectProperty(IRI("P"));
		OWLClassExpression AAndB = factory.getOWLObjectIntersectionOf(A, B);
		OWLClassExpression PSomeAAndB = factory.getOWLObjectSomeValuesFrom(P, AAndB);
		OWLClassExpression PSomeA = factory.getOWLObjectSomeValuesFrom(P, B);
		OWLClassExpression PSomeB = factory.getOWLObjectSomeValuesFrom(P, B);
		OWLAxiom PDecl = factory.getOWLDeclarationAxiom(P);
		manager.addAxiom(ontology,PDecl);
		addSubClassOf(A,C);
		OWLAxiom CEquivPSomeAAndB = factory.getOWLEquivalentClassesAxiom(C,PSomeAAndB);
		manager.addAxiom(ontology,CEquivPSomeAAndB);
		normalizer.normalize(ontology);
		
		boolean hasPSomeAEquiv = false;
		boolean hasPSomeBEquiv = false;
		for (OWLEquivalentClassesAxiom eq : ontology.getAxioms(AxiomType.EQUIVALENT_CLASSES))
		{
			if (false == hasPSomeAEquiv)
			{
				if (eq.contains(PSomeA))
				{
					hasPSomeAEquiv = true;
					for (OWLClassExpression ce : eq.getClassExpressionsMinus(PSomeA))
					{
						if (ce instanceof OWLClass)
						{
							// Check that we have declaration axioms for all of the named classes.
							assertTrue(ontology.containsAxiom(factory.getOWLDeclarationAxiom(ce.asOWLClass())));
						}
					}
				}
			}
			
			if (false == hasPSomeBEquiv)
			{
				if (eq.contains(PSomeB))
				{
					hasPSomeBEquiv = true;
					for (OWLClassExpression ce : eq.getClassExpressionsMinus(PSomeA))
					{
						if (ce instanceof OWLClass)
						{
							// Check that we have declaration axioms for all of the named classes.
							assertTrue(ontology.containsAxiom(factory.getOWLDeclarationAxiom(ce.asOWLClass())));
						}
					}
				}
			}
			if (hasPSomeBEquiv && hasPSomeAEquiv)
			{
				break;
			}
		}
		assertTrue(hasPSomeBEquiv && hasPSomeAEquiv);
	}
}
