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
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.logging.Log; 
import org.apache.commons.logging.LogFactory; 
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import de.uni_rostock.goodod.tools.Configuration;


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
		Logger root = Logger.getRootLogger();
		if (false == root.getAllAppenders().hasMoreElements())
		{
			root.addAppender(new ConsoleAppender(
					new PatternLayout(PatternLayout.TTCC_CONVERSION_PATTERN)));
			root.setLevel(Level.INFO);
		}
		config = Configuration.getConfiguration(args);
    
	
		if (config.getBoolean("debug", false))
		{
			root.setLevel(Level.DEBUG);
		}
		
    	OntologyTest theTest = null;
    	String testFile = config.getString("testFile");
    	try
    	{
    		theTest = new OntologyTest(config.configurationAt("testDescription"));
    		theTest.executeTest();
    	}
    	catch (Throwable e)
    	{
    		logger.fatal("Fatal error", e);
    		System.exit(1);
    	}
    	
    	logger.info(theTest.toString());
	String similarityType = config.getString("similarity");
	if (!(similarityType.equals("csc") || similarityType.equals("sc")))
	{
		similarityType = "csc";
	}
    	String baseName = similarityType + "-" + testFile.substring(0, (testFile.length() - 6));
    	
    	File precisionFile =  null; 
    	File recallFile = null;
    	File fmeasureFile = null;
    	
    	
    	precisionFile = new File(baseName + ".precision.csv");
    	recallFile = new File(baseName + ".recall.csv");
    	fmeasureFile = new File(baseName + ".fmeasure.csv");
    	try
    	{
    		theTest.writePrecisionTable(new FileWriter(precisionFile));
    		theTest.writeRecallTable(new FileWriter(recallFile));
    		theTest.writeFMeasureTable(new FileWriter(fmeasureFile));
    	}
    	catch (IOException e)
    	{
    		logger.warn("Could not write test data", e);
    	}
    }
	
}
