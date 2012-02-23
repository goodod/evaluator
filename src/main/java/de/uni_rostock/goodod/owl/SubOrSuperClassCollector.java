/**
  Copyright (C) 2011 The University of Rostock.
 
  Written by:  Niels Grewe
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

import org.semanticweb.owlapi.model.*;
import java.util.Set;
import java.util.HashSet;

/**
 * Abstract superclass for collecting the sub- or superclass expressions of
 * a given class in a set of ontologies.
 * @author Niels Grewe <niels.grewe@uni-rostock.de>
 *
 */
public abstract class SubOrSuperClassCollector {
	protected final Set<OWLOntology> ontologies;
	protected final OWLClass sourceClass;
	private final Set<OWLClass> queue;
	private final Set<OWLClass> targetClasses;
	SubOrSuperClassCollector(OWLClass c,Set<OWLOntology> ont)
	{
		ontologies = ont;
		sourceClass = c;
		targetClasses = new HashSet<OWLClass>();
		queue = new HashSet<OWLClass>();
	}
	
	abstract protected Set<OWLClassExpression> getExpressionsStartingFrom(OWLClass c);
	
	private Set<OWLClass> getClassesStartingFrom(OWLClass c)
	{
		Set<OWLClass>results = new HashSet<OWLClass>();
		Set<OWLClassExpression> thisLayer = getExpressionsStartingFrom(c);
		for (OWLClassExpression ce : thisLayer)
		{
			if (ce instanceof OWLClass)
			{
				OWLClass theClass = ce.asOWLClass();
				if (targetClasses.contains(theClass))
				{
					continue;
				}
				results.add(theClass);
			}
		}
	return results;
	}
	
	public Set<OWLClass> collect()
	{
		queue.addAll(getClassesStartingFrom(sourceClass));
		Set<OWLClass> newQueue = new HashSet<OWLClass>();
		while (0 != queue.size())
		{
			targetClasses.addAll(queue);
			for (OWLClass c : queue)
			{
				newQueue.addAll(getClassesStartingFrom(c));
			}
			queue.clear();
			queue.addAll(newQueue);
			newQueue.clear();
		}
	return targetClasses;
	}
	
	
}
