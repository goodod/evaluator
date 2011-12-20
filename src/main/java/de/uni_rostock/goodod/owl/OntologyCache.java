/**
  Copyright (C) 2011 The University of Rostock.
 
  Written by:  Niels Grewe <niels.grewe@uni-rostock.de>
  Created: 20.12.2011
  
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

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.SimpleIRIMapper;

import java.io.File;
import java.net.URI;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

/**
 * @author Niels Grewe <niels.grewe@uni-rostock.de>
 *
 */
public class OntologyCache {

	private final OWLOntologyLoaderConfiguration config;
	private final SimpleIRIMapper bioTopLiteMapper;
	private final Map<URI,OWLOntology> ontologies;
	
	public OntologyCache(URI commonBioTopLiteURI, Set<IRI>importsToIgnore)
	{
		OWLOntologyLoaderConfiguration interimConfig = new OWLOntologyLoaderConfiguration();
		for (IRI theIRI : importsToIgnore)
		{
			interimConfig = interimConfig.addIgnoredImport(theIRI);
		}
		//FIXME: Some student ontologies have bogus imports. This is not the real way to deal with it
		config = interimConfig.setSilentMissingImportsHandling(true);
		bioTopLiteMapper = new SimpleIRIMapper(IRI.create("http://purl.org/biotop/biotoplite.owl"),IRI.create(commonBioTopLiteURI));
	
		ontologies = new HashMap<URI,OWLOntology>(24);
	}
	
	public synchronized OWLOntology getOntologyAtURI(URI theURI) throws OWLOntologyCreationException
	{
		OWLOntology ontology = ontologies.get(theURI);
		if (null == ontology)
		{
			ontology = getNewOntologyAtURI(theURI);
		}
		return ontology;
	}
	
	public synchronized void removeOntologyAtURI(URI u)
	{
		ontologies.remove(u);
	}
	
	public synchronized void flushCache()
	{
		ontologies.clear();
	}
	
	private OWLOntology getNewOntologyAtURI(URI u) throws OWLOntologyCreationException
	{
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		manager.addIRIMapper(bioTopLiteMapper);
		FileDocumentSource source = new FileDocumentSource(new File(u));
		OWLOntology ontology = manager.loadOntologyFromOntologyDocument(source, config);
		ontologies.put(u,ontology);
		return ontology;
	}
	
	public OWLOntologyLoaderConfiguration getOntologyLoderConfiguration()
	{
		return config;
	}
}


