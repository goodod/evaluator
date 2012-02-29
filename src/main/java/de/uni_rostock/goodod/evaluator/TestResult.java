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
public class TestResult {

	private int precisionCount;
	private int recallCount;
	private double accumulatedPrecision;
	private double accumulatedRecall;
	private Set<URI>computed;
	private Set<URI>reference;

	public TestResult(int preCount, double p, int recCount, double r, Set<URI>comp, Set<URI> ref)
	{
		precisionCount = preCount;
		accumulatedPrecision = p;
		recallCount = recCount;
		accumulatedRecall = r;
		computed = comp;
		reference = ref;
	}
	
	public double getMeanPrecision()
	{
		return accumulatedPrecision/precisionCount;
	}
	
	public double getMeanRecall()
	{
		return accumulatedRecall/recallCount;
	}
	
	public double getMeanFMeasure()
	{
		return (2 * getMeanRecall() * getMeanPrecision())/ (getMeanRecall() + getMeanPrecision());
	}
	
	public Set<URI> getReferenceSet()
	{
		return reference;
	}
	
	public Set<URI> getComputedSet()
	{
		return computed;
	}
	
	@Override
	public String toString()
	{
		return "" + getMeanPrecision() + '\t' + getMeanRecall() + '\t' + getMeanFMeasure();
	}
}
