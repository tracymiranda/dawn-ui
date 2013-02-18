package org.dawnsci.plotting.tools.region;

import java.text.DecimalFormat;

import org.dawb.common.ui.plot.region.IRegion;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.roi.LinearROI;
import uk.ac.diamond.scisoft.analysis.roi.ROIBase;
import uk.ac.diamond.scisoft.analysis.roi.RectangularROI;

public class MeasurementLabelProvider extends ColumnLabelProvider {

	private static final Logger logger = LoggerFactory.getLogger(MeasurementLabelProvider.class);
	
	private int column;
	private AbstractRegionTableTool tool;
	private DecimalFormat format;

	public MeasurementLabelProvider(AbstractRegionTableTool tool, int i) {
		this.column = i;
		this.tool   = tool;
		this.format = new DecimalFormat("##0.00E0");
	}

	private static final String NA = "-";

	@Override
	public String getText(Object element) {
		
		if (!(element instanceof IRegion)) return null;
		final IRegion    region = (IRegion)element;

		ROIBase roi = tool.getROI(region);

		try {
			Object fobj = null;

			switch(column) {
			case 0:
				return region.getLabel();
			case 1:
				if(roi instanceof LinearROI)
					fobj = ((LinearROI)roi).getPoint()[0];
				else if (roi instanceof RectangularROI)
					fobj = ((RectangularROI)roi).getPoint()[0];
				else
					;
				return fobj == null ? NA : format.format(fobj);
			case 2: // dx
				if(roi instanceof LinearROI)
					fobj = ((LinearROI)roi).getPoint()[1];
				else if (roi instanceof RectangularROI)
					fobj = ((RectangularROI)roi).getPoint()[1];
				else
					;
				return fobj == null ? NA : format.format(fobj);
			case 3: // dy
				if(roi instanceof LinearROI)
					fobj = ((LinearROI)roi).getEndPoint()[0];
				else if(roi instanceof RectangularROI)
					fobj = ((RectangularROI)roi).getEndPoint()[0];
				else
					;
				return fobj == null ? NA : format.format(fobj);
			case 4: // length
				if(roi instanceof LinearROI)
					fobj = ((LinearROI)roi).getEndPoint()[1];
				else if(roi instanceof RectangularROI)
					fobj = ((RectangularROI)roi).getEndPoint()[1];
				else
					;
				return fobj == null ? NA : format.format(fobj);
			case 5: // max
				final double max = tool.getMaxIntensity(region);
			    if (Double.isNaN(max)) return "-";
				return format.format(max);
			case 6: // sum
				final double sum = tool.getSum(region);
				if(Double.isNaN(sum)) return "-";
				return format.format(sum);
			case 7: // isPlot TODO: to be replaced by images 
				if(roi.isPlot())
					return "true";
				else
					return "false";
//			case 7: // out rad
//				if (roi instanceof SectorROI) {
//					SectorROI sroi = (SectorROI) roi;
//					fobj = sroi.getRadius(1);
//				}
//				return fobj == null ? NA : format.format(fobj);
//			case 8: // region
//				return tool.getROI(region).toString();
//	
			default:
				return "Not found";
			}
		} catch (Throwable ne) {
			// One must not throw RuntimeExceptions like null pointers from this
			// methd becuase the user gets an eclipse dialog confusing them with 
			// the error
			logger.error("Cannot get value in info table", ne);
			return "";
		}
	}
	
	public String getToolTipText(Object element) {
		return "Any selection region can be used in measurement tool. Try box and axis selections as well as line...";
	}

}