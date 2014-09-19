/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.processing.ui;

import org.dawnsci.common.widgets.table.SeriesItemLabelProvider;
import org.eclipse.swt.graphics.Image;

final class OperationLabelProvider extends SeriesItemLabelProvider  {

	@Override
	public String getText(Object element) {
		
		if(!(element instanceof OperationDescriptor)) return super.getText(element);
		
		OperationDescriptor des = (OperationDescriptor)element;
		
		// Other columns
		if (column>0) {
			try {
				switch (column) {
				case 1:
					return des.getSeriesObject().getInputRank().getLabel();
				case 2:
					return des.getSeriesObject().getOutputRank().getLabel();
				}
			} catch (Exception ne) {
				return ne.getMessage();
			}
		}
		return "  "+des.getName();
	}
	
	public Image getImage(Object element) {
		if (column>0) return null;
		if(!(element instanceof OperationDescriptor)) return super.getImage(element);
		OperationDescriptor des = (OperationDescriptor)element;
		return des.getImage();
	}

}
