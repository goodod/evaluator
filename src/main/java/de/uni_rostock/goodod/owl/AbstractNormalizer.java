/**
  Copyright (C) 2012 The University of Rostock.
 
  Written by:  Niels Grewe
  Created: 23.02.2012
  
  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  as published by the Free Software Foundation; either
  version 3 of the License, or (at your option) any later version.

  You should have received a copy of the GNU General Public
  License along with this program; see the file COPYING.
  If not, write to the Free Software Foundation,
  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 */
package de.uni_rostock.goodod.owl;


import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLAxiomVisitorAdapter;

/**
 * @author Niels Grewe
 * This abstract normalizer superclass inherits from OWLAxiomVisitorAdapter
 * because many normalizers need to visit axioms, where the default in
 * OWLAxiomVisitorAdapter is a no-op for every axiom. 
 */
public abstract class AbstractNormalizer extends  OWLAxiomVisitorAdapter implements Normalizer{
	protected OWLOntology ontology;
	protected OWLOntologyManager manager;
	protected OWLDataFactory factory;
	protected Set<OWLOntologyChange> changes;
	public AbstractNormalizer(OWLOntology ont)
	{
		ontology = ont;
		manager = ontology.getOWLOntologyManager();
		factory = manager.getOWLDataFactory();
		changes = new HashSet<OWLOntologyChange>();
	}
	
	public OWLOntology getOntology()
	{
		return ontology;
	}
	
	public void normalize() throws OWLOntologyCreationException {

		Set<OWLClass> classes = ontology.getClassesInSignature(true);
		Set<IRI> IRIs = new HashSet<IRI>();
		for (OWLClass c : classes) {
			IRIs.add(c.getIRI());
		}
		normalize(IRIs);
	}
}
