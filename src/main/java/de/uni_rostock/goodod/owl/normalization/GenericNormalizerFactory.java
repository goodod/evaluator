/**
  Copyright (C) 2012 The University of Rostock.
 
  Written by:  Niels Grewe
  Created: 21.07.2012
  
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.semanticweb.owlapi.model.OWLOntology;

import de.uni_rostock.goodod.owl.normalization.AbstractNormalizerFactory;

/**
 * @author Niels Grewe
 *
 */
public class GenericNormalizerFactory extends AbstractNormalizerFactory {

	private Class<? extends Normalizer> normalizerClass;
	private static Log logger = LogFactory.getLog(GenericNormalizerFactory.class);
	public GenericNormalizerFactory(Class<? extends Normalizer> theNormalizerClass)
	{
		normalizerClass = theNormalizerClass;
	}
	/* (non-Javadoc)
	 * @see de.uni_rostock.goodod.owl.normalization.NormalizerFactory#getNormalizerForOntology(org.semanticweb.owlapi.model.OWLOntology)
	 */
	public Normalizer getNormalizerForOntology(OWLOntology ont) {
		
		Normalizer norm = null;
		try
		{
			norm = normalizerClass.newInstance();
		}
		catch (Throwable t)
		{
			logger.warn("Could not create normalizer of type " + normalizerClass.getCanonicalName());
			norm = null;
		}
		return norm;
	}

}
