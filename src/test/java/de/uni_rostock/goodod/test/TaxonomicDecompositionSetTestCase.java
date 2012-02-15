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

import junit.framework.TestCase;

import org.junit.*;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import de.uni_rostock.goodod.owl.TaxonomicDecompositionCollector;
/**
 * @author Niels Grewe
 *
 */
public class TaxonomicDecompositionSetTestCase extends TestCase {

	final private OWLDataFactory factory = OWLManager.getOWLDataFactory();
	final private String baseIRI = "http://www.phf.uni-rostock.de/goodod/test.owl#";
	final private Set<OWLClassExpression> expected = new HashSet<OWLClassExpression>();
	
	@Before public void setUp()
	{
		expected.clear();
	}
	
	private Set<OWLClassExpression> TDS(OWLClassExpression e)
	{
		return TaxonomicDecompositionCollector.collect(e);
	}
	
	private IRI IRI(String fragment)
	{
		return IRI.create(baseIRI + fragment);
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
}
