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

package de.uni_rostock.goodod.owl.normalization;

import java.util.*;
import org.semanticweb.owlapi.model.*;
/**
 * The normalizer interface can be adopted by any class that is used to
 * preprocess ontologies prior to evaluation.
 * 
 * @author Niels Grewe <niels.grewe@uni-rostock.de>
 *
 */
public interface Normalizer {
  

	/**
	 * Executes the normalization process
	 * .
	 */
	void normalize() throws OWLOntologyCreationException ;
	
	/**
	 * Normalizes the ontology by just taking into account the entities related
	 * to the named IRIs.
	 * 
	 * @param IRIs The IRIs to consider when normalizing
	 */
	void normalize(Set<IRI>IRIs) throws OWLOntologyCreationException ;
}
