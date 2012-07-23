/**
  Copyright (C) 2011 The University of Rostock.
 
  Written by:  Niels Grewe
  Created: 17.12.2011
  
  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  as published by the Free Software Foundation; either
  version 3 of the License, or (at your option) any later version.

  You should have received a copy of the GNU General Public
  License along with this program; see the file COPYING.
  If not, write to the Free Software Foundation,
  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 */
package de.uni_rostock.goodod.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.configuration.plist.PropertyListConfiguration;
import org.apache.commons.configuration.plist.XMLPropertyListConfiguration;
import org.apache.commons.configuration.CombinedConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.HierarchicalConfiguration.Node;
import org.apache.commons.configuration.SubnodeConfiguration;

import org.apache.commons.cli.*;

import org.apache.commons.logging.Log; 
import org.apache.commons.logging.LogFactory; 

import de.uni_rostock.goodod.evaluator.EvaluatorApp;

/**
 * Singleton class to hold tool-global configuration values. 
 * 
 * @author Niels Grewe <niels.grewe@uni-rostock.de>
 *
 */
public class Configuration {

	private CombinedConfiguration config;
	private static Configuration theConfiguration;
	private static Log logger = LogFactory.getLog(Configuration.class);
	final private Options options = new Options();
	private Configuration(String args[])
	{
		setupCommandLineOptions();
		config = new CombinedConfiguration();
		// Comandline arguments have highest precedence, add them first.
		config.addConfiguration(getConfigMap(args), "environment");
		String configFile = config.getString("configFile");
		PropertyListConfiguration mainConfig = null;
    	try
    	{
    		if ((null == configFile) || configFile.isEmpty())
    		{
    			ClassLoader loader = EvaluatorApp.class.getClassLoader();
    			mainConfig = new PropertyListConfiguration(loader.getResource("config.plist"));
    		
    		}
    		else
    		{
    			mainConfig = new PropertyListConfiguration(configFile);
    		}
    	}
    	catch (ConfigurationException configExc)
    	{
    		logger.fatal("Could not load configuration", configExc);
    		System.exit(1);
    	}
    	config.addConfiguration(mainConfig, "configurationFile");
    	logger.debug("Configuration loaded");
	}
	
	public String getString(String key)
	{
		return config.getString(key);
	}
	
	public String [] getStringArray(String key)
	{
		return config.getStringArray(key);

	}
	
	public int getInt(String key)
	{
		return config.getInt(key);
	}
	
	public boolean getBoolean(String key)
	{
		return config.getBoolean(key);
	}
	
	public boolean getBoolean(String key, boolean def)
	{
		return config.getBoolean(key, def);
	}
	
	public SubnodeConfiguration configurationAt(String key)
	{
		return config.configurationAt(key);
	}
	
	
	
	public static Configuration getConfiguration()
	{
		return getConfiguration(null);
	}
	
	public static synchronized Configuration getConfiguration(String args[]) { 
		  if (null == theConfiguration)
		  { 
			  theConfiguration = new Configuration(args); 
		  } 
		  return theConfiguration; 
	} 

	@SuppressWarnings("static-access")
	private void setupCommandLineOptions()
	{
		
		Option config = OptionBuilder.withArgName("configFile")
				.hasArg()
				.withDescription("Specify the location of the global configuration file.")
				.withLongOpt("config")
				.create('c');
		Option threads = OptionBuilder.withArgName("num")
				.hasArg()
				.withDescription("Specify the number of threads to use.")
				.withLongOpt("threads")
				.withType(Number.class)
				.create('t');
		Option similarity = OptionBuilder.withArgName("algorithm")
				.hasArg()
				.withDescription("Specify the similarity measurement algorithm.")
				.withLongOpt("similarity")
				.create("s");
		
		options.addOption(config);
		options.addOption(threads);
		options.addOption(similarity);
		options.addOption("h", "help", false, "Print this help text");
		options.addOption("d", "debug", false, "Enable debugging");
	}
	private HierarchicalConfiguration getConfigMap(String args[])
	{
		
    	CombinedConfiguration cfg = new CombinedConfiguration();
    	HierarchicalConfiguration envCfg = new CombinedConfiguration();
		String repoRoot = System.getenv("GOODOD_REPO_ROOT");
		
		envCfg.addProperty("repositoryRoot", repoRoot);
    	if (null == args)
    	{
    		return cfg;
    	}
    	GnuParser cmdLineParser = new GnuParser();
    	CommandLine cmdLine = null;
    	try
    	{
    		// parse the command line arguments
    		cmdLine = cmdLineParser.parse( options, args );
    	}
    	 catch( ParseException exception )
    	 {
    		 logger.fatal("Could not validate command-line", exception);
    		 System.exit(1);
    	 }
    	
    	if (cmdLine.hasOption('c'))
    	{
    		envCfg.addProperty("configFile", cmdLine.getOptionValue('c'));
    	}
    	if (cmdLine.hasOption('t'))
    	{
    		envCfg.addProperty("threadCount", cmdLine.getOptionObject('t'));
    	}
    	
    	if (cmdLine.hasOption('s'))
    	{
    		envCfg.addProperty("similarity", cmdLine.getOptionValue('s'));
    	}
    	if (cmdLine.hasOption('h'))
    	{
    		envCfg.addProperty("helpMode", true);
    	}
    	if (cmdLine.hasOption('d'))
    	{
    		envCfg.addProperty("debug", true);
    	}
    	
    	//Fetch the remaining arguments, but alas, commons-cli is not generics aware
    	@SuppressWarnings("unchecked")
		List<String> argList = cmdLine.getArgList();
    	HierarchicalConfiguration testConfig = null;
    	try
    	{
    		if (argList.isEmpty())
    		{
    			logger.fatal("No test specification provided");
    			System.exit(1);
    		}
    		else if (1 == argList.size())
	    	{
	    		File testFile = new File(argList.get(0));
	    		if (isXMLConfig(testFile))
	    		{
	    			testConfig = new XMLPropertyListConfiguration(testFile);
	    		}
	    		else
	    		{
	    			testConfig = new PropertyListConfiguration(testFile);
	    		}
	    		envCfg.addProperty("testFile", testFile.toString());
	    	}
	    	else
	    	{
	    		/*
	    		 *  For > 1 file, we assume that both are ontologies and we
	    		 *  construct ourselves a test case configuration for them.
	    		 */
	    		testConfig = new HierarchicalConfiguration();
	    		String ontologyA = argList.get(0);
	    		String ontologyB = argList.get(1);
	    		testConfig.addProperty("testName", "Comparison of " + ontologyA + " and " + ontologyB);
	    		testConfig.addProperty("notInRepository", true);
	    		Node studentOntologies = new Node("studentOntologies");
	    		Node groupA = new Node("groupA", Collections.singletonList(ontologyA));
	    		Node groupB = new Node("groupB", Collections.singletonList(ontologyB));
	    		studentOntologies.addChild(groupA);
	    		studentOntologies.addChild(groupB);
	    		testConfig.getRoot().addChild(studentOntologies);
	    		if (2 < argList.size())
	    		{
	    			logger.warn("Ignoring extra arguments to comparison between individual ontologies");
	    		}
	    		envCfg.addProperty("testFile", "unknown.plist");
	    	}
    	}
    	catch (Throwable t)
    	{
    		logger.fatal("Could not load test configuration", t);
    		System.exit(1);
    	}
    	cfg.addConfiguration(envCfg, "environment");
    	cfg.addConfiguration(testConfig, "TestSubTree", "testDescription");
		return cfg;
	}

	private static boolean isXMLConfig(File confFile) throws FileNotFoundException, IOException
	{
		FileReader reader = new FileReader(confFile);
		char beginning [] = new char [7];
		// we search for "<?xml" but need two additional characters to accommodate a potential BOM
		reader.read(beginning, 0, 7);
		if (   ('<' == beginning[0])
			&& ('?' == beginning[1])
			&& ('x' == beginning[2])
			&& ('m' == beginning[3])
			&& ('l' == beginning[4])
		   )
		{
			reader.close();
			return true;
		}
		
		// Case with byte order mark:
		if (   ('<' == beginning[2])
				&& ('?' == beginning[3])
				&& ('x' == beginning[4])
				&& ('m' == beginning[5])
				&& ('l' == beginning[6])
			   )
			{
				reader.close();
				return true;
			}
		reader.close();
		return false;
	}
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder("Root keys:" + '\n');
		@SuppressWarnings("unchecked")
		Iterator<String> iter = config.getKeys();
		String s = null;
		while (iter.hasNext())
		{
			s = iter.next();
			builder.append(s + '\n');
		}
		return builder.toString();
	}
	
	
	public SubnodeConfiguration configurationFromDomainForClassWithShorthandSuffix(
	  String domain,
	  Class<?>theClass,
	  String suffix)
	{
		String className = theClass.getName();
		String shorthand = null;
		SubnodeConfiguration theConf = null;
		// commons-configuration uses dots to separate parts of a key path, so
		// we need to mangle the class name.
		String mangledName = className.replace(".", "..");
		
		// Check whether we can get a shorthand for the configuration of this class:
		if (className.endsWith(suffix))
		{
			int lastDot = className.lastIndexOf(".");
			shorthand = className.substring((lastDot + 1), (className.length() - suffix.length()));
		}
		String key = null;
		if (shorthand != null)
		{
			key = domain + '.' + shorthand;

			if (false == config.getKeys(key).hasNext())
			{
				key = null;
			}
		}
		if (key == null)
		{
			key = domain + '.' + mangledName;
			if (false == config.getKeys(key).hasNext())
			{
				key = null;
			}
		}
		
		if (key != null)
		{
			theConf = config.configurationAt(key);
		}
		
		return theConf;
	}
}
