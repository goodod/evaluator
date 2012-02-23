/**
  Copyright (C) 2011 The University of Rostock.
 
  Written by:  Niels Grewe <niels.grewe@uni-rostock.de>
  Created: 14.12.2011
  
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

import java.util.concurrent.ExecutionException;

/**
 * @author Niels Grewe <niels.grewe@uni-rostock.de>
 *
 */
public class FMeasureComparisonResult extends ComparisonResult {

	private double precision;
	private double recall;
	FMeasureComparisonResult(String method, OntologyPair thePair, double thePrecision, double theRecall) throws InterruptedException, ExecutionException
	{
		super(method, thePair);
		precision = thePrecision;
		recall = theRecall;
		
	}
	
	public double getPrecision()
	{
		return precision;
	}
	
	public double getRecall()
	{
		return recall;
	}
	
	public double getFMeasure()
	{
		return ((2 * precision * recall)/(precision + recall));
	}
	/* (non-Javadoc)
	 * @see de.uni_rostock.goodod.owl.ComparisonResult#getSimilarity()
	 */
	@Override
	public double getSimilarity() {
		return getFMeasure();
	}

	/* (non-Javadoc)
	 * @see de.uni_rostock.goodod.owl.ComparisonResult#getSimilarityType()
	 */
	@Override
	public String getSimilarityType() {
		return "F-Measure";
	}
	
	@Override
	public String toString()
	{
		String header = super.toString();
		return header.concat("Precision: " + precision + '\n' +
				"Recall: " + recall + '\n');
	}
}
