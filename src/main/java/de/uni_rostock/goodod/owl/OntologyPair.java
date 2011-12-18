/**
  Copyright (C) 2011 The University of Rostock.
 
  Written by:  Niels Grewe <niels.grewe@uni-rostock.de>
  Created:  December 2011
  
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

import java.net.URI;
import java.util.Set;
import java.io.File;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.*;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.SimpleIRIMapper;

public class OntologyPair {
	private OWLOntologyLoaderConfiguration config;
	private OWLOntology ontologyA;
	private OWLOntology ontologyB;
	
	public OntologyPair(URI commonBioTop, URI ontA, URI ontB, Set<IRI> importsToIgnore) throws OWLOntologyCreationException
	{	
		/*
		 * Create a configuration that lets us ignore (and later remove) the group specific imports. 
		 */
		config = new OWLOntologyLoaderConfiguration();
		for (IRI theIRI : importsToIgnore)
		{
			config = config.addIgnoredImport(theIRI);
		}
		//FIXME: Some student ontologies have bogus imports. This is not the real way to deal with it
		config = config.setSilentMissingImportsHandling(true);
		
		/*
		 * Create a mapping for BioTopLite
		 */
		SimpleIRIMapper bioTopLiteMapper = new SimpleIRIMapper(IRI.create("http://purl.org/biotop/biotoplite.owl"),IRI.create(commonBioTop));
		
		// Create the managers.
		OWLOntologyManager managerA = OWLManager.createOWLOntologyManager();
		OWLOntologyManager managerB = OWLManager.createOWLOntologyManager();
		/*
		 * Install a mapping to BioTopLite into each manager. /
		 */
		managerA.addIRIMapper(bioTopLiteMapper);
		managerB.addIRIMapper(bioTopLiteMapper);
		
		/*
		 * Now load the ontologies.
		 */
		FileDocumentSource sourceA = new FileDocumentSource(new File(ontA));
		FileDocumentSource sourceB = new FileDocumentSource(new File(ontB));
		try
		{
			ontologyA = managerA.loadOntologyFromOntologyDocument(sourceA, config);
			ontologyB = managerB.loadOntologyFromOntologyDocument(sourceB, config);
		}
		catch (OWLOntologyCreationException exception)
		{
			// We just rethrow this and let the caller print the error.
			throw exception;
		}
	}
	public OWLOntology getOntologyA()
	{
		return ontologyA;
	}
	public OWLOntology getOntologyB()
	{
		return ontologyB;
	}
	
	public OWLOntologyLoaderConfiguration getLoaderConfiguration()
	{
		return config;
	}
	
	public void normalizeWithNormalizer(Normalizer normalizer) throws OWLOntologyCreationException
	{
		try
		{
			normalizer.normalize(ontologyA);
			normalizer.normalize(ontologyB);
		}
		catch (OWLOntologyCreationException exception)
		{
			throw exception;
		}
	}
	
	public void normalizeWithNormalizer(Normalizer normalizer, Set<IRI>IRIsToConsider) throws OWLOntologyCreationException
	{
		try
		{
			normalizer.normalize(ontologyA, IRIsToConsider);
			normalizer.normalize(ontologyB, IRIsToConsider);
		}
		catch (OWLOntologyCreationException e)
		{
			throw e;
		}
	}
	
	@Override
	public String toString()
	{
		OWLOntologyManager manA = ontologyA.getOWLOntologyManager();
		OWLOntologyManager manB = ontologyB.getOWLOntologyManager();
		return manA.getOntologyDocumentIRI(ontologyA).toQuotedString().concat(" against ").concat(manB.getOntologyDocumentIRI(ontologyB).toQuotedString());
	}
}
