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

import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.model.OWLOntology;

/**
 * @author Niels Grewe
 *
 */
public class NormalizerChainFactory extends AbstractNormalizerFactory {

	private List<? extends NormalizerFactory> factories;
	
	public NormalizerChainFactory(NormalizerFactory... someFactories)
	{
		List<NormalizerFactory> fs = new ArrayList<NormalizerFactory>();
		
		for (NormalizerFactory f : someFactories)
		{
			fs.add(f);
		}
		factories = fs;
	}
	/* (non-Javadoc)
	 * @see de.uni_rostock.goodod.owl.NormalizerFactory#getNormalizerForOntology(org.semanticweb.owlapi.model.OWLOntology)
	 */
	
	public Normalizer getNormalizerForOntology(OWLOntology ont) {
		if (factories.isEmpty())
		{
			return null;
		}
		List<Normalizer> norms = new ArrayList<Normalizer>(factories.size());
		for (NormalizerFactory f : factories)
		{
			norms.add(f.getNormalizerForOntology(ont));
		}
		return new NormalizerChain(norms);
	}

}