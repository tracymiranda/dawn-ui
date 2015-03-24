package org.dawnsci.plotting.draw2d.tests;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.dawnsci.plotting.draw2d.swtxy.AspectAxis;
import org.dawnsci.plotting.draw2d.swtxy.ImageTrace;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.dawnsci.analysis.api.dataset.IDataset;
import org.eclipse.dawnsci.analysis.dataset.impl.AbstractDataset;
import org.eclipse.dawnsci.analysis.dataset.impl.Dataset;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.dawnsci.plotting.api.PlotType;
import org.eclipse.dawnsci.plotting.api.PlottingFactory;
import org.eclipse.dawnsci.plotting.api.trace.IImageTrace;
import org.eclipse.nebula.visualization.xygraph.linearscale.Range;
import org.eclipse.swt.widgets.Composite;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class AutoscaleTest extends PluginTestBase{
	
	private IPlottingSystem system;
	private IDataset imageData;
	private IImageTrace trace;

	@Parameters
	public static Collection<Object[]> data() {
		List<Object[]> params = new ArrayList<>();
	
		Dataset input;
	
		// normal data
		input = AbstractDataset.arange(100, Dataset.FLOAT64);
		input.setShape(10, 10);
		params.add(new Object[] {input});
		
		// normal data with some negative values
		input = AbstractDataset.arange(-100, 100, 2, Dataset.FLOAT64);
		input.setShape(10, 10);
		params.add(new Object[] {input});

		return params;
	}
	
	public AutoscaleTest(IDataset imageData) {
		this.imageData = imageData;
	}
	
	protected void createControl(Composite parent) throws Exception {
		system = PlottingFactory.createPlottingSystem();
		system.createPlotPart(parent, "plot", null, PlotType.IMAGE, null);
	}
	
	@Before
	public void createProvidersAndTrace() throws Exception {
		readAndDispatch();
		trace = system.createImageTrace("trace");
		readAndDispatch(10);
	}
	
	@Test
	public void testAutoscaleIsConsistent() throws Exception {
		
		// Create our plot and set a log scale on the x axis
		system.createPlot1D(null, Arrays.asList(imageData), new NullProgressMonitor());
		
		AspectAxis xAxis = ((ImageTrace) trace).getXAxis();
		AspectAxis yAxis = ((ImageTrace) trace).getYAxis();
		
		xAxis.setLogScale(true);
		
		// Autoscale once, this result should be reproducible with subsequent calls
		system.autoscaleAxes();
		
		Range xAutoscaleRange = xAxis.getRange();
		Range yAutoscaleRange = yAxis.getRange();
		
		for (int i = 0; i < 5; i++){
			
			system.autoscaleAxes();
			
			Range xAutoscaleNewRange = xAxis.getRange();
			Range yAutoscaleNewRange = yAxis.getRange();
			
			assertEquals(xAutoscaleNewRange, xAutoscaleRange);
			assertEquals(yAutoscaleNewRange, yAutoscaleRange);
		}
	}
};
	
