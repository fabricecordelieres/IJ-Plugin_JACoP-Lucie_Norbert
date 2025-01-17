import java.io.File;

import ij.IJ;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;

/**
 * Lucie_Norbert.java
 * 
 * Created on 4 oct. 2024
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

public class Lucie_Norbert_Single implements PlugIn{
	String oriWalls="";
	String oriPDs="";
	String segWalls="";
	String segPDs="";
	
	String output=Prefs.get("Lucie_Norbert_Batch_Output.String", "");
	int enlargeWalls=(int) Prefs.get("Lucie_Norbert_Batch_enlargeWalls.double", 2);
	int enlargePDs= (int) Prefs.get("Lucie_Norbert_Batch_enlargeWalls.double", 1);
	
	String[] luts=IJ.getLuts();
	String lut=Prefs.get("Lucie_Norbert_Batch_LUT", luts[0]);

	@Override
	public void run(String arg) {
		//Checks
		boolean checksOk=true;
		if(WindowManager.getImageCount()<4) {
			IJ.error("At least 4 images should be opened:\n-Original walls\n-Original PDs\n-Segmented walls\n-Segmented PDs");
			checksOk=false;
		}
		
		if(RoiManager.getInstance()==null) {
			IJ.error("Outlines of the cell walls\nshould be added to the ROI Manager\nbefore launching the plugin");
			checksOk=false;
		}else {
			if(RoiManager.getInstance().getCount()==0) {
				IJ.error("Outlines of the cell walls\nshould be added to the ROI Manager\nbefore launching the plugin");
				checksOk=false;
			}
		}
		
		if(checksOk) {
			if(GUI()) {
				process();
			}
		}
		
		//TODO: average, med and SD per tag put on per tag
	}
	
	/**
	 * Displays the GUI and stores the values in the class variables
	 * @return true in case everything went right, false in case the GUI was closed
	 */
	public boolean GUI() {
		String[] images=WindowManager.getImageTitles();
		
		GenericDialog gd= new GenericDialog("Lucie_Norbert");
		gd.addChoice("Original walls image", images, images[0]);
		gd.addChoice("Original PDs image", images, images[1]);
		gd.addChoice("Segmented walls image", images, images[2]);
		gd.addChoice("Segmented PDs image", images, images[2]);
		gd.addDirectoryField("Where to save output ?", "");
		gd.addNumericField("Enlarge walls to count PDs (pixels)", enlargeWalls);
		gd.addNumericField("Enlarge PDs to quantify signal (pixels)", enlargePDs);
		gd.addChoice("LUT for colormaps", luts, lut);
		gd.showDialog();
		
		if(gd.wasCanceled()) return false;
		
		oriWalls=gd.getNextChoice();
		oriPDs=gd.getNextChoice();
		segWalls=gd.getNextChoice();
		segPDs=gd.getNextChoice();
		output=gd.getNextString();
		enlargeWalls=(int) gd.getNextNumber();
		enlargePDs=(int) gd.getNextNumber();
		lut=gd.getNextChoice();
		
		output=output.endsWith(File.separator)?output:output+File.separator;
		
		Prefs.set("Lucie_Norbert_Batch_Output.String", output);
		Prefs.set("Lucie_Norbert_Batch_enlargeWalls.double", enlargeWalls);
		Prefs.set("Lucie_Norbert_Batch_enlargeWalls.double", enlargePDs);
		Prefs.set("Lucie_Norbert_Batch_LUT.String", lut);
		
		return true;
	}
	
	/**
	 * Batch processes all the files, based on the ones found in the PDs segmentation folder
	 */
	public void process() {
		WallsPDsAnalysis wpa=new WallsPDsAnalysis();
		wpa.setImages(oriWalls, oriPDs, segWalls, segPDs);
		
		String basename=segPDs.substring(segPDs.indexOf("_")+1);
		basename=basename.substring(0, basename.lastIndexOf("."));
		
		wpa.process(enlargeWalls, enlargePDs);
		wpa.save(output, basename, lut);
	}

}
