package org.dawnsci.plotting.histogram.ui;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
public class HistogramLockTest extends PluginTestBase {

	private static HistogramToolPage2 histogramToolPage;
	private IDataset imageData;
	private Double calculatedMin;
	private Double calculatedMax;
	private Double resizeMin1;
	private Double resizeMax1;
	private Double resizeMin2;
	private Double resizeMax2;

	@Parameters
	public static Collection<Object[]> data() {
		// input, expected (null for == input)
		List<Object[]> params = new ArrayList<>();

		Dataset input;
		Number calculatedMin;
		Number calculatedMax;
		Number resizeMin1;
		Number resizeMax1;
		Number resizeMin2;
		Number resizeMax2;

		// normal data
		input = AbstractDataset.arange(100, Dataset.FLOAT64);
		input.setShape(10, 10);
		calculatedMin = input.min();
		calculatedMax = input.max();
		resizeMin1 = (Number) (10.0);
		resizeMax1 = (Number) (70.0);
		resizeMin2 = (Number) (-5.0);
		resizeMax2 = (Number) (110.0);
		params.add(new Object[] { input, calculatedMin, calculatedMax,
				resizeMin1, resizeMax1, resizeMin2, resizeMax2, });

		// normal data with some negative values
		input = AbstractDataset.arange(-100, 100, 2, Dataset.FLOAT64);
		input.setShape(10, 10);
		calculatedMin = input.min();
		calculatedMax = input.max();
		resizeMin1 = (Number) (-50.0);
		resizeMax1 = (Number) (60.0);
		resizeMin2 = (Number) (-110.0);
		resizeMax2 = (Number) (120.0);

		params.add(new Object[] { input, calculatedMin, calculatedMax,
				resizeMin1, resizeMax1, resizeMin2, resizeMax2, });

		return params;
	}

	public HistogramLockTest(IDataset imageData, Double calculatedMin,
			Double calculatedMax, Double resizeMin1, Double resizeMax1,
			Double resizeMin2, Double resizeMax2) {
		this.imageData = imageData;
		this.calculatedMin = calculatedMin;
		this.calculatedMax = calculatedMax;
		this.resizeMin1 = resizeMin1;
		this.resizeMax1 = resizeMax1;
		this.resizeMin2 = resizeMin2;
		this.resizeMax2 = resizeMax2;

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
		IDE.openEditor(page, file, "org.dawb.workbench.editors.ImageEditor",
				true);

		IWorkbenchPart activePart = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage().getActivePart();
		final IToolPageSystem sys = (IToolPageSystem) activePart
				.getAdapter(IToolPageSystem.class);

		page.showView("org.dawb.workbench.plotting.views.toolPageView.fixed",
				"org.dawnsci.plotting.histogram.histogram_tool_page_2",
				IWorkbenchPage.VIEW_ACTIVATE);
		IToolPage tool = sys
				.getToolPage("org.dawnsci.plotting.histogram.histogram_tool_page_2");
		histogramToolPage = (HistogramToolPage2) tool;
		assertNotNull(histogramToolPage);

	}

	@Test
	public void lockStateTest() {
		// Allow time for the trace to be created
		readAndDispatch(5);

		HistogramViewer viewer = histogramToolPage.getHistogramViewer();
		IHistogramProvider provider = viewer.getHistogramProvider();
		IAction lockAction = histogramToolPage.getLockAction();

		// Get the original min/max
		double imageFileMin = provider.getMin();
		double imageFileMax = provider.getMax();

		// Set the lock on
		lockAction.setChecked(true);
		lockAction.run();

		readAndDispatch(5);

		IImageTrace imageTrace = histogramToolPage.getImageTrace();
		imageTrace.setData(imageData, null, true);

		readAndDispatch(5);

		double minLockOn = provider.getMin();
		double maxLockOn = provider.getMax();

		assertTrue(minLockOn == imageFileMin);
		assertTrue(maxLockOn == imageFileMax);

		// Set the lock off
		lockAction.setChecked(false);
		lockAction.run();

		// Set the data again, to trigger inputChanged updates
		imageTrace.setData(imageData, null, true);

		// Assert our min/max are as expected
		double minLockOff = provider.getMin();
		double maxLockOff = provider.getMax();

		assertTrue(minLockOff == calculatedMin.doubleValue());
		assertTrue(maxLockOff == calculatedMax.doubleValue());

		// Set the lock on
		lockAction.setChecked(true);
		lockAction.run();

		// Resize the area
		provider.setMin(resizeMin1);
		provider.setMax(resizeMax1);

		readAndDispatch(5);

		// Set the data again, to trigger inputChanged updates
		imageTrace.setData(imageData, null, true);
		readAndDispatch(5);

		double minLockOn1 = provider.getMin();
		double maxLockOn1 = provider.getMax();

		assertTrue(minLockOn1 == resizeMin1);
		assertTrue(maxLockOn1 == resizeMax1);

		// Resize the area again, lock should respect this new min/max
		// for the new trace
		provider.setMin(resizeMin2);
		provider.setMax(resizeMax2);

		readAndDispatch(5);

		// Set the data again, to trigger inputChanged updates
		imageTrace.setData(imageData, null, true);
		readAndDispatch(5);

		double minLockOn2 = provider.getMin();
		double maxLockOn2 = provider.getMax();

		assertTrue(minLockOn2 == resizeMin2);
		assertTrue(maxLockOn2 == resizeMax2);
	}
}
