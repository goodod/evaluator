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


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.configuration.SubnodeConfiguration;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;


/**
 * @author Niels Grewe
 *
 */
public class BasicNormalizerFactory extends AbstractNormalizerFactory {
	protected Map<IRI,IRI> importMap;
	
	public BasicNormalizerFactory()
	{
		importMap = new HashMap<IRI,IRI>();
		populateMap();
	}
	public BasicNormalizerFactory(Map<IRI,IRI>map)
	{
		importMap = map;
	}


	public Normalizer getNormalizerForOntology(OWLOntology ont) {
		return new BasicNormalizer(ont, importMap);
	}
	
	protected void populateMap()
	{
		if (null == config)
		{
			return;
		}
		SubnodeConfiguration mapCfg = config.configurationAt("importMap");
		@SuppressWarnings("unchecked")
		Iterator<String> iter = mapCfg.getKeys();
		while (iter.hasNext())
		{
			String key = iter.next();
			String oldImport = key.replace("..", ".");
			String newImport = mapCfg.getString(key);
			importMap.put(IRI.create(oldImport), IRI.create(newImport));
		}

	}

}
