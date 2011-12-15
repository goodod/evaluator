/**
  Copyright (C) 2011 The University of Rostock.
 
  Written by:  Niels Grewe <niels.grewe@uni-rostock.de>
  Created: 12.12.2011
  
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


public abstract class ComparisonResult
{
	private OntologyPair pair;
	private String comparisonMethod;
	public ComparisonResult(String method, OntologyPair aPair)
	{
		pair = aPair;
		comparisonMethod = method;
	}
	
	public OntologyPair getOntologyPair()
	{
		return pair;
	}
	
	public String getComparisonMethod()
	{
		return comparisonMethod;
	}
	
	abstract public double getSimilarity();
	
	abstract public String getSimilarityType();
	
	public String toString()
	{
		return "Ontology Pair: " + pair + '\n' +
				"Compared using: " + comparisonMethod + '\n' +
				"Similarity (" + getSimilarityType() + "): " + getSimilarity() + '\n';
	}
}
