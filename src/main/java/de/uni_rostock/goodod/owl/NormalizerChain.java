/**
  Copyright (C) 2011 The University of Rostock.
 
  Written by:  Niels Grewe <niels.grewe@uni-rostock.de>
  Created: 15.12.2011
  
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

import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

/**
 * This class implements a simple chaining of ontology normalizations.
 * 
 * @author Niels Grewe <niels.grewe@uni-rostock.de>
 *
 */
public class NormalizerChain implements Normalizer {

	private List<? extends Normalizer> normalizers;
	public NormalizerChain(List<? extends Normalizer> theNormalizers)
	{
		normalizers = theNormalizers;
	}
	/* (non-Javadoc)
	 * @see de.uni_rostock.goodod.owl.Normalizer#normalize(org.semanticweb.owlapi.model.OWLOntology)
	 */
	@Override
	public void normalize(OWLOntology ont) throws OWLOntologyCreationException {
		for (Normalizer n : normalizers)
		{
			n.normalize(ont);
		}
	}

	/* (non-Javadoc)
	 * @see de.uni_rostock.goodod.owl.Normalizer#normalize(org.semanticweb.owlapi.model.OWLOntology, java.util.Set)
	 */
	@Override
	public void normalize(OWLOntology ont, Set<IRI> IRIs)
			throws OWLOntologyCreationException {
		for (Normalizer n : normalizers)
		{
			n.normalize(ont,IRIs);
		}
	}
	
	public List<? extends Normalizer>getNormalizers()
	{
		return normalizers;
	}
	
	/**
	 * Tests whether the chain contains a normalizer of the specified type.
	 * 
	 * @param aClass The class to test against.
	 * @return True if a normalizer is equivalent to aClass, a subclass of it,
	 * or implements the interface described by aClass. 
	 */
	public boolean containsNormalizerOfClass(Class<?> aClass)
	{
		for (Normalizer n : normalizers)
		{
			if (aClass.isAssignableFrom(n.getClass()))
			{
				return true;
			}
		}
		return false;
	}

}
