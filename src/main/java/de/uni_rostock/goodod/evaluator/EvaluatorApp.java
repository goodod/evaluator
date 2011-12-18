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

package de.uni_rostock.goodod.evaluator;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


import org.apache.commons.logging.Log; 
import org.apache.commons.logging.LogFactory; 
import org.semanticweb.owlapi.model.*;

import de.uni_rostock.goodod.owl.*;

/**
 * Principal class for the evaluator. It loads the configuration and schedules
 * comparisons to be run.
 */
public class EvaluatorApp 
{
	
	private static Configuration config;
	private static Log logger = LogFactory.getLog(EvaluatorApp.class);
    
	public static void main( String[] args )
    {
		String repoRoot = null;
		
		config = Configuration.getConfiguration(args);
    
    	String testFile = config.getString("testDescription");
    	OntologyTest theTest = null;
    	try
    	{
    		theTest = new OntologyTest(new File(testFile));
    		theTest.executeTest();
    	}
    	catch (Throwable e)
    	{
    		logger.fatal("Fatal error", e);
    		System.exit(1);
    	}
    	
    	logger.info(theTest.toString());
    	
    	/*
    	if (null == repoRoot)
    	{
    		repoRoot = "";
    	}
    	File biotopF = new File(repoRoot.concat(File.separator.concat(config.getString("bioTopLiteSource"))));
    	File ontologyAF = new File(config.getString("ontologyA"));
    	File ontologyBF = new File(config.getString("ontologyB"));
    	
    	if (false == biotopF.canRead())
    	{
    		logger.fatal("Could not read BioTopLite.");
    		System.exit(1);
    	}
    	Set<URI> ignore = new HashSet<URI>();
    	for (String str: config.getStringArray("ignoredImports"))
    	{
    		try
    		{
    		   ignore.add(new URI(str));
    		}
    		catch (Throwable e)
    		{
    			logger.warn("Could not create URI", e);
    		}
    	}
    	OntologyPair pair = null;
    	try
    	{
    		pair = new OntologyPair(biotopF.toURI(),ontologyAF.toURI(),ontologyBF.toURI(),ignore);
    	}
    	catch (OWLOntologyCreationException e)
    	{
    		logger.fatal("Could not create ontologies.", e);
    		System.exit(1);
    	}
    	IRI biotopIRI = IRI.create("http://purl.org/biotop/biotoplite.owl");
    	Map<IRI,IRI> importMap = new HashMap<IRI,IRI>();
    	for (URI u : ignore)
    	{
    		importMap.put(IRI.create(u),biotopIRI);
    	}
    	
    	BasicImportingNormalizer norm = new BasicImportingNormalizer(pair.getLoaderConfiguration());
    	norm.setImportMappings(importMap);
    	
    	try
    	{
    		pair.normalizeWithNormalizer(norm);
    	}
    	catch (OWLOntologyCreationException e)
    	{
    		logger.fatal("Could not normalize ontologies",e);
    		System.exit(1);
    	}
    	
    	 //First run: Semantic cotopy without/with imports.
    	 
    	SCComparator comp = new SCComparator(pair, false);
    	ComparisonResult res = comp.compare();
    	logger.info("SCComparison result without imports:" + '\n' + res.toString());
    	comp = new SCComparator(pair, true);
    	res = comp.compare();
    	logger.info("SCComparison result with imports:" + '\n' + res.toString());
    	
    	
    	 // Second run: Common semantic cotopy without/with imports.
    	
    	comp = new CSCComparator(pair, false);
    	res = comp.compare();
    	logger.info("CSCComparison result without imports:" + '\n' + res.toString());
    	comp = new CSCComparator(pair, true);
    	res = comp.compare();
    	logger.info("CSCComparison result with imports:" + '\n' + res.toString());
	*/
    }
	
}
