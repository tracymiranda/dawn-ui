/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.slicing.tools.hyper;

import java.util.List;

import org.eclipse.dawnsci.analysis.api.dataset.IDataset;
import org.eclipse.dawnsci.analysis.api.dataset.ILazyDataset;
import org.eclipse.dawnsci.analysis.api.dataset.Slice;
import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.dawnsci.plotting.api.region.IRegion.RegionType;

/**
 * Interface for creating an object to reduce an ND array for display in the Hyperwindow
 * 
 * TODO FIXME Interfaces require Javadoc!
 */
public interface IDatasetROIReducer {

	boolean isOutput1D();
	
	IDataset reduce(ILazyDataset data, List<IDataset> axes, IROI roi, Slice[] slices, int[] order);
	
	List<RegionType> getSupportedRegionType();
	
	IROI getInitialROI(List<IDataset> axes, int[] order);
	
	boolean supportsMultipleRegions();
	
	List<IDataset> getAxes();
	
}
