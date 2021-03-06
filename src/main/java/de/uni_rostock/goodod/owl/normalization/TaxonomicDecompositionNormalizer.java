/**
  Copyright (C) 2011 The University of Rostock.
 
  Written by:  Niels Grewe <niels.grewe@uni-rostock.de>
  Created: 28.12.2011
  
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



import java.util.Set;

import de.uni_rostock.goodod.owl.ClassExpressionNameProvider;
import de.uni_rostock.goodod.owl.TaxonomicDecompositionCollector;
import de.uni_rostock.goodod.owl.normalization.ClassExpressionNamingNormalizer;

import org.semanticweb.owlapi.model.*;

/**
 * The TaxonomicDecompositionNormalizer computes the taxonomic decomposition of
 * every complex definition in an equivalentClasses axiom of an ontology. It is
 * designed to run after a ClassExpressionNamingNormalizer, which pushes all
 * complex definitions into the equivalentClasses axioms and before a subsumption
 * materialization normalizer which computes the subclassOf axioms between them.
 * 
 * @author Niels Grewe <niels.grewe@uni-rostock.de>
 *
 */
public class TaxonomicDecompositionNormalizer extends ClassExpressionNamingNormalizer {

	
	/**
	 * @param o The ontology to normalize
	 */
	public TaxonomicDecompositionNormalizer(OWLOntology o) {
		super(o);
	}

	
	public TaxonomicDecompositionNormalizer(OWLOntology o, ClassExpressionNameProvider prvd)
	{
		super(o, prvd);
	}
	
	// The superclass implementation of normalize() does "the right thing"™.
	
	
	@Override
	public void visit(OWLSubClassOfAxiom arg0)
	{
		//NoOp.
	}
	
	public void visit(OWLClassAssertionAxiom ax) {
		//NoOp.
	}
	
	@Override
	public void visit(OWLEquivalentClassesAxiom arg0)
	{
		Set<OWLClassExpression> expressions = arg0.getClassExpressions();
		for (OWLClassExpression e : expressions)
		{
			if (false == (e instanceof OWLClass))
			{
				fillInTDSForClassExpression(e);
			}
		}
	}
	
	private void fillInTDSForClassExpression(OWLClassExpression e)
	{
		Set<OWLClassExpression> decompositionSet = TaxonomicDecompositionCollector.collect(e);
		OWLClass owlThing = factory.getOWLThing();
		for (OWLClassExpression component : decompositionSet)
		{
			if (false == (component instanceof OWLClass))
			{
				OWLClass newClass = factory.getOWLClass(IRIForClassExpression(component));
				OWLEquivalentClassesAxiom equiv = factory.getOWLEquivalentClassesAxiom(
						newClass, component);
				OWLSubClassOfAxiom subCl = factory.getOWLSubClassOfAxiom(newClass, owlThing);
				changes.add(new AddAxiom(ontology,equiv));
				changes.add(new AddAxiom(ontology,subCl));
			}
		}
	}
}
