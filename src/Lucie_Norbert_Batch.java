import java.io.File;
import java.io.FilenameFilter;

import ij.IJ;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

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

public class Lucie_Norbert_Batch implements PlugIn{
	String input=Prefs.get("Lucie_Norbert_Batch_Input.String", "");
	String output=Prefs.get("Lucie_Norbert_Batch_Output.String", "");
	String PDsChannel=Prefs.get("Lucie_Norbert_Batch_PDsChannel.String", "1");
	String wallsChannel="2";
	
	int enlargeWalls=(int) Prefs.get("Lucie_Norbert_Batch_enlargeWalls.double", 2);
	int enlargePDs= (int) Prefs.get("Lucie_Norbert_Batch_enlargeWalls.doublle", 1);
	
	String[] luts=IJ.getLuts();
	String lut=Prefs.get("Lucie_Norbert_Batch_LUT.String", luts[0]);

	@Override
	public void run(String arg) {
		if(GUI()) {
			process();
		}
		
		//TODO: average, med and SD per tag put on per tag
	}
	
	/**
	 * Displays the GUI and stores the values in the class variables
	 * @return true in case everything went right, false in case the GUI was closed
	 */
	public boolean GUI() {
		GenericDialog gd= new GenericDialog("Lucie_Norbert");
		gd.addDirectoryField("Where are the images ?", "");
		gd.addDirectoryField("Where to save output ?", "");
		gd.addChoice("Which channel corresponds to PDs ?", new String[]{"1", "2"}, PDsChannel);
		gd.addNumericField("Enlarge walls to count PDs (pixels)", enlargeWalls);
		gd.addNumericField("Enlarge PDs to quantify signal (pixels)", enlargePDs);
		gd.addChoice("LUT for colormaps", luts, lut);
		gd.showDialog();
		
		if(gd.wasCanceled()) return false;
		
		input=gd.getNextString();
		output=gd.getNextString();
		PDsChannel=gd.getNextChoice();
		enlargeWalls=(int) gd.getNextNumber();
		enlargePDs=(int) gd.getNextNumber();
		lut=gd.getNextChoice();
		
		if(PDsChannel=="2") wallsChannel="1";
		
		input=input.endsWith(File.separator)?input:input+File.separator;
		output=output.endsWith(File.separator)?output:output+File.separator;
		
		Prefs.set("Lucie_Norbert_Batch_Input.String", input);
		Prefs.set("Lucie_Norbert_Batch_Output.String", output);
		Prefs.set("Lucie_Norbert_Batch_PDsChannel.String", PDsChannel);
		Prefs.set("Lucie_Norbert_Batch_enlargeWalls.double", enlargeWalls);
		Prefs.set("Lucie_Norbert_Batch_enlargeWalls.double", enlargePDs);
		Prefs.set("Lucie_Norbert_Batch_LUT.String", lut);
		
		return true;
	}
	
	/**
	 * Batch processes all the files, based on the ones found in the PDs segmentation folder
	 */
	public void process() {
		String cellSegFolder=input+"Cell segmentation"+File.separator;
		String oriImgFolder=input+"Original images"+File.separator;
		String PDsSegFolder=input+"PD detection"+File.separator;
		
		if(new File(cellSegFolder).exists() && new File(oriImgFolder).exists() && new File(PDsSegFolder).exists()) {
			String[] filesList=new File(PDsSegFolder).list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.toLowerCase().endsWith(".tif");
				}
			});
			
			WallsPDsAnalysis wpa=new WallsPDsAnalysis();
			for(int i=0; i<filesList.length; i++) {
				String basename=filesList[i].replace("C"+PDsChannel+"-", "");
				basename=basename.substring(0, basename.lastIndexOf("."));
				
				String imgCellsOri=oriImgFolder+"C"+wallsChannel+"-"+basename+".tif";
				String imgPDsOri=oriImgFolder+"C"+PDsChannel+"-"+basename+".tif";
				String imgScaled=cellSegFolder+"C"+wallsChannel+"-"+basename+".png";
				String imgPDsSeg=PDsSegFolder+"C"+PDsChannel+"-"+basename+".tif";
				String pathRM=cellSegFolder+"C"+wallsChannel+"-"+basename+".zip";
				
				
				boolean allFound=wpa.setPaths(imgCellsOri, imgPDsOri, imgScaled, imgPDsSeg, pathRM);
				if(allFound) {
					wpa.process(enlargeWalls, enlargePDs);
					wpa.save(output, basename, lut);
				}else{
					IJ.log("Missing at least one file for basename "+basename+"\n------------");
				}
			}
			
		}else {
			IJ.error("Failed checks", "The input folder misses at least\none of the following subfolders:\nCell segmentation\nOriginal images\nD detection");
		}
		
	}

}
