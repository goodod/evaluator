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


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.*;

import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.rdf.model.ModelFactory;


import de.uni_rostock.goodod.owl.OntologyPair;
import fr.inrialpes.exmo.ontowrap.jena25.JENAOntologyFactory;
import fr.inrialpes.exmo.ontosim.*;
import fr.inrialpes.exmo.ontosim.entity.TripleBasedEntitySim;
import fr.inrialpes.exmo.ontosim.entity.model.Entity;
import fr.inrialpes.exmo.ontosim.set.MaxCoupling;
import fr.inrialpes.exmo.ontosim.set.SetMeasure;

/**
 * @author Niels Grewe
 * This class implements comparisons using triple based entity similarity.
 * It requires ontologies to be converted to the JENA API.
 */
public class TripleBasedEntitySimComparator extends OntoSimComparator {

	/**
	 * This needs a special factory to generate JENA OntModels
	 */
	static private JENAOntologyFactory jenaOntoWrapFactory = new JENAOntologyFactory();
	
	public TripleBasedEntitySimComparator(OntologyPair p, boolean includeImports) throws Throwable
	{
		super(p, includeImports);
		
		// Get a serialization of the OWLAPI representation:
		ByteArrayOutputStream sourceA = outputStreamForOntology(pair.getOntologyA());
		ByteArrayOutputStream sourceB = outputStreamForOntology(pair.getOntologyB());
		
		// Also record their base URIs
		String uriA = pair.getOntologyA().getOntologyID().getOntologyIRI().toString();
		String uriB = pair.getOntologyB().getOntologyID().getOntologyIRI().toString();
		
		// Place them in a new input stream for JENA
		ByteArrayInputStream destinationA = new ByteArrayInputStream(sourceA.toByteArray());
		ByteArrayInputStream destinationB = new ByteArrayInputStream(sourceB.toByteArray());
		
		// Indicate to the JVM that it can collect the output streams
		sourceA = null;
		sourceB = null;
	
		// Create two OWL OntModels for the JENA representation of our ontologies:
		OntModel jenaA = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM, null );
		OntModel jenaB = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM, null );
		
		// Deserialize the ontologies from the buffer:
		jenaA.read(destinationA, uriA);
		jenaB.read(destinationB, uriB);
		
		// Throw away the input streams:
		destinationA = null;
		destinationB = null;
		
		// place the new models in our ivars. (Note: They're HeavyLoadedOntology<OntModel> now)
		ontologyA = jenaOntoWrapFactory.newOntology(jenaA);
		ontologyB = jenaOntoWrapFactory.newOntology(jenaB);
	}
	
	public SimilarityDissimilarityResult compare() throws ExecutionException, InterruptedException
	{
		// here: MaxCoupling, AverageLinkage, Hausdorff
		// here: TripleBasedEntitySim (OLAEntitySim doesn't work with OWL2)
		
		// Make the SetMeasure fully generic in order to get the casting right.
		SetMeasure<?> gm = new MaxCoupling<Entity<OntResource>>(new TripleBasedEntitySim());
		
		// It is unfortunate, but we need the unchecked cast here…
		@SuppressWarnings("unchecked")
		OntologySpaceMeasure m = new OntologySpaceMeasure((SetMeasure<Entity<?>>) gm); 
		return new SimilarityDissimilarityResult(getComparsionMethod(),
		  pair,
		  m.getSim(ontologyA, ontologyB),
		  m.getDissim(ontologyA, ontologyB));
		
	}

	
	
	
	public String getComparsionMethod()
	{
		return "Triple based entity similarity with MWMGM aggregation";
	}
	
	/**
	 * Serialized an ontology to an in-memory RDF/XML representation. This is required for 
	 * deserializing it using the JENA API.
	 * @param ont The ontology to serialize
	 * @return A byte array output stream containing an RDF/XML serialization of the ontology.
	 * @throws OWLOntologyStorageException 
	 * @throws IOException 
	 */
	private ByteArrayOutputStream outputStreamForOntology(OWLOntology ont) throws OWLOntologyStorageException, IOException
	{
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		OWLOntologyManager manager = ont.getOWLOntologyManager();
        // Since JENA might not play very well with imports, we remove them,
		// although it is a tad suboptimal.
		Set<OWLImportsDeclaration> imports = ont.getImportsDeclarations();
        for (OWLImportsDeclaration i : imports)
        {
        	manager.applyChange(new RemoveImport(ont, i));
        }
        
        manager.saveOntology(ont, new RDFXMLOntologyFormat(), stream);
        // Make sure all writes end up in the buffer:
        stream.flush();
        return stream;
	}
	
}
