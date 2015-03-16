package org.dawnsci.plotting.histogram.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.dawnsci.plotting.histogram.IHistogramProvider;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.dawnsci.analysis.api.dataset.IDataset;
import org.eclipse.dawnsci.analysis.dataset.impl.AbstractDataset;
import org.eclipse.dawnsci.analysis.dataset.impl.Dataset;
import org.eclipse.dawnsci.plotting.api.tool.IToolPage;
import org.eclipse.dawnsci.plotting.api.tool.IToolPageSystem;
import org.eclipse.dawnsci.plotting.api.trace.IImageTrace;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.intro.IIntroPart;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class HistogramLockTest extends PluginTestBase{
	
	private static HistogramToolPage2 histogramToolPage;
	private IDataset imageData;
	private Double expectedMin;
	private Double expectedMax;


	@Parameters
	public static Collection<Object[]> data() {
		// input, expected (null for == input)
		List<Object[]> params = new ArrayList<>();

		Dataset input;
		Double expectedMin;
		Double expectedMax;

		// normal data
		input = AbstractDataset.arange(100, Dataset.FLOAT64);
		input.setShape(10, 10);
		expectedMin = new Double(0.0);
		expectedMax = new Double(99.0);
		params.add(new Object[] {input, expectedMin, expectedMax});

		// normal data with some negative values
		input = AbstractDataset.arange(-100, 100, 2, Dataset.FLOAT64);
		input.setShape(10, 10);
		expectedMin = new Double(-100.0);
		expectedMax = new Double(98.0);
		params.add(new Object[] {input, expectedMin, expectedMax});

		return params;
	}


	public HistogramLockTest(IDataset imageData,
			Double expectedMin, Double expectedMax) {
		this.imageData = imageData;
		this.expectedMin = expectedMin;
		this.expectedMax = expectedMax;
		
	}
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		IIntroPart part = PlatformUI.getWorkbench().getIntroManager()
				.getIntro();
		PlatformUI.getWorkbench().getIntroManager().closeIntro(part);

		IWorkbenchPage page = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage();
	
		IProject project = ResourcesPlugin.getWorkspace().getRoot()
				.getProject("data");
		IFile file = project.getFile("examples/pilatus300k.edf");
		IDE.openEditor(page, file,
				"org.dawb.workbench.editors.ImageEditor", true);

		IWorkbenchPart activePart = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage().getActivePart();
		final IToolPageSystem sys = (IToolPageSystem) activePart
				.getAdapter(IToolPageSystem.class);

		page.showView(
				"org.dawb.workbench.plotting.views.toolPageView.fixed",
				"org.dawnsci.plotting.histogram.histogram_tool_page_2",
				IWorkbenchPage.VIEW_ACTIVATE);
		IToolPage tool = sys
				.getToolPage("org.dawnsci.plotting.histogram.histogram_tool_page_2");
		histogramToolPage = (HistogramToolPage2) tool;
		assertNotNull(histogramToolPage);
		
	}
	
	@Test
	public void lockStateTest()
	{
		// Allow time for the trace to be created
		readAndDispatch(5);
		
		HistogramViewer viewer = histogramToolPage.getHistogramViewer();
		IHistogramProvider provider = viewer.getHistogramProvider();
		IAction lockAction = histogramToolPage.getLockAction();
		
		// Get the original min/max
		double originalMin = provider.getMin();
		double originalMax = provider.getMax();
		
		// Set the lock o
		lockAction.setChecked(true);
		lockAction.run();
		
		readAndDispatch(5);
		
		IImageTrace imageTrace = histogramToolPage.getImageTrace();
		imageTrace.setData(imageData, null, true);
		
		readAndDispatch(5);
		
		double minLockOn = provider.getMin();
		double maxLockOn = provider.getMax();
		
		assertTrue(originalMin == minLockOn);
		assertTrue(originalMax == maxLockOn);
		
		// Set the lock off
		lockAction.setChecked(false);
		lockAction.run();
		
		// Set the data again, to trigger inputChangedUpdates
		imageTrace.setData(imageData, null, true);
		
		// Assert our min/max are as expected
		double minLockOff = provider.getMin();
		double maxLockOff = provider.getMax();
		
		assertTrue(minLockOff == expectedMin.doubleValue());
		assertTrue(maxLockOff == expectedMax.doubleValue());
		
		// Set the lock on
		lockAction.setChecked(true);
		lockAction.run();
		
		// Resize the area
		/*int userMin = 10;
		int userMax = 70;
		viewer.setUserMin(userMin);
		viewer.setUserMax(userMax);
		
		//Set the data again, to trigger inputChangedUpdates
		imageTrace.setData(imageData, null, true);
		lockAction.run();
		
		double minLockOn2 = provider.getMin();
		double maxLockOn2 = provider.getMax();
		
		assertTrue(minLockOn2 == userMin);
		assertTrue(maxLockOn2 == userMax);*/
	}
}
