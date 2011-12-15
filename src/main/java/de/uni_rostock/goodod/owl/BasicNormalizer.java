/**
  Copyright (C) 2011 The University of Rostock.
 
  Written by:  Niels Grewe <niels.grewe@uni-rostock.de>
  Created: 09.12.2011
  
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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Vector;

import org.semanticweb.owlapi.model.*;


/**
 * The basic normalizer only performs import rerouting.
 * 
 * @author Niels Grewe
 * 
 */
public class BasicNormalizer implements Normalizer, OWLAxiomVisitor {
	protected Map<IRI, IRI> IRIMap;
	private OWLOntology ontology;
	private OWLOntologyManager manager;
	private OWLDataFactory factory;
	private IRITransformFactory transformFactory;
	protected Set<OWLOntologyChange> changes;

	public BasicNormalizer() {
		changes = new HashSet<OWLOntologyChange>();
		transformFactory = new IRITransformFactory();
	}

	/**
	 * @see de.uni_rostock.goodod.owl.Normalizer#setImportMappings(java.util.Map)
	 */
	public void setImportMappings(Map<IRI, IRI> oldToNewIRIMap) {
		IRIMap = oldToNewIRIMap;

	}

	/**
	 * Performs default normalization by replacing imports.
	 * 
	 * @see de.uni_rostock.goodod.owl.Normalizer#normalize(org.semanticweb.owlapi.model.OWLOntology)
	 */
	public void normalize(OWLOntology ont) throws OWLOntologyCreationException {
		/*
		 * Don't do anything if we do not have any mappings.
		 */
		if ((null == IRIMap) || IRIMap.isEmpty()) {
			return;
		}
		/*
		 * Store the ontology as state.
		 */
		ontology = ont;

		/*
		 * Fetch ourselves the manager and data factory to reuse.
		 */
		manager = ont.getOWLOntologyManager();
		factory = manager.getOWLDataFactory();

		/*
		 * Basic normalization algorithm is the following:
		 * 
		 * - Get the imports declarations that match the key IRIs from IRIMap.
		 *   Add RemoveImport instances for them and AddImport changes for the
		 *   value IRIs.
		 * - Visit all axioms from the ontology.
		 * - For each axiom, check whether the axiom references an IRI that
		 *   begins with an IRI from the keys in our IRIMap.
		 * - Construct a new axiom by replacing the occurences of the key IRI
		 *   with the value one.
		 * - Add axiom changes for the the old/new axiom pair.
		 * - When the tree has been walked successfully, synchronize our axiom list with the
		 *   manager.
		 */
		replaceImports();
		Set<OWLAxiom> axioms = ontology.getAxioms();
		for (OWLAxiom axiom : axioms)
		{
			axiom.accept(this);
		}
		
		/* 
		 * Apply our changes.
		 */
		manager.applyChanges(new ArrayList<OWLOntologyChange>(changes));
		
		/*
		 * Get rid of the factory reference, we need a new one for further
		 * normalizations.
		 */
		factory = null;
		manager = null;
		ontology = null;
	}

	/**
	 * Since the basic normalizer does only perform import replacement, this
	 * method just calls the standard normalization method.
	 * 
	 * @see de.uni_rostock.goodod.owl.Normalizer#normalize(org.semanticweb.owlapi.model.OWLOntology,
	 *      java.util.Set)
	 */
	public void normalize(OWLOntology ont, Set<IRI> IRIs) throws OWLOntologyCreationException  {
		normalize(ont);
	}

	private void replaceImports()
	{
		
		Set<OWLImportsDeclaration> imports = ontology.getImportsDeclarations(); 
		for (OWLImportsDeclaration decl : imports)
		{
			IRI oldIRI = decl.getIRI();
			if (IRIMap.containsKey(oldIRI))
			{
				RemoveImport removeOld = new RemoveImport(ontology, decl);
				IRI newIRI = IRIMap.get(oldIRI);
				OWLImportsDeclaration newDecl = factory.getOWLImportsDeclaration(newIRI);
				AddImport addNew = new AddImport(ontology, newDecl);
				changes.add(removeOld);
				changes.add(addNew);
			}
		}
	}

	/**
	 * Checks whether the axiom needs to be processed because it contains IRIs
	 * requiring changes
	 * 
	 * @param axiom The axiom to consider.
	 */
	private boolean axiomNeedsUpdate(OWLAxiom axiom)
	{
		for (OWLEntity e : axiom.getSignature())
		{
			String entityName = e.getIRI().toString();
			for (IRI requested : IRIMap.keySet())
			{
				if(entityName.startsWith(requested.toString()))
				{
					return true;
				}
			}
		}
		return false;
	}
	
	private IRI newIRIForIRI(IRI oldIRI)
	{
		IRI newIRI = oldIRI;
		String IRIString = oldIRI.toString();
		for (IRI requested : IRIMap.keySet())
		{
			String reqString = requested.toString();
			if (IRIString.startsWith(reqString))
			{
				// We extract everything that follows the requested key string.
				String lastComponent = IRIString.substring((reqString.length()));
				
				// The value for that key will be the new start of the IRI.
				IRI replacementStart = IRIMap.get(requested);
				
				// Concatenate the two and produce an IRI from them.
				String newFullIRI = replacementStart.toString().concat(lastComponent);
				newIRI = IRI.create(newFullIRI);
				break;
			}
		}
		return newIRI;
	}
	
	
	/**
	 * Checks if an individual is anonymous or named. Anonymous individuals are returned unchanged,
	 * named ones are renamed as necessary and returned afterwards.
	 * 
	 * @param ind The individual to be renamed, if applicable
	 * @return The new individual that had its IRI replaced if necessary.
	 */
	private OWLIndividual newIndividualIfNamed(OWLIndividual ind)
	{
		if (ind.isAnonymous())
		{
			return ind;
		}
		IRI newIRI = newIRIForIRI(ind.asOWLNamedIndividual().getIRI());
		return factory.getOWLNamedIndividual(newIRI);
	}
	
	/**
	 * Creates and schedules OWLOntologyChanges that replace one axiom with the other. 
	 * @param oldAxiom The axiom to be replaced.
	 * @param newAxiom the axiom to be inserted.
	 * @return The new axiom, in case something still needs to use it.
	 */
	protected OWLAxiom exchangeAxioms(OWLAxiom oldAxiom, OWLAxiom newAxiom)
	{
		RemoveAxiom remChange = new RemoveAxiom(ontology,oldAxiom);
		AddAxiom addChange = new AddAxiom(ontology,newAxiom);
		changes.add(remChange);
		changes.add(addChange);
		return newAxiom;
	}
	
	
	private IRIRewriteTransform getT()
	{
		return transformFactory.getIRITransform();
	}
	
	private void returnT(IRIRewriteTransform t)
	{
		transformFactory.returnIRITransform(t);
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAnnotationAxiomVisitor#visit(org.semanticweb
	 * .owlapi.model.OWLAnnotationAssertionAxiom)
	 */
	public void visit(OWLAnnotationAssertionAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		OWLAnnotationProperty prop = arg0.getProperty();
		// Subjects are either IRIs or anonymous individuals
		OWLAnnotationSubject subj = arg0.getSubject();
		// Values are IRIs, Literals or anonymous individuals
		OWLAnnotationValue val = arg0.getValue();
		
		IRI propIRI = prop.getIRI();
		IRI newPropIRI = newIRIForIRI(propIRI);
		if (false == propIRI.equals(newPropIRI))
		{
			prop = factory.getOWLAnnotationProperty(newPropIRI);
		}
		
		if (subj instanceof IRI)
		{
			subj = newIRIForIRI((IRI)subj);
		}
		if (val instanceof IRI)
		{
			val = newIRIForIRI((IRI)val);
		}
		OWLAnnotationAssertionAxiom newAx = factory.getOWLAnnotationAssertionAxiom(prop, subj, val);
		exchangeAxioms(arg0,newAx);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAnnotationAxiomVisitorEx#visit(org.semanticweb
	 * .owlapi.model.OWLSubAnnotationPropertyOfAxiom)
	 */
	public void visit(OWLSubAnnotationPropertyOfAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		OWLAnnotationProperty sub = arg0.getSubProperty();
		OWLAnnotationProperty sup = arg0.getSuperProperty();
		IRI subIRI = sub.getIRI();
		IRI supIRI = sup.getIRI();
		IRI newSubIRI = newIRIForIRI(subIRI);
		IRI newSupIRI = newIRIForIRI(supIRI);
		if (false == subIRI.equals(newSubIRI))
		{
			sub = factory.getOWLAnnotationProperty(newSubIRI);
		}
		if (false == supIRI.equals(newSupIRI))
		{
			sup = factory.getOWLAnnotationProperty(newSupIRI);
		}
		exchangeAxioms(arg0,factory.getOWLSubAnnotationPropertyOfAxiom(sub,sup));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAnnotationAxiomVisitor#visit(org.semanticweb
	 * .owlapi.model.OWLAnnotationPropertyDomainAxiom)
	 */
	public void visit(OWLAnnotationPropertyDomainAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		OWLAnnotationProperty prop = arg0.getProperty();
		IRI propIRI = prop.getIRI();
		IRI newPropIRI = newIRIForIRI(propIRI);
		if (false == propIRI.equals(newPropIRI))
		{
			prop = factory.getOWLAnnotationProperty(newPropIRI);
		}
		IRI domainIRI = newIRIForIRI(arg0.getDomain());
		exchangeAxioms(arg0,factory.getOWLAnnotationPropertyDomainAxiom(prop, domainIRI));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAnnotationAxiomVisitor#visit(org.semanticweb
	 * .owlapi.model.OWLAnnotationPropertyRangeAxiom)
	 */
	public void visit(OWLAnnotationPropertyRangeAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		OWLAnnotationProperty prop = arg0.getProperty();
		IRI propIRI = prop.getIRI();
		IRI newPropIRI = newIRIForIRI(propIRI);
		if (false == propIRI.equals(newPropIRI))
		{
			prop = factory.getOWLAnnotationProperty(newPropIRI);
		}
		IRI rangeIRI = newIRIForIRI(arg0.getRange());
		exchangeAxioms(arg0,factory.getOWLAnnotationPropertyRangeAxiom(prop, rangeIRI));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi
	 * .model.OWLDeclarationAxiom)
	 */
	public void visit(OWLDeclarationAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		OWLEntity entity = arg0.getEntity();
		EntityType<?> type = entity.getEntityType();
		IRI entityIRI = newIRIForIRI(entity.getIRI());
		exchangeAxioms(arg0, factory.getOWLDeclarationAxiom(factory.getOWLEntity(type,entityIRI)));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi
	 * .model.OWLSubClassOfAxiom)
	 */
	public void visit(OWLSubClassOfAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		
		IRIRewriteTransform transform = transformFactory.getIRITransform();
		OWLClassExpression sub;
		OWLClassExpression sup = arg0.getSuperClass().accept(transform);
		if (arg0.isGCI())
		{
			//subclass is anonymous, we do not need to process it.
			sub = arg0.getSubClass();
		}
		else
		{
			sub = arg0.getSubClass().accept(transform);
		}
		transformFactory.returnIRITransform(transform);
		exchangeAxioms(arg0,factory.getOWLSubClassOfAxiom(sub,sup));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi
	 * .model.OWLNegativeObjectPropertyAssertionAxiom)
	 */
	public void visit(OWLNegativeObjectPropertyAssertionAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		IRIRewriteTransform t = transformFactory.getIRITransform();
		OWLObjectPropertyExpression prop = (OWLObjectPropertyExpression)arg0.getProperty().accept(t);
		OWLIndividual subj = newIndividualIfNamed(arg0.getSubject());
		OWLIndividual obj = newIndividualIfNamed(arg0.getObject());
		transformFactory.returnIRITransform(t);
		exchangeAxioms(arg0,factory.getOWLNegativeObjectPropertyAssertionAxiom(prop, subj, obj));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi
	 * .model.OWLAsymmetricObjectPropertyAxiom)
	 */
	public void visit(OWLAsymmetricObjectPropertyAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		IRIRewriteTransform t = getT();
		OWLObjectPropertyExpression prop = (OWLObjectPropertyExpression) arg0.getProperty().accept(t);
		returnT(t);
		exchangeAxioms(arg0,factory.getOWLAsymmetricObjectPropertyAxiom(prop));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi
	 * .model.OWLReflexiveObjectPropertyAxiom)
	 */
	public void visit(OWLReflexiveObjectPropertyAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		IRIRewriteTransform t = getT();
		OWLObjectPropertyExpression prop = (OWLObjectPropertyExpression) arg0.getProperty().accept(t);
		returnT(t);
		exchangeAxioms(arg0,factory.getOWLReflexiveObjectPropertyAxiom(prop));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi
	 * .model.OWLDisjointClassesAxiom)
	 */
	public void visit(OWLDisjointClassesAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		HashSet<OWLClassExpression> newClasses = new HashSet<OWLClassExpression>();
		IRIRewriteTransform t = getT();
		for (OWLClassExpression c : arg0.getClassExpressions())
		{
			newClasses.add(c.accept(t));
		}
		returnT(t);
		exchangeAxioms(arg0,factory.getOWLDisjointClassesAxiom(newClasses));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi
	 * .model.OWLDataPropertyDomainAxiom)
	 */
	public void visit(OWLDataPropertyDomainAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		IRIRewriteTransform t = getT();
		
		OWLDataPropertyExpression prop = (OWLDataPropertyExpression)arg0.getProperty().accept(t);
		OWLClassExpression cls = arg0.getDomain().accept(t);
		returnT(t);
		exchangeAxioms(arg0,factory.getOWLDataPropertyDomainAxiom(prop, cls));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi
	 * .model.OWLObjectPropertyDomainAxiom)
	 */
	public void visit(OWLObjectPropertyDomainAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		IRIRewriteTransform t = getT();
		
		OWLObjectPropertyExpression prop = (OWLObjectPropertyExpression)arg0.getProperty().accept(t);
		OWLClassExpression cls = arg0.getDomain().accept(t);
		returnT(t);
		exchangeAxioms(arg0,factory.getOWLObjectPropertyDomainAxiom(prop, cls));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi
	 * .model.OWLEquivalentObjectPropertiesAxiom)
	 */
	public void visit(OWLEquivalentObjectPropertiesAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		IRIRewriteTransform t = getT();
		HashSet<OWLObjectPropertyExpression> newProps = new HashSet<OWLObjectPropertyExpression>();
		for (OWLObjectPropertyExpression p : arg0.getProperties())
		{
			newProps.add((OWLObjectPropertyExpression)p.accept(t));
		}
		returnT(t);
		exchangeAxioms(arg0,factory.getOWLEquivalentObjectPropertiesAxiom(newProps));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi
	 * .model.OWLNegativeDataPropertyAssertionAxiom)
	 */
	public void visit(OWLNegativeDataPropertyAssertionAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		IRIRewriteTransform t = transformFactory.getIRITransform();
		OWLDataPropertyExpression prop = (OWLDataPropertyExpression)arg0.getProperty().accept(t);
		OWLIndividual subj = newIndividualIfNamed(arg0.getSubject());
		OWLLiteral obj = arg0.getObject();
		transformFactory.returnIRITransform(t);
		exchangeAxioms(arg0,factory.getOWLNegativeDataPropertyAssertionAxiom(prop, subj, obj));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi
	 * .model.OWLDifferentIndividualsAxiom)
	 */
	public void visit(OWLDifferentIndividualsAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		Set<OWLIndividual> individuals = new HashSet<OWLIndividual>();
		for (OWLIndividual i : arg0.getIndividuals())
		{
			individuals.add(newIndividualIfNamed(i));
		}
		exchangeAxioms(arg0,factory.getOWLDifferentIndividualsAxiom(individuals));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi
	 * .model.OWLDisjointDataPropertiesAxiom)
	 */
	public void visit(OWLDisjointDataPropertiesAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		HashSet<OWLDataPropertyExpression> newProps = new HashSet<OWLDataPropertyExpression>();
		IRIRewriteTransform t = getT();
		for (OWLDataPropertyExpression p : arg0.getProperties())
		{
			newProps.add((OWLDataPropertyExpression)p.accept(t));
		}
		returnT(t);
		exchangeAxioms(arg0,factory.getOWLDisjointDataPropertiesAxiom(newProps));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi
	 * .model.OWLDisjointObjectPropertiesAxiom)
	 */
	public void visit(OWLDisjointObjectPropertiesAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		HashSet<OWLObjectPropertyExpression> newProps = new HashSet<OWLObjectPropertyExpression>();
		IRIRewriteTransform t = getT();
		for (OWLObjectPropertyExpression p : arg0.getProperties())
		{
			newProps.add((OWLObjectPropertyExpression)p.accept(t));
		}
		returnT(t);
		exchangeAxioms(arg0,factory.getOWLDisjointObjectPropertiesAxiom(newProps));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi
	 * .model.OWLObjectPropertyRangeAxiom)
	 */
	public void visit(OWLObjectPropertyRangeAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		IRIRewriteTransform t = getT();
		
		OWLObjectPropertyExpression prop = (OWLObjectPropertyExpression)arg0.getProperty().accept(t);
		OWLClassExpression cls = arg0.getRange().accept(t);
		returnT(t);
		exchangeAxioms(arg0,factory.getOWLObjectPropertyRangeAxiom(prop, cls));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi
	 * .model.OWLObjectPropertyAssertionAxiom)
	 */
	public void visit(OWLObjectPropertyAssertionAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		IRIRewriteTransform t = transformFactory.getIRITransform();
		OWLObjectPropertyExpression prop = (OWLObjectPropertyExpression)arg0.getProperty().accept(t);
		OWLIndividual subj = newIndividualIfNamed(arg0.getSubject());
		OWLIndividual obj = newIndividualIfNamed(arg0.getObject());
		transformFactory.returnIRITransform(t);
		exchangeAxioms(arg0,factory.getOWLObjectPropertyAssertionAxiom(prop, subj, obj));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi
	 * .model.OWLFunctionalObjectPropertyAxiom)
	 */
	public void visit(OWLFunctionalObjectPropertyAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		IRIRewriteTransform t = getT();
		OWLObjectPropertyExpression prop = (OWLObjectPropertyExpression) arg0.getProperty().accept(t);
		returnT(t);
		exchangeAxioms(arg0,factory.getOWLFunctionalObjectPropertyAxiom(prop));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi
	 * .model.OWLSubObjectPropertyOfAxiom)
	 */
	public void visit(OWLSubObjectPropertyOfAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		
		IRIRewriteTransform transform = getT();
		OWLObjectPropertyExpression sub = (OWLObjectPropertyExpression)arg0.getSubProperty().accept(transform);
		OWLObjectPropertyExpression sup = (OWLObjectPropertyExpression)arg0.getSuperProperty().accept(transform);
		returnT(transform);
		exchangeAxioms(arg0,factory.getOWLSubObjectPropertyOfAxiom(sub,sup));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi
	 * .model.OWLDisjointUnionAxiom)
	 */
	public void visit(OWLDisjointUnionAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		HashSet<OWLClassExpression> newClasses = new HashSet<OWLClassExpression>();
		IRIRewriteTransform t = getT();
		OWLClass cls = (OWLClass)arg0.getOWLClass().accept(t);
		for (OWLClassExpression c : arg0.getClassExpressions())
		{
			newClasses.add(c.accept(t));
		}
		returnT(t);
		exchangeAxioms(arg0,factory.getOWLDisjointUnionAxiom(cls,newClasses));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi
	 * .model.OWLSymmetricObjectPropertyAxiom)
	 */
	public void visit(OWLSymmetricObjectPropertyAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		IRIRewriteTransform t = getT();
		OWLObjectPropertyExpression prop = (OWLObjectPropertyExpression) arg0.getProperty().accept(t);
		returnT(t);
		exchangeAxioms(arg0,factory.getOWLSymmetricObjectPropertyAxiom(prop));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi
	 * .model.OWLDataPropertyRangeAxiom)
	 */
	public void visit(OWLDataPropertyRangeAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		IRIRewriteTransform t = getT();
		
		OWLDataPropertyExpression prop = (OWLDataPropertyExpression)arg0.getProperty().accept(t);
		OWLDataRange range = arg0.getRange().accept(t);
		returnT(t);
		exchangeAxioms(arg0,factory.getOWLDataPropertyRangeAxiom(prop, range));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi
	 * .model.OWLFunctionalDataPropertyAxiom)
	 */
	public void visit(OWLFunctionalDataPropertyAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		IRIRewriteTransform t = getT();
		OWLDataPropertyExpression prop = (OWLDataPropertyExpression) arg0.getProperty().accept(t);
		returnT(t);
		exchangeAxioms(arg0,factory.getOWLFunctionalDataPropertyAxiom(prop));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi
	 * .model.OWLEquivalentDataPropertiesAxiom)
	 */
	public void visit(OWLEquivalentDataPropertiesAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		IRIRewriteTransform t = getT();
		HashSet<OWLDataPropertyExpression> newProps = new HashSet<OWLDataPropertyExpression>();
		for (OWLDataPropertyExpression p : arg0.getProperties())
		{
			newProps.add((OWLDataPropertyExpression)p.accept(t));
		}
		returnT(t);
		exchangeAxioms(arg0,factory.getOWLEquivalentDataPropertiesAxiom(newProps));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi
	 * .model.OWLClassAssertionAxiom)
	 */
	public void visit(OWLClassAssertionAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		IRIRewriteTransform t = getT();
		OWLClassExpression cls = arg0.getClassExpression().accept(t);
		OWLIndividual ind = newIndividualIfNamed(arg0.getIndividual());
		returnT(t);
		exchangeAxioms(arg0,factory.getOWLClassAssertionAxiom(cls,ind));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi
	 * .model.OWLEquivalentClassesAxiom)
	 */
	public void visit(OWLEquivalentClassesAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		IRIRewriteTransform t = getT();
		HashSet<OWLClassExpression> newClasses = new HashSet<OWLClassExpression>();
		for (OWLClassExpression c : arg0.getClassExpressions())
		{
			newClasses.add(c.accept(t));
		}
		returnT(t);
		exchangeAxioms(arg0,factory.getOWLEquivalentClassesAxiom(newClasses));
		

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi
	 * .model.OWLDataPropertyAssertionAxiom)
	 */
	public void visit(OWLDataPropertyAssertionAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		IRIRewriteTransform t = getT();
		OWLDataPropertyExpression prop = (OWLDataPropertyExpression)arg0.getProperty().accept(t);
		returnT(t);
		OWLIndividual subj = newIndividualIfNamed(arg0.getSubject());
		// Literals do not need any special treatment.
		OWLLiteral obj = arg0.getObject();
		exchangeAxioms(arg0,factory.getOWLDataPropertyAssertionAxiom(prop, subj, obj));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi
	 * .model.OWLTransitiveObjectPropertyAxiom)
	 */
	public void visit(OWLTransitiveObjectPropertyAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		IRIRewriteTransform t = getT();
		OWLObjectPropertyExpression prop = (OWLObjectPropertyExpression) arg0.getProperty().accept(t);
		returnT(t);
		exchangeAxioms(arg0,factory.getOWLTransitiveObjectPropertyAxiom(prop));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi
	 * .model.OWLIrreflexiveObjectPropertyAxiom)
	 */
	public void visit(OWLIrreflexiveObjectPropertyAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		IRIRewriteTransform t = getT();
		OWLObjectPropertyExpression prop = (OWLObjectPropertyExpression) arg0.getProperty().accept(t);
		returnT(t);
		exchangeAxioms(arg0,factory.getOWLIrreflexiveObjectPropertyAxiom(prop));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi
	 * .model.OWLSubDataPropertyOfAxiom)
	 */
	public void visit(OWLSubDataPropertyOfAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		IRIRewriteTransform t = getT();
		OWLDataPropertyExpression sub = (OWLDataPropertyExpression)arg0.getSubProperty().accept(t);
		OWLDataPropertyExpression sup = (OWLDataPropertyExpression)arg0.getSuperProperty().accept(t);
		returnT(t);
		exchangeAxioms(arg0,factory.getOWLSubDataPropertyOfAxiom(sub, sup));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi
	 * .model.OWLInverseFunctionalObjectPropertyAxiom)
	 */
	public void visit(OWLInverseFunctionalObjectPropertyAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		IRIRewriteTransform t = getT();
		OWLObjectPropertyExpression prop = (OWLObjectPropertyExpression) arg0.getProperty().accept(t);
		returnT(t);
		exchangeAxioms(arg0,factory.getOWLInverseFunctionalObjectPropertyAxiom(prop));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi
	 * .model.OWLSameIndividualAxiom)
	 */
	public void visit(OWLSameIndividualAxiom arg0) {
		Set<OWLIndividual> individuals = new HashSet<OWLIndividual>();
		for (OWLIndividual i : arg0.getIndividuals())
		{
			individuals.add(newIndividualIfNamed(i));
		}
		exchangeAxioms(arg0,factory.getOWLSameIndividualAxiom(individuals));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi
	 * .model.OWLSubPropertyChainOfAxiom)
	 */
	public void visit(OWLSubPropertyChainOfAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		IRIRewriteTransform t = getT();
		List<OWLObjectPropertyExpression> chain = new ArrayList<OWLObjectPropertyExpression>();
		OWLObjectPropertyExpression sup = (OWLObjectPropertyExpression) arg0.getSuperProperty().accept(t);
		for (OWLObjectPropertyExpression p : arg0.getPropertyChain())
		{
			chain.add((OWLObjectPropertyExpression)p.accept(t));
		}
		returnT(t);
		exchangeAxioms(arg0,factory.getOWLSubPropertyChainOfAxiom(chain, sup));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi
	 * .model.OWLInverseObjectPropertiesAxiom)
	 */
	public void visit(OWLInverseObjectPropertiesAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		IRIRewriteTransform t = getT();
		OWLObjectPropertyExpression forward = (OWLObjectPropertyExpression) arg0.getFirstProperty().accept(t);
		OWLObjectPropertyExpression inverse = (OWLObjectPropertyExpression) arg0.getSecondProperty().accept(t);
		returnT(t);
		exchangeAxioms(arg0,factory.getOWLInverseObjectPropertiesAxiom(forward,inverse));
		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi
	 * .model.OWLHasKeyAxiom)
	 */
	public void visit(OWLHasKeyAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		IRIRewriteTransform t = getT();
		OWLClassExpression cls = arg0.getClassExpression().accept(t);
		HashSet<OWLPropertyExpression<?,?>> newProps = new HashSet<OWLPropertyExpression<?,?>>();
		for (OWLPropertyExpression<?,?> p : arg0.getPropertyExpressions())
		{
			newProps.add((OWLPropertyExpression<?,?>)p.accept(t));
		}
		returnT(t);
		exchangeAxioms(arg0,factory.getOWLHasKeyAxiom(cls,newProps));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi
	 * .model.OWLDatatypeDefinitionAxiom)
	 */
	public void visit(OWLDatatypeDefinitionAxiom arg0) {
		if (false == axiomNeedsUpdate(arg0))
		{
			return;
		}
		IRIRewriteTransform t = getT();
		OWLDatatype type = (OWLDatatype)arg0.getDatatype().accept(t);
		OWLDataRange range = arg0.getDataRange().accept(t);
		returnT(t);
		exchangeAxioms(arg0,factory.getOWLDatatypeDefinitionAxiom(type, range));
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi.model.SWRLRule)
	 */
	public void visit(SWRLRule arg0) {
		// ignore
	}
	
	
	private class IRIRewriteTransform extends ExpressionTransform
	{
		public IRIRewriteTransform()
		{
			super();
		}

		/**
		 * Replaces an IRI by looking up the mapping to the new IRI.
		 * @see org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx#visit(org.semanticweb.owlapi.model.OWLClass)
		 */
		public OWLClassExpression visit(OWLClass arg0) {
			IRI newIRI = newIRIForIRI(arg0.getIRI());
			
			return factory.getOWLClass(newIRI);
		}

		/* (non-Javadoc)
		 * @see org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx#visit(org.semanticweb.owlapi.model.OWLObjectIntersectionOf)
		 */
		public OWLClassExpression visit(OWLObjectIntersectionOf arg0) {
			Set<OWLClassExpression> newOperands = new HashSet<OWLClassExpression>();
			for (OWLClassExpression op : arg0.getOperands())
			{
				OWLClassExpression newOp = op.accept(this);
				newOperands.add(newOp);
			}
			return factory.getOWLObjectIntersectionOf(newOperands);
		}

		/* (non-Javadoc)
		 * @see org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx#visit(org.semanticweb.owlapi.model.OWLObjectUnionOf)
		 */
		public OWLClassExpression visit(OWLObjectUnionOf arg0) {
			Set<OWLClassExpression> newOperands = new HashSet<OWLClassExpression>();
			for (OWLClassExpression op : arg0.getOperands())
			{
				OWLClassExpression newOp = op.accept(this);
				newOperands.add(newOp);
			}
			
			return factory.getOWLObjectUnionOf(newOperands);
		}

		/* (non-Javadoc)
		 * @see org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx#visit(org.semanticweb.owlapi.model.OWLObjectComplementOf)
		 */
		public OWLClassExpression visit(OWLObjectComplementOf arg0) {
			
			OWLClassExpression newOp = arg0.getOperand().accept(this);
			return factory.getOWLObjectComplementOf(newOp);
		}

		/* (non-Javadoc)
		 * @see org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx#visit(org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom)
		 */
		public OWLClassExpression visit(OWLObjectSomeValuesFrom arg0) {
			OWLClassExpression newFiller = arg0.getFiller().accept(this);
			OWLObjectPropertyExpression newProperty =(OWLObjectPropertyExpression)arg0.getProperty().accept(this);
			return factory.getOWLObjectSomeValuesFrom(newProperty, newFiller);
		}

		/* (non-Javadoc)
		 * @see org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx#visit(org.semanticweb.owlapi.model.OWLObjectAllValuesFrom)
		 */
		public OWLClassExpression visit(OWLObjectAllValuesFrom arg0) {
			OWLClassExpression newFiller = arg0.getFiller().accept(this);
			OWLObjectPropertyExpression newProperty =(OWLObjectPropertyExpression) arg0.getProperty().accept(this);
			return factory.getOWLObjectAllValuesFrom(newProperty, newFiller);
		}

		/* (non-Javadoc)
		 * @see org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx#visit(org.semanticweb.owlapi.model.OWLObjectHasValue)
		 */
		public OWLClassExpression visit(OWLObjectHasValue arg0) {
			OWLObjectPropertyExpression newProperty = (OWLObjectPropertyExpression) arg0.getProperty().accept(this);
			OWLIndividual newIndividual = newIndividualIfNamed(arg0.getValue());
			return factory.getOWLObjectHasValue(newProperty, newIndividual);
		}

		/* (non-Javadoc)
		 * @see org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx#visit(org.semanticweb.owlapi.model.OWLObjectMinCardinality)
		 */
		public OWLClassExpression visit(OWLObjectMinCardinality arg0) {
			OWLObjectPropertyExpression newProperty =(OWLObjectPropertyExpression) arg0.getProperty().accept(this);
			return factory.getOWLObjectMinCardinality(arg0.getCardinality(),newProperty);
			
		}

		/* (non-Javadoc)
		 * @see org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx#visit(org.semanticweb.owlapi.model.OWLObjectExactCardinality)
		 */
		public OWLClassExpression visit(OWLObjectExactCardinality arg0) {
			OWLObjectPropertyExpression newProperty =(OWLObjectPropertyExpression) (OWLObject)arg0.getProperty().accept(this);
			return factory.getOWLObjectExactCardinality(arg0.getCardinality(),newProperty);
		}

		/* (non-Javadoc)
		 * @see org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx#visit(org.semanticweb.owlapi.model.OWLObjectMaxCardinality)
		 */
		public OWLClassExpression visit(OWLObjectMaxCardinality arg0) {
			OWLObjectPropertyExpression newProperty =(OWLObjectPropertyExpression) arg0.getProperty().accept(this);
			return factory.getOWLObjectMaxCardinality(arg0.getCardinality(),newProperty);
		}

		/* (non-Javadoc)
		 * @see org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx#visit(org.semanticweb.owlapi.model.OWLObjectHasSelf)
		 */
		public OWLClassExpression visit(OWLObjectHasSelf arg0) {
			OWLObjectPropertyExpression newProperty =(OWLObjectPropertyExpression) arg0.getProperty().accept(this);
			return factory.getOWLObjectHasSelf(newProperty);
		}

		/* (non-Javadoc)
		 * @see org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx#visit(org.semanticweb.owlapi.model.OWLObjectOneOf)
		 */
		public OWLClassExpression visit(OWLObjectOneOf arg0) {
			Set<OWLIndividual> oldIndividuals = arg0.getIndividuals();
			HashSet<OWLIndividual> newIndividuals = new HashSet<OWLIndividual>(oldIndividuals.size());
			for (OWLIndividual ind : oldIndividuals)
			{
				newIndividuals.add(newIndividualIfNamed(ind));
			}
			return factory.getOWLObjectOneOf(newIndividuals);
		}

		/* (non-Javadoc)
		 * @see org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx#visit(org.semanticweb.owlapi.model.OWLDataSomeValuesFrom)
		 */
		public OWLClassExpression visit(OWLDataSomeValuesFrom arg0) {
			OWLDataPropertyExpression newProperty = (OWLDataPropertyExpression)arg0.getProperty().accept(this);
			OWLDataRange newRange = arg0.getFiller().accept(this);
			return factory.getOWLDataSomeValuesFrom(newProperty,newRange);
		}

		/* (non-Javadoc)
		 * @see org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx#visit(org.semanticweb.owlapi.model.OWLDataAllValuesFrom)
		 */
		public OWLClassExpression visit(OWLDataAllValuesFrom arg0) {
			OWLDataPropertyExpression newProperty = (OWLDataPropertyExpression)arg0.getProperty().accept(this);
			OWLDataRange newRange = arg0.getFiller().accept(this);
			return factory.getOWLDataAllValuesFrom(newProperty,newRange);
		}

		/* (non-Javadoc)
		 * @see org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx#visit(org.semanticweb.owlapi.model.OWLDataHasValue)
		 */
		public OWLClassExpression visit(OWLDataHasValue arg0) {
			OWLDataPropertyExpression newProperty = (OWLDataPropertyExpression)arg0.getProperty().accept(this);
			OWLLiteral literal = arg0.getValue();
			
			return factory.getOWLDataHasValue(newProperty,literal);
		}

		/* (non-Javadoc)
		 * @see org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx#visit(org.semanticweb.owlapi.model.OWLDataMinCardinality)
		 */
		public OWLClassExpression visit(OWLDataMinCardinality arg0) {
			OWLDataPropertyExpression newProperty =(OWLDataPropertyExpression) arg0.getProperty().accept(this);
			return factory.getOWLDataMinCardinality(arg0.getCardinality(),newProperty);
		}

		/* (non-Javadoc)
		 * @see org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx#visit(org.semanticweb.owlapi.model.OWLDataExactCardinality)
		 */
		public OWLClassExpression visit(OWLDataExactCardinality arg0) {
			OWLDataPropertyExpression newProperty =(OWLDataPropertyExpression) arg0.getProperty().accept(this);
			return factory.getOWLDataExactCardinality(arg0.getCardinality(),newProperty);
		}

		/* (non-Javadoc)
		 * @see org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx#visit(org.semanticweb.owlapi.model.OWLDataMaxCardinality)
		 */
		public OWLClassExpression visit(OWLDataMaxCardinality arg0) {
			OWLDataPropertyExpression newProperty =(OWLDataPropertyExpression) arg0.getProperty().accept(this);
			return factory.getOWLDataMaxCardinality(arg0.getCardinality(),newProperty);
		}

		/**
		 * Transforms the object property by replacing the IRI if necessary.
		 * @see org.semanticweb.owlapi.model.OWLPropertyExpressionVisitorEx#visit(org.semanticweb.owlapi.model.OWLObjectProperty)
		 */
		public OWLObject visit(OWLObjectProperty arg0) {
			// Base case
			IRI newIRI = newIRIForIRI(arg0.getIRI());
			return factory.getOWLObjectProperty(newIRI);
		}

		/* (non-Javadoc)
		 * @see org.semanticweb.owlapi.model.OWLPropertyExpressionVisitorEx#visit(org.semanticweb.owlapi.model.OWLObjectInverseOf)
		 */
		public OWLObject visit(OWLObjectInverseOf arg0) {
			// The cast is safe, we ca only have inverse object properties of other object properties.
			OWLObjectPropertyExpression newInverse = (OWLObjectPropertyExpression)arg0.getInverse().accept(this);
			return factory.getOWLObjectInverseOf(newInverse);
		}

		/* (non-Javadoc)
		 * @see org.semanticweb.owlapi.model.OWLPropertyExpressionVisitorEx#visit(org.semanticweb.owlapi.model.OWLDataProperty)
		 */
		public OWLObject visit(OWLDataProperty arg0) {
			// Base case
			IRI newIRI = newIRIForIRI(arg0.getIRI());
			return factory.getOWLDataProperty(newIRI);
		}

		/* (non-Javadoc)
		 * @see org.semanticweb.owlapi.model.OWLDataRangeVisitorEx#visit(org.semanticweb.owlapi.model.OWLDatatype)
		 */
		public OWLDataRange visit(OWLDatatype arg0) {
			if (arg0.isBuiltIn())
			{
				return arg0;
			}
			return factory.getOWLDatatype(newIRIForIRI(arg0.getIRI()));
		}

		/* (non-Javadoc)
		 * @see org.semanticweb.owlapi.model.OWLDataRangeVisitorEx#visit(org.semanticweb.owlapi.model.OWLDataOneOf)
		 */
		public OWLDataRange visit(OWLDataOneOf arg0) {
			// OWLDataOneOf contains only literals, we do not need to rewrite them.
			return arg0;
		}

		/* (non-Javadoc)
		 * @see org.semanticweb.owlapi.model.OWLDataRangeVisitorEx#visit(org.semanticweb.owlapi.model.OWLDataComplementOf)
		 */
		public OWLDataRange visit(OWLDataComplementOf arg0) {
			OWLDataRange newComplement = arg0.getDataRange().accept(this);
			return factory.getOWLDataComplementOf(newComplement);
		}

		/* (non-Javadoc)
		 * @see org.semanticweb.owlapi.model.OWLDataRangeVisitorEx#visit(org.semanticweb.owlapi.model.OWLDataIntersectionOf)
		 */
		public OWLDataRange visit(OWLDataIntersectionOf arg0) {
			Set<OWLDataRange> newOperands = new HashSet<OWLDataRange>();
			for (OWLDataRange op : arg0.getOperands())
			{
				OWLDataRange newOp = op.accept(this);
				newOperands.add(newOp);
			}
			return factory.getOWLDataIntersectionOf(newOperands);
		}

		/* (non-Javadoc)
		 * @see org.semanticweb.owlapi.model.OWLDataRangeVisitorEx#visit(org.semanticweb.owlapi.model.OWLDataUnionOf)
		 */
		public OWLDataRange visit(OWLDataUnionOf arg0) {
			Set<OWLDataRange> newOperands = new HashSet<OWLDataRange>();
			for (OWLDataRange op : arg0.getOperands())
			{
				OWLDataRange newOp = op.accept(this);
				newOperands.add(newOp);
			}
			return factory.getOWLDataUnionOf(newOperands);
		}

		/* (non-Javadoc)
		 * @see org.semanticweb.owlapi.model.OWLDataRangeVisitorEx#visit(org.semanticweb.owlapi.model.OWLDatatypeRestriction)
		 */
		public OWLDataRange visit(OWLDatatypeRestriction arg0) {
			OWLDatatype newType = (OWLDatatype)arg0.getDatatype().accept(this);
			// Facets are fixed in the OWL 2 Spec, so we do not need to perform conversions on them.
			Set<OWLFacetRestriction> restrictions = arg0.getFacetRestrictions();
			return factory.getOWLDatatypeRestriction(newType,restrictions);
		}
		
	}
	
	/**
	 * The IRITransformFactory hands out transformer objects. 
	 * @author Niels Grewe
	 *
	 */
	private class IRITransformFactory extends Object
	{
		private final int targetSize = 8; 
		private int actualSize;
		private Vector<IRIRewriteTransform> inUse;
		private Vector<IRIRewriteTransform> available;
		IRITransformFactory()
		{
			actualSize = targetSize;
			inUse = new Vector<IRIRewriteTransform>(targetSize);
			available = new Vector<IRIRewriteTransform>(targetSize);
			for (int i=0; i < targetSize; i++)
			{
				available.add(new IRIRewriteTransform());
			}
		}
		
		public IRIRewriteTransform getIRITransform()
		{
			IRIRewriteTransform transform;
			if (available.isEmpty())
			{
				//In this case, we create a new transform.
				transform = new IRIRewriteTransform();
				inUse.addElement(transform);
				actualSize= actualSize + 1;
			}
			else
			{
				transform = available.elementAt(0);
				inUse.addElement(transform);
				available.removeElement(transform);
			}
			return transform;
		}
		
		public void returnIRITransform(IRIRewriteTransform transform)
		{
			if (false == inUse.contains(transform))
			{
				return;
			}
			inUse.removeElement(transform);
			// If we are at or below our target size, we just ignore the transform.
			if (actualSize <= targetSize)
			{
				actualSize = actualSize - 1;
				
			}
			else
			{
				available.addElement(transform);
			}
		}
	}
}
