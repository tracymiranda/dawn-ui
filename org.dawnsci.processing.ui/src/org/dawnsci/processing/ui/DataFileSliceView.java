package org.dawnsci.processing.ui;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.dawb.common.services.ServiceManager;
import org.dawb.common.services.conversion.IConversionContext;
import org.dawb.common.services.conversion.IConversionContext.ConversionScheme;
import org.dawb.common.services.conversion.IConversionService;
import org.dawb.common.services.conversion.IProcessingConversionInfo;
import org.dawb.common.ui.monitor.ProgressMonitorWrapper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.dawnsci.plotting.api.PlottingFactory;
import org.eclipse.dawnsci.plotting.api.trace.IImageTrace;
import org.eclipse.dawnsci.plotting.api.trace.ITrace;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.ui.part.ViewPart;

import uk.ac.diamond.scisoft.analysis.SDAPlotter;
import uk.ac.diamond.scisoft.analysis.dataset.IDataset;
import uk.ac.diamond.scisoft.analysis.dataset.ILazyDataset;
import uk.ac.diamond.scisoft.analysis.dataset.Slice;
import uk.ac.diamond.scisoft.analysis.io.IDataHolder;
import uk.ac.diamond.scisoft.analysis.io.LoaderFactory;
import uk.ac.diamond.scisoft.analysis.metadata.AxesMetadata;
import uk.ac.diamond.scisoft.analysis.metadata.MaskMetadata;
import uk.ac.diamond.scisoft.analysis.metadata.OriginMetadataImpl;
import uk.ac.diamond.scisoft.analysis.monitor.IMonitor;
import uk.ac.diamond.scisoft.analysis.processing.IExecutionVisitor;
import uk.ac.diamond.scisoft.analysis.processing.IOperation;
import uk.ac.diamond.scisoft.analysis.processing.OperationData;
import uk.ac.diamond.scisoft.analysis.processing.RichDataset;
import uk.ac.diamond.scisoft.analysis.processing.visitors.HierarchicalFileExecutionVisitor;
import uk.ac.diamond.scisoft.analysis.slice.SliceVisitor;
import uk.ac.diamond.scisoft.analysis.slice.Slicer;

import org.dawnsci.conversion.ConversionContext;;

public class DataFileSliceView extends ViewPart {

	List<String> filePaths = new ArrayList<String>();
	TableViewer viewer;
	IConversionService service;
	IConversionContext context;
	ImageProcessConvertPage convertPage;
	
	@Override
	public void createPartControl(Composite parent) {
		
		try {
			service = (IConversionService)ServiceManager.getService(IConversionService.class);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		parent.setLayout(new GridLayout());
		
		viewer = new TableViewer(parent);
		viewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		viewer.setLabelProvider(new LabelProvider());
		viewer.setContentProvider(new BasicContentProvider());
		viewer.setInput(filePaths.toArray(new String[filePaths.size()]));
		
		DropTargetAdapter dropListener = new DropTargetAdapter() {
			@Override
			public void drop(DropTargetEvent event) {
				Object dropData = event.data;
				if (dropData instanceof TreeSelection) {
					TreeSelection selectedNode = (TreeSelection) dropData;
					Object obj[] = selectedNode.toArray();
					for (int i = 0; i < obj.length; i++) {
						if (obj[i] instanceof IFile) {
							IFile file = (IFile) obj[i];
							addFile(file.getLocation().toOSString());
						}
					}
				} else if (dropData instanceof String[]) {
					for (String path : (String[])dropData){
						addFile(path);
					}
				}
				
				viewer.setInput(filePaths.toArray(new String[filePaths.size()]));
			}
		};
		
		DropTarget dt = new DropTarget(viewer.getControl(), DND.DROP_MOVE | DND.DROP_DEFAULT | DND.DROP_COPY);
		dt.setTransfer(new Transfer[] { TextTransfer.getInstance(),
				FileTransfer.getInstance(), ResourceTransfer.getInstance(),
				LocalSelectionTransfer.getTransfer() });
		dt.addDropListener(dropListener);
		
		Button clear = new Button(parent, SWT.NONE);
		clear.setText("Clear");
		clear.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				filePaths.clear();
				context = null;
				try {
					SDAPlotter.clearPlot("Output");
					SDAPlotter.clearPlot("Input");
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				viewer.setInput(filePaths.toArray(new String[filePaths.size()]));
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
		
		Button run = new Button(parent, SWT.NONE);
		run.setText("Run");
		run.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				IOperation[] ops = getOperations();
				
				if (ops != null) {
					
					final IOperation[] fop = ops;
					
					context.setUserObject(new IProcessingConversionInfo() {

						@Override
						public IOperation[] getOperationSeries() {
							return fop;
						}

						@Override
						public IExecutionVisitor getExecutionVisitor(String fileName) {
							return new HierarchicalFileExecutionVisitor(fileName);
						}

					});
				}
					
				ProgressMonitorDialog dia = new ProgressMonitorDialog(Display.getCurrent().getActiveShell());

				try {
					dia.run(true, false, new IRunnableWithProgress() {

						@Override
						public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
							context.setMonitor(new ProgressMonitorWrapper(monitor));
							try {
								service.process(context);
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

						}
					});
				} catch (InvocationTargetException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
		
		getSite().getPage().addSelectionListener("org.dawnsci.processing.ui.processingView",new ISelectionListener() {
			
			@Override
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {
				selection.toString();
				
				if (selection instanceof StructuredSelection && ((StructuredSelection)selection).getFirstElement() instanceof OperationDescriptor) {
					OperationDescriptor des = (OperationDescriptor)((StructuredSelection)selection).getFirstElement();
					try {
						update(des.getSeriesObject());
					} catch (InstantiationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				//update(null);
				
			}
		});

	}
	
	private void addFile(String filePath) {
		
		if (context == null) {
			context = service.open(filePath);
			
			Wizard wiz = new Wizard() {
				//set 
				@Override
				public boolean performFinish() {
					return true;
				}
			};
			
			wiz.setNeedsProgressMonitor(true);
			convertPage = null;
			convertPage = new ImageProcessConvertPage();
			wiz.addPage(convertPage);
			final WizardDialog wd = new WizardDialog(getSite().getShell(),wiz);
			wd.create();
			convertPage.setContext(context);

			if (wd.open() == WizardDialog.OK) {
				context = convertPage.getContext();
				context.setConversionScheme(ConversionScheme.PROCESS);
				
				try {
					
					update(null);
					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				filePaths.add(filePath);
			}
		} else {
			String dsName = context.getDatasetNames().get(0);
			try {
				IDataHolder holder = LoaderFactory.getData(filePath, null);
				ILazyDataset lazyDataset = holder.getLazyDataset(dsName);
				if (lazyDataset != null) {
					filePaths.add(filePath);
					if (context instanceof ConversionContext) {
						((ConversionContext)context).setFilePaths(filePaths.toArray(new String[filePaths.size()]));
					}
					
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		
	}
	
	private void update(final IOperation end) {
	
			if (context == null) return;
			
			Job update = new Job("update") {
				
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					
					try {
						
					
					final IDataHolder   dh = LoaderFactory.getData(context.getFilePaths().get(0));
					ILazyDataset lazyDataset = dh.getLazyDataset(context.getDatasetNames().get(0));
					
					final IDataset firstSlice = Slicer.getFirstSlice(new RichDataset(lazyDataset, null), context.getSliceDimensions());
					SDAPlotter.imagePlot("Input", firstSlice);
					
					EscapableSliceVisitor sliceVisitor = getSliceVisitor(getOperations(), getOutputExecutionVisitor(), lazyDataset, Slicer.getDataDimensions(lazyDataset.getShape(), context.getSliceDimensions()));
					sliceVisitor.setEndOperation(end);
					sliceVisitor.visit(firstSlice, null, null);
					} catch (Exception e) {
						e.printStackTrace();
						return Status.CANCEL_STATUS;
					}
					
					return Status.OK_STATUS;
				}
			};
			
			update.schedule();
		
	}
	
	private IOperation[] getOperations() {
		IViewPart view = getSite().getPage().findView("org.dawnsci.processing.ui.processingView");
		
		Object ob = view.getAdapter(IOperation.class);
		IOperation[] ops = null;
		
		if (ob == null) return null;
		
		if (ob.getClass().isArray() && Array.get(ob, 0) instanceof IOperation) {
			ops = (IOperation[])ob;
		}
		
		return ops;
	}
	
	private EscapableSliceVisitor getSliceVisitor(final IOperation[] series, final IExecutionVisitor visitor, final ILazyDataset lz, final int[] dataDims) {
		
		return new EscapableSliceVisitor(lz, visitor, dataDims, series);
		
	}
	
	private IExecutionVisitor getOutputExecutionVisitor() {
		return new IExecutionVisitor() {
			
			@Override
			public void passDataThroughUnmodified(IOperation... operations) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void notify(IOperation intermediateData, OperationData data,
					Slice[] slices, int[] shape, int[] dataDims) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public boolean isRequiredToModifyData(IOperation operation) {
				// TODO Auto-generated method stub
				return true;
			}
			
			@Override
			public void init() throws Exception {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void executed(OperationData result, IMonitor monitor,
					Slice[] slices, int[] shape, int[] dataDims) throws Exception {
				IDataset out = result.getData();
				
				out = out.squeeze();
				SDAPlotter.clearPlot("Output");
				if (out.getRank() == 2)  {
					
					List<AxesMetadata> axList = result.getData().getMetadata(AxesMetadata.class);
					
					if (axList == null || axList.isEmpty()) {
						SDAPlotter.imagePlot("Output", out);
					} else {
						ILazyDataset[] axes = axList.get(0).getAxes();
						ILazyDataset lz0 = axes[dataDims[0]];
						ILazyDataset lz1 = axes[dataDims[1]];
						IDataset ax0 = null;
						IDataset ax1 = null;
						if (lz0 != null) ax0 = lz0.getSlice().squeeze();
						if (lz1 != null) ax1 = lz1.getSlice().squeeze();
						
						SDAPlotter.imagePlot("Output", ax0,ax1,out);
					}
					
					List<MaskMetadata> mList = result.getData().getMetadata(MaskMetadata.class);
					
					if (mList == null || mList.isEmpty()) return;
					
					MaskMetadata m = mList.get(0);
					ILazyDataset mask = m.getMask();
					
					final IDataset md = mask.getSlice().squeeze();
					
					if (!Arrays.equals(md.getShape(), out.getShape())) return;
					
					Display.getDefault().syncExec(new Runnable() {
						
						@Override
						public void run() {
							IPlottingSystem system = PlottingFactory.getPlottingSystem("Output");
							if (system == null) return;
							Collection<ITrace> traces = system.getTraces(IImageTrace.class);
							if (traces == null || traces.isEmpty()) return;
							IImageTrace t = (IImageTrace)traces.iterator().next();
							t.setMask(md);
							system.repaint();							
						}
					});
					
				}
				
				if (out.getRank() == 1) {
					List<AxesMetadata> mList = result.getData().getMetadata(AxesMetadata.class);
					if (mList == null || mList.isEmpty()) {
						SDAPlotter.plot("Output", out);
					}
					
					ILazyDataset[] axes = mList.get(0).getAxes();
					ILazyDataset lz = axes[dataDims[0]];
					IDataset ax = null;
					if (lz != null) ax = lz.getSlice().squeeze();
					
					if (ax != null && Arrays.equals(ax.getShape(), out.getShape())) {
						SDAPlotter.plot("Output", ax, out);
					}
				}
				
			}
			
			@Override
			public void close() throws Exception {
				// TODO Auto-generated method stub
				
			}
		};
	}

	@Override
	public void setFocus() {
		if (viewer != null) viewer.getTable().setFocus();

	}
	
	private class EscapableSliceVisitor implements SliceVisitor {

		
		private ILazyDataset lz;
		private IExecutionVisitor visitor;
		private int[] dataDims;
		private IOperation[] series;
		private IOperation endOperation;
		
		public EscapableSliceVisitor(ILazyDataset lz, IExecutionVisitor visitor, int[] dataDims, IOperation[] series) {
			this.lz = lz;
			this.visitor = visitor;
			this.dataDims = dataDims;
			this.series = series;
		}
		
		public void setEndOperation(IOperation op) {
			endOperation = op;
		}
		
		@Override
		public void visit(IDataset slice, Slice[] slices,
				int[] shape) throws Exception {
			
			slice.addMetadata(new OriginMetadataImpl(lz, slices, dataDims));
			
			OperationData  data = new OperationData(slice, (Serializable[])null);
								
			for (IOperation i : series) {
				OperationData tmp = i.execute(data.getData(), null);
				data = visitor.isRequiredToModifyData(i) ? tmp : data;
				visitor.notify(i, data, slices, shape, dataDims); // Optionally send intermediate result
				if (i == endOperation) break;
			}
			
			visitor.executed(data, null, slices, shape, dataDims); // Send result.
		}
		
		
	}
	
	private class BasicContentProvider implements IStructuredContentProvider {

		@Override
		public Object[] getElements(Object inputElement) {
			return (String[]) inputElement;
		}

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}

}