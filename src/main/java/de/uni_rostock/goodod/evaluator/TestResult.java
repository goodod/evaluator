/**
  Copyright (C) 2012 The University of Rostock.
 
  Written by:  thebeing
  Created: 18.07.2012
  
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
 * @author Niels Grewe
 *
 * Generic class for recording similarity test results over groups of ontologies.
 */
public class TestResult {

	protected Set<URI> computed;
	protected Set<URI> reference;
	protected double meanSimilarity;

	/**
	 * 
	 */
	public TestResult(int simCount, double accSim, Set<URI>comp, Set<URI> ref) {
		super();
		computed = comp;
		reference = ref;
		meanSimilarity = accSim/(double)simCount;
	}

	public Set<URI> getReferenceSet() {
		return reference;
	}

	public Set<URI> getComputedSet() {
		return computed;
	}

	public double getMeanSimilarity()
	{
		return meanSimilarity;
	}
	
	@Override
	public String toString()
	{
		return "" + getMeanSimilarity();
	}
}