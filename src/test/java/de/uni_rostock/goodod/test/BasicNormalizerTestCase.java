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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.*;
import org.junit.*;

import de.uni_rostock.goodod.owl.BasicNormalizer;

/**
 * @author Niels Grewe
 *
 */
public class BasicNormalizerTestCase extends AbstractNormalizerTestCase {

	@Override
	@Before public void setUp() throws OWLOntologyCreationException
	{
		super.setUp();
		normalizer = new BasicNormalizer();
		Map<IRI,IRI> importMap = new HashMap<IRI,IRI>();
		importMap.put(biotopA, biotopCanonical);
		importMap.put(biotopB, biotopCanonical);
		((BasicNormalizer)normalizer).setImportMappings(importMap);
	}
	
	@Test public void testReplacesBioTopA() throws OWLOntologyCreationException
	{
		IRI variantA = IRI.create(biotopA.toString() + "A");
		IRI canonical = IRI.create(biotopCanonical.toString() + "A");
		OWLClass variantClass = factory.getOWLClass(variantA);
		OWLClass canonicalClass = factory.getOWLClass(canonical);
		addClass(variantClass);
		normalizer.normalize(ontology);
		Set<OWLClass> classes = ontology.getClassesInSignature();
		assertTrue(classes.contains(canonicalClass));
		assertFalse(classes.contains(variantClass));
	}
	
	@Test public void testReplacesBioTopB() throws OWLOntologyCreationException
	{
		IRI variantB = IRI.create(biotopB.toString() + "A");
		IRI canonical = IRI.create(biotopCanonical.toString() + "A");
		OWLClass variantClass = factory.getOWLClass(variantB);
		OWLClass canonicalClass = factory.getOWLClass(canonical);
		addClass(variantClass);
		normalizer.normalize(ontology);
		Set<OWLClass> classes = ontology.getClassesInSignature();
		assertTrue(classes.contains(canonicalClass));
		assertFalse(classes.contains(variantClass));
	}

	@Test public void testDoesNotReplaceNonBioTopClass() throws OWLOntologyCreationException
	{
		IRI canonical = IRI.create(biotopCanonical.toString() + "A");
		OWLClass nonBioTopClass = factory.getOWLClass(IRI("A"));
		OWLClass bioTopClass = factory.getOWLClass(canonical);
		addClass(nonBioTopClass);
		normalizer.normalize(ontology);
		Set<OWLClass> classes = ontology.getClassesInSignature();
		assertFalse(classes.contains(bioTopClass));
		assertTrue(classes.contains(nonBioTopClass));
	}
}



