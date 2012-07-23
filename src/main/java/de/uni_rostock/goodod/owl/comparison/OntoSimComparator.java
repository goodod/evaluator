/**
  Copyright (C) 2011 The University of Rostock.
 
  Written by:  Niels Grewe <niels.grewe@uni-rostock.de>
  Created: 14.12.2011
  Modified for OntoSim-Integration by: Martin Boeker <martin.boeker@uniklinik-freiburg.de>
  Modification date: 2012-06-12
  
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


import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.semanticweb.owlapi.model.IRI;

import de.uni_rostock.goodod.owl.*;



import fr.inrialpes.exmo.ontowrap.HeavyLoadedOntology;

import fr.inrialpes.exmo.ontowrap.owlapi30.OWLAPI3OntologyFactory;

/**
 * Abstract superclass of classes implementing similarity measures from OntoSim.
 * @author Niels Grewe <niels.grewe@uni-rostock.de>
 *
 */
public abstract class OntoSimComparator implements Comparator {

	static private OWLAPI3OntologyFactory ontoWrapFactory = new OWLAPI3OntologyFactory();
	protected static Log logger = LogFactory.getLog(OntoSimComparator.class);
	protected OntologyPair pair;
	protected HeavyLoadedOntology<?> ontologyA;
	protected HeavyLoadedOntology<?> ontologyB;
	boolean includeImports;
	
	/**
	 * 
	 * @param thePair The ontology pair to compare.
	 * @param doIncludeImports Whether the imports closure of the ontologies
	 * should be taken into account for comparisons.
	 * @throws Throwable 
	 */
	public OntoSimComparator(OntologyPair thePair, boolean doIncludeImports) throws Throwable
	{
		super();
		pair = thePair;
		includeImports = doIncludeImports;
		ontologyA = ontoWrapFactory.newOntology(pair.getOntologyA());
		ontologyB = ontoWrapFactory.newOntology(pair.getOntologyB());
	}
	
	public ComparisonResult compare(Set<IRI>iriSet) throws ExecutionException, InterruptedException
	{
		// Unfortunately, 
		logger.warn("Partial comparison unsupported by OntoSim.");
		return compare();
	}
	
	/* (non-Javadoc)
	 * @see de.uni_rostock.goodod.owl.Comparator#compare()
	 */
	
	/* 
	 * NOTICE:
	 * The commented-out block to follow contains old code to generate comparisons. OntoSimCompator is 
	 * now an abstact class, so this will in the future reappear in concrete subclasses.
	 */
	/*public FMeasureComparisonResult compare() throws InterruptedException, ExecutionException {
		//Set<IRI> noIRIs = Collections.emptySet();
		//return compareClasses(pair.getOntologyA().getClassesInSignature(includeImports), noIRIs);
		
		OntologyFactory.setDefaultFactory("fr.inrialpes.exmo.ontowrap.owlapi30.OWLAPI3OntologyFactory");
		
		OWLAPI3OntologyFactory ontologyFactory = null;
		try {
			ontologyFactory = (OWLAPI3OntologyFactory)OntologyFactory.getFactory();
		} 
		catch (Throwable e)
    	{
    		System.out.println("ONTOLOGY-FACTORY ERROR: " +  e.toString());
    	}
		
		double newPrec = 0;
		double newRec = 0;
		HeavyLoadedOntology<?> ontology1 = null;
		HeavyLoadedOntology<?> ontology2 = null;
		
		////////////////////////////////////////////////////////
		// works only single threaded because of file operation
		////////////////////////////////////////////////////////
		
		try {
			ontology1 = ontologyFactory.newOntology(pair.getOntologyA());
			ontology2 = ontologyFactory.newOntology(pair.getOntologyB());
			
		} 
		catch (Throwable e)
    	{
    		System.out.println("COMPARE ERROR: " +  e.toString());
    		newPrec = -1;
    		newRec = -1;
    	}
			
		////////////////////////////////////////////////////////
		// following block TripleBasedEntitySim with MaxCoupling 
		// only with JENA - w/wo imports, w/wo normalization
		////////////////////////////////////////////////////////
		SetMeasure<Entity<?>> gm = null;
		OntologySpaceMeasure m2 = null;
		try {
			// here: MaxCoupling, AverageLinkage, Hausdorff
			// here: TripleBasedEntitySim (OLAEntitySim doesn't work with OWL2)
			// gm = new AverageLinkage(new TripleBasedEntitySim());
			m2 = new OntologySpaceMeasure(gm); }
		catch (Throwable e) {System.out.println("COMPARE ERROR 1.5: " +  e.toString());}
		
		//double dies = 0;
		try {
			newPrec = m2.getSim(ontology1, ontology2);
			newRec = m2.getDissim(ontology1, ontology2);
			//dies = m2.getMeasureValue(o1, o2);
		} catch (Throwable e) {System.out.println("COMPARE ERROR 2: " +  e.toString());}
		/////// END TripleBasedEntitySim block ///////////////// 
	}*/

}

