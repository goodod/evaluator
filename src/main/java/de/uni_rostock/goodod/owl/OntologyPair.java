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
	private FutureTask<OWLOntology> futureA;
	private FutureTask<OWLOntology> futureB;
	private OWLOntology ontologyA;
	private OWLOntology ontologyB;
	
	public OntologyPair(OntologyCache theCache, URI ontA, URI ontB) throws OWLOntologyCreationException
	{	
		cache = theCache;
		try
		{
			futureA = cache.getOntologyAtURI(ontA);
			futureB = cache.getOntologyAtURI(ontB);
		}
		catch (OWLOntologyCreationException exception)
		{
			// We just rethrow this and let the caller print the error.
			throw exception;
		}
	}
	public OWLOntology getOntologyA() throws InterruptedException, ExecutionException
	{
		if (null == ontologyA)
		{
			ontologyA = futureA.get();
		}
		return ontologyA;
	}
	public OWLOntology getOntologyB() throws InterruptedException, ExecutionException
	{
		if (null == ontologyB)
		{
			ontologyB = futureB.get();
		}
		return ontologyB;
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
			OWLOntologyManager manA = getOntologyA().getOWLOntologyManager();
			OWLOntologyManager manB = getOntologyB().getOWLOntologyManager();
			desc = manA.getOntologyDocumentIRI(getOntologyA()).toQuotedString().concat(" against ").concat(manB.getOntologyDocumentIRI(getOntologyB()).toQuotedString());
		}
		catch (Throwable e)
		{
			desc = "Indescriptible pair.";
		}
		return desc;
	}
}
