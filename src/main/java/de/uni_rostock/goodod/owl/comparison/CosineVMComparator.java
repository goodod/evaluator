/**
  Copyright (C) 2012 The University of Rostock.
 
  Written by:  thebeing
  Created: 17.07.2012
  
  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  as published by the Free Software Foundation; either
  version 3 of the License, or (at your option) any later version.

  You should have received a copy of the GNU General Public
  License along with this program; see the file COPYING.
  If not, write to the Free Software Foundation,
  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 */
package de.uni_rostock.goodod.owl.comparison;


import java.util.Vector;
import java.util.concurrent.ExecutionException;


import de.uni_rostock.goodod.owl.OntologyPair;
import fr.inrialpes.exmo.ontowrap.*;
import fr.inrialpes.exmo.ontosim.*;
import fr.inrialpes.exmo.ontosim.vector.CosineVM;
import fr.inrialpes.exmo.ontosim.vector.model.DocumentCollection;

/**
 * @author Niels Grewe
 * This class implements comparisons using a cosine vector space model on the
 * ontology labels, weighted by frequency. Cf. Euzenat et al. 2009, p. 15
 * (3.1.2).
 */
public class CosineVMComparator extends OntoSimComparator {

	
	private final Vector<LoadedOntology<?>> ontologyVector = new Vector<LoadedOntology<?>>();;
	public CosineVMComparator(OntologyPair p, boolean includeImports) throws Throwable
	{
		super(p, includeImports);
		ontologyVector.add((LoadedOntology<?>)ontologyA);
		ontologyVector.add((LoadedOntology<?>)ontologyB);
	}
	
	public synchronized SimilarityDissimilarityResult compare() throws ExecutionException, InterruptedException
	{
	VectorSpaceMeasure m
	  = new VectorSpaceMeasure(ontologyVector,new CosineVM(), DocumentCollection.WEIGHT.TF);

	
	return new SimilarityDissimilarityResult(getComparsionMethod(),
	  pair,
	  m.getSim(ontologyA, ontologyB),
	  m.getDissim(ontologyA, ontologyB));
	}

	
	public String getComparsionMethod()
	{
		return "Cosine index vector space model with frequency weighting";
	}
	
}
