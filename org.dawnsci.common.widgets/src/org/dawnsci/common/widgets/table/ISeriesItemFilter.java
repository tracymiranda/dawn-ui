package org.dawnsci.common.widgets.table;

import java.util.Collection;

public interface ISeriesItemFilter {

	/**
	 * Return the list of ISeriesItemDescriptor's which may follow itemDescriptor.
	 * 
	 * If itemDescriptor is null, should return complete list of all possible ISeriesItemDescriptor's
	 * 
	 * @param itemDescriptor, may be null
	 * @return
	 */
	Collection<ISeriesItemDescriptor> getDescriptors(String contents, int position, ISeriesItemDescriptor itemDescriptor);

}