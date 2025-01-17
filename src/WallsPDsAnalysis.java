import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.NewImage;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.HyperStackConverter;
import ij.plugin.RoiEnlarger;
import ij.plugin.RoiScaler;
import ij.plugin.filter.MaximumFinder;
import ij.plugin.frame.RoiManager;
import ij.process.ImageStatistics;

/**
 * WallsPDsAnalysis.java
 * 
 * Created on 30 sept. 2024
 * Fabrice P. Cordelieres, fabrice.cordelieres at gmail.com
 * 
 * Copyright (C) 2024 Fabrice P. Cordelieres
 *
 * License:
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * This class is aimed at analysing cell walls/PDs and saving the data, Rois and visual output
 */
public class WallsPDsAnalysis {
	ImagePlus ipOriWalls=null;
	ImagePlus ipOriPDs=null;
	ImagePlus ipSegmentedPDs=null;
	ImagePlus ipScaledWalls=null;
	RoiManager unscaledRoiWalls=null;
	RoiManager scaledRoiWalls=null;
	RoiManager scaledRoiWallsPerTag=null;
	int enlargeWalls=2;
	int enlargePDs=2;
	
	ResultsTable resultsPerCell=null;
	ResultsTable resultsStatsPerTagFromCells=null;
	ResultsTable resultsPerTag=null;
	
	String[] categories=null;
	
	/**
	 * Sets the path where all images/RoiManager content are found and loads images/RoiManager.
	 * @param pathOriWalls path to the original walls image
	 * @param pathOriPDs path to the original PDs image
	 * @param pathScaledWalls path to the scaled PDs image
	 * @param pathSegmentedPDs path to the segmented PDs image (should be a mask where PDs appear as single points)
	 * @param pathRoiManagerWalls path to the zip file containing all the cell segmentation ROIs
	 * @return true in case all files were found, false otherwise
	 */
	public boolean setPaths(String pathOriWalls, String pathOriPDs,  String pathScaledWalls, String pathSegmentedPDs, String pathRoiManagerWalls) {
		boolean isFound=check(pathOriWalls, pathOriPDs, pathScaledWalls, pathSegmentedPDs, pathRoiManagerWalls);
		
		//Reset RoiManagers and ResultsTable
		unscaledRoiWalls=null;
		scaledRoiWalls=null;
		scaledRoiWallsPerTag=null;
		resultsPerCell=null;
		resultsStatsPerTagFromCells=null;
		resultsPerTag=null;
		categories=null;
		
		if(isFound) {
			ipOriWalls=new ImagePlus(pathOriWalls);
			ipOriPDs=new ImagePlus(pathOriPDs);
			ipScaledWalls=new ImagePlus(pathScaledWalls);
			ipSegmentedPDs=new ImagePlus(pathSegmentedPDs);
			unscaledRoiWalls=new RoiManager(true);
			unscaledRoiWalls.open(pathRoiManagerWalls);
			scaledRoiWalls=new RoiManager(true);
		}else {
			ipOriWalls=null;
			ipOriPDs=null;
			ipSegmentedPDs=null;
			ipScaledWalls=null;
			unscaledRoiWalls=null;
			scaledRoiWalls=null;
			scaledRoiWallsPerTag=null;
			resultsPerCell=null;
			resultsStatsPerTagFromCells=null;
			resultsPerTag=null;
			categories=null;
		}
		
		return isFound;	
	}
	
	/**
	 * Sets the ImagePlus based on already opened images and their names. Takes the RoiManager as the container for cell walls
	 * @param nameOriWalls name of the original walls image
	 * @param nameOriPDs name of the original PDs image
	 * @param nameScaledWalls name of the scaled PDs image
	 * @param nameSegmentedPDs name of the segmented PDs image (should be a mask where PDs appear as single points)
	 */
	public void setImages(String nameOriWalls, String nameOriPDs,  String nameScaledWalls, String nameSegmentedPDs) {
		ipOriWalls=WindowManager.getImage(nameOriWalls);
		ipOriPDs=WindowManager.getImage(nameOriPDs);
		ipScaledWalls=WindowManager.getImage(nameScaledWalls);
		ipSegmentedPDs=WindowManager.getImage(nameSegmentedPDs);
		unscaledRoiWalls=RoiManager.getInstance();
		scaledRoiWalls=new RoiManager(true);
		resultsPerCell=null;
		resultsStatsPerTagFromCells=null;
		resultsPerTag=null;
		categories=null;
	}
	
	/**
	 * Checks if all expected files exist
	 * @param pathOriWalls path to the original walls image
	 * @param pathOriPDs path to the original PDs image
	 * @param pathScaledWalls path to the scaled PDs image
	 * @param pathSegmentedPDs path to the segmented PDs image (should be a mask where PDs appear as single points)
	 * @param pathRoiManagerWalls path to the zip file containing all the cell segmentation ROIs
	  @return true in case all files were found, false otherwise. Logs the paths to missing files.
	 */
	private boolean check(String pathOriWalls, String pathOriPDs,  String pathScaledWalls, String pathSegmentedPDs, String pathRoiManagerWalls) {
		boolean isFound=new File(pathOriWalls).exists() && new File(pathOriPDs).exists() && new File(pathScaledWalls).exists() && new File(pathSegmentedPDs).exists() && new File(pathRoiManagerWalls).exists();
		
		//Informations to guide the user about what is missing and how missing files should be named
		if(!isFound) IJ.log("------------\nMissing at least one file:");
		if(!new File(pathOriWalls).exists()) IJ.log(pathOriWalls+" not found");
		if(!new File(pathOriPDs).exists()) IJ.log(pathOriPDs+" not found");
		if(!new File(pathScaledWalls).exists()) IJ.log(pathScaledWalls+" not found");
		if(!new File(pathSegmentedPDs).exists()) IJ.log(pathSegmentedPDs+" not found");
		if(!new File(pathRoiManagerWalls).exists()) IJ.log(pathRoiManagerWalls+" not found");
		
		return isFound;
	}
	
	/**
	 * Depending on the scale difference between the OriWalls image and the ScaledWalls image, a new RoiManager object is created,
	 * storing scaled Rois. Performs all analysis, per cell and per tag and feeds two ResultsTable object with measurements
	 * @param enlargeWalls enlargement (in pixels) to be performed on the walls Rois to count PDs. Doesn't affect the cell area measurement
	 * @param enlargePDs enlargement (in pixels) to be performed on the PDs Rois to quantify the signal associated to PDs.
	 */
	public void process(int enlargeWalls, int enlargePDs) {
		for(Roi r:unscaledRoiWalls.getRoisAsArray()) scaledRoiWalls.addRoi(r);
		
		scaleRois((double) ipOriWalls.getWidth()/ipScaledWalls.getWidth());
		scaledRoiWallsPerTag=fuseRoisPerTag("_", scaledRoiWalls, ipOriWalls);
		
		
		//Quantify per cell
		resultsPerCell=quantify(scaledRoiWalls.getRoisAsArray(), "_", enlargeWalls, enlargePDs);
		
		//Quantify per tag
		resultsPerTag=quantify(scaledRoiWallsPerTag.getRoisAsArray(), "_", enlargeWalls, enlargePDs);
	}
	
	/**
	 * Saves the scaled Rois and the results (per cell and per tag) in the output folder, using the provided basename
	 * @param pathOutput path to the output folder
	 * @param basename basename to be used when saving elements
	 * @param lut name of the LUT to be applied to the image
	 */
	public void save(String pathOutput, String basename, String lut) {
		scaledRoiWalls.save(pathOutput+basename+"_scaledRoisWalls.zip");
		scaledRoiWallsPerTag.save(pathOutput+basename+"_scaledRoisWallsPerTag.zip");
		resultsPerCell.save(pathOutput+basename+"_resultsPerCell.csv");
		resultsPerTag.save(pathOutput+basename+"_resultsPerTag.csv");
		
		//Color Maps
		String[] type={"_perIndividualCell_", "_perTagAsOneCell_"};
		RoiManager[] rms={scaledRoiWalls, scaledRoiWallsPerTag};
		ResultsTable[] rts={resultsPerCell, resultsPerTag};
		
		for(int i=0; i<rms.length; i++) {
			String[] parameters=rts[i].getHeadings();
			for(int j=0; j<parameters.length; j++) {
				if(parameters[j]!="RoiName" && parameters[j]!="Structure") {
					ImagePlus ip=createColorMap(ipOriWalls, parameters[j], rms[i], rts[i], lut);
					IJ.run(ip, "Calibration Bar...", "location=[Upper Right] fill=None label=White number=5 decimal=0 font=12 zoom=12 overlay");
					new FileSaver(ip).saveAsZip(pathOutput+basename+"_ColorMap"+type[i]+parameters[j]+".zip");
				}
			}
		}
		
		
	}
	
	/**
	 * Adapts ROIs from the unscaledRoiWalls Roi Manager object made on image of size a to an image of size b (scaleFactor=b/a) and stored in the scaledRoiWalls object
	 * @param scaleFactor scale ratio between the image where the ROIs where drawn and the image onto which they should be adapted
	 */
	private void scaleRois(double scaleFactor) {
		for(int i=0; i<scaledRoiWalls.getCount(); i++) {
			IJ.showStatus("Scaling: Processing ROI "+(i+1)+"/"+(scaledRoiWalls.getCount()));
			Roi r=scaledRoiWalls.getRoi(i);
			//Error can later arise from Rois having not area: remove them
			if(r.getStatistics().area!=0.0) {
				double x=r.getContourCentroid()[0];
				double y=r.getContourCentroid()[1];
				r.translate((scaleFactor-1)*x, (scaleFactor-1)*y);
				r=RoiScaler.scale(r, scaleFactor, scaleFactor, true);
				scaledRoiWalls.setRoi(r, i);
			}else {
				scaledRoiWalls.select(i);
				scaledRoiWalls.runCommand((ImagePlus) null, "Delete");
				IJ.log("Found one ROI with area=0.0 and removed it (index: "+i+", name: "+r.getName()+")");
				i=i==0?0:i--; //In case the 1st Roi is supressed
			}
		}
	}
	
	/**
	 * Supposes that all the ROIs in the ROIManager have been named in the form tag+separator+whatever.
	 * Extracts the unique tags and stores the indexes of the ROIs carrying the tag
	 * @param separator the char that separates the tag from the rest of the ROI's name
	 * @param rm the RoiManager object containing the Rois to fuse
	 * @return a hashmap where the key is a String carrying the tag and value is an ArrayList of the indexes of all ROIs carrying this tag
	 */
	private final HashMap<String, ArrayList<Integer>> categorizeRoisPerNames(String separator, RoiManager rm) {
		Roi[] roisArray=rm.getRoisAsArray();
		
		HashMap<String, ArrayList<Integer>> namesIndex=new HashMap<String, ArrayList<Integer>>();
		for(int i=0; i<roisArray.length; i++) {
			Roi r=roisArray[i];
			String basename="";
			if(r.getName().split(separator).length>1) {
				basename=r.getName().split(separator)[0];
				ArrayList<Integer> currIndexes=namesIndex.get(basename);
				currIndexes=currIndexes==null?new ArrayList<Integer>():currIndexes;
				currIndexes.add(i);
				namesIndex.put(basename, currIndexes);
			}
		}
		categories=namesIndex.keySet().toArray(new String[namesIndex.size()]);
		return namesIndex;
	}
	
	
	/**
	 * Supposes that all the ROIs in the ROIManager have been named in the form tag+separator+whatever.
	 * Extracts the unique tags and stores the indexes of the ROIs carrying the tag
	 * Fuses the ROIs carrying a same tag (XOR)
	 * @param separator the char that separates the tag from the rest of the ROI's name
	 * @param rm the RoiManager object containing the Rois to fuse
	 * @return a RoiManager object containing the fused Rois
	 */
	private RoiManager fuseRoisPerTag(String separator, RoiManager rm, ImagePlus ip) {
		HashMap<String, ArrayList<Integer>> categorized=categorizeRoisPerNames(separator, rm);
		
		String[] categories=categorized.keySet().toArray(new String[categorized.size()]);
		RoiManager fusedRois=new RoiManager(true);
		
		for(int i=0; i<categories.length; i++) {
			IJ.showStatus("Fusing Rois: Processing category "+(i+1)+"/"+(categories.length));
			int[] indexes=categorized.get(categories[i]).stream().mapToInt(a->a).toArray();
			if(indexes.length>1) {
				rm.setSelectedIndexes(indexes);
				rm.runCommand(ip,"Combine");
			}else {
				rm.select(ip, indexes[0]);
			}
			
			Roi toAdd=ip.getRoi();
			toAdd.setName(categories[i]);
			toAdd.setStrokeColor(rm.getRoi(indexes[0]).getStrokeColor());
			
			fusedRois.addRoi(toAdd);
		}
		ip.resetRoi();
		
		return fusedRois;
	}
	
	/**
	 * Quantifies the PDs number and signal, and extracts cells morphological parameters
	 * @param rois array of ROIs containing cells' delineations
	 * @param separator separator used in the ROI's name to separated the category (tag) of the Roi from the rest of the name
	 * @param enlargeWalls enlargement (in pixels) to be performed to include all PDs in the count and counter-balance possible segmentation errors
	 * @param enlargePDs enlargement (in pixels) to be performed to integrate the PDs' signal beyond the simple detection point
	 * @return
	 */
	private ResultsTable quantify(Roi[] rois, String separator, int enlargeWalls, int enlargePDs) {
		ResultsTable rt=new ResultsTable();
		Calibration c=ipSegmentedPDs.getCalibration();
		ipSegmentedPDs.setCalibration(new Calibration());
		
		int rowIndex=0;
		
		for(Roi r:rois) {
			IJ.showStatus("Quantification: Processing ROI "+(rowIndex+1)+"/"+(rois.length));
			
			ipSegmentedPDs.setRoi(r);
			ImageStatistics is=ipSegmentedPDs.getStatistics(ImageStatistics.AREA+ImageStatistics.MEAN);
			rt.setValue("RoiName", rowIndex, r.getName());
			rt.setValue("Structure", rowIndex, r.getName().split(separator)[0]);
			rt.setValue("Area_Cell_"+c.getUnit()+"2", rowIndex, is.area*(c.pixelWidth*c.pixelHeight));
			
			Roi enlargeWallsRois=r!=null?RoiEnlarger.enlarge(r, enlargeWalls):r;
			ipSegmentedPDs.setRoi(enlargeWallsRois);
			int nPDs=(int) ((is.mean*is.longPixelCount)/255);
			rt.setValue("Nb_PDs_(enlarge="+enlargeWalls+")", rowIndex, nPDs);
			
			MaximumFinder mf=new MaximumFinder();
			mf.findMaxima(ipSegmentedPDs.getProcessor(), 0, MaximumFinder.POINT_SELECTION, false);
			Roi pointRoi=ipSegmentedPDs.getRoi();
			pointRoi=pointRoi!=null?RoiEnlarger.enlarge(pointRoi, enlargePDs):pointRoi;
			
			ipOriPDs.setRoi(pointRoi);
			is=ipOriPDs.getStatistics(ImageStatistics.AREA+ImageStatistics.MEAN);
			
			rt.setValue("Area_PDs_"+c.getUnit()+"2_(enlarge="+enlargePDs+")", rowIndex, is.area*(c.pixelWidth*c.pixelHeight));
			rt.setValue("Nb_PDs_per_Cell_Area_"+c.getUnit()+"2", rowIndex, nPDs/(is.area*(c.pixelWidth*c.pixelHeight)));
			rt.setValue("Mean_Signal_PDs_perPixel_(enlarge="+enlargePDs+")", rowIndex++, is.mean);
		}
		
		ipSegmentedPDs.setCalibration(c);
		ipSegmentedPDs.resetRoi();
		ipOriPDs.resetRoi();
		
		return rt;
	}
	
	/**
	 * Creates a colorMap from extracted data
	 * @param ip an example image, from which dimensions are extracted to generate teh map
	 * @param paramName name of the parameter to draw on the map
	 * @param rm RoiManager containing the Rois
	 * @param rt ResultsTable containing the measurements
	 * @param lut color table to apply to the image
	 * @return an Imageplus containing the colormap
	 */
	private ImagePlus createColorMap(ImagePlus ip, String paramName, RoiManager rm, ResultsTable rt, String lut) {
		//Creates the output image based on the input image
		int[] dimensions=ip.getDimensions();
		ImagePlus out=NewImage.createFloatImage(ip.getTitle()+"_colorMap_for_"+paramName, dimensions[0], dimensions[1], dimensions[3]*dimensions[4], NewImage.FILL_BLACK);
		
		//Only converts to an hyperstack if needed
		if(out.getNSlices()>1) out=HyperStackConverter.toHyperStack(out, 1, dimensions[3], dimensions[4]);
		
		//Get values for the input paramName
		double[] param=rt.getColumn(paramName);
		
		//Colors all ROIs based on the relevant parameter
		for(int i=0; i<rm.getCount(); i++) {
			IJ.showStatus("Color map: Processing ROI "+(i+1)+"/"+(rm.getCount()));
			Roi roi=rm.getRoi(i);
			int zPos=roi.getZPosition();
			int tPos=roi.getTPosition();
			out.setPositionWithoutUpdate(1, zPos, tPos);
			out.setRoi(roi);
			IJ.run(out, "Add...", "value="+param[i]);
			out.resetRoi();
		}
		
		out.resetDisplayRange();
		IJ.run(out, lut, "");
		IJ.getLuts();
        IJ.run(out, "Calibration Bar...", "location=[Upper Right] fill=None label=White number=5 decimal=0 font=12 zoom=12 overlay");
		return out;
	}
	
	private void getStatsFromCells() {
		resultsStatsPerTagFromCells=new ResultsTable();
		String[] headings=resultsPerCell.getHeadings();
		
		for(int i=0; i<categories.length; i++) {
			for(int j=0; j<headings.length; j++) {
				//Créer une image de la results table, une ligne par colonne
				//Scanner les lignes qui get stat pour avoir mean et SD et n
			}
		}
	}

}
