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

import org.apache.commons.configuration.SubnodeConfiguration;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import de.uni_rostock.goodod.tools.Configuration;

/**
 * @author Niels Grewe
 *
 */
public abstract class AbstractNormalizerFactory implements NormalizerFactory {

	protected SubnodeConfiguration config;

	public AbstractNormalizerFactory()
	{
		// Just set the configuration if it's available.
		config = Configuration.getConfiguration().configurationFromDomainForClassWithShorthandSuffix("normalizers",
		  this.getClass(),
		  "NormalizerFactory");
		
	}
	/* (non-Javadoc)
	 * @see de.uni_rostock.goodod.owl.NormalizerFactory#normalize(org.semanticweb.owlapi.model.OWLOntology)
	 */
	public void normalize(OWLOntology ont) throws OWLOntologyCreationException {
		Normalizer n = getNormalizerForOntology(ont);
		if (null != n)
		{
			n.normalize();
		}
	}

	/* (non-Javadoc)
	 * @see de.uni_rostock.goodod.owl.NormalizerFactory#normalize(org.semanticweb.owlapi.model.OWLOntology, java.util.Set)
	 */
	public void normalize(OWLOntology ont, Set<IRI> IRIs)
			throws OWLOntologyCreationException {
		Normalizer n = getNormalizerForOntology(ont);
		if (null != n)
		{
			n.normalize(IRIs);
		
		}
	}
	
}
