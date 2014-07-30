package org.dawnsci.common.widgets.table;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.dawnsci.common.richbeans.internal.GridUtils;
import org.dawnsci.common.widgets.Activator;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.bindings.keys.IKeyLookup;
import org.eclipse.jface.bindings.keys.KeyLookupFactory;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TableViewerFocusCellManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;

/**
 * This class is a table of a series of operations.
 * 
 * The widget provides and add button which when pressed can add another
 * item into the table. You may extend this table to provide for instance
 * a table which edits series of mathematical functions.
 * 
 * @author fcp94556
 *
 */
public class SeriesTable {

	private TableViewer          tableViewer;
	private SeriesEditingSupport editingSupport;
    private ISeriesValidator     validator;
	private CLabel errorLabel;
	/**
	 * Create the control for the table. The icon provider is checked for label and
	 * icon for the first, name column in the table. It must provide at least an
	 * icon for a given SeriesItem implementation. If it does not provide a label 
	 * then the 'getName()' method of SeriesItem is used.
	 * 
	 * @param parent       - the SWT composite to add the table to.
	 * @param iconProvider - a provider which must at least give the icon for a given SeriesItem
	 */
	public void createControl(Composite parent, SeriesItemLabelProvider iconProvider) {
		
		tableViewer = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE) {
		    protected void inputChanged(Object input, Object oldInput) {
		    	super.inputChanged(input, oldInput);
		    	checkValid((List<ISeriesItemDescriptor>)input);
		    }
		};

		tableViewer.getTable().setLinesVisible(true);
		tableViewer.getTable().setHeaderVisible(true);
		tableViewer.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));

		TableViewerFocusCellManager focusCellManager = new TableViewerFocusCellManager(tableViewer, new FocusCellOwnerDrawHighlighter(tableViewer));
		ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(tableViewer) {
			@Override
			protected boolean isEditorActivationEvent(
					ColumnViewerEditorActivationEvent event) {
				// TODO see AbstractComboBoxCellEditor for how list is made visible
				return super.isEditorActivationEvent(event)
						|| (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && (event.keyCode == KeyLookupFactory
								.getDefault().formalKeyLookup(
										IKeyLookup.ENTER_NAME)));
			}
		};

		TableViewerEditor.create(tableViewer, focusCellManager, actSupport,
				ColumnViewerEditor.TABBING_HORIZONTAL
						| ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR
						| ColumnViewerEditor.TABBING_VERTICAL
						| ColumnViewerEditor.KEYBOARD_ACTIVATION);


		tableViewer.setContentProvider(new SeriesContentProvider());
		
		this.errorLabel = new CLabel(parent, SWT.WRAP);
		errorLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		errorLabel.setImage(Activator.getImageDescriptor("icons/error.png").createImage());
		GridUtils.setVisible(errorLabel,         false);

		createColumns(iconProvider);
		
	}
	
	
	protected void checkValid(List<ISeriesItemDescriptor> series) {
		
		if (validator==null) return;
		final String errorMessage = validator.getErrorMessage(series);
		GridUtils.setVisible(errorLabel,         errorMessage!=null);
		errorLabel.setText(errorMessage!=null ? errorMessage : "");
	}

	/**
	 * This method is called to create the columns of the table.
	 * The columns required may be overridden depending on the 
	 * objects defined by setInput(...) to provide additional 
	 * columns which are optionally editable.
	 * 
	 */
	protected void createColumns(SeriesItemLabelProvider iconProv) {
		createNameColumn("Name", iconProv);
	}

	/**
	 * May be null or empty array. Must be called to set content provider on table.
	 * Should be called after createControl(...)
	 * 
	 * @param input
	 */
	public void setInput(Collection<? extends ISeriesItemDescriptor> currentItems, ISeriesItemFilter content) {
		
		editingSupport.setSeriesItemDescriptorProvider(content);
		if (currentItems==null) currentItems = Collections.emptyList();
		tableViewer.setInput(currentItems);
	}
	
	/**
	 * Call to remove all items from the list.
	 */
	public void clear() {
		tableViewer.setInput(Collections.emptyList());
	}

	/**
	 * 
	 * @param name
	 * @param prov
	 */
	protected void createNameColumn(final String name, final SeriesItemLabelProvider delegate) {
		
		TableViewerColumn nameColumn = new TableViewerColumn(tableViewer, SWT.NONE);
		nameColumn.getColumn().setWidth(300);
		nameColumn.getColumn().setMoveable(true);
		nameColumn.getColumn().setText(name);
		
		nameColumn.setLabelProvider(delegate);

		this.editingSupport = new SeriesEditingSupport(tableViewer, new SeriesLabelProvider(delegate));
		nameColumn.setEditingSupport(editingSupport);

	}

	
	public void dispose() {
		tableViewer.getControl().dispose();
	}

	public void setFocus() {
		tableViewer.getControl().setFocus();
	}

	public void registerSelectionProvider(IViewSite viewSite) {
		viewSite.setSelectionProvider(tableViewer);
	}

	public void setLockEditing(boolean checked) {
		SeriesContentProvider prov = (SeriesContentProvider)tableViewer.getContentProvider();
		prov.setLockEditing(checked);
		tableViewer.refresh();
	}

	public Collection<ISeriesItemDescriptor> getSeriesItems() {
		SeriesContentProvider prov = (SeriesContentProvider)tableViewer.getContentProvider();
		return prov.getSeriesItems();
	}

	/**
	 * Add a new operation to the list.
	 */
	public void addNew() {
		tableViewer.cancelEditing();
		tableViewer.editElement(ISeriesItemDescriptor.NEW, 0);
	}
	
    /**
     * Remove the selected item, if any.
     * @return true if something is selected and we deleted it.
     */
	public boolean delete() {
		
		final IStructuredSelection  sel      = (IStructuredSelection)tableViewer.getSelection();
		final ISeriesItemDescriptor selected = (ISeriesItemDescriptor)sel.getFirstElement();
		if (selected==null) return false;
		if (selected==ISeriesItemDescriptor.NEW) return false;
		
		SeriesContentProvider prov = (SeriesContentProvider)tableViewer.getContentProvider();
        return prov.delete(selected);
	}


	public TableViewerColumn createColumn(String name, int mod, int width, final CellLabelProvider prov) {
		
		final TableViewerColumn col = new TableViewerColumn(tableViewer, SWT.LEFT);
		col.getColumn().setWidth(width);
		col.getColumn().setMoveable(true);
		col.getColumn().setText(name);
		
		col.setLabelProvider(prov);
		return col;
	}

	public void setMenuManager(MenuManager rightClick) {
		tableViewer.getControl().setMenu(rightClick.createContextMenu(tableViewer.getControl()));
	}


	public ISeriesValidator getValidator() {
		return validator;
	}


	public void setValidator(ISeriesValidator validator) {
		this.validator = validator;
	}
}