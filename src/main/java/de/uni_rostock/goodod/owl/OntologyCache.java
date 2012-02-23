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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.SimpleIRIMapper;

import de.uni_rostock.goodod.tools.Configuration;



import java.io.File;
import java.net.URI;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

/**
 * @author Niels Grewe <niels.grewe@uni-rostock.de>
 *
 */
public class OntologyCache {

	private final int threadCount;
	private ExecutorService executor;
	private final OWLOntologyLoaderConfiguration config;
	private final SimpleIRIMapper bioTopLiteMapper;
	private final Map<URI,OWLOntology> ontologies;
	private final Map<URI,FutureTask<OWLOntology>> futures;
	private static Log logger = LogFactory.getLog(OntologyCache.class);
	private NormalizerFactory normalizerFactory;
	
	public OntologyCache(URI commonBioTopLiteURI, Set<IRI>importsToIgnore)
	{

		threadCount = Configuration.getConfiguration().getInt("threadCount");
		executor = Executors.newFixedThreadPool(threadCount);
		OWLOntologyLoaderConfiguration interimConfig = new OWLOntologyLoaderConfiguration();
		for (IRI theIRI : importsToIgnore)
		{
			interimConfig = interimConfig.addIgnoredImport(theIRI);
		}
		config = interimConfig.setSilentMissingImportsHandling(true);
		bioTopLiteMapper = new SimpleIRIMapper(IRI.create("http://purl.org/biotop/biotoplite.owl"),IRI.create(commonBioTopLiteURI));
	
		ontologies = new HashMap<URI,OWLOntology>(24);
		futures = new HashMap<URI,FutureTask<OWLOntology>>(24);
	}
	
	public OWLOntology getOntologyAtURI(URI theURI) throws OWLOntologyCreationException
	{
		OWLOntology ontology = getOldOntologyAtURI(theURI);
		if (null == ontology)
		{
			FutureTask<OWLOntology>ontologyFuture = getOntologyFutureAtURI(theURI);
			try
			{
				ontology = ontologyFuture.get();
			}
			catch (Throwable e)
			{
				logger.warn("Could not load ontology", e);
			}
		}
		return ontology;
	}
	
	private synchronized void putOntologyAtURI(URI u, OWLOntology ontology)
	{
		ontologies.put(u, ontology);
		if (futures.containsKey(u))
		{
			futures.remove(u);
		}
	}
	private synchronized OWLOntology getOldOntologyAtURI(URI theURI) throws OWLOntologyCreationException
	{
		return ontologies.get(theURI);
	}
	public synchronized void removeOntologyAtURI(URI u)
	{
		ontologies.remove(u);
	}
	
	public synchronized void flushCache()
	{
		ontologies.clear();
	}
	
	private synchronized FutureTask<OWLOntology> getOntologyFutureAtURI(final URI u) throws OWLOntologyCreationException
	{
		FutureTask<OWLOntology> future = null;
		
		future = futures.get(u);
		if (null != future)
		{
			return future;
		}
		future = new FutureTask<OWLOntology>(new Callable<OWLOntology>()
				{
					public OWLOntology call() throws ExecutionException
					{
						OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
						manager.addIRIMapper(bioTopLiteMapper);
						FileDocumentSource source = new FileDocumentSource(new File(u));
						OWLOntology ontology;
						try 
						{
							ontology = manager.loadOntologyFromOntologyDocument(source, config);
						
							logger.info("Loading and normalizing ontology from " + u.toString() + ".");
							if (null != normalizerFactory)
							{
								normalizerFactory.normalize(ontology);
							}
						}
						catch (OWLOntologyCreationException e)
						{
							throw new ExecutionException(e);
						}
						putOntologyAtURI(u, ontology);
						return ontology;
					}});
		futures.put(u, future);
		executor.execute(future);
		
		return future;
	}
	
	public void setNormalizerFactory(NormalizerFactory n)
	{
		normalizerFactory = n;
		
	}
	
	public NormalizerFactory getNormalizerFactory()
	{
		return normalizerFactory;
	}
	
	public OWLOntologyLoaderConfiguration getOntologyLoaderConfiguration()
	{
		return config;
	}
	
	public void teardown()
	{
		flushCache();
		executor.shutdownNow();
		executor = null;
		
	}
	
	@Override
	public void finalize() throws Throwable
	{
		try
		{
			if (null != executor)
			{
				executor.shutdownNow();
			}
		}
		finally
		{
			super.finalize();
		}
	}
}


