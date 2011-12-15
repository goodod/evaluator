/**
  Copyright (C) 2011 The University of Rostock.
 
  Written by:  Niels Grewe <niels.grewe@uni-rostock.de>
  Created: 12.12.2011
  
  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  as published by the Free Software Foundation; either
  version 3 of the License, or (at your option) any later version.

  You should have received a copy of the GNU General Public
  License along with this program; see the file COPYING.
  If not, write to the Free Software Foundation,
  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 */
package de.uni_rostock.goodod.owl;

import org.semanticweb.owlapi.model.*;
import java.util.Stack;

/** 
 * @author Niels Grewe <niels.grewe@uni-rostock.de>
 *
 */
public abstract class ExpressionTransform implements
		OWLClassExpressionVisitorEx<OWLClassExpression> ,OWLPropertyExpressionVisitorEx<OWLObject>, OWLDataRangeVisitorEx<OWLDataRange> {

	protected Stack<OWLClassExpression> stack;
	public ExpressionTransform()
	{
		stack = new Stack<OWLClassExpression>();
	}
}
