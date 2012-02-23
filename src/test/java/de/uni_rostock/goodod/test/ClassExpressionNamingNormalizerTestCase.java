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

import java.util.HashSet;
import java.util.Set;

import org.junit.*;
import de.uni_rostock.goodod.owl.ClassExpressionNamingNormalizerFactory;

import org.semanticweb.owlapi.model.*;
/**
 * @author Niels Grewe
 *
 */
public class ClassExpressionNamingNormalizerTestCase extends
		AbstractNormalizerTestCase {

	@Override
	@Before public void setUp() throws OWLOntologyCreationException
	{
		super.setUp();
		normalizer = new ClassExpressionNamingNormalizerFactory();
	}
	
	@Test public void testSimpleSubClassOfSimple() throws OWLOntologyCreationException
	{
		OWLClass A = addClass("A");
		OWLClass B = addClass("B");
		addSubClassOf(B, A);
		normalizer.normalize(ontology);
		Set<OWLClass> expected = new HashSet<OWLClass>();
		expected.add(A);
		expected.add(B);
		Set<OWLClass> actual = ontology.getClassesInSignature();
		assertEquals(expected, actual);
	}
	
	@Test public void testSimpleSubClassOfComplex() throws OWLOntologyCreationException
	{
		OWLClass A = addClass("A");
		OWLClass B = addClass("B");
		OWLClassExpression notB = factory.getOWLObjectComplementOf(B);
		addSubClassOf(notB, A);
		normalizer.normalize(ontology);
		Set<OWLSubClassOfAxiom> subAxioms = ontology.getAxioms(AxiomType.SUBCLASS_OF);
		for (OWLSubClassOfAxiom a : subAxioms)
		{
			assertFalse(a.getSubClass().equals(notB) && a.getSuperClass().equals(A));
		}
		Set<OWLEquivalentClassesAxiom> equivAxioms = ontology.getAxioms(AxiomType.EQUIVALENT_CLASSES);
		boolean notBHasEquiv = false;
		for (OWLEquivalentClassesAxiom a : equivAxioms)
		{
			notBHasEquiv = a.contains(notB);
			if (true == notBHasEquiv)
			{
				for (OWLClassExpression ce : a.getClassExpressionsMinus(notB))
				{
					if (ce instanceof OWLClass)
					{
						// Check that we have declaration axioms for all of the named classes.
						assertTrue(ontology.containsAxiom(factory.getOWLDeclarationAxiom(ce.asOWLClass())));
					}
				}
				break;
			}
		}
		assertTrue(notBHasEquiv);
		
	}
	
	@Test public void testComplexSubClassOfSimple() throws OWLOntologyCreationException
	{
		OWLClass A = addClass("A");
		OWLClass B = addClass("B");
		OWLClassExpression notB = factory.getOWLObjectComplementOf(B);
		addSubClassOf(A, notB);
		normalizer.normalize(ontology);
		Set<OWLSubClassOfAxiom> subAxioms = ontology.getAxioms(AxiomType.SUBCLASS_OF);
		for (OWLSubClassOfAxiom a : subAxioms)
		{
			assertFalse(a.getSuperClass().equals(notB) && a.getSubClass().equals(A));
		}
		Set<OWLEquivalentClassesAxiom> equivAxioms = ontology.getAxioms(AxiomType.EQUIVALENT_CLASSES);
		boolean notBHasEquiv = false;
		for (OWLEquivalentClassesAxiom a : equivAxioms)
		{
			notBHasEquiv = a.contains(notB);
			if (true == notBHasEquiv)
			{
				for (OWLClassExpression ce : a.getClassExpressionsMinus(notB))
				{
					if (ce instanceof OWLClass)
					{
						// Check that we have declaration axioms for all of the named classes.
						assertTrue(ontology.containsAxiom(factory.getOWLDeclarationAxiom(ce.asOWLClass())));
					}
				}
				break;
			}
		}
		assertTrue(notBHasEquiv);
		
	}
	
	@Test public void testComplexSubClassOfComplex() throws OWLOntologyCreationException
	{
		OWLClass A = addClass("A");
		OWLClass B = addClass("B");
		OWLClassExpression notB = factory.getOWLObjectComplementOf(B);
		OWLClassExpression notA = factory.getOWLObjectComplementOf(A);
		addSubClassOf(notA, notB);
		normalizer.normalize(ontology);
		Set<OWLSubClassOfAxiom> subAxioms = ontology.getAxioms(AxiomType.SUBCLASS_OF);
		for (OWLSubClassOfAxiom a : subAxioms)
		{
			assertFalse(a.getSuperClass().equals(notB) && a.getSubClass().equals(notA));
		}
		Set<OWLEquivalentClassesAxiom> equivAxioms = ontology.getAxioms(AxiomType.EQUIVALENT_CLASSES);
		boolean notBHasEquiv = false;
		for (OWLEquivalentClassesAxiom a : equivAxioms)
		{
			notBHasEquiv = a.contains(notB);
			if (true == notBHasEquiv)
			{
				for (OWLClassExpression ce : a.getClassExpressionsMinus(notB))
				{
					if (ce instanceof OWLClass)
					{
						// Check that we have declaration axioms for all of the named classes.
						assertTrue(ontology.containsAxiom(factory.getOWLDeclarationAxiom(ce.asOWLClass())));
					}
				}
				break;
			}
		}
		boolean notAHasEquiv = false;
		for (OWLEquivalentClassesAxiom a : equivAxioms)
		{
			notAHasEquiv = a.contains(notA);
			if (true == notAHasEquiv)
			{
				for (OWLClassExpression ce : a.getClassExpressionsMinus(notA))
				{
					if (ce instanceof OWLClass)
					{
						// Check that we have declaration axioms for all of the named classes.
						assertTrue(ontology.containsAxiom(factory.getOWLDeclarationAxiom(ce.asOWLClass())));
					}
				}
				break;
			}
		}
		assertTrue(notAHasEquiv);
	}
	
	@Test public void testComplexEquivalences() throws OWLOntologyCreationException
	{
		OWLClass A = addClass("A");
		OWLClass B = addClass("B");
		OWLClassExpression notB = factory.getOWLObjectComplementOf(B);
		OWLClassExpression notA = factory.getOWLObjectComplementOf(A);
		OWLAxiom equivAx = factory.getOWLEquivalentClassesAxiom(notB, notA);
		manager.addAxiom(ontology, equivAx);
		normalizer.normalize(ontology);
		Set<OWLEquivalentClassesAxiom> equivAxioms = ontology.getAxioms(AxiomType.EQUIVALENT_CLASSES);
		boolean notBHasEquiv = false;
		for (OWLEquivalentClassesAxiom a : equivAxioms)
		{
			assertFalse(a.contains(notB) && a.contains(notA));
			notBHasEquiv = a.contains(notB);
			if (true == notBHasEquiv)
			{
				for (OWLClassExpression ce : a.getClassExpressionsMinus(notB))
				{
					if (ce instanceof OWLClass)
					{
						// Check that we have declaration axioms for all of the named classes.
						assertTrue(ontology.containsAxiom(factory.getOWLDeclarationAxiom(ce.asOWLClass())));
					}
				}
				break;
			}
		}
		boolean notAHasEquiv = false;
		for (OWLEquivalentClassesAxiom a : equivAxioms)
		{
			notAHasEquiv = a.contains(notA);
			if (true == notAHasEquiv)
			{
				for (OWLClassExpression ce : a.getClassExpressionsMinus(notA))
				{
					if (ce instanceof OWLClass)
					{
						// Check that we have declaration axioms for all of the named classes.
						assertTrue(ontology.containsAxiom(factory.getOWLDeclarationAxiom(ce.asOWLClass())));
					}
				}
				break;
			}
		}
		assertTrue(notAHasEquiv);
	}
}
