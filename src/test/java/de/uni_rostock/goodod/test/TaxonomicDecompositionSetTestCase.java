/**
  Copyright (C) 2012 The University of Rostock.
 
  Written by:  Niels Grewe
  Created: 15.02.2012
  
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

import de.uni_rostock.goodod.owl.TaxonomicDecompositionCollector;
/**
 * @author Niels Grewe
 * Test cases for taxonomic decomposition.
 */
public class TaxonomicDecompositionSetTestCase extends AbstractTestCase {

	final private Set<OWLClassExpression> expected = new HashSet<OWLClassExpression>();
	
	@Before public void setUp()
	{
		expected.clear();
	}
	
	private Set<OWLClassExpression> TDS(OWLClassExpression e)
	{
		return TaxonomicDecompositionCollector.collect(e);
	}
	

	@Test public void testDecomposeClassIntersection()
	{
		OWLClass A = factory.getOWLClass(IRI("A"));
		OWLClass B = factory.getOWLClass(IRI("B"));
		OWLClassExpression AAndB = factory.getOWLObjectIntersectionOf(A, B);
		expected.add(A);
		expected.add(B);
		expected.add(AAndB);
		Set<OWLClassExpression> actual = TDS(AAndB);
		assertEquals(expected, actual);
	}
	
	@Test public void testDecomposeClassUnion()
	{
		OWLClass A = factory.getOWLClass(IRI("A"));
		OWLClass B = factory.getOWLClass(IRI("B"));
		OWLClassExpression AOrB = factory.getOWLObjectUnionOf(A, B);
		expected.add(A);
		expected.add(B);
		expected.add(AOrB);
		Set<OWLClassExpression> actual = TDS(AOrB);
		assertEquals(expected, actual);
	}
	
	@Test public void testDecomposeClass()
	{
		OWLClass A = factory.getOWLClass(IRI("A"));
		assertEquals(Collections.singleton(A),TDS(A));
	}
	
	@Test public void testDecomposeClassComplementSimple()
	{
		OWLClass A = factory.getOWLClass(IRI("A"));
		OWLClassExpression notA = factory.getOWLObjectComplementOf(A);
		assertEquals(Collections.singleton(notA),TDS(notA));
	}
	
	@Test public void testDecomposeClassComplementComplex()
	{
		OWLClass A = factory.getOWLClass(IRI("A"));
		OWLClass B = factory.getOWLClass(IRI("B"));
		OWLClassExpression AAndB = factory.getOWLObjectIntersectionOf(A, B);
		OWLClassExpression notAAndB = factory.getOWLObjectComplementOf(AAndB);
		OWLClassExpression notA = factory.getOWLObjectComplementOf(A);
		OWLClassExpression notB = factory.getOWLObjectComplementOf(B);
		expected.add(notA);
		expected.add(notB);
		expected.add(notAAndB);
		Set<OWLClassExpression> actual = TDS(notAAndB);
		assertEquals(expected, actual);
	}
	
	@Test public void testDecomposeObjectSomeValuesFromSimple()
	{
		OWLClass A = factory.getOWLClass(IRI("A"));
		OWLObjectProperty P = factory.getOWLObjectProperty(IRI("P"));
		OWLClassExpression PsomeA = factory.getOWLObjectSomeValuesFrom(P, A);
		assertEquals(Collections.singleton(PsomeA), TDS(PsomeA));
	}
	
	@Test public void testDecomposeObjectSomeValuesFromComplex()
	{
		OWLClass A = factory.getOWLClass(IRI("A"));
		OWLObjectProperty P = factory.getOWLObjectProperty(IRI("P"));
		OWLClass B = factory.getOWLClass(IRI("B"));
		OWLClassExpression AAndB = factory.getOWLObjectIntersectionOf(A, B);
		OWLClassExpression PSomeAAndB = factory.getOWLObjectSomeValuesFrom(P, AAndB);
		OWLClassExpression PSomeA = factory.getOWLObjectSomeValuesFrom(P, A);
		OWLClassExpression PSomeB = factory.getOWLObjectSomeValuesFrom(P, B);
		expected.add(PSomeA);
		expected.add(PSomeB);
		expected.add(PSomeAAndB);
		assertEquals(expected, TDS(PSomeAAndB));
	}
	@Test public void testDecomposeObjectAllValuesFromSimple()
	{
		OWLClass A = factory.getOWLClass(IRI("A"));
		OWLObjectProperty P = factory.getOWLObjectProperty(IRI("P"));
		OWLClassExpression POnlyA = factory.getOWLObjectAllValuesFrom(P, A);
		assertEquals(Collections.singleton(POnlyA), TDS(POnlyA));
	}
	
	@Test public void testDecomposeObjectAllValuesFromComplex()
	{
		OWLClass A = factory.getOWLClass(IRI("A"));
		OWLObjectProperty P = factory.getOWLObjectProperty(IRI("P"));
		OWLClass B = factory.getOWLClass(IRI("B"));
		OWLClassExpression AAndB = factory.getOWLObjectIntersectionOf(A, B);
		OWLClassExpression POnlyAAndB = factory.getOWLObjectAllValuesFrom(P, AAndB);
		OWLClassExpression POnlyA = factory.getOWLObjectAllValuesFrom(P, A);
		OWLClassExpression POnlyB = factory.getOWLObjectAllValuesFrom(P, B);
		expected.add(POnlyA);
		expected.add(POnlyB);
		expected.add(POnlyAAndB);
		assertEquals(expected, TDS(POnlyAAndB));
	}
	/* Tests missing for more arcane constructs:
	 * - Object(Min|Max|Exact)Cardinality
	 * - ObjectOneOf
	 * - ObjectHasValue
	 * - ObjectHasSelf
	 * - All data property constructs
	 */
}
