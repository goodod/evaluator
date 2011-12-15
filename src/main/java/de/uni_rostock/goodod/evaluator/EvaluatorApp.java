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

import org.apache.commons.configuration.plist.PropertyListConfiguration;
import org.apache.commons.configuration.ConfigurationException;

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
	private static PropertyListConfiguration config;
	
	private static Log logger = LogFactory.getLog(EvaluatorApp.class);
    
	public static void main( String[] args )
    {
		Map<String,String>configuration = getConfigMap(args);
    	String configFile = configuration.get("configFile");

    	try
    	{
    		if (configFile.isEmpty())
    		{
    			ClassLoader loader = EvaluatorApp.class.getClassLoader();
    			config = new PropertyListConfiguration(loader.getResource("config.plist"));
    		
    		}
    		else
    		{
    			config = new PropertyListConfiguration(configFile);
    		}
    	}
    	catch (ConfigurationException configExc)
    	{
    		logger.fatal("Could not load configuration", configExc);
    		System.exit(1);
    	}

    	String repoRoot = configuration.get("repositoryRoot");
    	if ((null == repoRoot) || repoRoot.isEmpty())
    	{
    		repoRoot = config.getString("repositoryRoot");
    	}
    	if (null == repoRoot)
    	{
    		repoRoot = "";
    	}
    	File biotopF = new File(repoRoot.concat(File.separator.concat(config.getString("bioTopLiteSource"))));
    	File ontologyAF = new File(configuration.get("ontologyA"));
    	File ontologyBF = new File(configuration.get("ontologyB"));
    	
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
    	/*
    	 * First run: Semantic cotopy without/with imports.
    	 */
    	SCComparator comp = new SCComparator(pair, false);
    	ComparisonResult res = comp.compare();
    	logger.info("SCComparison result without imports:" + '\n' + res.toString());
    	comp = new SCComparator(pair, true);
    	res = comp.compare();
    	logger.info("SCComparison result with imports:" + '\n' + res.toString());
    	
    	/*
    	 * Second run: Common semantic cotopy without/with imports.
    	 */
    	comp = new CSCComparator(pair, false);
    	res = comp.compare();
    	logger.info("CSCComparison result without imports:" + '\n' + res.toString());
    	comp = new CSCComparator(pair, true);
    	res = comp.compare();
    	logger.info("CSCComparison result with imports:" + '\n' + res.toString());

    }
	
	
	private static Map<String,String> getConfigMap(String args[])
	{
		String configFile = "";
    	String ontFileA = "";
    	String ontFileB = "";
    	String repoRoot = "";
    	Map<String,String> cfg = new HashMap<String,String>();
    	boolean gotConfig = false;
    	boolean gotOntA = false;
    	boolean gotOntB = false;
    	boolean expectConfigFile = false;
    
    	/*
    	 *  We only support the "-c" or "--config=" command-line switch to
    	 *  determine the location of the configuration file.
    	 */
    	for (String arg : args)
    	{
    		if (false == gotConfig)
    		{
    			if (false == expectConfigFile)
    			{
    				if(arg.startsWith("-"))
    				{
    					if (arg.endsWith("c"))
    					{
    						expectConfigFile = true;
    						continue;
    					}
    				}
    				else if(arg.startsWith("--config=") && (arg.length() > 9))
    				{
    					configFile = arg.substring(9);
    					gotConfig = true;
    					continue;
    				}
    			}
    			else if (true == expectConfigFile)
    			{
    				if (false == arg.isEmpty())
    				{
    					configFile = arg;
    					gotConfig = true;
    					continue;
    				}
    			}
    		}
    		if (false == gotOntA)
    		{
    			if (false == arg.isEmpty())
    			{
    				ontFileA = arg;
    				gotOntA = true;
    				continue;
    			}
    		}
    		if (false == gotOntB)
    		{
    			if (false == arg.isEmpty())
    			{
    				ontFileB = arg;
    				gotOntB = true;
    				continue;
    			}
    		}
    	}
    	
    	repoRoot = System.getenv("GOODOD_REPO_ROOT");
    	cfg.put("configFile", configFile);
    	cfg.put("ontologyA", ontFileA);
    	cfg.put("ontologyB", ontFileB);
    	cfg.put("repositoryRoot", repoRoot);
		return cfg;
	}
}
