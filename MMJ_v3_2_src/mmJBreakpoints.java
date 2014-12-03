import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.IJ;
import java.io.*;
import java.util.ArrayList;
import java.util.*;
import java.awt.*;
/**

*/
public class mmJBreakpoints{

	/***********************  Constructors */
	ArrayList<Integer> breakpoints;
	
	public mmJBreakpoints() {
		breakpoints = new ArrayList<Integer>();
	}
	
	public mmJBreakpoints(ArrayList<Integer> bPoints) {
		breakpoints = bPoints;
	}
	
	public mmJBreakpoints(int [] iPoints) {
		breakpoints = new ArrayList<Integer>();
		set_breakpoints(iPoints);
	}

	/***********************  Add/Remove breakpoints */
	
	public void remove_last_breakpoint(){
		int index=breakpoints.size()-1;
		breakpoints.remove(index);
	}
	
	public void remove_breakpoint(int iPoint){
		int index=breakpoints.indexOf(iPoint);
		if(index>=0){
			breakpoints.remove(index);
		}
	}
	
	public void remove_breakpoints(int [] iPoints){
		for(int i=0; i<iPoints.length; i++){
			remove_breakpoint(iPoints[i]);
		}
	}	

	
	public void remove_all_breakpoints(){
		breakpoints = new ArrayList<Integer>();
	}
	
	public void add_breakpoint(int iPoint){
		//int index=breakpoints.indexOf(iPoint);
		//if(index==0){
			breakpoints.add(new Integer(iPoint));
		//}
		Collections.sort(breakpoints);
	}
	
	public void move_breakpoint(int yfrom, int yto){
		int index=breakpoints.indexOf(yfrom);
		if(index>=0){
			breakpoints.set(index, yto);
		}
		Collections.sort(breakpoints);
	}	
	
	public void add_breakpoints(int [] iPoints){
		for(int i=0; i<iPoints.length; i++){
			//int index=breakpoints.indexOf(iPoints[i]);
			//if(index==0){
				breakpoints.add(new Integer(iPoints[i]));
			//}
		}
		Collections.sort(breakpoints);
	}
	
	/***********************  Drawing functions */

	public void draw_breakpoints(ImageProcessor ipz, int offset_x, int offset_y, int channel_width){
		int [] this_breakpoints = get_breakpoints();
    	for (int i = 0; i < this_breakpoints.length; i++){
    	
    		//LINE
    		int w=3;
        	Roi roiL = new Roi(offset_x+2, offset_y+this_breakpoints[i], w, 2);
			ipz.setRoi(roiL);
			ipz.setLineWidth(1);
			ipz.setColor(new Color(255,255,150));
			//ipz.setValue(255);  
			ipz.draw(roiL); 	
			
			Roi roiR = new Roi(offset_x+channel_width-w-2,  offset_y+this_breakpoints[i],  w, 2);
			ipz.setRoi(roiR);
			ipz.setLineWidth(1);
			ipz.setColor(new Color(255,255,150));
			//ipz.setValue(255);  
			ipz.draw(roiR); 	
			
			//OVALS
			/*
			int radius=3;
			OvalRoi or1 = new OvalRoi(offset_x-radius, offset_y+this_breakpoints[i]-radius, 2*radius, 2*radius);
			ipz.setRoi(or1);
			ipz.setValue(255);  
			ipz.fill(ipz.getMask());
			
			OvalRoi or2 = new OvalRoi(offset_x+channel_width-radius, offset_y+this_breakpoints[i]-radius, 2*radius, 2*radius);
			ipz.setRoi(or2);
			ipz.setValue(255);  
			ipz.fill(ipz.getMask());
			*/
    	}
	}
	
	/*
	public void undraw_breakpoints(ImageProcessor ipz, int offset_x, int offset_y){
		int [] this_breakpoints = get_breakpoints();
    	for (int i = 0; i < this_breakpoints.length; i++){
    		//LINE
        	Roi roi = new Roi(0, this_breakpoints[i], channel_width, 1);
			ipz.setRoi(roi);
			ipz.setLineWidth(1);
			ipz.setValue(123);  
			ipz.draw(roi); 	
			
			//OVALS
			int radius=3;
			OvalRoi or1 = new OvalRoi(offset_x-radius, offset_y+this_breakpoints[i]-radius, 2*radius, 2*radius);
			ipz.setRoi(or1);
			ipz.setValue(123);  
			ipz.fill(ipz.getMask());
			
			OvalRoi or2 = new OvalRoi(offset_x+channel_width-radius, offset_y+this_breakpoints[i]-radius, 2*radius, 2*radius);
			ipz.setRoi(or2);
			ipz.setValue(123);  
			ipz.fill(ipz.getMask());
    	}
	}		
	*/
	/***********************  Import/Export */
	
	public void toFile(String dir_channel){
	/*
		ImagePlus imp = new ImagePlus(id_channel, istack);
		(new File(dir_channel)).mkdir();
       	IJ.saveAs(imp, "tif", dir_channel+"/"+id_channel+".tif"); 
		IJ.log("Saved in "+dir_channel+"/"+id_channel+".tif");
	*/
	}
	
	public String toString(){
		String ret =":: ";
		Iterator<Integer> ix=breakpoints.iterator();
        while(ix.hasNext()) {		
       		Integer bpoint=(Integer)ix.next();
       		if(ix!=null){
	       		ret+=" "+(bpoint.intValue());
       		}
        }
		return ret;
	}
	
	public void log(){
		IJ.log(""+toString());
	}

	/***********************  Gets/Sets */

	public int [] get_breakpoints(){
		return convertIntegers(breakpoints);
	}

	public int getNumBreakpoints(){
		return breakpoints.size();
	}

	public int get_near_breakpoint(int x0, int max_dist){
		int [] this_breakpoints = get_breakpoints();
		int closest_distance=max_dist;
		int closest_breakpoint=-1;
    	for (int i = 0; i < this_breakpoints.length; i++){
    		int this_distance=Math.abs(this_breakpoints[i]-x0);
    		//IJ.log(i+" dist="+this_distance+" xN="+this_breakpoints[i]+" x0="+x0);
    		if(closest_breakpoint<0 || closest_distance>this_distance){
    			closest_distance=this_distance;
    			if(this_distance<max_dist){
    				closest_breakpoint=this_breakpoints[i];
    			}
    		}
    	}
    	return closest_breakpoint;
	}

	public void set_breakpoints(int [] iPoints){
		breakpoints = new ArrayList<Integer>();
		for(int i=0; i<iPoints.length; i++){
			breakpoints.add(new Integer(iPoints[i]));
		}
		//Collections.sort(breakpoints);
	}
	
	
	/***********************  Auxiliary functions */
	public int[] convertIntegers(ArrayList<Integer> integers){
    	int[] ret = new int[integers.size()];
    	Iterator<Integer> iterator = integers.iterator();
    	for (int i = 0; i < ret.length; i++){
        	ret[i] = iterator.next().intValue();
    	}
    	return ret;
	}
	
}

