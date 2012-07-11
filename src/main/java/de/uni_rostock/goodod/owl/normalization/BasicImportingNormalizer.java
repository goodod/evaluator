/**
  Copyright (C) 2011 The University of Rostock.
 
  Written by:  Niels Grewe
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
package de.uni_rostock.goodod.owl.normalization;

import java.util.Set;
import java.util.Map;
import org.semanticweb.owlapi.model.*;

/**
 * @author Niels Grewe
 *
 */
public class BasicImportingNormalizer extends BasicNormalizer {

	private OWLOntologyLoaderConfiguration config;
	public BasicImportingNormalizer(OWLOntology ont, Map<IRI,IRI> importMap, OWLOntologyLoaderConfiguration conf)
	{
		super(ont, importMap);
		config = conf;
	}
	
	public void normalize() throws OWLOntologyCreationException
	{
		super.normalize();
		Set<OWLImportsDeclaration> imports = ontology.getImportsDeclarations();
	
		for (OWLImportsDeclaration i : imports)
		{
			IRI theIRI = i.getIRI();
			IRI remappedIRI = IRIMap.get(theIRI);
			if (null != remappedIRI)
			{
				theIRI = remappedIRI;
			}
			
			if (false == manager.contains(theIRI))
			{
				try
				{
					manager.makeLoadImportRequest(i,config);
				}
				catch (OWLOntologyCreationException e)
				{
					throw e;
				}
			}
		}
		
	}
	
	@Override
	public void normalize(Set<IRI>targetIRIs) throws OWLOntologyCreationException 
	{
		try
		{
			normalize();
		}
		catch (OWLOntologyCreationException e)
		{
			throw e;
		}
	}
}
