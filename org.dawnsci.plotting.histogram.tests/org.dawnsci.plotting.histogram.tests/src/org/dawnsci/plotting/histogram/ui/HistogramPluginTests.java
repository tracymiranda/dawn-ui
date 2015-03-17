package org.dawnsci.plotting.histogram.ui;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.dawnsci.analysis.api.dataset.IDataset;
import org.eclipse.dawnsci.plotting.api.histogram.HistogramBound;
import org.eclipse.dawnsci.plotting.api.histogram.IPaletteService;
import org.eclipse.dawnsci.plotting.api.tool.IToolPage;
import org.eclipse.dawnsci.plotting.api.tool.IToolPageSystem;
import org.eclipse.dawnsci.plotting.api.trace.ILineTrace;
import org.eclipse.dawnsci.plotting.api.trace.IPaletteTrace;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.intro.IIntroPart;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class HistogramPluginTests extends PluginTestBase {

	private static HistogramToolPage2 histogramToolPage;

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
	public void testPaletteUpdatesFromLockAction() {
		// Allow time for the trace to be created
		readAndDispatch(5);
		IPaletteTrace trace = histogramToolPage.getPaletteTrace();

		boolean lockActionState = true;
		histogramToolPage.getLockAction().setChecked(lockActionState);
		histogramToolPage.getLockAction().run();
		boolean palleteLockState = !trace.isRescaleHistogram();
		assertEquals(palleteLockState, lockActionState);

		lockActionState = false;
		histogramToolPage.getLockAction().setChecked(lockActionState);
		histogramToolPage.getLockAction().run();
		palleteLockState = !trace.isRescaleHistogram();
		assertEquals(palleteLockState, lockActionState);
	}

	@Test
	public void testLockActionUpdatesFromPalette() {
		// Allow time for the trace to be created
		readAndDispatch(5);
		IPaletteTrace trace = histogramToolPage.getPaletteTrace();

		boolean palleteLockState = true;
		trace.setRescaleHistogram(!palleteLockState);
		boolean lockActionState = histogramToolPage.getLockAction().isChecked();
		assertEquals(palleteLockState, lockActionState);

		palleteLockState = false;
		trace.setRescaleHistogram(!palleteLockState);
		lockActionState = histogramToolPage.getLockAction().isChecked();
		assertEquals(palleteLockState, lockActionState);
	}
	
	@Test
	public void testTraceUpdatesMinMaxCutFromPalette(){
		// Allow time for the trace to be created
		readAndDispatch(5);
		
		IPaletteTrace trace = histogramToolPage.getPaletteTrace();
		assertNotNull(trace);
		
		// Set the mincut/maxcut to -/+ infinity initially
		double rangeMinInitial = Double.NEGATIVE_INFINITY;
		double rangeMaxInitial = Double.POSITIVE_INFINITY;
		
		trace.setMinCut(new HistogramBound(rangeMinInitial, 
				trace.getMinCut().getColor()));
		trace.setMaxCut(new HistogramBound(rangeMaxInitial, 
				trace.getMaxCut().getColor()));

		IDataset xBefore = histogramToolPage.getHistogramViewer()
				.getHistogramProvider().getDatasets().getX();
		
		// Get min and max, ignoring NaNs and infinities
		double minBefore = (xBefore.min(true, true)).doubleValue();
		double maxBefore = (xBefore.max(true, true)).doubleValue();
		
		// Use these to adjust minCut and maxCut; move the bounds in by 25% each
		double oldRange = maxBefore - minBefore;
		double rangeMin = minBefore + 0.25 * oldRange;
		double rangeMax = maxBefore - 0.25 * oldRange;
		
		// Set the min/maxCut on the palette
		trace.setMinCut(new HistogramBound(rangeMin, 
				trace.getMinCut().getColor()));
		trace.setMaxCut(new HistogramBound(rangeMax, 
				trace.getMaxCut().getColor()));
		
		// Allow time for the listeners to be fired
		readAndDispatch(5);
		
		IDataset xAfter = histogramToolPage.getHistogramViewer()
				.getHistogramProvider().getDatasets().getX();
		
		// Get min and max, ignoring NaNs and infinities
		double minAfter = (xAfter.min(true, true)).doubleValue();
		double maxAfter = (xAfter.max(true, true)).doubleValue();
		
		// Check our min/max has been clipped accordingly
		assertTrue(minAfter >= rangeMin);
		assertTrue(maxAfter <= rangeMax);
		
		// Now expand the bounds out, check again
		
		double rangeMinExpandOut = minBefore - 0.25 * oldRange;
		double rangeMaxExpandOut = maxBefore + 0.25 * oldRange;
		
		// Set the min/maxCut on the palette
		trace.setMinCut(new HistogramBound(rangeMinExpandOut, 
				trace.getMinCut().getColor()));
		trace.setMaxCut(new HistogramBound(rangeMaxExpandOut,
				trace.getMaxCut().getColor()));
		
		// Allow time for the listeners to be fired
		readAndDispatch(5);
		
		IDataset xAfterExpandOut = histogramToolPage.getHistogramViewer()
				.getHistogramProvider().getDatasets().getX();
		
		// Get min and max, ignoring NaNs and infinities
		double minAfterExpandOut = (xAfterExpandOut.min(true, true)).doubleValue();
		double maxAfterExpandOut = (xAfterExpandOut.max(true, true)).doubleValue();
		
		assertTrue(minAfterExpandOut >= rangeMinExpandOut);
		assertTrue(maxAfterExpandOut <= rangeMaxExpandOut);
		
	}
	

}
