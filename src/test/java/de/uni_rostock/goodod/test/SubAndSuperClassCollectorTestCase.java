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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.*;

import org.semanticweb.owlapi.model.*;

import de.uni_rostock.goodod.owl.SubClassCollector;
import de.uni_rostock.goodod.owl.SuperClassCollector;

/**
 * @author Niels Grewe
 * Test cases for helper classes that collect sub/superclass closures.
 */
public class SubAndSuperClassCollectorTestCase extends AbstractTestCase {

	private OWLOntology ontology;
	
	@Override
	@Before public void setUp() throws OWLOntologyCreationException
	{
		ontology = manager.createOntology(IRI.create(baseIRI));
		OWLClass A = factory.getOWLClass(IRI("A"));
		OWLClass B = factory.getOWLClass(IRI("B"));
		OWLClass C = factory.getOWLClass(IRI("C"));
		OWLClass D = factory.getOWLClass(IRI("D"));
		OWLClass E = factory.getOWLClass(IRI("E"));
		OWLAxiom declA = factory.getOWLDeclarationAxiom(A);
		OWLAxiom declB = factory.getOWLDeclarationAxiom(B);
		OWLAxiom declC = factory.getOWLDeclarationAxiom(C);
		OWLAxiom declD = factory.getOWLDeclarationAxiom(D);
		OWLAxiom declE = factory.getOWLDeclarationAxiom(E);
		OWLAxiom CSubB = factory.getOWLSubClassOfAxiom(C, B);
		OWLAxiom BSubA = factory.getOWLSubClassOfAxiom(B, A);
		OWLAxiom ESubD = factory.getOWLSubClassOfAxiom(E, D);
		manager.addAxiom(ontology, declA);
		manager.addAxiom(ontology, declB);
		manager.addAxiom(ontology, declC);
		manager.addAxiom(ontology, CSubB);
		manager.addAxiom(ontology, BSubA);
		/* D and E are in the ontology to check that we do not pick up unrelated classes. */
		manager.addAxiom(ontology, declD);
		manager.addAxiom(ontology, declE);
		manager.addAxiom(ontology, ESubD);
	}

	@Test public void testSubclassCollection()
	{
		/* We collect all subclasses of A */
		OWLClass c = factory.getOWLClass(IRI("A"));
		Set<OWLClass> expected = new HashSet<OWLClass>();
		expected.add(factory.getOWLClass(IRI("B")));
		expected.add(factory.getOWLClass(IRI("C")));
		Set<OWLClass> actual = SubClassCollector.collect(c, Collections.singleton(ontology));
		assertEquals(expected,actual);
}
	
	@Test public void testSuperclassCollection()
	{
		/* We collect all subclasses of A */
		OWLClass c = factory.getOWLClass(IRI("C"));
		Set<OWLClass> expected = new HashSet<OWLClass>();
		expected.add(factory.getOWLClass(IRI("A")));
		expected.add(factory.getOWLClass(IRI("B")));
		Set<OWLClass> actual = SuperClassCollector.collect(c, Collections.singleton(ontology));
		assertEquals(expected,actual);
	}
}