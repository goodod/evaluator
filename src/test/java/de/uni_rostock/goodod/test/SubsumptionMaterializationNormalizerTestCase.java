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

import de.uni_rostock.goodod.owl.SubsumptionMaterializationNormalizer;

/**
 * @author Niels Grewe
 *
 */
public class SubsumptionMaterializationNormalizerTestCase extends
		AbstractNormalizerTestCase {

	@Override
	@Before public void setUp() throws OWLOntologyCreationException
	{
		super.setUp();
		normalizer = new SubsumptionMaterializationNormalizer();
	}
	
	@Test public void testFindSubsumption() throws OWLOntologyCreationException
	{
		OWLClass A = addClass("A");
		OWLClass B = addClass("B");
		OWLClass C = addClass("C");
		OWLClassExpression notA = factory.getOWLObjectComplementOf(A);
		OWLAxiom BEquivNotA = factory.getOWLEquivalentClassesAxiom(B, notA);
		OWLAxiom CSubNotA = factory.getOWLSubClassOfAxiom(C, notA);
		OWLAxiom CSubB = factory.getOWLSubClassOfAxiom(C, B);
		manager.addAxiom(ontology, CSubNotA);
		manager.addAxiom(ontology, BEquivNotA);
		assertFalse(ontology.containsAxiom(CSubB));
		normalizer.normalize(ontology);
		assertTrue(ontology.containsAxiom(CSubB));
	}
	
	@Test public void testCleanHierarchy() throws OWLOntologyCreationException
	{
		OWLClass A = addClass("A");
		OWLClass B = addClass("B");
		OWLClass C = addClass("C");
		OWLAxiom CSubB = factory.getOWLSubClassOfAxiom(C, B);
		OWLAxiom BSubA = factory.getOWLSubClassOfAxiom(B, A);
		OWLAxiom CSubA = factory.getOWLSubClassOfAxiom(C, A);
		
		manager.addAxiom(ontology, CSubA);
		manager.addAxiom(ontology, BSubA);
		manager.addAxiom(ontology, CSubB);
		normalizer.normalize(ontology);
		assertFalse(ontology.containsAxiom(CSubA));
		assertTrue(ontology.containsAxiom(BSubA));
		assertTrue(ontology.containsAxiom(CSubB));
	}
	@Test public void testEliminateCycles() throws OWLOntologyCreationException
	{
		OWLClass A = addClass("A");
		OWLClass B = addClass("B");
		OWLClass C = addClass("C");
		OWLAxiom CSubB = factory.getOWLSubClassOfAxiom(C, B);
		OWLAxiom BSubA = factory.getOWLSubClassOfAxiom(B, A);
		OWLAxiom ASubC = factory.getOWLSubClassOfAxiom(A, C);
		
		manager.addAxiom(ontology, CSubB);
		manager.addAxiom(ontology, BSubA);
		manager.addAxiom(ontology, ASubC);
		normalizer.normalize(ontology);
		assertFalse(ontology.containsAxiom(ASubC));
		assertFalse(ontology.containsAxiom(CSubB));
		assertFalse(ontology.containsAxiom(BSubA));
		boolean isEquivAAndB = false;
		boolean isEquivBAndC = false;
		boolean isEquivAAndC = false;
		for (OWLEquivalentClassesAxiom eq : ontology.getAxioms(AxiomType.EQUIVALENT_CLASSES))
		{
			if (false == isEquivAAndB)
			{
				if (eq.contains(A) && eq.contains(B))
				{
					isEquivAAndB = true;
				}
			}
			if (false == isEquivBAndC)
			{
				if (eq.contains(B) && eq.contains(C))
				{
					isEquivBAndC = true;
				}
			}
			if (false == isEquivAAndC)
			{
				if (eq.contains(A) && eq.contains(C))
				{
					isEquivAAndC = true;
				}
			}
		}
		assertTrue(isEquivAAndB);
		assertTrue(isEquivBAndC);
		assertTrue(isEquivAAndC);
	}
}
