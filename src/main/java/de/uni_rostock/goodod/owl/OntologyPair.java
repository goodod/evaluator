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

import org.semanticweb.owlapi.model.*;


public class OntologyPair {
	private OntologyCache cache;
	private OWLOntology ontologyA;
	private OWLOntology ontologyB;
	
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
		return cache.getOntologyLoaderConfiguration();
	}
	
	public void normalizeWithNormalizerFactory(NormalizerFactory normalizer) throws OWLOntologyCreationException
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
	
	public void normalizeWithNormalizerFactory(NormalizerFactory normalizer, Set<IRI>IRIsToConsider) throws OWLOntologyCreationException
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
