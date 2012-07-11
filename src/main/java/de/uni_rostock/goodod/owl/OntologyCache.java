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


import java.io.File;
import java.net.URI;
import java.util.Map.Entry;
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
	private final Set<OWLOntologyIRIMapper> mappers;
	private final Map<URI,FutureTask<OWLOntology>> futures;
	private static Log logger = LogFactory.getLog(OntologyCache.class);
	private NormalizerFactory normalizerFactory;
	private int pendingFutures;

	
	public OntologyCache(Set<OWLOntologyIRIMapper>IRIMappers, Set<IRI>importsToIgnore, int threads)
	{
		threadCount = threads;
		pendingFutures = 0;
		executor = Executors.newFixedThreadPool(threadCount);
		OWLOntologyLoaderConfiguration interimConfig = new OWLOntologyLoaderConfiguration();
		
		for (IRI theIRI : importsToIgnore)
		{
			interimConfig = interimConfig.addIgnoredImport(theIRI);
		}
		interimConfig = interimConfig.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
		config = interimConfig;
		mappers = IRIMappers;
		futures = new HashMap<URI,FutureTask<OWLOntology>>(24);
	}
	
	public OWLOntology getOntologySynchronouslyAtURI(URI theURI) throws OWLOntologyCreationException
	{
		OWLOntology ontology = null;
		FutureTask<OWLOntology>ontologyFuture = getOntologyFutureAtURI(theURI);
		try
		{
			ontology = ontologyFuture.get();
		}
		catch (Throwable e)
		{
			logger.warn("Could not load ontology", e);
		}

		return ontology;
	}
	
	private synchronized void futureDone()
	{
		pendingFutures--;
	}

	public synchronized void removeOntologyAtURI(URI u)
	{
		FutureTask<OWLOntology> future = futures.get(u);
		if (null == future)
		{
			return;
		}
		else if (false == future.isDone())
		{
			future.cancel(true);
		}
		futures.remove(u);
	}
	
	public synchronized void flushCache()
	{
		for (Entry<URI, FutureTask<OWLOntology>> f : futures.entrySet())
		{
			FutureTask<OWLOntology> future = f.getValue();
			if (false == future.isDone())
			{
				future.cancel(true);
			}
		}
		futures.clear();
	}
	
	public FutureTask<OWLOntology> getOntologyAtURI(URI theURI) throws OWLOntologyCreationException
	{
		int waitCount = 0;
		while (pendingFutures > threadCount)
		{
			if (0 == ++waitCount % 8 )
			{
				
				/* 
				 * Thight loop a few times, then yield in order to let
				 * the other threads finish.
				 */
				Thread.yield();
			}
		}
		return getOntologyFutureAtURI(theURI);
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
						for (OWLOntologyIRIMapper m : mappers)
						{
							manager.addIRIMapper(m);
						}
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
						// Mark this future as done.
						futureDone();
						return ontology;
					}});
		futures.put(u, future);
		// We track our pending futures
		pendingFutures++;
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


