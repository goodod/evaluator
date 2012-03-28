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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration.plist.PropertyListConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.SubnodeConfiguration;

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

	private Map<String,String>envAndCommandLine;
	private PropertyListConfiguration config;
	private static Configuration theConfiguration;
	private static Log logger = LogFactory.getLog(Configuration.class);
	
	private Configuration(String args[])
	{
		envAndCommandLine = getConfigMap(args);
		String configFile = envAndCommandLine.get("configFile");

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
    	logger.debug("Configuration loaded");
	}
	
	public String getString(String key)
	{
		String value = envAndCommandLine.get(key);
		if (null != value)
		{
			return value;
		}
		return config.getString(key);
	}
	
	public String [] getStringArray(String key)
	{
		// String arrays are only stored in the "true" configuration.
		return config.getStringArray(key);

	}
	
	public int getInt(String key)
	{
		String value = envAndCommandLine.get(key);
		if (null == value)
		{
			return config.getInt(key);
		}
		try
		{
			return Integer.parseInt(value);
		}
		catch (NumberFormatException e)
		{
			logger.warn("Could not interpret value '" + value + "' for key '" + key + "' as integer.", e);
		}
		return 0;
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

	private static Map<String,String> getConfigMap(String args[])
	{
		String configFile = "";
    	String testDescriptionFile = "";
    	String threadCount = null;
	String similarity = null;
    	String repoRoot = System.getenv("GOODOD_REPO_ROOT");
    	Map<String,String> cfg = new HashMap<String,String>();
    	boolean gotConfig = false;
    	boolean gotDescriptionFile = false;
    	boolean gotThreadCount = false;
    	boolean expectConfigFile = false;
    	boolean expectThreadCount = false;
   	boolean expectSimilarity = false;
	boolean gotSimilarity = false; 
    	cfg.put("repositoryRoot", repoRoot);
    	if (null == args)
    	{
    		return cfg;
    	}
    	/*
    	 *  We only support the "-c", "--config=", "-t", "--threadCount=" command-line switches to
    	 *  determine the location of the configuration file and the number of threads to use.
    	 */
    	for (String arg : args)
    	{
    		if ((false == gotConfig) || (false == gotThreadCount) || (false == gotSimilarity)) 
    		{
    			if ((false == expectConfigFile) && (false == expectThreadCount) && (false == expectSimilarity))
    			{
    				if(arg.startsWith("-") && (2 == arg.length())) 
    				{
    					if (arg.endsWith("c"))
    					{
    						expectConfigFile = true;
    						continue;
    					}
    					else if (arg.endsWith("t"))
    					{
    						expectThreadCount = true;
    						continue;
    					}
					else if (arg.endsWith("s"))
					{
						expectSimilarity = true;
						continue;
					}
    				}
    				else if(arg.startsWith("--config=") && (arg.length() > 9))
    				{
    					configFile = arg.substring(9);
    					gotConfig = true;
    					continue;
    				}
    				else if (arg.startsWith("--threadCount=") && (arg.length() > 14))
    				{
    					threadCount = arg.substring(14);
    					gotThreadCount = true;
    					continue;
    					
    				}
				else if (arg.startsWith("--similarity=") && (arg.length() > 13))
				{
					similarity= arg.substring(13);
					gotSimilarity = true;
				}
    			}
    			else if (true == expectConfigFile)
    			{
    				if (false == arg.isEmpty())
    				{
    					configFile = arg;
    					gotConfig = true;
    					expectConfigFile = false;
    					continue;
    				}
    			}
    			else if (true == expectThreadCount)
    			{
    				if (false == arg.isEmpty())
    				{
    					threadCount = arg;
    					gotThreadCount = true;
    					expectThreadCount = false;
    					continue;
    				}
    			}
			else if (true == expectSimilarity)
			{
				if (false == arg.isEmpty())
				{
					similarity = arg;
					gotSimilarity = true;
					expectSimilarity = false;
					continue;

				}
			}
    		}
    		if (false == gotDescriptionFile)
    		{
    			if (false == arg.isEmpty())
    			{
    				testDescriptionFile = arg;
    				gotDescriptionFile = true;
    				continue;
    			}
    		}

    	}
    	
    	
    	cfg.put("configFile", configFile);
    	cfg.put("testDescription", testDescriptionFile);
    	if (gotThreadCount)
    	{
    		cfg.put("threadCount", threadCount);
    	}
	if (gotSimilarity)
	{
		cfg.put("similarity", similarity);
	}

    	
		return cfg;
	}
}
