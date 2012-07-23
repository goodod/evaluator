/**
  Copyright (C) 2012 The University of Rostock.
 
  Written by:  Niels Grewe
  Created: 23.02.2012
  
  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  as published by the Free Software Foundation; either
  version 3 of the License, or (at your option) any later version.

  You should have received a copy of the GNU General Public
  License along with this program; see the file COPYING.
  If not, write to the Free Software Foundation,
  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 */
package de.uni_rostock.goodod.owl.normalization;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.semanticweb.owlapi.model.OWLOntology;


/**
 * @author Niels Grewe
 *
 */
public class NormalizerChainFactory extends AbstractNormalizerFactory {

	private List<? extends NormalizerFactory> factories;
	private static Log logger = LogFactory.getLog(NormalizerChainFactory.class);
	/**
	 * The default constructor reads the chain of normalizers to use from the
	 * configuration file.
	 */
	public NormalizerChainFactory()
	{
		List<NormalizerFactory> fs = new ArrayList<NormalizerFactory>();
		String[] factoryNames = config.getStringArray("chain");
		for (String name : factoryNames)
		{
			String qualifiedName = null;
			
			if (name.contains("."))
			{
				/* 
				 * If it contains a dot, we require it to be a valid Java
				 * binary name for the class (i.e. including the package.
				 */
				qualifiedName = name;
			}
			else
			{
				/*
				 * Classes from this package are precious little buggers, they 
				 * can be identified by a shorthand.
				 */
				qualifiedName = "de.uni_rostock.goodod.owl.normalization." + name + "NormalizerFactory";
			}
			
			
			
			
			Class<?> factoryClass = null;
			try
			{
				factoryClass = this.getClass().getClassLoader().loadClass(qualifiedName);
			} 
			catch (ClassNotFoundException e)
			{
				logger.warn("Could not load normalizer factory class'"+ qualifiedName + "'", e);
			}
			if (null != factoryClass)
			{
				/*
				 * Introspect the class to find out whether it is a proper
				 * factory or in fact a run of the mill normalizer. In the latter case,
				 * add a generic normalizer factor set up for the normalizer class.
				 * Otherwise, add the factory class. 
				 * 
				 * Unfortunately, this doesn't seem to be working, so it's commented out right now.
				for (Class<?>c : factoryClass.getInterfaces())
				{
					logger.info("Interface: " + c.getCanonicalName());
				}
				if (interfaces.contains(Normalizer.class))
				{
					fs.add(new GenericNormalizerFactory(factoryClass.asSubclass(Normalizer.class)));
				}
				else if (interfaces.contains(NormalizerFactory.class))
				{*/
					try
					{
						fs.add((NormalizerFactory)factoryClass.newInstance());
					}
					catch (Throwable t)
					{
						logger.warn("Could not instantiate class", t);
					}
				/*}
				else
				{
					logger.warn(qualifiedName + " does implement neither the Normalizer nor the NormalizerFactory interfaces.");
				}*/
			}
		}
		
		factories = fs;
		logger.debug("Loaded factories as per configuration file: " + factories);
	}
	public NormalizerChainFactory(NormalizerFactory... someFactories)
	{
		List<NormalizerFactory> fs = new ArrayList<NormalizerFactory>();
		
		for (NormalizerFactory f : someFactories)
		{
			fs.add(f);
		}
		factories = fs;
	}
	/* (non-Javadoc)
	 * @see de.uni_rostock.goodod.owl.NormalizerFactory#getNormalizerForOntology(org.semanticweb.owlapi.model.OWLOntology)
	 */
	
	public Normalizer getNormalizerForOntology(OWLOntology ont) {
		if (factories.isEmpty())
		{
			return null;
		}
		List<Normalizer> norms = new ArrayList<Normalizer>(factories.size());
		for (NormalizerFactory f : factories)
		{
			norms.add(f.getNormalizerForOntology(ont));
		}
		return new NormalizerChain(norms);
	}

}