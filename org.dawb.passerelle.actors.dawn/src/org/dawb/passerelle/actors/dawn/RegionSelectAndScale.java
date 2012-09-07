package org.dawb.passerelle.actors.dawn;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.dawb.passerelle.common.actors.AbstractDataMessageTransformer;
import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.passerelle.common.parameter.roi.ROIParameter;

import com.isencia.passerelle.actor.ProcessingException;

import ptolemy.actor.Actor;
import ptolemy.actor.Director;
import ptolemy.actor.Initializable;
import ptolemy.actor.Manager;
import ptolemy.actor.Receiver;
import ptolemy.actor.util.FunctionDependency;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.InvalidStateException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.DatasetUtils;
import uk.ac.diamond.scisoft.analysis.roi.ROIBase;
import uk.ac.diamond.scisoft.analysis.roi.ROIProfile;
import uk.ac.diamond.scisoft.analysis.roi.RectangularROI;

public class RegionSelectAndScale extends AbstractDataMessageTransformer {

	public ROIParameter selectionROI;
	public StringParameter roiName;
	public StringParameter datasetName;
	public StringParameter anglesAxisAdjustName;
	public StringParameter energyAxisAdjustName;
	public StringParameter angles;
	public StringParameter energies;

	public RegionSelectAndScale(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		selectionROI = new ROIParameter(this, "selectionROI");
		registerConfigurableParameter(selectionROI);
		roiName = new StringParameter(this, "roiName");
		registerConfigurableParameter(roiName);
		datasetName = new StringParameter(this, "datasetName");
		registerConfigurableParameter(datasetName);
		anglesAxisAdjustName = new StringParameter(this, "xAxisAdjustName");
		registerConfigurableParameter(anglesAxisAdjustName);
		energyAxisAdjustName = new StringParameter(this, "yAxisAdjustName");
		registerConfigurableParameter(energyAxisAdjustName);
		angles = new StringParameter(this, "angles");
		registerConfigurableParameter(angles);
		energies = new StringParameter(this, "energies");
		registerConfigurableParameter(energies);
	}

	@Override
	protected DataMessageComponent getTransformedMessage(
			List<DataMessageComponent> cache) throws ProcessingException {
		// get the data out of the message, name of the item should be specified
		final Map<String, Serializable>  data = MessageUtils.getList(cache);
		final Map<String, String> scalar = MessageUtils.getScalar(cache);

		// get the roi out of the message, name of the roi should be specified
		RectangularROI roi = (RectangularROI) selectionROI.getRoi();

		// check the roi list, and see if one exists
		try {
			Map<String, ROIBase> rois = MessageUtils.getROIs(cache);

			if(rois.containsKey(roiName.getExpression())) {
				if (rois.get(roiName.getExpression()) instanceof RectangularROI) {
					roi = (RectangularROI) rois.get(roiName.getExpression());
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}



		// prepare the output message
		DataMessageComponent result = new DataMessageComponent();

		// put all the datasets in for reprocessing
		for (String key : data.keySet()) {
			result.addList(key, (AbstractDataset) data.get(key));
		}

		// Normalise the specified dataset
		String name = datasetName.getExpression();
		String angleAdjustName = anglesAxisAdjustName.getExpression();
		String energyAdjustName = energyAxisAdjustName.getExpression();
		String anglesName = angles.getExpression();
		String energiesName = energies.getExpression();

		if (data.containsKey(name) && data.containsKey(anglesName) && data.containsKey(energiesName)) {

			// Now get the Dataset and axis
			AbstractDataset dataDS = ((AbstractDataset)data.get(name)).clone();
			AbstractDataset anglesDS = ((AbstractDataset)data.get(anglesName)).clone();
			AbstractDataset energiesDS = ((AbstractDataset)data.get(energiesName)).clone();

			// now check to see what the offsets are before calculating the conversions
			double angleOffset = 0.0;
			double energyOffset = 0.0;
			if(scalar.containsKey(angleAdjustName)) {
				try {
					angleOffset = Double.parseDouble(scalar.get(angleAdjustName));
				} catch (Exception e) {
					// TODO: handle exception
				}
			}

			if(scalar.containsKey(energyAdjustName)) {
				try {
					energyOffset = Double.parseDouble(scalar.get(energyAdjustName));
				} catch (Exception e) {
					// TODO: handle exception
				}
			}

			// Apply the corrections to the datasets
			energiesDS.isubtract(energyOffset);
			anglesDS.isubtract(energyOffset);

			// then get the region
			final int yInc = roi.getPoint()[1]<roi.getEndPoint()[1] ? 1 : -1;
			final int xInc = roi.getPoint()[0]<roi.getEndPoint()[0] ? 1 : -1;

			AbstractDataset dataRegion = dataDS;
			AbstractDataset angleRegion = anglesDS;
			AbstractDataset energyRegion = energiesDS;

			try {
				dataRegion = dataRegion.getSlice(new int[] { (int) roi.getPoint()[1], (int) roi.getPoint()[0] },
						new int[] { (int) roi.getEndPoint()[1], (int) roi.getEndPoint()[0] },
						new int[] {yInc, xInc});

				angleRegion = angleRegion.getSlice(new int[] { (int) roi.getPoint()[1] },
						new int[] { (int) roi.getEndPoint()[1] },
						new int[] {yInc});

				energyRegion = energyRegion.getSlice(new int[] {(int) roi.getPoint()[0] },
						new int[] {(int) roi.getEndPoint()[0] },
						new int[] {xInc});
				
			} catch (Exception e) {
				// TODO: handle exception
			}
			
			// FIXME, need to have some way of representing functions at this point.
			
			
			
			//result.addList(ds.getName()+"_norm", ds.idivide(correction));
			//result.addList(ds.getName()+"_correction_map", correction);
		}

		// do the correction and put that into the pipeline., with a name that should be specified.
		return result;
	}

	@Override
	protected String getOperationName() {
		// TODO Auto-generated method stub
		return null;
	}



}