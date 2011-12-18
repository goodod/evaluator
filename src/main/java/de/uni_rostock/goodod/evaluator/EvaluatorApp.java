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

import org.apache.commons.logging.Log; 
import org.apache.commons.logging.LogFactory; 


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
    	
    }
	
}
