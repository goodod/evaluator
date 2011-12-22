/**
  Copyright (C) 2011 The University of Rostock.
 
  Written by:  thebeing
  Created: 21.12.2011
  
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Reasoner;


/**
 * Materializes the subsumption hierarchy of an ontology using the HermiT
 * reasoner.
 * @author Niels Grewe
 *
 */
public class SubsumptionMaterializationNormalizer implements Normalizer {

	private OWLOntology ontology;
	private OWLOntologyManager manager;
	private OWLDataFactory factory;
	private Reasoner reasoner;
	private Set<OWLOntologyChange> changes;
	private static Log logger = LogFactory.getLog(SubsumptionMaterializationNormalizer.class);
	
	public SubsumptionMaterializationNormalizer()
	{
		changes = new HashSet<OWLOntologyChange>();
	}
	/* (non-Javadoc)
	 * @see de.uni_rostock.goodod.owl.Normalizer#normalize(org.semanticweb.owlapi.model.OWLOntology)
	 */
	@Override
	public void normalize(OWLOntology ont) throws OWLOntologyCreationException {
		
		Set<OWLClass> classes = ont.getClassesInSignature(true);
		Set<IRI> IRIs = new HashSet<IRI>();
		for (OWLClass c : classes)
		{
			IRIs.add(c.getIRI());
		}
		normalize(ont, IRIs);
	}

	/* (non-Javadoc)
	 * @see de.uni_rostock.goodod.owl.Normalizer#normalize(org.semanticweb.owlapi.model.OWLOntology, java.util.Set)
	 */
	@Override
	public synchronized void normalize(OWLOntology ont, Set<IRI> IRIs)
			throws OWLOntologyCreationException {
		
		logger.debug("Running subsumption materialization.");
		// Setup the state for this run.
		setupForOntology(ont);
		
		logger.debug("Classifying with reasoner.");
		// Let the reasoner do the classification
		reasoner.classifyClasses();
		
		
		// Find all entailed subsumptions
		findEntailedSubsumptions(IRIs);
		
		// Eliminate cycles.
		removeCyclicSubsumptions();
		
		// Clean the hierarchy to eliminate redundant axioms:
		
		cleanClassHierarchy();
		
		finish();
	}
	
	private void setupForOntology(OWLOntology ont)
	{
		ontology = ont;
		manager = ontology.getOWLOntologyManager();
		factory = manager.getOWLDataFactory();
		Configuration reasonerConfig = new Configuration();
		reasonerConfig.throwInconsistentOntologyException = false;
		//reasonerConfig.individualTaskTimeout = 10000;
		reasoner = new Reasoner(reasonerConfig, ontology);
	}
	
	private void commitChanges()
	{
		if (0 == changes.size())
		{
			return;
		}
		if (null != manager)
		{
			manager.applyChanges(new ArrayList<OWLOntologyChange>(changes));
		}
		changes.clear();
		if (null != reasoner)
		{
			reasoner.flush();
		}
	}
	
	private void finish()
	{
		if (0 != changes.size())
		{
			commitChanges();
		}
		reasoner = null;
		factory = null;
		manager = null;
		ontology = null;
	}

	private void findEntailedSubsumptions(Set<IRI> IRIs)
	{
		Set<OWLClass> classes = ontology.getClassesInSignature(true);
		
		for (OWLClass c : classes)
		{
			for (IRI i : IRIs)
			{
				OWLClass other = factory.getOWLClass(i);
				if (other.equals(c))
				{
					// Don't bother with trivialities
					continue;
				}
				OWLAxiom ax = factory.getOWLSubClassOfAxiom(c, other);
				OWLAxiom invAx = factory.getOWLSubClassOfAxiom(other,c);
				if (reasoner.isEntailed(ax))
				{
					changes.add(new AddAxiom(ontology,ax));
				}
				if (reasoner.isEntailed(invAx))
				{
					changes.add(new AddAxiom(ontology,invAx));
				}
			}
		}
		logger.debug("Found " + changes.size() + " new subsumptions.");
		// Update the ontology with the changes we found.
		commitChanges();
	}

	private void removeCyclicSubsumptions()
	{
		Set<List<OWLClass>> cycles = getCycles();
		logger.debug("Detected " + getCycles().size() + " cycles.");
		for (List<OWLClass> cycle : cycles)
		{
			removeCyclicSubsumption(cycle);
		}
		commitChanges();
	}
	
	private void cleanClassHierarchy()
	{
		Set<OWLAxiom> axioms = ontology.getAxioms();
		for (OWLAxiom ax: axioms)
		{
			// Consider only SubclassOf axioms
			if (ax.isOfType(AxiomType.SUBCLASS_OF))
			{
				OWLClassExpression subEx = ((OWLSubClassOfAxiom)ax).getSubClass();
				OWLClassExpression superEx = ((OWLSubClassOfAxiom)ax).getSuperClass();
				
				// Consider only named classes
				if ((subEx instanceof OWLClass) && (superEx instanceof OWLClass))
				{
					
					/*
					 * Get the subclasses of the superclass and the
					 * superclasses of the subclass. If the resulting sets
					 * share a member, the subclass relation is entailed by
					 * transitivity alone and we can safely remove the axiom.
					 */
					Set<OWLClass> subs = transitiveSuperClasses(subEx.asOWLClass());
					Set<OWLClass> supers = transitiveSubClasses(superEx.asOWLClass());
					
					if (false == Collections.disjoint(subs, supers))
					{
						changes.add(new RemoveAxiom(ontology,ax));
					}
				}
			}
		}
		
		logger.debug("Removed " + changes.size() + " redundant subsumptions.");
		commitChanges();
	}
	
	private void removeCyclicSubsumption(List<OWLClass> cycle)
	{
		/*
		 * Handling cycles <= 1 is nonsensical.
		 */
		if (1 >= cycle.size())
		{
			return;
		}
		
		/*
		 * The top node of the list is at index 0. So for every n,
		 * n+1 subclassOf n. And also for the class at the last position m,
		 * m subClassOf 0. Which fits neatly into the following loop.
		 */
		OWLClass superClass = cycle.get((cycle.size() - 1));
		
		/*
		 * We also decide on the topmost class in the cycle as the canonical name. 
		 */
		OWLClass topmost = cycle.get(0);
		
		for (OWLClass subClass : cycle)
		{
			OWLAxiom subClassAx = factory.getOWLSubClassOfAxiom(subClass, superClass);
			changes.add(new RemoveAxiom(ontology, subClassAx));
			if (false == topmost.equals(subClass))
			{
				OWLAxiom equivAx = factory.getOWLEquivalentClassesAxiom(topmost, subClass);
				changes.add(new AddAxiom(ontology, equivAx));
			}
			superClass = subClass;
		}
		
	}
	
	private Set<List<OWLClass>> getCycles()
	{
		OWLClass top = factory.getOWLThing();
		return getCycles(null, Collections.singleton(top));
	}
	
	private Set<OWLClass> getNamedSubClasses(OWLClass c)
	{
		Set<OWLClass> classes = new HashSet<OWLClass>();
		Set<OWLClassExpression> expressions = c.getSubClasses(ontology.getImportsClosure());
		for (OWLClassExpression ce : expressions)
		{
			if (ce instanceof OWLClass)
			{
				classes.add(ce.asOWLClass());
			}
		}
		return classes;
	}
	
	private Set<List<OWLClass>> getCycles(List<OWLClass> baseList, Set<OWLClass> classes)
	{
		Set<List<OWLClass>> cycles = new HashSet<List<OWLClass>>();
		if (null == baseList)
		{
			baseList = new Vector<OWLClass>();
		}
		Map<OWLClass,List<OWLClass>> queueMap = new HashMap<OWLClass,List<OWLClass>>();
		for (OWLClass c : classes)
		{
			queueMap.put(c,baseList);
		}
		Map<OWLClass,List<OWLClass>> newQueueMap = new HashMap<OWLClass,List<OWLClass>>();
		while (0 != queueMap.size())
		{
			for (Entry<OWLClass, List<OWLClass>> e : queueMap.entrySet())
			{
				OWLClass c = e.getKey();
				List<OWLClass> list = e.getValue();
			
				int index = list.indexOf(c);
				if (-1 == index)
				{
					// This means the node is not already present in our list
					List<OWLClass> newList = new Vector<OWLClass>(list);
					newList.add(c);
					for (OWLClass nextClass : getNamedSubClasses(c))
					{
						newQueueMap.put(nextClass, newList);
					}
				}
				else
				{
					/*
					 * If we already found the class in our list, we have reached a
					 * cycle.
					 * The cycle begins at the index we just found and ends at the
					 * end of the list.
					 */
					cycles.add(list.subList(index, (list.size() - 1)));
				}
			}
			queueMap.clear();
			queueMap.putAll(newQueueMap);
			newQueueMap.clear();	
		}
		return cycles;
	}

	protected Set<OWLClass>transitiveSubClasses(OWLClass c)
	{
		return new SubClassCollector(c, ontology.getImportsClosure()).collect();
	}

	protected Set<OWLClass>transitiveSuperClasses(OWLClass c)
	{
		return new SuperClassCollector(c, ontology.getImportsClosure()).collect();
	}
	
}