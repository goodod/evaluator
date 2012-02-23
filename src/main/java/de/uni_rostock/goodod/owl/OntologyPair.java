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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import org.semanticweb.owlapi.model.*;


public class OntologyPair {
	private OntologyCache cache;
	private FutureTask<OWLOntology> ontologyA;
	private FutureTask<OWLOntology> ontologyB;
	
	public OntologyPair(OntologyCache theCache, URI ontA, URI ontB) throws OWLOntologyCreationException
	{	
		cache = theCache;
		try
		{
			ontologyA = cache.getOntologyAtURI(ontA);
			ontologyB = cache.getOntologyAtURI(ontB);
		}
		catch (OWLOntologyCreationException exception)
		{
			// We just rethrow this and let the caller print the error.
			throw exception;
		}
	}
	public OWLOntology getOntologyA() throws InterruptedException, ExecutionException
	{
		return ontologyA.get();
	}
	public OWLOntology getOntologyB() throws InterruptedException, ExecutionException
	{
		return ontologyB.get();
	}
	
	public OWLOntologyLoaderConfiguration getLoaderConfiguration()
	{
		return cache.getOntologyLoaderConfiguration();
	}
	
	
	@Override
	public String toString()
	{
		String desc = "";
		try
		{
			OWLOntologyManager manA = ontologyA.get().getOWLOntologyManager();
			OWLOntologyManager manB = ontologyB.get().getOWLOntologyManager();
			desc = manA.getOntologyDocumentIRI(ontologyA.get()).toQuotedString().concat(" against ").concat(manB.getOntologyDocumentIRI(ontologyB.get()).toQuotedString());
		}
		catch (Throwable e)
		{
			
		}
		return desc;
	}
}
