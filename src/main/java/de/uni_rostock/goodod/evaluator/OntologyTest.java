/**
  Copyright (C) 2011 The University of Rostock.
 
  Written by:  Niels Grewe <niels.grewe@uni-rostock.de>
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
package de.uni_rostock.goodod.evaluator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.configuration.plist.*;
import org.apache.commons.configuration.AbstractHierarchicalFileConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.SubnodeConfiguration;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.model.*;

import de.uni_rostock.goodod.owl.*;

/**
 * This class encapsulates a single ontology test.
 * 
 * @author Niels Grewe <niels.grewe@uni-rostock.de>
 *
 */
public class OntologyTest {

	private Configuration globalConfig;
	private AbstractHierarchicalFileConfiguration testConfig;
	private URI rawOntology;
	private URI modelOntology;
	private Set<URI> groupAOntologies;
	private Set<URI> groupBOntologies;
	private URI bioTopLiteURI;
	private Map<IRI,IRI>importMap;
	private Set<IRI>testIRIs;
	private Map<URI,Map<URI,FMeasureComparisonResult>> resultMap;
	private boolean considerImports;
	private static Log logger = LogFactory.getLog(OntologyTest.class);
	
	public OntologyTest(File testDescription) throws FileNotFoundException, IOException, OWLOntologyCreationException, ConfigurationException
	{
		
		// Get a reference to the global configuration:
		globalConfig = Configuration.getConfiguration();
		
		// Check whether we are loading an XML property list or a plain on for the test.
		if (isXMLConfig(testDescription))
		{
			testConfig = new XMLPropertyListConfiguration(testDescription);
		}
		else
		{
			testConfig = new PropertyListConfiguration(testDescription);;
		}
		
		// Gather URIs for the raw, model and student ontologies.
		String repoRoot = globalConfig.getString("repositoryRoot");
		String testDir = repoRoot + File.separator + globalConfig.getString("testDir");
		String groupADir = repoRoot + File.separator + globalConfig.configurationAt("groupDirs").getString("groupA");
		String groupBDir = repoRoot + File.separator + globalConfig.configurationAt("groupDirs").getString("groupB");
		File rawFile = new File(testDir + File.separator + testConfig.getString("rawOntology"));
		rawOntology = rawFile.toURI();
		File modelFile = new File(testDir + File.separator + testConfig.getString("modelOntology"));
		modelOntology = modelFile.toURI();
		groupAOntologies = new HashSet<URI>(12);
		groupBOntologies = new HashSet<URI>(12);
		SubnodeConfiguration studentOntConf = testConfig.configurationAt("studentOntologies");
		for (String fileName : studentOntConf.getStringArray("groupA"))
		{
			File studFile = new File(groupADir + File.separator + fileName);
			groupAOntologies.add(studFile.toURI());
		}
		for (String fileName : studentOntConf.getStringArray("groupB"))
		{
			File studFile = new File(groupBDir + File.separator + fileName);
			groupBOntologies.add(studFile.toURI());
		}
		
		// create the result map:
		resultMap = new HashMap<URI,Map<URI,FMeasureComparisonResult>>(25);
	
		// Get URIs for BioTopLite and the ignored imports.
		File biotopF = new File(globalConfig.getString("repositoryRoot") + File.separator + globalConfig.getString("bioTopLiteSource"));
		if (false == biotopF.canRead())
		{
			logger.warn("Could not read BioTopLite.");
		}
		bioTopLiteURI = biotopF.toURI();
		
		
		IRI biotopIRI = IRI.create("http://purl.org/biotop/biotoplite.owl");
    	importMap = new HashMap<IRI,IRI>();
    	
    		
		for (String str: globalConfig.getStringArray("ignoredImports"))
    	{
			importMap.put(IRI.create(str),biotopIRI);
    	}
		
		testIRIs = getIRIsToTest();
		
		considerImports = true;
	}
	
	public void setConsiderImports(boolean withImports)
	{
		considerImports = withImports;
	}
	
	public boolean getConsiderImports()
	{
		return considerImports;
	}
	
	public Set<IRI> getIgnoredImports()
	{
		return importMap.keySet();
	}
	
	public void executeTest() throws Throwable
	{

		
    	
    	
    	Set<URI> allOntologies = new HashSet<URI>(25);
    	allOntologies.addAll(groupAOntologies);
    	allOntologies.addAll(groupBOntologies);
    	allOntologies.add(modelOntology);
    	logger.info("Running comparisons for test '" + getTestName() +"'.");
   
    	for (URI u1 : allOntologies)
    	{
    		for (URI u2 : allOntologies)
    		{
    			OntologyPair p = new OntologyPair(bioTopLiteURI, u1, u2, getIgnoredImports());
    			runComparison(u1, u2, p);
    		}
 
    	}
    	logger.info("Comparisons on '" + getTestName() + "' completed.");
	}
	
	private void runComparison(URI o1, URI o2, OntologyPair pair)
	{
		BasicImportingNormalizer norm = new BasicImportingNormalizer(pair.getLoaderConfiguration());
    	norm.setImportMappings(importMap);
    	
    	try
    	{
    		pair.normalizeWithNormalizer(norm);
    	}
    	catch (OWLOntologyCreationException e)
    	{
    		logger.fatal("Could not normalize ontologies",e);
    	}
    	CSCComparator comp = new CSCComparator(pair, considerImports);
    	FMeasureComparisonResult res = null;
    	if (null == testIRIs)
    	{
    		res = comp.compare();
    	}
    	else
    	{
    		res = comp.compare(testIRIs);
    	}
    	Map<URI,FMeasureComparisonResult> innerMap = resultMap.get(o1);
    	if (null == innerMap)
    	{
    		innerMap = new HashMap<URI,FMeasureComparisonResult>(25);
    		resultMap.put(o1, innerMap);
    	}
    	//logger.info("Got comparison result:" + '\n' + res.toString());
    	innerMap.put(o2, res);
	}
	
	private String getTestName()
	{
		return testConfig.getString("testName", "Unnamed Test");
	}
	
	
	private Set<IRI>getIRIsToTest()
	{
		OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();
		config = config.setSilentMissingImportsHandling(true);
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		FileDocumentSource rawSource = new FileDocumentSource(new File(rawOntology));
		OWLOntology o = null;
		try
		{
			o = manager.loadOntologyFromOntologyDocument(rawSource, config);
		}
		catch (OWLOntologyCreationException e)
		{
			logger.warn("Could not load raw test classes", e);
		}
		Set<OWLClass> classes = o.getClassesInSignature();

		//Find the test class
		for (OWLClass c : classes)
		{
			String fragment = c.getIRI().getFragment();
		
			if ((null != fragment)
			  && (fragment.equalsIgnoreCase("Test")))
			{
				Set<OWLClassExpression> subClasses = c.getSubClasses(o);
				Set<IRI> subIRIs = new HashSet<IRI>(subClasses.size());
				
				for (OWLClassExpression ce : subClasses)
				{
					if (ce instanceof OWLClass)
					{
						subIRIs.add(ce.asOWLClass().getIRI());
					}
				}
				return subIRIs;
			}
		}
		return null;
	}
	
	private boolean isXMLConfig(File confFile) throws FileNotFoundException, IOException
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
				return true;
			}
		
		return false;
	}
	
	public TestResult getTestResultBetween(Set<URI> computed, Set<URI> reference)
	{
		
		int i = 0;
		double pre = 0;
		double rec = 0;
		double fm = 0;
		for (Map.Entry<URI,Map<URI,FMeasureComparisonResult>> e1 : resultMap.entrySet())
		{
			if (computed.contains(e1.getKey()))
			{
				for (Map.Entry<URI, FMeasureComparisonResult> e2 : e1.getValue().entrySet())
				{
					if (reference.contains(e2.getKey()) &&
						(false == e1.getKey().equals(e2.getKey())))
					{
						i++;
						pre += e2.getValue().getPrecision();
						rec += e2.getValue().getRecall();
						fm  += e2.getValue().getFMeasure();
					}
				}
			}
		}
		return new TestResult(i,pre,rec,fm,reference,computed);
	}
	
	public TestResult getTestResultGroupAAgainstReference()
	{
		Set<URI> ref = Collections.singleton(modelOntology);
		return getTestResultBetween(groupAOntologies, ref);
	}
	
	public TestResult getTestResultGroupBAgainstReference()
	{
		Set<URI> ref = Collections.singleton(modelOntology);
		return getTestResultBetween(groupBOntologies, ref);
	}
	
	public TestResult getTestResultAllAgainstReference()
	{
		Set<URI> ref = Collections.singleton(modelOntology);
		Set<URI> studentOnt = new HashSet<URI>(24);
		studentOnt.addAll(groupAOntologies);
		studentOnt.addAll(groupBOntologies);
		return getTestResultBetween(studentOnt, ref);
	}
	
	public TestResult getTestResultAllAgainstAll()
	{
		Set<URI> studentOnt = new HashSet<URI>(24);
		studentOnt.addAll(groupAOntologies);
		studentOnt.addAll(groupBOntologies);
		return getTestResultBetween(studentOnt, studentOnt);
	}
	
	public TestResult getTestResultGroupAInternal()
	{
		return getTestResultBetween(groupAOntologies,groupAOntologies);
	}
	
	public TestResult getTestResultGroupBInternal()
	{
		return getTestResultBetween(groupBOntologies,groupBOntologies);
	}
	
	public TestResult getTestResultGroupAAgainstGroupB()
	{
		return getTestResultBetween(groupAOntologies,groupBOntologies);
	}
	
	public TestResult getTestResultGroupBAgainstGroupA()
	{
		return getTestResultBetween(groupBOntologies,groupAOntologies);
	}
	
	
	@Override
	public String toString()
	{
		TestResult internalA = getTestResultGroupAInternal();
		TestResult internalB = getTestResultGroupBInternal();
		TestResult AvsB = getTestResultGroupAAgainstGroupB();
		TestResult BvsA = getTestResultGroupBAgainstGroupA();
		TestResult AvsRef = getTestResultGroupAAgainstReference();
		TestResult BvsRef = getTestResultGroupBAgainstReference();
		TestResult AllvsRef = getTestResultAllAgainstReference();
		TestResult AllvsAll = getTestResultAllAgainstAll();
		return "Test result report for '" + getTestName() + "' (mean values)" + '\n' +
				'\t' + '\t' + "Precision" + '\t' + '\t' + "Recall" + '\t'+ '\t' + '\t' + "F-Measure" + '\n' +
				"all vs. model" + '\t' + AllvsRef.toString() + '\n' +
				"A vs. model" + '\t' + AvsRef.toString() + '\n' +
				"B vs. model" + '\t' + BvsRef.toString() + '\n' +
				"all vs. all" + '\t' + AllvsAll.toString() + '\n' +
				"A vs. B" + '\t' + '\t' + AvsB.toString() + '\n' +
				"B vs. A" + '\t' + '\t' + BvsA.toString() + '\n' +
				"A internal" + '\t' + internalA.toString() + '\n' +
				"B internal" + '\t' + internalB.toString();
	
	}
	
	private String shortNameForURI(URI u)
	{
	
		String s = u.toString();
		s = s.substring((u.getScheme().length()+1));
		String repoRoot = globalConfig.getString("repositoryRoot");
		String testDir = repoRoot + File.separator + globalConfig.getString("testDir");
		String groupADir = repoRoot + File.separator + globalConfig.configurationAt("groupDirs").getString("groupA");
		String groupBDir = repoRoot + File.separator + globalConfig.configurationAt("groupDirs").getString("groupB");
		
		String marker = "";
		if (groupAOntologies.contains(u))
		{
			marker = "A:";
		}
		else if (groupBOntologies.contains(u))
		{
			marker = "B:";
		}
		
		if (s.startsWith(testDir))
		{
			return s.substring(testDir.length());
		}
		else if (s.startsWith(groupADir))
		{
			return marker + s.substring(groupADir.length());
		}
		else if (s.startsWith(groupBDir))
		{
			return marker + s.substring(groupBDir.length());
		}
		return s;
	}
	
	private String tableHeader(List<URI> uris)
	{
		String header = "\"\",";
		for (URI u : uris)
		{
			String s = '"'+ shortNameForURI(u) + '"';
			header = header.concat(s).concat(",");
		}
		return header.substring(0, (header.length()-1));
		
	}

	private enum StatType { PRECISION, RECALL, FMEASURE };
	
	private String tableLine(URI u, List<URI>ontologies, StatType type)
	{
		String line = '"' + shortNameForURI(u) + '"' + ",";
		for (URI u2 : ontologies)
		{
			FMeasureComparisonResult res = resultMap.get(u).get(u2);
			double value = 0;
			if (null != res)
			{
				switch (type)
				{
				case PRECISION:
					value = res.getPrecision();
					break;
				case RECALL:
					value = res.getRecall();
					break;
				case FMEASURE:
					value = res.getFMeasure();
					break;
				}
				line = line + '"' + value + '"' + ",";
			}
			else
			{
				// Empty value:
				line = line + '"' + '"' + ",";
			}
		}
		
		return line.substring(0,(line.length()-1));
	}
	private void writeTable(FileWriter writer, StatType type) throws IOException
	{
		String theTable = "";
		Set<URI> allOntologies = new HashSet<URI>(25);
    	allOntologies.addAll(groupAOntologies);
    	allOntologies.addAll(groupBOntologies);
    	allOntologies.add(modelOntology);
    	List<URI> ontologyList = new ArrayList<URI>(allOntologies);
    	theTable = tableHeader(ontologyList) + '\n';
    	for (URI u : ontologyList)
    	{
    		theTable = theTable + tableLine(u, ontologyList, type) + '\n';
    	}
    	writer.write(theTable);
    	writer.flush();
	}
	
	public void writePrecisionTable(FileWriter w) throws IOException
	{
		writeTable(w, StatType.PRECISION);
	}
	
	public void writeRecallTable(FileWriter w) throws IOException
	{
		writeTable(w, StatType.RECALL);
	}
	
	public void writeFMeasureTable(FileWriter w) throws IOException
	{
		writeTable(w, StatType.FMEASURE);
	}
	

}
