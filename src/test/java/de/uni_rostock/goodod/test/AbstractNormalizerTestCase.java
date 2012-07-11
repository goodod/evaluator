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

import de.uni_rostock.goodod.owl.normalization.NormalizerFactory;

/**
 * @author Niels Grewe
 *
 */
public abstract class AbstractNormalizerTestCase extends AbstractTestCase {

	protected OWLOntology ontology;
	protected NormalizerFactory normalizer;
	protected final IRI biotopCanonical = IRI.create("http://purl.org/biotop/biotoplite.owl");
	protected final IRI biotopA = IRI.create("http://purl.org/biotop/biotoplite_group_A_TEST.owl");
	protected final IRI biotopB = IRI.create("http://purl.org/biotop/biotoplite_group_B_TEST.owl");
	@Override
	@Before public void setUp() throws OWLOntologyCreationException
	{
		super.setUp();
		ontology = manager.createOntology(IRI.create(baseIRI));
	}
	
	protected OWLClass addClass(String name)
	{
		OWLClass c = factory.getOWLClass(IRI(name));
		addClass(c);
		return c;
	}
	
	protected void addClass(OWLClass c)
	{		
		OWLAxiom decl = factory.getOWLDeclarationAxiom(c);
		addAxiom(decl);
	}
	
	protected void addSubClassOf(OWLClassExpression sub, OWLClassExpression sup)
	{
		OWLAxiom axiom = factory.getOWLSubClassOfAxiom(sub, sup);
		addAxiom(axiom);
	}
	
	protected void addAxiom(OWLAxiom ax)
	{
		manager.addAxiom(ontology, ax);
	}
	
}


