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

import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.semanticweb.owlapi.model.IRI;

/**
 * @author Niels Grewe <niels.grewe@uni-rostock.de>
 *
 */
public interface Comparator {

	/**
	 * Performs a comparison over the entirety of the two ontologies.
	 *
	 * @returns A parameterized result for the comparison.
	 */
	ComparisonResult compare() throws InterruptedException, ExecutionException;
	
	/**
	 * Performs a comparison over the named set of class IRIs.
	 * 
	 * @param classIRIs The class IRIs to consider for the comparison.
	 * @return A parameteried result for the comparison.
	 */
	ComparisonResult compare(Set<IRI>classIRIs) throws InterruptedException, ExecutionException;
}
