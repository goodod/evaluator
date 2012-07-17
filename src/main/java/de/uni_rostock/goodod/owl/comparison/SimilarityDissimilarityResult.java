/**
  Copyright (C) 2012 The University of Rostock.
 
  Written by:  Niels Grewe <niels.grewe@uni-rostock.de>
  Created: 17.07.2012
  
  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  as published by the Free Software Foundation; either
  version 3 of the License, or (at your option) any later version.

  You should have received a copy of the GNU General Public
  License along with this program; see the file COPYING.
  If not, write to the Free Software Foundation,
  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 */
package de.uni_rostock.goodod.owl.comparison;

import java.util.concurrent.ExecutionException;

import de.uni_rostock.goodod.owl.OntologyPair;

/**
 * @author Niels Grewe
 * Generic class for reporting similarity and dissimilarity for an ontology pair.
 */
public class SimilarityDissimilarityResult extends ComparisonResult {

	protected double similarity;
	protected double dissimilarity;
	
	public SimilarityDissimilarityResult(final String method, OntologyPair pair, double theSimilarity, double theDissimilarity) throws InterruptedException, ExecutionException
	{
		super(method, pair);
		similarity = theSimilarity;
		dissimilarity = theDissimilarity;
	}
	/* (non-Javadoc)
	 * @see de.uni_rostock.goodod.owl.comparison.ComparisonResult#getSimilarity()
	 */
	@Override
	public double getSimilarity() {
		return similarity;
	}

	/**
	 * 
	 * @return The dissimilarity between the ontologies in the pair.
	 */
	public double getDissimilarity() {
		return dissimilarity;
	}
	/* (non-Javadoc)
	 * @see de.uni_rostock.goodod.owl.comparison.ComparisonResult#getSimilarityType()
	 */
	@Override
	public String getSimilarityType() {
		// No specifics about this kind of similarity.
		return "Generic";
	}
	
	@Override
	public String toString() {
		return super.toString() + "Dissimilarity (Generic): " + getDissimilarity() + '\n';
	}

}
