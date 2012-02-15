/**
  Copyright (C) 2011 The University of Rostock.
 
  Written by:  Niels Grewe <niels.grewe@uni-rostock.de>
  Created: 22.12.2011
  
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

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * This collector collects all named subclasses of the given class in the ontology set.
 * @author Niels Grewe <niels.grewe@uni-rostock.de>
 *
 */
public final class SubClassCollector extends SubOrSuperClassCollector {

	public SubClassCollector(OWLClass c,Set<OWLOntology> o)
	{
		super(c,o);
	}
	/* (non-Javadoc)
	 * @see de.uni_rostock.goodod.owl.SubOrSuperClassCollector#getExpressionsStartingFrom(org.semanticweb.owlapi.model.OWLClass)
	 */
	@Override
	protected Set<OWLClassExpression> getExpressionsStartingFrom(OWLClass c) {
		return c.getSubClasses(ontologies);
	}

	/**
	 * Returns all subclasses of a given class in a set of ontologies.
	 * @param c The class from which to start collecting.
	 * @param o The set of ontologies from which to collect.
	 * @return The (potentially indirect) subclasses of c.
	 */
	public static Set<OWLClass> collect(OWLClass c, Set<OWLOntology> o)
	{
		SubClassCollector col = new SubClassCollector(c, o);
		return col.collect();
	}
}
