package org.dawb.workbench.plotting.tools.profile;

import ncsa.hdf.object.Group;

import org.dawb.common.ui.plot.region.IRegion;
import org.dawb.hdf5.IHierarchicalDataFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.DatasetUtils;
import uk.ac.diamond.scisoft.analysis.roi.ROIProfile;
import uk.ac.diamond.scisoft.analysis.roi.SectorROI;

public class RadialProfileTool extends SectorProfileTool {


	@Override
	protected AbstractDataset[] getXAxis(final SectorROI sroi, AbstractDataset[] integrals) {
		
		final AbstractDataset xi = DatasetUtils.linSpace(sroi.getRadius(0), sroi.getRadius(1), integrals[0].getSize(), AbstractDataset.FLOAT64);
		xi.setName("Radius (pixel)");
		
		if (!sroi.hasSeparateRegions())  return new AbstractDataset[]{xi};
		
		final AbstractDataset xii = DatasetUtils.linSpace(sroi.getRadius(0), sroi.getRadius(1), integrals[1].getSize(), AbstractDataset.FLOAT64);
		xii.setName("Radius (pixel)");
		
		return new AbstractDataset[]{xi, xii};
	}

	@Override
	protected AbstractDataset[] getIntegral(AbstractDataset data,
			                              AbstractDataset mask, 
			                              SectorROI       sroi, 
			                              IRegion         region,
			                              boolean         isDrag) {


		AbstractDataset[] profile = ROIProfile.sector(data, mask, sroi, true, false, isDrag);
		
        if (profile==null) return null;
				
		final AbstractDataset integral = profile[0];
		integral.setName("Radial Profile "+region.getName());
		
		// If not symmetry profile[2] is null, otherwise plot it.
	    if (profile.length>=3 && profile[2]!=null && sroi.hasSeparateRegions()) {
	    	
			final AbstractDataset reflection = profile[2];
			reflection.setName("Symmetry "+region.getName());

			return new AbstractDataset[]{integral, reflection};
	    	
	    } else {
	    	return new AbstractDataset[]{integral};
	    }
	}

	@Override
	public IStatus export(IHierarchicalDataFile file, Group parent, AbstractDataset data, IProgressMonitor monitor) throws Exception {
        throw new Exception("Not implemented as yet!");
	}
}
