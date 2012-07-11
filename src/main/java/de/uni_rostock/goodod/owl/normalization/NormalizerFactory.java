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
package de.uni_rostock.goodod.owl.normalization;

import java.util.Set;

import de.uni_rostock.goodod.owl.normalization.Normalizer;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
/**
 * @author Niels Grewe
 *
 */
public interface NormalizerFactory {
	/**
	 * Creates a normalizer for use with an ontology.
	 * @param ont The ontology to normalize
	 * @return A normalizer for the ontology
 	 */
	public Normalizer getNormalizerForOntology(OWLOntology ont);
	
	/**
	 * Normalizes the named ontology.
	 * 
	 * @param ont The ontology to be normalized.
	 */
	void normalize(OWLOntology ont) throws OWLOntologyCreationException ;
	
	/**
	 * Normalizes the ontology by just taking into account the entities related
	 * to the named IRIs.
	 * 
	 * @param ont The ontology to benormalized.
	 * @param IRIs The IRIs to consider when normalizing
	 */
	void normalize(OWLOntology ont, Set<IRI>IRIs) throws OWLOntologyCreationException ;
}
