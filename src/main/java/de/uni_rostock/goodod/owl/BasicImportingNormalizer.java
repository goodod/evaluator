/**
  Copyright (C) 2011 The University of Rostock.
 
  Written by:  thebeing
  Created: 13.12.2011
  
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

import java.util.Set;

import org.semanticweb.owlapi.model.*;

/**
 * @author thebeing
 *
 */
public class BasicImportingNormalizer extends BasicNormalizer {

	private OWLOntologyLoaderConfiguration config;
	public BasicImportingNormalizer(OWLOntologyLoaderConfiguration conf)
	{
		super();
		config = conf;
	}
	
	public void normalize(OWLOntology ont) throws OWLOntologyCreationException
	{
		super.normalize(ont);
		Set<OWLImportsDeclaration> imports = ont.getImportsDeclarations();
		OWLOntologyManager man = ont.getOWLOntologyManager();
	
		for (OWLImportsDeclaration i : imports)
		{
			IRI theIRI = i.getIRI();
			IRI remappedIRI = IRIMap.get(theIRI);
			if (null != remappedIRI)
			{
				theIRI = remappedIRI;
			}
			
			if (false == man.contains(theIRI))
			{
				try
				{
					man.makeLoadImportRequest(i,config);
				}
				catch (OWLOntologyCreationException e)
				{
					throw e;
				}
			}
		}
		
	}
	
	@Override
	public void normalize(OWLOntology ont, Set<IRI>targetIRIs) throws OWLOntologyCreationException 
	{
		try
		{
			normalize(ont);
		}
		catch (OWLOntologyCreationException e)
		{
			throw e;
		}
	}
}
