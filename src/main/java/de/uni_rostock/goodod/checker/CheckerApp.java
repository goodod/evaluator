/**
  Copyright (C) 2011 The University of Rostock.
 
  Written by:  Niels Grewe <niels.grewe@uni-rostock.de>
  Created:  December 2011
  
  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  as published by the Free Software Foundation; either
  version 3 of the License, or (at your option) any later version.

  You should have received a copy of the GNU General Public
  License along with this program; see the file COPYING.
  If not, write to the Free Software Foundation,
  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 */

package de.uni_rostock.goodod.checker;

import java.io.File;
import java.util.HashMap;

import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log; 
import org.apache.commons.logging.LogFactory; 

import de.uni_rostock.goodod.owl.BasicImportingNormalizerFactory;
import de.uni_rostock.goodod.owl.SubClassCollector;
import de.uni_rostock.goodod.tools.Configuration;

import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.HermiT.Configuration.ExistentialStrategyType;
import org.semanticweb.HermiT.Configuration.TableauMonitorType;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.model.*;
//import org.semanticweb.owlapi.reasoner.ConsoleProgressMonitor;

//import org.semanticweb.owlapi.reasoner.ReasonerProgressMonitor;
import org.semanticweb.owlapi.util.SimpleIRIMapper;


/**
 * Principal class for the monotonicity checker. It loads an ontology and
 * compares inconsistencies before and after import changes.
 */
public class CheckerApp 
{
	
	private static Configuration config;
	private static Log logger = LogFactory.getLog(CheckerApp.class);
    
	public static void main( String[] args ) throws OWLOntologyCreationException
    {
		config = Configuration.getConfiguration(args);
		String bioTopVariantA = "biotoplite_group_A_TEST.owl";
		String bioTopVariantB = "biotoplite_group_B_TEST.owl";
		String repoRoot = config.getString("repositoryRoot");
		File commonBioTopF = new File(repoRoot + File.separator + config.getString("bioTopLiteSource"));
		
		String groupAFile = repoRoot + File.separator + "Results" + File.separator + "GruppeA" + File.separator + bioTopVariantA;
		String groupBFile = repoRoot + File.separator + "Results" + File.separator + "GruppeB" + File.separator + bioTopVariantB;
		String testFile = config.getString("testDescription");
    	IRI bioTopIRI = IRI.create("http://purl.org/biotop/biotoplite.owl");
		SimpleIRIMapper bioTopLiteMapper = new SimpleIRIMapper(bioTopIRI,IRI.create(commonBioTopF));
    	SimpleIRIMapper variantMapperA = new SimpleIRIMapper(IRI.create("http://purl.org/biotop/biotoplite_group_A_TEST.owl"),IRI.create(new File(groupAFile)));
    	SimpleIRIMapper variantMapperB = new SimpleIRIMapper(IRI.create("http://purl.org/biotop/biotoplite_group_B_TEST.owl"),IRI.create(new File(groupBFile)));
    	//logger.info("Loading ontology " + testFile + ".");
    	OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		manager.addIRIMapper(variantMapperA);
		manager.addIRIMapper(variantMapperB);
		manager.addIRIMapper(bioTopLiteMapper);
		FileDocumentSource source = new FileDocumentSource(new File(testFile));
		OWLOntology ontology = null;
		try 
		{
			ontology = manager.loadOntologyFromOntologyDocument(source);
		}
		catch (Throwable e)
		{
			logger.fatal("Loading failed", e);
			System.exit(1);
		}
		
		org.semanticweb.HermiT.Configuration reasonerConfig = new org.semanticweb.HermiT.Configuration();
		reasonerConfig.throwInconsistentOntologyException = false;
		//ReasonerProgressMonitor monitor = new ConsoleProgressMonitor();
		reasonerConfig.existentialStrategyType = ExistentialStrategyType.INDIVIDUAL_REUSE;
		//reasonerConfig.reasonerProgressMonitor = monitor;
		reasonerConfig.tableauMonitorType = TableauMonitorType.NONE;
		//reasonerConfig.individualTaskTimeout = 10000;
		Reasoner reasoner = new Reasoner(reasonerConfig, ontology);
		reasoner.classifyClasses();
		Set<OWLClass> before = reasoner.getUnsatisfiableClasses().getEntitiesMinus(manager.getOWLDataFactory().getOWLNothing());
		//logger.info("Found " + before.size() + " inconsistent classes before import change.");
		logger.debug(before);
        
		reasoner.dispose();
		reasoner = null;
		manager.removeOntology(ontology);
		ontology = null;
		
		Map<IRI,IRI> importMap = new HashMap<IRI,IRI>();
    	
		OWLOntologyLoaderConfiguration interimConfig = new OWLOntologyLoaderConfiguration();
		for (String str: config.getStringArray("ignoredImports"))
    	{
			IRI ignoredIRI = IRI.create(str);
			importMap.put(ignoredIRI,bioTopIRI);
			
			interimConfig = interimConfig.addIgnoredImport(ignoredIRI);
    	}
		
		interimConfig = interimConfig.setSilentMissingImportsHandling(true);
		try 
		{
			ontology = manager.loadOntologyFromOntologyDocument(source, interimConfig);
		}
		catch (Throwable e)
		{
			logger.fatal("Loading failed", e);
			System.exit(1);
		}
		BasicImportingNormalizerFactory n = new BasicImportingNormalizerFactory(importMap, interimConfig);
		
		n.normalize(ontology);
		
		reasoner = new Reasoner(reasonerConfig, ontology);
		reasoner.classifyClasses();
		Set<OWLClass> after = reasoner.getUnsatisfiableClasses().getEntitiesMinus(manager.getOWLDataFactory().getOWLNothing());
		
		//logger.info("Found " + after.size() + " inconsistent classes after import change.");
		logger.debug(after);
		
		/*
		 * We need some tidying afterwards. The after set can contain
		 * inconsistent classes that are inconsistent only because in the new
		 * import, they are subclasses of a class that was already inconsistent before.
		 * Hence we remove them from the after set.  
		 */
		for (OWLClass c : before)
		{
			Set <OWLClass> subclasses = SubClassCollector.collect(c, manager.getImportsClosure(ontology));
			for (OWLClass subC : subclasses)
			{
				if ((true == after.contains(subC)) && (false == before.contains(subC)))
				{
					after.remove(subC);
				}
			}
		}
		int difference = before.size() - after.size();
		
		if (0 == difference)
		{
			logger.info(testFile + ": OK");
		}
		else
		{
			logger.warn(testFile + ": Import change is not neutral to inconsistencies (" + before.size() + '/' + after.size() + ")");
		}
    }
}
