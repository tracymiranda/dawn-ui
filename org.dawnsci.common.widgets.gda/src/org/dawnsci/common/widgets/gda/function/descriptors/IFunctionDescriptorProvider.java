/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.common.widgets.gda.function.descriptors;

import org.eclipse.dawnsci.analysis.api.fitting.functions.IFunction;

public interface IFunctionDescriptorProvider {
	IFunctionDescriptor getDescriptor(String functionName);
	IFunctionDescriptor getDescriptor(IFunction functionInstance);
	IFunctionDescriptor[] getFunctionDescriptors();
	IFunction getFunction(String functionName) throws FunctionInstantiationFailedException;
}
