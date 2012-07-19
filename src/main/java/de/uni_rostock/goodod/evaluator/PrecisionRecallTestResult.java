/**
  Copyright (C) 2011 The University of Rostock.
 
  Written by:  Niels Grewe <niels.grewe@uni-rostock.de>
  Created: 18.12.2011
  
  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  as published by the Free Software Foundation; either
  version 3 of the License, or (at your option) any later version.

  You should have received a copy of the GNU General Public
  License along with this program; see the file COPYING.
  If not, write to the Free Software Foundation,
  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 */
package de.uni_rostock.goodod.evaluator;

import java.net.URI;
import java.util.Set;

/**
 * @author Niels Grewe <niels.grewe@uni-rostock.de>
 *
 */
public class PrecisionRecallTestResult extends TestResult {

	private double meanPrecision;
	private double meanRecall;
	public PrecisionRecallTestResult(int preCount, double p, int recCount, double r, Set<URI>comp, Set<URI> ref)
	{
		super(1, (2 * (r/recCount) * (p/preCount)) / ((r/recCount) + (p/preCount)), comp, ref);
		meanPrecision = p/preCount;
		meanRecall = r/recCount;
	}
	
	public double getMeanPrecision()
	{
		return meanPrecision;
	}
	
	public double getMeanRecall()
	{
		return meanRecall;
	}
	
	public double getMeanFMeasure()
	{
		return getMeanSimilarity();
	}
	
	@Override
	public String toString()
	{
		return "" + getMeanPrecision() + '\t' + getMeanRecall() + '\t' + getMeanFMeasure();
	}
}
