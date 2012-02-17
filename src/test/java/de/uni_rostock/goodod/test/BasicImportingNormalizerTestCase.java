/**
  Copyright (C) 2012 The University of Rostock.
 
  Written by:  thebeing
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

import org.junit.Before;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;

import de.uni_rostock.goodod.owl.BasicImportingNormalizer;

/**
 * @author thebeing
 *
 */
public class BasicImportingNormalizerTestCase extends
		AbstractNormalizerTestCase {
	@Override
	@Before public void setUp() throws OWLOntologyCreationException
	{
		super.setUp();
		
		OWLOntologyLoaderConfiguration loaderConf = new OWLOntologyLoaderConfiguration();
		loaderConf = loaderConf.addIgnoredImport(biotopA);
		loaderConf = loaderConf.addIgnoredImport(biotopB);
		loaderConf = loaderConf.setSilentMissingImportsHandling(true);
		normalizer = new BasicImportingNormalizer(loaderConf);
		Map<IRI,IRI> importMap = new HashMap<IRI,IRI>();
		importMap.put(biotopA, biotopCanonical);
		importMap.put(biotopB, biotopCanonical);
		((BasicImportingNormalizer)normalizer).setImportMappings(importMap);
	}
}
