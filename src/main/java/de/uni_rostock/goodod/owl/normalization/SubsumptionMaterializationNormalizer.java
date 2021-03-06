/**
  Copyright (C) 2011 The University of Rostock.
 
  Written by:  Niels Grewe
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
package de.uni_rostock.goodod.owl.normalization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.semanticweb.owlapi.model.*;

import org.semanticweb.owlapi.reasoner.InferenceType;

import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Configuration.ExistentialStrategyType;
import org.semanticweb.HermiT.Configuration.TableauMonitorType;
import org.semanticweb.HermiT.Reasoner;

import de.uni_rostock.goodod.owl.SubClassCollector;
import de.uni_rostock.goodod.owl.SuperClassCollector;


/**
 * Materializes the subsumption hierarchy of an ontology using the HermiT
 * reasoner.
 * @author Niels Grewe
 *
 */
public class SubsumptionMaterializationNormalizer extends AbstractNormalizer {

	private Reasoner reasoner;
	
	private static Log logger = LogFactory.getLog(SubsumptionMaterializationNormalizer.class);
	
	public SubsumptionMaterializationNormalizer(OWLOntology ont)
	{
		super(ont);
		Configuration reasonerConfig = new Configuration();
		reasonerConfig.throwInconsistentOntologyException = false;
		//ReasonerProgressMonitor monitor = new ConsoleProgressMonitor();
		reasonerConfig.existentialStrategyType = ExistentialStrategyType.INDIVIDUAL_REUSE;
		//reasonerConfig.reasonerProgressMonitor = monitor;
		reasonerConfig.tableauMonitorType = TableauMonitorType.NONE;
		//reasonerConfig.individualTaskTimeout = 10000;
		reasoner = new Reasoner(reasonerConfig, ontology);
	}
	/* (non-Javadoc)
	 * @see de.uni_rostock.goodod.owl.Normalizer#normalize(org.semanticweb.owlapi.model.OWLOntology)
	 */
	@Override
	public void normalize() throws OWLOntologyCreationException {
		
		Set<OWLClass> classes = ontology.getClassesInSignature(true);
		Set<IRI> IRIs = new HashSet<IRI>();
		for (OWLClass c : classes)
		{
			IRIs.add(c.getIRI());
		}
		normalize(IRIs);
	}

	/* (non-Javadoc)
	 * @see de.uni_rostock.goodod.owl.Normalizer#normalize(org.semanticweb.owlapi.model.OWLOntology, java.util.Set)
	 */
	
	public synchronized void normalize(Set<IRI> IRIs)
			throws OWLOntologyCreationException {
		
		logger.debug("Running subsumption materialization.");
		
		logger.debug("Classifying with reasoner.");
		// Let the reasoner do the classification
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
		
		
		// Find all entailed subsumptions
		findEntailedSubsumptions(IRIs);
		
		// We do cycle elimination implicitly when finding entailments.
		
		// Clean the hierarchy to eliminate redundant axioms:
		
		cleanClassHierarchy();
		
		finish();
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

	}
	
	private void finish()
	{
		if (0 != changes.size())
		{
			commitChanges();
		}
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
				boolean axEntailed = reasoner.isEntailed(ax);
				boolean invEntailed = reasoner.isEntailed(invAx);
				if (axEntailed && invEntailed)
				{
					// Equivalent classes, just emit a EquivalentClasses axiom
					OWLAxiom equiv = factory.getOWLEquivalentClassesAxiom(c, other);
					changes.add(new AddAxiom(ontology, equiv));
				}
				else
				{
					if (axEntailed)
					{
						changes.add(new AddAxiom(ontology,ax));
					}
					if (invEntailed)
					{
						changes.add(new AddAxiom(ontology,invAx));
					}
				}
			}
		}
		logger.debug("Found " + changes.size() + " new subsumptions.");
		// Update the ontology with the changes we found.
		commitChanges();
	}

	
	private void cleanClassHierarchy()
	{
		Set<OWLSubClassOfAxiom> axioms = ontology.getAxioms(AxiomType.SUBCLASS_OF, true);
		for (OWLSubClassOfAxiom ax: axioms)
		{
			
			OWLClassExpression subEx = ax.getSubClass();
			OWLClassExpression superEx = ax.getSuperClass();
			
			// Consider only named classes
			if ((subEx instanceof OWLClass) && (superEx instanceof OWLClass))
			{
				
				/*
				 * Get the subclasses of the superclass and the
				 * superclasses of the subclass. If the resulting sets
				 * share a member (modulo the classes in the axiom), the subclass
				 * relation is entailed by transitivity alone and we can safely
				 * remove the axiom.
				 */
				Set<OWLClass> subs = transitiveSuperClasses(subEx.asOWLClass());					
				Set<OWLClass> supers = transitiveSubClasses(superEx.asOWLClass());
				subs.remove(superEx);
				supers.remove(subEx);
				if (false == Collections.disjoint(subs, supers))
				{
					changes.add(new RemoveAxiom(ontology,ax));
				}
			}
			
			/*
			 * Special-case: If the superclass has "Test" (case-insensitively)
			 * as its fragment, remove the axiom.
			 */
			if (superEx instanceof OWLClass)
			{
				IRI superIRI = ((OWLClass) superEx).getIRI();
				String fragment = superIRI.getFragment();
				if (fragment.equalsIgnoreCase("TEST"))
				{
					RemoveAxiom removeAx = new RemoveAxiom(ontology, ax);
					if (false == changes.contains(removeAx))
					{
						changes.add(removeAx);
					}
				}
			}
		}
		
		logger.debug("Removed " + changes.size() + " redundant subsumptions.");
		commitChanges();
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