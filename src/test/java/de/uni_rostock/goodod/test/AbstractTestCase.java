/**
  Copyright (C) 2012 The University of Rostock.
 
  Written by:  thebeing
  Created: 16.02.2012
  
  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  as published by the Free Software Foundation; either
  version 3 of the License, or (at your option) any later version.

  You should have received a copy of the GNU General Public
  License along with this program; see the file COPYING.
  If not, write to the Free Software Foundation,
  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 */
package de.uni_rostock.goodod.test;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.junit.*;
import junit.framework.TestCase;

/**
 * @author Niels Grewe
 * Abstract class for testcases
 */
public abstract class AbstractTestCase extends TestCase {
	final protected String baseIRI = "http://www.phf.uni-rostock.de/goodod/test.owl";
	final protected OWLDataFactory factory = OWLManager.getOWLDataFactory();
	protected OWLOntologyManager manager;
	/**
	 * Generate an IRI for use in the test.
	 * @param fragment The fragment to append (will be prefixed with '#')
	 * @return An IRI based on the base for our test.
	 */
	protected IRI IRI(String fragment)
	{
		return IRI.create(baseIRI + "#" + fragment);
	}
	
	@Override
	@Before public void setUp() throws OWLOntologyCreationException
	{
		manager = OWLManager.createOWLOntologyManager(factory);
	}
	
}
