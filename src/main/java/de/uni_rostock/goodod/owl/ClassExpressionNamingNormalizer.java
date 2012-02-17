/**
  Copyright (C) 2011 The University of Rostock.
 
  Written by:  thebeing
  Created: 16.12.2011
  
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
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.OWLAxiomVisitorAdapter;

/**
 * Implements part of the normalization algorithm described in Vrandečić/Sure
 * 2007. It will create named classes for all complex class expressions used in
 * subclassOf and equivalentClasses axioms. This normalizer is usually only
 * useful in conjunction with the subsumption materialization normalizer.
 * 
 * @author Niels Grewe <niels.grewe@uni-rostock.de>
 * 
 */
public class ClassExpressionNamingNormalizer extends OWLAxiomVisitorAdapter
		implements Normalizer {

	protected OWLOntology ontology;
	protected OWLOntologyManager manager;
	protected OWLDataFactory factory;
	protected Set<OWLOntologyChange> changes;
	protected ClassExpressionNameProvider nameProvider;
	private static Log logger = LogFactory
			.getLog(ClassExpressionNamingNormalizer.class);
	final static String autogeneratedURI = "http://www.iph.uni-rostock.de/goodod/autogen.owl#AutogeneratedClass";

	public ClassExpressionNamingNormalizer(ClassExpressionNameProvider provider) {
		changes = new HashSet<OWLOntologyChange>();
		nameProvider = provider;
	}

	public ClassExpressionNamingNormalizer() {
		changes = new HashSet<OWLOntologyChange>();
		nameProvider = new ClassExpressionNameProvider();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.uni_rostock.goodod.owl.Normalizer#normalize(org.semanticweb.owlapi
	 * .model.OWLOntology)
	 */
	@Override
	public void normalize(OWLOntology ont) throws OWLOntologyCreationException {

		Set<OWLClass> classes = ont.getClassesInSignature(true);
		Set<IRI> IRIs = new HashSet<IRI>();
		for (OWLClass c : classes) {
			IRIs.add(c.getIRI());
		}
		normalize(ont, IRIs);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.uni_rostock.goodod.owl.Normalizer#normalize(org.semanticweb.owlapi
	 * .model.OWLOntology, java.util.Set)
	 */
	@Override
	public void normalize(OWLOntology ont, Set<IRI> IRIs)
			throws OWLOntologyCreationException {

		ontology = ont;
		manager = ontology.getOWLOntologyManager();
		factory = manager.getOWLDataFactory();
		logger.debug("Generating names for anonymous classes.");
		// First step is to create named classes for all complex class
		// expressions:
		for (OWLSubClassOfAxiom a : ont.getAxioms(AxiomType.SUBCLASS_OF, true)) {
			for (OWLEntity e : a.getSignature()) {
				if (IRIs.contains(e.getIRI())) {
					a.accept(this);
					break;
				}
			}
		}

		// Flush out all subclassOf rewrites:
		flushChanges();

		// Rewrite EquivalentClasses und ClassAssertion axioms:
		for (OWLEquivalentClassesAxiom a : ont.getAxioms(
				AxiomType.EQUIVALENT_CLASSES, true)) {
			for (OWLEntity e : a.getSignature()) {
				if (IRIs.contains(e.getIRI())) {
					a.accept(this);
					break;
				}
			}
		}
		for (OWLClassAssertionAxiom a : ont.getAxioms(
				AxiomType.CLASS_ASSERTION, true)) {
			for (OWLEntity e : a.getSignature()) {
				if (IRIs.contains(e.getIRI())) {
					a.accept(this);
					break;
				}
			}
		}

		flushChanges();
		factory = null;
		manager = null;
		ontology = null;

	}

	protected void flushChanges() {
		if ((null != manager) && (0 < changes.size())) {
			manager.applyChanges(new ArrayList<OWLOntologyChange>(changes));
			changes.clear();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi
	 * .model.OWLSubClassOfAxiom)
	 */
	@Override
	public void visit(OWLSubClassOfAxiom arg0) {
		OWLClassExpression oldSuper = arg0.getSuperClass();
		OWLClassExpression oldSub = arg0.getSubClass();
		boolean subIsAtomic = oldSub instanceof OWLClass;
		boolean superIsAtomic = oldSuper instanceof OWLClass;
		OWLClassExpression newSuper = oldSuper;
		OWLClassExpression newSub = oldSub;

		if (subIsAtomic && superIsAtomic) {
			// If both are atomic, we do not need to do anything.
			return;
		}

		if (false == subIsAtomic) {
			// If the subclass is not atomic generate a description for it.
			IRI newIRI = IRIForClassExpression(oldSub);
			newSub = factory.getOWLClass(newIRI);
			OWLEquivalentClassesAxiom ax = factory
					.getOWLEquivalentClassesAxiom(newSub, oldSub);
			changes.add(new AddAxiom(ontology, ax));
		}
		if (false == superIsAtomic) {
			// Same check for the superclass.
			IRI newIRI = IRIForClassExpression(oldSuper);
			newSuper = factory.getOWLClass(newIRI);
			OWLEquivalentClassesAxiom ax = factory
					.getOWLEquivalentClassesAxiom(newSuper, oldSuper);
			changes.add(new AddAxiom(ontology, ax));
		}

		OWLSubClassOfAxiom replacement = factory.getOWLSubClassOfAxiom(newSub,
				newSuper);
		changes.add(new AddAxiom(ontology, replacement));
		changes.add(new RemoveAxiom(ontology, arg0));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi
	 * .model.OWLEquivalentClassesAxiom)
	 */
	@Override
	public void visit(OWLEquivalentClassesAxiom arg0) {

		Set<OWLClass> namedClasses = arg0.getNamedClasses();
		Set<OWLClassExpression> expressions = arg0.getClassExpressions();

		if ((2 == expressions.size()) && (namedClasses.size() >= 1)) {
			// Two is a pair, and if one or more is a named class, we're fine.
			return;
		}

		if (arg0.containsNamedEquivalentClass()) {

			OWLClass lastClass = null;

			for (OWLClass c : namedClasses) {
				for (OWLClassExpression ce : expressions) {
					OWLEquivalentClassesAxiom ax = factory
							.getOWLEquivalentClassesAxiom(c, ce);
					changes.add(new AddAxiom(ontology, ax));
				}
				// Remember to assert the pairwise equivalence of the primitive
				// classes.
				if (null != lastClass) {
					OWLEquivalentClassesAxiom ax = factory
							.getOWLEquivalentClassesAxiom(lastClass, c);
					changes.add(new AddAxiom(ontology, ax));
				}
				lastClass = c;
			}
		} else {
			Set<OWLClass> newClasses = new HashSet<OWLClass>();
			OWLClass lastClass = null;
			for (OWLClassExpression ce : expressions) {
				OWLClass c = factory.getOWLClass(IRIForClassExpression(ce));
				newClasses.add(c);
				OWLEquivalentClassesAxiom ax = factory
						.getOWLEquivalentClassesAxiom(c, ce);
				changes.add(new AddAxiom(ontology, ax));
			}

			for (OWLClass c : newClasses) {
				// Remember to assert the pairwise equivalence of the primitive
				// classes.

				if (null != lastClass) {
					OWLEquivalentClassesAxiom ax = factory
							.getOWLEquivalentClassesAxiom(lastClass, c);
					changes.add(new AddAxiom(ontology, ax));
				}
				lastClass = c;
			}
		}
		// We've replaced the axiom with pairwise equivalences to named classes;
		changes.add(new RemoveAxiom(ontology, arg0));
	}

	public void visit(OWLClassAssertionAxiom ax) {
		OWLClassExpression ce = ax.getClassExpression();
		OWLIndividual i = ax.getIndividual();
		if (ce instanceof OWLClass) {
			return;
		}

		OWLClass newClass = factory.getOWLClass(IRIForClassExpression(ce));
		OWLEquivalentClassesAxiom equiv = factory.getOWLEquivalentClassesAxiom(
				newClass, ce);
		OWLClassAssertionAxiom replacement = factory.getOWLClassAssertionAxiom(
				newClass, i);
		changes.add(new AddAxiom(ontology, equiv));
		changes.add(new AddAxiom(ontology, replacement));
		changes.add(new RemoveAxiom(ontology, ax));

	}

	protected OWLClass namedClassEquivalentToClassExpression(
			OWLClassExpression ce) {
		// If the expression is a class, return that.
		if (ce instanceof OWLClass) {
			return ce.asOWLClass();
		}

		/*
		 * Else, search through the equivalent classes axioms to find an axiom
		 * that specifies the requested class as equivalent to a named class.
		 */
		Set<OWLEquivalentClassesAxiom> equivs = ontology.getAxioms(
				AxiomType.EQUIVALENT_CLASSES, true);
		for (OWLEquivalentClassesAxiom e : equivs) {
			if (e.containsNamedEquivalentClass() && e.contains(ce)) {
				// Just return one of the named classes at random.
				Set<OWLClass> namedClasses = e.getNamedClasses();
				return namedClasses.iterator().next();
			}
		}
		return null;
	}

	protected IRI IRIForClassExpression(OWLClassExpression ce) {
		IRI theIRI = null;

		OWLClass c = namedClassEquivalentToClassExpression(ce);

		if (null != c) {
			return c.getIRI();
		}

		theIRI = nameProvider.IRIForClassExpression(ce);
		OWLAxiom classDecl = factory.getOWLDeclarationAxiom(factory
				.getOWLClass(theIRI));
		if (false == ontology.containsAxiom(classDecl))
		{
			// If the declaration axiom is not yet in the ontology, create one.
			AddAxiom addDecl = new AddAxiom(ontology, classDecl);
			if (false == changes.contains(addDecl))
			{
				// If it is not yet scheduled for inclusion, do so.
				changes.add(addDecl);
			}
		}
		return theIRI;
	}

}
