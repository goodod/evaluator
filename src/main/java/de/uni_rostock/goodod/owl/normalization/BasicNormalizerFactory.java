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


import java.util.Map;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;


/**
 * @author Niels Grewe
 *
 */
public class BasicNormalizerFactory extends AbstractNormalizerFactory {
	protected Map<IRI,IRI> importMap;
	
	public BasicNormalizerFactory(Map<IRI,IRI>map)
	{
		importMap = map;
	}


	public Normalizer getNormalizerForOntology(OWLOntology ont) {
		return new BasicNormalizer(ont, importMap);
	}

}
