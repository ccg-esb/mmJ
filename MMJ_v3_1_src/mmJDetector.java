import ij.*;
import ij.IJ;
import ij.gui.*;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.plugin.filter.PlugInFilter;
import ij.measure.*;
import ij.process.*;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.BackgroundSubtracter;
import java.awt.*;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Arrays;  
import java.util.Random;
import java.util.*;
import java.util.Collections;


public class mmJDetector{

	boolean DEBUG = false;

	/*********************  PARAMS   ************************/	
	int channel_width;
	int channel_height;
	int numPoints;
	int numSteps;
	double step_max;
	int size_window;
	int thres;
	int deltaY;	
	
	int margin_y;
	int max_growth_rate;
	
	public mmJDetector(){
		channel_width=0;
		channel_height=0;
		numPoints=0;
		numSteps=0;
		step_max=0;
		size_window=0;
		thres=0;	
		deltaY=0;
		margin_y=20;  //No breakpoints in the edges
		max_growth_rate=0;
	}
	
	public mmJDetector(int channel_width, int channel_height){
		this.channel_width=channel_width;
		this.channel_height=channel_height;
		
		//SHOULD LOAD PARAMETERS FROM SOMEWHERE
		numPoints=100;
		numSteps=50;
		step_max=50;
		size_window=30;		
		thres=50;	
		deltaY=10;
		margin_y=20;
		max_growth_rate=20;
	}
	
	
	
	/************************ DETECT BREAKPOINTS  ************************/	
	
	public int find_missing_breakpoint(ImageProcessor ipz, int near_breakpoint){
				int startX=0;
				int startY=near_breakpoint-max_growth_rate;
				int endX=channel_width;
				int endY=near_breakpoint+max_growth_rate;
	
				byte[] channel_pixels = new byte[channel_width*2*max_growth_rate];
			
				//First obtain the pixels
				int nRows = 0; 
				for (int y = startY; y < endY; y++){
            		int index = nRows*channel_width;
                	int nCols = 0;
                	for (int x = startX; x < startX+channel_width; x++){
               			channel_pixels[index+nCols] =(byte)ipz.getPixel(x,y);
                		nCols++;
               		}
               		
                	
                	nRows++;
           		}
           		
           		ByteProcessor proc = new ByteProcessor(channel_width, 2*max_growth_rate, channel_pixels, null);
    			ImagePlus imp_missing = new ImagePlus("Missing", proc); 
    			
           		//Now find the intensity of every row
				int [] sumRow = new int[2*max_growth_rate];
				int best_intensity=0;
				int best_breakpoint=0;
				for(int y=0; y<2*max_growth_rate; y++){
					sumRow[y]=measureIntensityLine(startX, y, endX, y, channel_pixels, channel_width, 2*max_growth_rate, false);
					//IJ.log(y+" "+sumRow[y]);
					if(sumRow[y]>best_intensity){
						best_intensity=sumRow[y];
						best_breakpoint=y;
					}
				}
				
				if((best_intensity/channel_width)>thres){
					best_breakpoint+=startY;
				}else{
					best_breakpoint=0;
				}
		return best_breakpoint;
	}
	

	public void detect_breakpoints(mmJChannel this_channel){
	
		ImageStack istack_channel= this_channel.getImageStack();
		int sizeStack=istack_channel.getSize();

		ImagePlus imp_channel = new ImagePlus("Channel "+this_channel.get_id(), istack_channel);
		final ImageProcessor ip = imp_channel.getProcessor();
		
		int startX=0;
		int startY=0;
		int endX=channel_width;
		int endY=channel_height;

		//if (imp_channel.getType() == ImagePlus.GRAY8) {   //NECESSARY?
		
			for(int z=1; z<sizeStack+1; z++){
				IJ.showStatus("Detecting breakpoints in channel "+this_channel.get_id()+" at frame "+z);
				ImageProcessor ipz=istack_channel.getProcessor(z);
				byte[] channel_pixels = new byte[channel_width*channel_height];
			
				//First obtain the pixels
				int nRows = 0; 
				for (int y = startY; y < startY+channel_height; y++){
            		int index = nRows*channel_width;
                	int nCols = 0;
                	for (int x = startX; x < startX+channel_width; x++){
               			channel_pixels[index+nCols] =(byte)ipz.getPixel(x,y);
                		nCols++;
               		}
                	nRows++;
           		}
           	
           		//Now find the intensity of every row
				int [] sumRow = new int[channel_height];
				for(int y=startY; y<endY; y++){
					sumRow[y]=measureIntensityLine(startX, y, endX, y, channel_pixels, channel_width, channel_height, false);
				}
				
				//Now compute difference
				int [] diffRow= new int[endY-startY];
				int i=0;
				for(int y=startY+deltaY; y<endY-deltaY; y++){
					int thisDiff=(sumRow[y-deltaY]-sumRow[y+deltaY]);
					diffRow[i]=thisDiff;
					i++;
				}
				
				//Now search for breakpoints
				int [] breakpoints = find_breakpoints(sumRow);
				
				//Now cluster breakpoints
				breakpoints=cluster_breakpoints(breakpoints, diffRow); //Collapse near-points
				
				//Now update channel
				this_channel.set_breakpoints(z, breakpoints);
				
				if(DEBUG) log_breakpoints(z, breakpoints);
    		}  //for stack
		//}else{ //if gray8
		//	IJ.log("ERROR: Convert image to 8-bit");
		//}
	}
	
	public int [] cluster_breakpoints(int [] breakpoints, int [] contrastRow){
	
		ArrayList<Integer> the_points = new ArrayList<Integer>();
		ArrayList<Integer> count_points = new ArrayList<Integer>();
		ArrayList<Integer> contrast_points = new ArrayList<Integer>();
		ArrayList<Integer> deleted_points = new ArrayList<Integer>();
	
		for(int n=0; n<breakpoints.length; n++){
			if(the_points.contains(breakpoints[n])){
				int index=the_points.indexOf(new Integer(breakpoints[n]));
				count_points.set(index, new Integer(((Integer)count_points.get(index)).intValue()+1));
			}else{
				the_points.add(new Integer(breakpoints[n]));
				count_points.add(new Integer(1));
				contrast_points.add(new Integer(contrastRow[n]));
			}
		}
		
		//Collapse identical breakpoints and group those that are near
		int [] clustered_breakpoints = convertIntegers(the_points);
		int [] counted_breakpoints = convertIntegers(count_points);
		int [] contrast_breakpoints = convertIntegers(contrast_points);
		for(int n=0; n<clustered_breakpoints.length; n++){
			if(DEBUG) IJ.log(clustered_breakpoints[n]+" :: "+contrast_breakpoints[n]+"  ("+counted_breakpoints[n]+")");
			
			for(int m=0; m<clustered_breakpoints.length; m++){
				if(counted_breakpoints[m]>0 && counted_breakpoints[n]>0 && n!=m && Math.abs(clustered_breakpoints[n]-clustered_breakpoints[m])<size_window){
					if(DEBUG) IJ.log("GROUPING "+clustered_breakpoints[n]+" and "+clustered_breakpoints[m]);
					if(counted_breakpoints[n]>counted_breakpoints[m]){
						deleted_points.add(the_points.get(m));
					}else{
						deleted_points.add(the_points.get(n));
					}
					
				}
			}
			
			//REMOVE IF NOT IN A HIGH-CONTRAST ZONE?
			/*if(contrast_breakpoints[n]<thres){
				if(DEBUG) IJ.log("REMOVING "+clustered_breakpoints[n]+" >> "+contrast_breakpoints[n]);
				deleted_points.add(the_points.get(n));
			}*/
			
			//REMOVE IF OUT OF BOUNDS
			if(clustered_breakpoints[n]<margin_y || clustered_breakpoints[n]>=(channel_height-2*margin_y)){
				deleted_points.add(the_points.get(n));
			}
			
		}
		
		//NOW *REALLY* DELETE POINTS
		for(int i=0; i<deleted_points.size(); i++){
			if(the_points.indexOf(deleted_points.get(i))>=0){
				the_points.remove(the_points.indexOf(deleted_points.get(i)));
				if(DEBUG) IJ.log("DELETING "+deleted_points.get(i));
			}
		}
		
		//CONVERT TO ARRAY AND SORT
		clustered_breakpoints = convertIntegers(the_points);
		Arrays.sort(clustered_breakpoints);
		
		return clustered_breakpoints;
	}
	
	public int[] convertIntegers(ArrayList<Integer> integers){
    	int[] ret = new int[integers.size()];
    	Iterator<Integer> iterator = integers.iterator();
    	for (int i = 0; i < ret.length; i++){
        	ret[i] = iterator.next().intValue();
    	}
    	return ret;
	}

	
	public int [] find_breakpoints(int [] yvalues){
		int [] breakpoints = new int[numPoints];
		
		//Initialise breakpoint
		for(int n=0; n<numPoints; n++){
			breakpoints[n]=(int)(channel_height*(new Random()).nextDouble());
		}
		
		//Now walk breakpoint
		for(int n=0; n<numPoints; n++){
			breakpoints[n]=walk_breakpoint(yvalues, breakpoints[n]);
		}
		return breakpoints;
	}
	
	public int walk_breakpoint(int [] yvalues, int x0){
		int xN=x0;
		for(int n=1; n<numSteps+1; n++){
			double step_size=(step_max+1)/n;
			double direction=0;
			if(xN+step_size<channel_height){
				if(yvalues[xN]<yvalues[(int)(xN+step_size)]){
					direction=1;
				}
			}
			if(xN-step_size>0){
				if(yvalues[xN]<yvalues[(int)(xN-step_size)]){
					direction=-1;
				}
			}
			if(DEBUG) IJ.log(xN+":: "+yvalues[(int)(xN+step_size)]+" < "+yvalues[xN]+" < "+yvalues[(int)(xN-step_size)]+"  step_size="+step_size+" direction="+direction+" --> "+(xN+direction*step_size));
			xN+=(int)(direction*step_size);
		}
		return xN;
	}
	
	
	public void log_breakpoints(int z, int [] breakpoints){
		IJ.log("Breakpoints detected in frame "+z+":");
		for(int n=0; n<breakpoints.length; n++){
			IJ.log("     "+breakpoints[n]);
		}
	}	
	
	/************************ DETECT CHANNELS  ************************/	
	public mmJChannel detect_channel(ImagePlus imp, int id_channel, int x0){
		mmJChannel this_channel=new mmJChannel(id_channel, channel_width, channel_height);
		final ImageProcessor ip = imp.getProcessor();
		ImagePlus target= new ImagePlus();
		
		int region_width=2*channel_width;
		int region_height=imp.getHeight();
		int startX=x0-region_width/2;
		int startY=0;
		
		//if (imp.getType() == ImagePlus.GRAY8) {  
			ImageStack istack = imp.getImageStack();
			int sizeStack = imp.getImageStackSize();
			
			for(int z=1; z<sizeStack+1; z++){
				IJ.showStatus("Detecting channel "+id_channel+" in frame "+z);
				ImageProcessor ipz=istack.getProcessor(z);
				byte[] region_pixels = new byte[region_width*region_height];
			
				int nRows = 0; 
				for (int y = startY; y < startY+region_height; y++){
            		int index = nRows*region_width;
                	int nCols = 0;
                	for (int x = startX; x < startX+region_width; x++){
               			region_pixels[index+nCols] =(byte)ipz.getPixel(x,y);
                		nCols++;
               		}
                nRows++;
           		}
           		//NOW CROP CHANNEL AND ADD TO CHANNEL STACK
           		crop_channel(this_channel, region_pixels, region_width, region_height, channel_width, channel_height, startX, startY);
			}	
		//}else{ //if gray8
		//	IJ.log("ERROR: Convert image to 8-bit");
		//}
		return this_channel;
	}
	
	public byte[] get_pixels_channel(ImagePlus imp, int startX, int startY, int z){
		byte[] pixels = new byte[channel_height*channel_width];
		//if (imp.getType() == ImagePlus.GRAY8) {  
			ImageStack istack = imp.getImageStack();

				ImageProcessor ipz=istack.getProcessor(z);
				
				int nRows = 0; 
				for (int y = startY; y < startY+channel_height; y++){
            		int index = nRows*channel_width;
                	int nCols = 0;
                	for (int x = startX; x < startX+channel_width; x++){
               			pixels[index+nCols] =(byte)ipz.getPixel(x,y);
                		nCols++;
               		}
                nRows++;
           		}
           		
		//}else{ //if gray8
		//	IJ.log("ERROR: Convert image to 8-bit");
		//}
		return pixels;
	}	
	
	public void crop_channel(mmJChannel this_channel, byte[] region_pixels, int region_width, int region_height, int channel_width, int channel_height, int startX, int startY){
		//First find the intensity of every column
		int [] sumLine = new int[region_width];
		for(int x=0; x<region_width; x++){
			sumLine[x]=measureIntensityLine(x, 0, x, region_height, region_pixels, region_width, region_height, true);
		}
		
		//Now a sliding-window of width channel_width
		int best_x=0;
		int prevWindow=0;
		for(int x=0; x<(region_width-channel_width); x++){

			int sumWindow=0;
			for(int xw=x; xw<(x+channel_width); xw++){
					sumWindow+=sumLine[xw];
			}
			if(sumWindow>prevWindow || x==0){
				best_x=x;
			}
			prevWindow=sumWindow;
		}
		
		//Now find the intensity of every row
		int [] sumRow = new int[region_height];
		for(int y=0; y<region_height; y++){
			sumRow[y]=measureIntensityLine(best_x-channel_width/2, y, best_x+channel_width/2, y, region_pixels, region_width, region_height, true);
		}
		
		//Now a sliding-window of width channel_height
		int best_y=0;
		prevWindow=0;
		for(int y=0; y<(region_height-channel_height); y++){
			int sumWindow=0;
			for(int yh=y; yh<(y+channel_height); yh++){
					sumWindow+=sumRow[yh];
			}
			//IJ.log(y+" >>"+sumWindow);
			
			if(sumWindow>prevWindow || y==0){
				best_y=y+margin_y;   
				
			}
			prevWindow=sumWindow;
		}		
		//IJ.log("*"+(best_y-margin_y));
		
		if(best_y+channel_height>region_height){
			best_y=region_height-channel_height;
		}
		
		ByteProcessor proc = new ByteProcessor(region_width, region_height, region_pixels, null);
    	ImagePlus imp_channel = new ImagePlus("Channel", proc); 
		ImageProcessor ip_channel = imp_channel.getProcessor();
		
		Roi channel_roi = new Roi(best_x, best_y, channel_width, channel_height) ;
		imp_channel.setRoi( channel_roi );
		
		ImageProcessor ip_channel_cropped = ip_channel.crop();
		imp_channel.setProcessor(imp_channel.getTitle(), ip_channel_cropped);  
		

		//UPDATES CHANNEL INFO
		this_channel.addSlice(imp_channel, best_x+startX, best_y+startY);
	}
	
	public mmJChannel load_channel(mmJChannel this_channel, ImagePlus imp, int id_channel, int offset_x, int offset_y, int slice ){
	
		final ImageProcessor ip = imp.getProcessor();
		ImagePlus target= new ImagePlus();
		
		//if (imp.getType() == ImagePlus.GRAY8) {  
			ImageStack istack = imp.getImageStack();
			int sizeStack = imp.getImageStackSize();
			
			int z=slice+1;
			
				IJ.showStatus("Loading channel "+id_channel+" in frame "+z);
				ImageProcessor ipz=istack.getProcessor(z);
				byte[] channel_pixels = new byte[channel_width*channel_height];
			
				int nRows = 0; 
				for (int y = offset_y; y < offset_y+channel_height; y++){
            		int index = nRows*channel_width;
                	int nCols = 0;
                	for (int x = offset_x; x < offset_x+channel_width; x++){
               			channel_pixels[index+nCols] =(byte)ipz.getPixel(x,y); 
                		nCols++;
               		}
                nRows++;
           		}
           		//NOW ADD TO CHANNEL STACK
           		ByteProcessor proc = new ByteProcessor(channel_width, channel_height, channel_pixels, null);
    			ImagePlus imp_channel = new ImagePlus("Channel", proc); 
				ImageProcessor ip_channel = imp_channel.getProcessor();
		
				//UPDATES CHANNEL INFO
				this_channel.addSlice(imp_channel, offset_x, offset_y);	
		//}else{ //if gray8
		//	IJ.log("ERROR: Convert image to 8-bit");
		//}
		
		return this_channel;
	}
	
	/************************ AUXILIARY FUNCTIONS  ************************/
	public int measureIntensityLine(int startX, int startY, int endX, int endY, byte[] region_pixels, int region_width, int region_height, boolean inBinary){
		//RE-ORDER IF NECESSARY
		if(startX>endX){
			int tmpX=endX;
			endX=startX;
			startX=tmpX;
		}
		if(startY>endY){
			int tmpY=endY;
			endY=startY;
			startY=tmpY;
		}
		
		ByteProcessor proc = new ByteProcessor(region_width, region_height, region_pixels, null);
    	ImagePlus imp_region = new ImagePlus("Region", proc); 
		
		if(inBinary){
			imp_region=createBinary(imp_region, true);
		}
		ImageProcessor ip_region = imp_region.getProcessor();
		
       	int sum=0;
       	double [] iLine=ip_region.getLine((double)startX, (double)startY,(double)endX, (double)endY);
       	for(int n=1; n<iLine.length; n++){
       		sum+=(int)iLine[n];
       	}
       	imp_region.flush();  
       	return sum;
	}
	
	private ImagePlus createBinary(ImagePlus img, boolean scale) {
        BinaryProcessor proc = new BinaryProcessor(new ByteProcessor(img.getImage()));
        proc.autoThreshold();
       
        return new ImagePlus(img.getTitle(), proc);
    }



}





