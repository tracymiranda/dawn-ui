/*
 * Copyright (c) 2014 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.plotting.tools.region;

import org.dawnsci.common.widgets.tree.ComboNode;
import org.dawnsci.common.widgets.tree.NumericNode;
import org.dawnsci.common.widgets.tree.ValueEditingSupport;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ICellEditorListener;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class RegionEditorEditingSupport extends ValueEditingSupport {

	private ColumnViewer viewer;
	private int column;

	public RegionEditorEditingSupport(ColumnViewer viewer, int column) {
		super(viewer);
		this.viewer = viewer;
		this.column = column;
	}

	@Override
	protected CellEditor getCellEditor(final Object element) {
		switch (column) {
		case 0:
			if (element instanceof RegionNode)
				return new TextCellEditor((Composite)viewer.getControl());
			break;
		case 1:
			if (element instanceof NumericNode)
				return createNumericEditor(element);
			break;
		case 2:
			if (element instanceof ComboNode)
				return createComboEditor(element);
			break;
		case 3:
			if (element instanceof RegionNode)
				return createBooleanEditor(element);
			break;
		case 4:
			if (element instanceof RegionNode)
				return createBooleanEditor(element);
			break;
		case 5:
			if (element instanceof RegionNode)
				return createBooleanEditor(element);
			break;
		}
		return null;
	}

	protected CellEditor createBooleanEditor(final Object element) {
		final RegionNode node = (RegionNode)element;
		final CheckboxCellEditor fse = new CheckboxCellEditor((Composite)viewer.getControl(), SWT.NONE);
		fse.addListener(new ICellEditorListener() {
			@Override
			public void editorValueChanged(boolean oldValidState, boolean newValidState) {
				setValue(element, newValidState);
			}

			@Override
			public void cancelEditor() {
				// TODO Auto-generated method stub

			}

			@Override
			public void applyEditorValue() {
				switch (column) {
				case 3:
					node.setVisible((Boolean) fse.getValue());
					break;
				case 4:
					node.setActive((Boolean) fse.getValue());
					break;
				case 5:
					node.setMobile((Boolean) fse.getValue());
					break;
				default:
					break;
				}
			}
		});
		return fse;
	}

	@Override
	protected boolean canEdit(Object element) {
		switch (column) {
		case 0:
			if (!(element instanceof RegionNode))
				return false;
			return ((RegionNode)element).isEditable();
		case 1:
			if (!(element instanceof NumericNode<?>))
				return false;
			return ((NumericNode<?>)element).isEditable();
		case 3:
			if (!(element instanceof RegionNode))
				return false;
			return true;
		case 4:
			if (!(element instanceof RegionNode))
				return false;
			return true;
		case 5:
			if (!(element instanceof RegionNode))
				return false;
			return true;
		}
		return false;
	}

	@Override
	protected Object getValue(Object element) {
		switch (column) {
		case 0:
			if (element instanceof RegionNode) {
				RegionNode node = (RegionNode)element;
				return node.getLabel();
			}
			break;
		case 1:
			if (element instanceof NumericNode<?>) {
				NumericNode<?> node = (NumericNode<?>)element;
				return node.getDoubleValue();
			}
			break;
		case 2:
			if (element instanceof ComboNode) {
				ComboNode node = (ComboNode) element;
				return node.getValue();
			}
			break;
		case 3:
			if (element instanceof RegionNode) {
				RegionNode node = (RegionNode)element;
				return node.isVisible();
			}
			break;
		case 4:
			if (element instanceof RegionNode) {
				RegionNode node = (RegionNode)element;
				return node.isActive();
			}
			break;
		case 5:
			if (element instanceof RegionNode) {
				RegionNode node = (RegionNode)element;
				return node.isMobile();
			}
			break;
		}
		return null;
	}

	@Override
	protected void setValue(Object element, Object value) {
		switch (column) {
		case 0:
			if (element instanceof RegionNode) {
				RegionNode node = (RegionNode)element;
				node.setLabel((String) value);
			}
			break;
		case 1:
			if (element instanceof NumericNode<?>) {
				NumericNode<?> node = (NumericNode<?>)element;
				node.setDoubleValue((Double)value);
			} 
			break;
		case 2:
			if (element instanceof ComboNode) {
				ComboNode node = (ComboNode)element;
				node.setValue((Integer) value);
			}
			break;
		case 3:
			if (element instanceof RegionNode) {
				RegionNode node = (RegionNode) element;
				node.setVisible((Boolean) value);
			}
			break;
		case 4:
			if (element instanceof RegionNode) {
				RegionNode node = (RegionNode) element;
				node.setActive((Boolean) value);
			}
			break;
		case 5:
			if (element instanceof RegionNode) {
				RegionNode node = (RegionNode) element;
				node.setMobile((Boolean) value);
			}
			break;
		}
		viewer.refresh(element);
	}
}
