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
package de.uni_rostock.goodod.owl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ExecutionException;

import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.*;

import fr.inrialpes.exmo.ontosim.*;
import fr.inrialpes.exmo.ontosim.entity.model.*;
import fr.inrialpes.exmo.ontosim.set.*;
import fr.inrialpes.exmo.ontosim.vector.CosineVM;
import fr.inrialpes.exmo.ontosim.vector.model.DocumentCollection;
import fr.inrialpes.exmo.ontosim.entity.*;

import fr.inrialpes.exmo.ontowrap.LoadedOntology;
import fr.inrialpes.exmo.ontowrap.OntologyFactory;

/**
 * Comparator for ontology pairs using semantic cotopy (cf. Dellschaft/Staab
 * 2006) without any special provisions.
 * @author Niels Grewe <niels.grewe@uni-rostock.de>
 *
 */
public class SCComparator implements Comparator {

	private OntologyPair pair;
	boolean includeImports;
	
	/**
	 * 
	 * @param thePair The ontology pair to compare.
	 * @param doIncludeImports Whether the imports closure of the ontologies
	 * should be taken into account for comparisons.
	 */
	public SCComparator(OntologyPair thePair, boolean doIncludeImports)
	{
		super();
		pair = thePair;
		includeImports = doIncludeImports;
	}
	
	/* (non-Javadoc)
	 * @see de.uni_rostock.goodod.owl.Comparator#compare()
	 */
	
	public FMeasureComparisonResult compare() throws InterruptedException, ExecutionException {
		//Set<IRI> noIRIs = Collections.emptySet();
		//return compareClasses(pair.getOntologyA().getClassesInSignature(includeImports), noIRIs);
		
		OntologyFactory.setDefaultFactory("fr.inrialpes.exmo.ontowrap.jena25.JENAOntologyFactory");
		
		OntologyFactory of = null;
		try {
			of = OntologyFactory.getFactory();
		} 
		catch (Throwable e)
    	{
    		System.out.println("ONTOLOGY-FACTORY ERROR: " +  e.toString());
    	}
		
		double newPrec = 0;
		double newRec = 0;
		LoadedOntology<?> o1 = null;
		LoadedOntology<?> o2 = null;
		
		////////////////////////////////////////////////////////
		// works only single threaded because of file operation
		////////////////////////////////////////////////////////
		
		try {
			System.out.println(pair.getOntologyA());
			System.out.println(pair.getOntologyB());
			OWLOntologyManager om = pair.getOntologyB().getOWLOntologyManager();
			Set<OWLImportsDeclaration> isa = pair.getOntologyA().getImportsDeclarations();
			for (Iterator<OWLImportsDeclaration> i = isa.iterator(); i.hasNext(); )
			{
				om.applyChange(new RemoveImport(pair.getOntologyA(), i.next()));	
			} 
			File af = new File("ooa.owl");
			OutputStream aout = new FileOutputStream(af);
			om.saveOntology(pair.getOntologyA(), new RDFXMLOntologyFormat(), aout);
			aout.flush();
			
			Set<OWLImportsDeclaration> isb = pair.getOntologyB().getImportsDeclarations();
			for (Iterator<OWLImportsDeclaration> i = isb.iterator(); i.hasNext(); )
			{
				om.applyChange(new RemoveImport(pair.getOntologyB(), i.next()));	
			}
			File bf = new File("oob.owl");
			OutputStream bout = new FileOutputStream(bf);
			om.saveOntology(pair.getOntologyB(), new RDFXMLOntologyFormat(), bout);
			bout.flush();
			
		} 
		catch (Throwable e)
    	{
    		System.out.println("COMPARE ERROR: " +  e.toString());
    		newPrec = -1;
    		newRec = -1;
    	}
		
		try {
			copyFile(new File("ooa.owl"), new File("ooc.owl"));
			copyFile(new File("oob.owl"), new File("ood.owl"));
		} catch (Throwable e) {System.out.println("COPY ERROR: " +  e.toString());}
		
		try {
			o1 = of.loadOntology(new File("ooc.owl").toURI());
			o2 = of.loadOntology(new File("ood.owl").toURI());
		} catch (Throwable e) {System.out.println("LOAD ERROR: " +  e.toString());}
		
		////////////////////////////////////////////////////////
		// following block TripleBasedEntitySim with MaxCoupling 
		// only with JENA - w/wo imports, w/wo normalization
		////////////////////////////////////////////////////////
		SetMeasure<Entity<?>> gm = null;
		OntologySpaceMeasure m2 = null;
		try {
			// here: MaxCoupling, AverageLinkage, Hausdorff
			// here: TripleBasedEntitySim (OLAEntitySim doesn't work with OWL2)
			gm = new AverageLinkage(new TripleBasedEntitySim());
			m2 = new OntologySpaceMeasure(gm); }
		catch (Throwable e) {System.out.println("COMPARE ERROR 1.5: " +  e.toString());}
		
		//double dies = 0;
		try {
			newPrec = m2.getSim(o1, o2);
			newRec = m2.getDissim(o1, o2);
			//dies = m2.getMeasureValue(o1, o2);
		} catch (Throwable e) {System.out.println("COMPARE ERROR 2: " +  e.toString());}
		/////// END TripleBasedEntitySim block ///////////////// 
		
		/*////////////////////////////////////////////////////////
		// following block CosineVM 
		// tested with JENA - w/wo imports, w/wo normalization
		////////////////////////////////////////////////////////
		Vector<LoadedOntology<?>> ontos = new Vector<LoadedOntology<?>>();
		ontos.add(o1);
		ontos.add(o2);
		VectorSpaceMeasure m;
		try {
			m = new VectorSpaceMeasure(ontos,new CosineVM(), DocumentCollection.WEIGHT.TF);
			newPrec = m.getSim(o1, o2);
			newRec = m.getDissim(o1, o2);
		} catch (Throwable e) {System.out.println("COMPARE CosineVM ERROR 2: " +  e.toString());}
		//////// END CosineVM block ///////////////// 
		*/		
		
		System.out.println(newPrec);
		return new FMeasureComparisonResult(getComparisonMethod(), pair, newPrec, newRec);
	}

	public FMeasureComparisonResult compare(Set<IRI> classIRIs) throws InterruptedException, ExecutionException {
		System.out.println("FALSCH");
		return compare();
	}
	
	void copyFile(File sourceFile, File destFile) throws IOException {
	    if(!destFile.exists()) {
	        destFile.createNewFile();
	    }

	    FileChannel source = null;
	    FileChannel destination = null;

	    try {
	        source = new FileInputStream(sourceFile).getChannel();
	        destination = new FileOutputStream(destFile).getChannel();
	        destination.transferFrom(source, 0, source.size());
	    }
	    finally {
	        if(source != null) {
	            source.close();
	        }
	        if(destination != null) {
	            destination.close();
	        }
	    }
	}

	
	protected String getComparisonMethod()
	{
		return "Semantic Cotopy Comparison";
	}
	
	protected Set<OWLClass>transitiveSubClasses(OWLClass c, OWLOntology o)
	{
		Set<OWLOntology> ontologies = null;
		if (includeImports)
		{
			ontologies = o.getImportsClosure();
		}
		else
		{
			ontologies = Collections.singleton(o);
		}
		return new SubClassCollector(c, ontologies).collect();
	}
	
	protected Set<OWLClass>transitiveSuperClasses(OWLClass c, OWLOntology o)
	{
		Set<OWLOntology> ontologies = null;
		if (includeImports)
		{
			ontologies = o.getImportsClosure();
		}
		else
		{
			ontologies = Collections.singleton(o);
		}
		return new SuperClassCollector(c, ontologies).collect();
	}
	
	/**
	 * Computes a characteristic extract from the ontology, consisting of all
	 * sub- and superclasses of the given class.
	 * 
	 * @param o The ontology from which to get the classes.
	 * @param c The class for which to fetch the extract.
	 * @return The set of all sub- and superclasses of the given class.
	 */
	private Set<OWLClass> semanticCotopy(OWLClass c, OWLOntology o)
	{
		Set<OWLClass> extract = new HashSet<OWLClass>();
		
		extract.addAll(transitiveSuperClasses(c, o));
		extract.addAll(transitiveSubClasses(c, o));
		
		/*
		 *  The class itself belongs to the extract as well and prevents us
		 *  from doing divisions by zero.
		 */
		extract.add(c);
		return extract;
	}
	
	/**
	 * Finds a class with the same name in the named ontology.
	 * @param classA The class for which to find a twin.
	 * @param o The ontology to consider.
	 * @return The class from the other ontology.
	 */
	protected OWLClass findClass(OWLClass classA, OWLOntology o)
	{
		IRI iriA = classA.getIRI();
		Set<OWLEntity> entities = o.getEntitiesInSignature(iriA, includeImports);
		if ((null != entities) && (0 != entities.size()))
		{
			for (OWLEntity e : entities)
			{
				if (e instanceof OWLClass)
				{
					return e.asOWLClass();
				}
			}
		}
		
		/* 
		 * If we got thus far, we have no exact match and need to search for a
		 * fragment-wise on.
		 */
		Set<OWLClass>classes = o.getClassesInSignature(includeImports);
		for (OWLClass c : classes)
		{
			if (equalIRIsOrFragments(iriA, c.getIRI()))
			{
				return c;
			}
		}
		
		return null;
	}
	
	protected boolean equalIRIsOrFragments(IRI iriA, IRI iriB)
	{
		if (iriA.equals(iriB))
		{
			return true;
		}
		if (iriA.getFragment().equals(iriB.getFragment()))
		{
			return true;
		}
		
		return false;
	}
	
	protected Set<OWLClass> commonClasses(Set<OWLClass> extractA, Set<OWLClass> extractB)
	{
		Set<OWLClass> commonClasses = new HashSet<OWLClass>();
		// TODO: Nested loop. There is probably a smarter way.
		for (OWLClass classA : extractA)
		{
			/*
			 *  But at least we can extract the loop invariant, just in case
			 *  the JVM is as stupid as we all think it is.
			 */
			IRI iriA = classA.getIRI();
			for (OWLClass classB : extractB)
			{
				if (equalIRIsOrFragments(iriA,classB.getIRI()))
				{
					commonClasses.add(classA);
				}
			}
			
		}
		return commonClasses;
	}


	protected double getTaxonomicPrecision(OWLClass classA, OWLClass classB, OWLOntology ontA, OWLOntology ontB)
	{
		return computeTaxonomicPrecision(classA, classB, ontA, ontB);
	}
	
	protected double computeTaxonomicPrecision(OWLClass classA, OWLClass classB, OWLOntology ontA, OWLOntology ontB)
	{
		if ((null == classA) || (null == classB))
		{
			//If one of the classes is null, we just return zero.
			return 0;
		}
		Set<OWLClass> extractA = semanticCotopy(classA, ontA);
		Set<OWLClass> extractB = semanticCotopy(classB, ontB);
		Set<OWLClass> commonExtract = commonClasses(extractA,extractB);
		

		return (((double)commonExtract.size())/(double)extractA.size());
	}
}

