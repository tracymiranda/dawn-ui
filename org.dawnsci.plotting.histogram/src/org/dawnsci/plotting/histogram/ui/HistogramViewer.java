package org.dawnsci.plotting.histogram.ui;

import org.dawnsci.common.widgets.spinner.FloatSpinner;
import org.dawnsci.plotting.histogram.IHistogramProvider;
import org.dawnsci.plotting.histogram.IHistogramProvider.IHistogramDatasets;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.dawnsci.analysis.dataset.roi.RectangularROI;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.dawnsci.plotting.api.PlotType;
import org.eclipse.dawnsci.plotting.api.PlottingFactory;
import org.eclipse.dawnsci.plotting.api.region.IROIListener;
import org.eclipse.dawnsci.plotting.api.region.IRegion;
import org.eclipse.dawnsci.plotting.api.region.IRegion.RegionType;
import org.eclipse.dawnsci.plotting.api.region.ROIEvent;
import org.eclipse.dawnsci.plotting.api.trace.ILineTrace;
import org.eclipse.dawnsci.plotting.api.trace.ILineTrace.TraceType;
import org.eclipse.dawnsci.plotting.api.trace.IPaletteTrace;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.progress.UIJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A viewer for a histogram (composite with a histogram plot, region of
 * interest, and plotting system with histogram trace lines and RGB trace
 * lines.)
 * <p>
 * Content providers for histogram viewer must implement the
 * <code>IHistogramProvider</code> interface and set it using
 * <code>setContentProvider</code>
 * </p>
 * <p>
 * Input to the histogram viewer must implement the <code>IPaletteTrace</code>
 * interface and get set using <code>setInput</code> method.
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public class HistogramViewer extends ContentViewer {

	private static final Logger logger = LoggerFactory
			.getLogger(HistogramViewer.class);

	private Composite composite;
	private IPlottingSystem histogramPlottingSystem = null;
	private IRegion region;

	private FloatSpinner minText;
	private FloatSpinner maxText;

	private ILineTrace histoTrace;
	private ILineTrace redTrace;
	private ILineTrace greenTrace;
	private ILineTrace blueTrace;

	private IROIListener histogramRegionListener = new IROIListener.Stub() {
		public void update(ROIEvent evt) {
			IROI roi = evt.getROI();
			if (roi instanceof RectangularROI) {
				RectangularROI rroi = (RectangularROI) roi;
				getHistogramProvider().setMin(rroi.getPoint()[0]);
				double max = rroi.getEndPoint()[0];
				System.out.print("New max: " + max);
				getHistogramProvider().setMax(max);
				System.out.println(" - received as:" + getHistogramProvider().getMax());
			}
		};
	};

	private UIJob repaintJob = new UIJob("Repaint traces") {

		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			updateTraces();
			return Status.OK_STATUS;
		}
	};

	/**
	 * Create a new Histogram Widget with a newly created plotting system.
	 *
	 * @param composite
	 *            parent composite to add widget to. Must not be
	 *            <code>null</code>
	 * @throws Exception
	 *             throws an exception if there is a failure to create a default
	 *             plotting system or region of interest
	 */
	public HistogramViewer(final Composite parent) throws Exception {
		this(parent, null, null, null);
	}

	/**
	 * Create a new Histogram Widget
	 *
	 * @param composite
	 *            parent composite to add widget to. Must not be
	 *            <code>null</code>
	 * @param title
	 *            Title string for plot
	 * @param plot
	 *            A plotting system to work with this widget. Can be
	 *            <code>null</code>, in which case a default plotting system
	 *            will be created
	 * @param site
	 *            the workbench site this widget sits in. This is used to set
	 *            commands and handlers. Can be <code>null</code>, in which case
	 *            handlers will not be added and a pop-up menu will not be
	 *            added.
	 * @throws Exception
	 *             throws an exception if there is a failure to create a default
	 *             plotting system or region of interest
	 */
	public HistogramViewer(final Composite parent, String title,
			IPlottingSystem plot, IActionBars site) throws Exception {

		createMinMaxSettings(parent);
		composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new FillLayout());

		if (plot != null) {
			histogramPlottingSystem = plot;
		} else {
			histogramPlottingSystem = PlottingFactory.createPlottingSystem();
		}

		histogramPlottingSystem.createPlotPart(composite, title, site,
				PlotType.XY, null);
		histogramPlottingSystem.setRescale(false);

		createRegion();
		createTraces();
	}

	private void createMinMaxSettings(Composite comp) {
		Composite composite = new Composite(comp, SWT.NONE);
		composite.setLayout(GridLayoutFactory.swtDefaults().numColumns(4)
				.create());
		composite.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, false).create());

		Label minLabel = new Label(composite, SWT.NONE);
		minLabel.setText("Min:");
		minLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false,
				false));
		minText = new FloatSpinner(composite, SWT.BORDER);
		minText.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false,
				false));
		Label maxLabel = new Label(composite, SWT.NONE);
		maxLabel.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));
		maxLabel.setText("Max:");
		maxText = new FloatSpinner(composite, SWT.BORDER);
		maxText.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false,
				false));

		minText.setFormat(12, 4);
		maxText.setFormat(12, 4);
		minText.setIncrement(1.0);
		maxText.setIncrement(1.0);
	}

	/**
	 * Create the region of interest for the histogram
	 */
	private void createRegion() throws Exception {
		region = histogramPlottingSystem.createRegion("Histogram Region",
				RegionType.XAXIS);
		histogramPlottingSystem.addRegion(region);
		region.addROIListener(histogramRegionListener);
	}

	/**
	 * Update Region
	 *
	 * @param histoMax
	 * @param histoMin
	 */
	private void updateRegion(double histoMin, double histoMax) {
		RectangularROI rroi = new RectangularROI(histoMin, 0, histoMax
				- histoMin, 1, 0);

		region.removeROIListener(histogramRegionListener);
		try {
			double max = rroi.getEndPoint()[0];
			System.out.println();
			System.out.print("updateRegion histoMax: " + histoMax);
			System.out.print(" getEndPoint: " + rroi.getEndPoint()[0]);
			region.setROI(rroi);
			System.out.print(" AFTER histoMax: " + histoMax);
			System.out.print(" getEndPoint: " + rroi.getEndPoint()[0]);
			RectangularROI afterroi = (RectangularROI)region.getROI();
			
			System.out.println(" REGION getEndPoint: " + afterroi.getEndPoint()[0]);
		} finally {
			region.addROIListener(histogramRegionListener);
		}
	}

	/**
	 * Update the min widget
	 * 
	 * @param min
	 */
	private void updateMin(double min) {
		minText.setMinimum(min);
	}

	/**
	 * Update the min widget
	 * 
	 * @param min
	 */
	private void updateMax(double max) {
		maxText.setMaximum(max);
	}

	/**
	 * Create traces
	 */
	private void createTraces() {
		// Set up the histogram trace
		histoTrace = histogramPlottingSystem.createLineTrace("Histogram");
		histoTrace.setTraceType(TraceType.AREA);
		histoTrace.setLineWidth(1);
		histoTrace.setTraceColor(new Color(null, 0, 0, 0));

		// Set up the RGB traces
		redTrace = histogramPlottingSystem.createLineTrace("Red");
		greenTrace = histogramPlottingSystem.createLineTrace("Green");
		blueTrace = histogramPlottingSystem.createLineTrace("Blue");

		redTrace.setLineWidth(2);
		greenTrace.setLineWidth(2);
		blueTrace.setLineWidth(2);

		redTrace.setTraceColor(new Color(null, 255, 0, 0));
		greenTrace.setTraceColor(new Color(null, 0, 255, 0));
		blueTrace.setTraceColor(new Color(null, 0, 0, 255));

		histogramPlottingSystem.addTrace(histoTrace);
		histogramPlottingSystem.addTrace(redTrace);
		histogramPlottingSystem.addTrace(greenTrace);
		histogramPlottingSystem.addTrace(blueTrace);

		histogramPlottingSystem.getSelectedXAxis().setTitle("Intensity");
		histogramPlottingSystem.getSelectedYAxis().setTitle("Log(Frequency)");
	}

	/**
	 * Update RGB traces
	 */
	private void updateTraces() {
		IHistogramDatasets data = getHistogramProvider().getDatasets();
		histoTrace.setData(data.getX(), data.getY());
		redTrace.setData(data.getRGBX(), data.getR());
		greenTrace.setData(data.getRGBX(), data.getG());
		blueTrace.setData(data.getRGBX(), data.getB());
		blueTrace.repaint();
		// if (rescale && updateAxis) {
		// histogramPlottingSystem.getSelectedXAxis().setRange(
		// histogramProvider.getMin(), histogramProvider.getMax());
		// }
		histogramPlottingSystem.getSelectedXAxis().setLog10(false);
		histogramPlottingSystem.getSelectedXAxis().setAxisAutoscaleTight(true);
		// histogramPlottingSystem.getSelectedXAxis().setLog10(btnColourMapLog.getSelection());

		histogramPlottingSystem.getSelectedYAxis().setAxisAutoscaleTight(true);

		// do if rescale is set....
		// TODO: under what conditions may we always autoscale??? //test for
		// isRescaleHistogram
		histogramPlottingSystem.autoscaleAxes();
		// histogramPlottingSystem.repaint();
	}

	/**
	 * Updates the region and traces
	 */
	@Override
	protected void inputChanged(Object input, Object oldInput) {
		logger.debug("HistogramViewer: inputchanged");

		if (input == null) {
			logger.debug("HistogramViewer: inputchanged input null");
		} else {
			logger.debug("HistogramViewer: inputchanged input " + input);
		}

		if (oldInput == null) {
			logger.debug("HistogramViewer: inputchanged oldInput null");
		} else {
			logger.debug("HistogramViewer: inputchanged oldInput " + oldInput);
		}
		
		System.out.println("Input Changed: " + getHistogramProvider().getMax());

		refresh();
	};

	/**
	 * Set the input for this viewer, must instantiate IPaletteTrace
	 */
	@Override
	public void setInput(Object input) {
		Assert.isTrue(input instanceof IPaletteTrace);
		super.setInput(input);
	}

	/**
	 * Set the content provider for this viewer, must instantiate
	 * IHistogramProvider
	 */
	@Override
	public void setContentProvider(IContentProvider contentProvider) {
		Assert.isTrue(contentProvider instanceof IHistogramProvider);
		super.setContentProvider(contentProvider);
	}

	@Override
	public void refresh() {
		System.out.print("refresh max:" + getHistogramProvider().getMax());
		updateRegion(getHistogramProvider().getMin(), getHistogramProvider()
				.getMax());
		updateMin(getHistogramProvider().getMin());
		updateMax(getHistogramProvider().getMax());
		updateTraces();
		System.out.println(" after:" + getHistogramProvider().getMax());
	}

	/**
	 * Resets the histogram to its original conditions
	 */
	public void reset() {
		// Things to reset
		// 1. histo min
		// 2. histo max
		// 3. ROI
		// 4. scaling
		// 5. ????

		// Things to not reset
		// 1. colour map
		// 2. ???

		// do we need a restore defaults???
	}

	/**
	 * Clean up viewer
	 */
	public void dispose() {
		region.removeROIListener(histogramRegionListener);
		histogramPlottingSystem.dispose();
	}

	/**
	 * Returns the histogram provider used by this widget, or <code>null</code>
	 * if no provider has been set yet.
	 *
	 * @return the histogram provider or <code>null</code> if none
	 */
	@Override
	public IHistogramProvider getContentProvider() {
		return (IHistogramProvider) super.getContentProvider();
	}

	/**
	 * Returns the histogramProvider for this class
	 *
	 * @return the histogram provider or <code>null</code> if none
	 */
	public IHistogramProvider getHistogramProvider() {
		return (IHistogramProvider) super.getContentProvider();
	}

	/**
	 * Get the plotting system associated with this histogram plot
	 *
	 * @return IPlottingSystem the plotting system associated with this
	 *         histogram
	 */
	public IPlottingSystem getHistogramPlot() {
		return histogramPlottingSystem;
	}

	@Override
	public Control getControl() {
		return composite;
	}

	/**
	 * Returns the parent composite
	 * 
	 * @return composite
	 */
	public Composite getComposite() {
		return composite;
	}

	@Override
	public ISelection getSelection() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setSelection(ISelection selection, boolean reveal) {
		// TODO Auto-generated method stub
	}

	/**
	 * For test purposes only
	 */
	protected ILineTrace[] getRGBTraces() {
		return new ILineTrace[] { redTrace, greenTrace, blueTrace };
	}
}
