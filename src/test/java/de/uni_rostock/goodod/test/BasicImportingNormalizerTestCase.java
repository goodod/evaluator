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

import java.io.File;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.LogFactory;
import org.junit.*;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.SimpleIRIMapper;

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
		IRI physicalTestBioTop = null;
		try
		{
			physicalTestBioTop = IRI.create(BasicImportingNormalizerTestCase.class.getResource(File.separatorChar + "biotoplite.owl").toURI());
		} 
		catch (URISyntaxException e)
		{
			LogFactory.getLog(BasicImportingNormalizerTestCase.class).error("Could not get phyiscal BioTopURI", e);
		}
		SimpleIRIMapper bioTopMapper = new SimpleIRIMapper(biotopCanonical, physicalTestBioTop);
		manager.addIRIMapper(bioTopMapper);
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
	
	@Test public void testReplacesBioTopA() throws OWLOntologyCreationException
	{
		OWLImportsDeclaration importA = factory.getOWLImportsDeclaration(biotopA);
		OWLImportsDeclaration importCanonical = factory.getOWLImportsDeclaration(biotopCanonical);
		AddImport addImport = new AddImport(ontology, importA);
		manager.applyChange(addImport);
		normalizer.normalize(ontology);
		Set<OWLImportsDeclaration> imports = ontology.getImportsDeclarations();
		assertTrue(imports.contains(importCanonical));
		assertFalse(imports.contains(importA));
		assertFalse(ontology.getClassesInSignature(true).isEmpty());
	}
	
	@Test public void testReplacesBioTopB() throws OWLOntologyCreationException
	{
		OWLImportsDeclaration importB = factory.getOWLImportsDeclaration(biotopA);
		OWLImportsDeclaration importCanonical = factory.getOWLImportsDeclaration(biotopCanonical);
		AddImport addImport = new AddImport(ontology, importB);
		manager.applyChange(addImport);
		normalizer.normalize(ontology);
		Set<OWLImportsDeclaration> imports = ontology.getImportsDeclarations();
		assertTrue(imports.contains(importCanonical));
		assertFalse(imports.contains(importB));
		assertFalse(ontology.getClassesInSignature(true).isEmpty());
	}
}
