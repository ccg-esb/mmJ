import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.IJ;
import java.io.*;
import java.util.ArrayList;
import java.util.*;
import java.awt.*;
import ij.measure.ResultsTable; 
import java.awt.image.BufferedImage;
import ij.measure.*;
/**

*/
public class mmJChannel{

	boolean DEBUG=false;
	
	/***********************  Drawing parameters */
	int box_width=2;
	int margin_y=0;

	/***********************  Parameters */
	int max_levels=0;  //0: Mothers, -1: ALL  (IT DOESNT WORK FOR >1!!!!!)

	int id_channel;
	int w;
	int h;
	int numSlices=0;
	ImageStack istack;
	final ArrayList<Object> offset_x = new ArrayList<Object>();
	final ArrayList<Object> offset_y = new ArrayList<Object>();
	
	ArrayList<mmJBreakpoints> breakpoints;
	ArrayList<mmJCell> cells;
	
	mmJAnalyzer analyzer = new mmJAnalyzer();


	/***********************  Constructors */
	public mmJChannel() {
		id_channel=0;
		w=0;
		h=0;
		istack=new ImageStack();
		breakpoints = new ArrayList<mmJBreakpoints>();
		cells = new ArrayList<mmJCell>();
	}

	public mmJChannel(int id_channel){
		this.id_channel=id_channel;
		this.w=0;
		this.h=0;
		istack=new ImageStack();
		breakpoints = new ArrayList<mmJBreakpoints>();
		cells = new ArrayList<mmJCell>();
	}

	public mmJChannel(int id_channel, int w, int h){
		this.id_channel=id_channel;
		this.w=w;
		this.h=h;
		istack=new ImageStack(w, h);
		breakpoints = new ArrayList<mmJBreakpoints>();
		cells = new ArrayList<mmJCell>();
	}
	
		
	
	/************* BREAKPOINTS */
	
	public int [] get_breakpoints(int slice){
		mmJBreakpoints bPoint=breakpoints.get(slice-1);
		return bPoint.get_breakpoints();
	}
	
	public int get_near_breakpoint(int slice, int x0, int delta){
		mmJBreakpoints bPoint=breakpoints.get(slice-1);
		return bPoint.get_near_breakpoint(x0, delta);
	}
	
	public void set_breakpoints(int slice, int [] iPoints){
		mmJBreakpoints bPoint = new mmJBreakpoints(iPoints);
		if(slice<breakpoints.size()){
			breakpoints.set(slice-1, bPoint);
		}else{
			breakpoints.add(slice-1, bPoint);
		}
	}

	public void remove_breakpoint(int slice, int iPoint){
		mmJBreakpoints bPoint=breakpoints.get(slice-1);
		bPoint.remove_breakpoint(iPoint);
	}


	public void remove_all_breakpoints(int slice){
		mmJBreakpoints bPoint=breakpoints.get(slice-1);
		bPoint.remove_all_breakpoints();
	}
	
	public void remove_all_breakpoints_hereafter(int this_slice){
		for(int slice=this_slice; slice<=istack.getSize(); slice++){
			//IJ.log("Deleting breakpoints frame "+slice);
			mmJBreakpoints bPoint=breakpoints.get(slice-1);
			bPoint.remove_all_breakpoints();
		}
	}
	

	public void log_breakpoints(int slice){
		mmJBreakpoints bPoint=breakpoints.get(slice-1);
		bPoint.log();
	}
	
	public void log_all_breakpoints(){
		Iterator<mmJBreakpoints> ib=breakpoints.iterator();
		int i=1;
        while(ib.hasNext()) {
				mmJBreakpoints bPoint=ib.next();
				if(bPoint!=null){
					IJ.log("   Frame "+i+" "+bPoint.toString());
					i++;
				}
		}
	}
	
	public void log_channel(){
		IJ.log(toString());
		IJ.log("\n   Breakpoints:");
		log_all_breakpoints();
	}
	
	public void add_breakpoint(int slice, int iPoint){
		mmJBreakpoints bPoint=breakpoints.get(slice-1);
		bPoint.add_breakpoint(iPoint);
	}
	
	public void add_breakpoints(int slice, int [] iPoints){
		mmJBreakpoints bPoint=breakpoints.get(slice-1);
		bPoint.add_breakpoints(iPoints);
	}	
	
	public void move_breakpoint(int slice, int yfrom, int yto){
		mmJBreakpoints bPoint=breakpoints.get(slice-1);
		bPoint.move_breakpoint(yfrom, yto);
	}
	


	/************* Drawing functions */
	
	public void drawChannel(ImagePlus imp_original){
	
		ImageStack istack_original = imp_original.getImageStack();
		int sizeStack = imp_original.getImageStackSize();
		for(int z=1; z<sizeStack+1; z++){
			int [] offset = getOffset(z);
			ImageProcessor ipz=istack_original.getProcessor(z);
			if(offset[0]>=0 && offset[1]>=0){
				//RECTANGULAR CONTOUR
				Roi roi = new Roi(offset[0]+box_width/2, offset[1]+box_width/2, w-box_width, h-box_width);
				ipz.setRoi(roi);
				ipz.setLineWidth(box_width);
				ipz.setColor(new Color(255,255,150));
				//ipz.setValue(255);  
				ipz.draw(roi);  
			}
		}
		imp_original.updateAndDraw();
	}
	
	public void drawChannel(ImagePlus imp_original, int z){
		ImageStack istack_original = imp_original.getImageStack();
		int sizeStack = imp_original.getImageStackSize();
			int [] offset = getOffset(z);
			ImageProcessor ipz=istack_original.getProcessor(z);
			
			
			
			if(offset[0]>0 && offset[1]>0){
				//RECTANGULAR CONTOUR
				Roi roi = new Roi(offset[0]+box_width/2, offset[1]+box_width/2, w-box_width, h-box_width);
				ipz.setRoi(roi);
				ipz.setLineWidth(box_width);
				//ipz.setValue(255); 
				ipz.setColor(new Color(255,255,150));
				ipz.draw(roi);  
			}
		imp_original.updateAndDraw();
	}
	
	public void undrawChannels(ImagePlus imp_original){
		for(int z=1; z<istack.getSize()+1; z++){
			undrawChannel(imp_original, z);
		}
	}
	
	public void undrawChannel(ImagePlus imp_original, int z){
		ImageStack istack_original = imp_original.getImageStack();
		
		int sizeStack = imp_original.getImageStackSize();
		
			int [] offset = getOffset(z);
			
			//IJ.log(">"+offset[0]+" "+offset[1]);
			
			ImageProcessor ipz_original=istack_original.getProcessor(z);
			if(offset[0]>0 && offset[1]>0){

				ImageProcessor ipz_channel=istack.getProcessor(z);
				int nRows = 0; 
				for (int y = 0; y < h; y++){
            		int index = nRows*w;
                	int nCols = 0;
                	for (int x = 0; x < w; x++){
                		ipz_original.putPixelValue(x+offset[0], y+offset[1]+margin_y, ipz_channel.getPixelValue(x,y));
               			nCols++;
               		}
                nRows++;
           		}
			}
			
		imp_original.updateAndDraw();
	}	
	
	public void draw_breakpoints(ImagePlus imp_original, int slice, int channel_width){
		ImageStack istack_original = imp_original.getImageStack();
		ImageProcessor ipz=istack_original.getProcessor(slice);
		if(slice<=istack_original.getSize()){
			mmJBreakpoints bPoint=breakpoints.get(slice-1);
			bPoint.draw_breakpoints(ipz, ((Integer)offset_x.get(slice-1)).intValue(), ((Integer)offset_y.get(slice-1)).intValue(), channel_width);
		}
		imp_original.show();
	}
	
	public void draw_all_breakpoints(ImagePlus imp_original, int channel_width){
		ImageStack istack_original = imp_original.getImageStack();
		for(int slice=1; slice<=istack_original.getSize(); slice++){
			ImageProcessor ipz=istack_original.getProcessor(slice);
			mmJBreakpoints bPoint=breakpoints.get(slice-1);
			bPoint.draw_breakpoints(ipz, ((Integer)offset_x.get(slice-1)).intValue(), ((Integer)offset_y.get(slice-1)).intValue(), channel_width);
		}
	}
	
	public void draw_cells(ImagePlus imp){
	
		Iterator<mmJCell> icell=cells.iterator();
		while(icell.hasNext()) {
			mmJCell this_cell=icell.next();
			if(this_cell!=null){
				this_cell.draw_cell(imp, offset_x, offset_y);
			}
		}
		
	}
	
	/************* Auxiliary functions */
	public String toString(){
		String ret ="*** Channel: "+id_channel+"\n   numFrames="+numSlices+"\n   w="+w+"\n   h="+h;
		
		ret+="\n   offset_x= ";
		Iterator<Object> ix=offset_x.iterator();
        while(ix.hasNext()) {		
       		Integer off_x=(Integer)ix.next();
       		if(ix!=null){
	       		ret+=" "+off_x.intValue();
       		}
        }
        
        ret+="\n   offset_y= ";
        Iterator<Object> iy=offset_y.iterator();
        while(iy.hasNext()) {		
       		Integer off_y=(Integer)iy.next();
       		if(iy!=null){
	       		ret+=" "+off_y.intValue();
       		}
        }
		return ret;
	}
	
	public void addSlice(ImagePlus imp_slice, int best_x, int best_y){
		  numSlices++;
	
	      ImageProcessor ip_slice = imp_slice.getProcessor();  
          istack.addSlice("slice "+numSlices, ip_slice); 
		  //imp_slice.show();
		  
		  offset_x.add(new Integer(best_x));
		  offset_y.add(new Integer(best_y));
	}
	
	public void setSlice(int z, byte[] pixels){
		/*
		   ByteProcessor proc = new ByteProcessor(w, h, pixels, null);
		   ImagePlus imp_slice = new ImagePlus("Cell", proc); 
		   //imp_slice.show();
		   ImageProcessor ip_slice = imp_slice.getProcessor();
		*/
		istack.setPixels(pixels, z); 
	}
	
	/************* Import/Export */
	public void export_channel(ImagePlus imp0, String dir_channel, boolean gfp){
		dir_channel=dir_channel+"channels";
		String str=new String();
		//if(gfp){
			str=imp0.getTitle().substring(0,imp0.getTitle().indexOf("."))+"_";
		//}else{
		//	str="phase_";
		//}
	
		ImagePlus imp = new ImagePlus("Channel "+id_channel, istack);
		(new File(dir_channel)).mkdir();
       	IJ.saveAs(imp, "tif", dir_channel+"/"+str+id_channel+".tif"); 
		if(DEBUG) IJ.log("Saved in "+dir_channel+"/"+str+id_channel+".tif");
		
		//imp.show();
	}
	
	public void export_cells(mmJCell this_cell, ImagePlus imp_original, String dir_channel, boolean gfp){
		
		int box_height=5;
		int box_gap=1;  //HERE*************
		
			(new File(dir_channel+"cells/")).mkdir();
			
			if(this_cell==null){
				Iterator<mmJCell> icell=cells.iterator();
				this_cell=icell.next();
			}
			int sizeStack = imp_original.getImageStackSize();
				
				if(this_cell!=null){
				
					int frameBorn=this_cell.get_frame_born();
					if(DEBUG) IJ.log("Exporting "+toBinary((long)this_cell.get_id_cell())+" id_cell: "+this_cell.get_id_cell()+", Born at "+frameBorn);
				
					ImageStack stack_cell = new ImageStack(w, h);
					double [] intensities_cell = new double[istack.getSize()];
					if(gfp){
						intensities_cell=this_cell.get_intensities();
					}
					
					for(int slice=1; slice<=istack.getSize(); slice++){
						ImageProcessor ipz=istack.getProcessor(slice);
						
						int y2=this_cell.get_y1(slice-1);
						int y1=this_cell.get_y2(slice-1); 	
						if(DEBUG) IJ.log(slice+"\t y1: "+y1+" y2:"+y2);				
						
						if(y1>=0 && y2>=0){
							
						   byte[] cell_pixels = new byte[w*h];
						   int nRows = 10; 
						   for (int y = 0; y < y1; y++){
								byte this_color=(byte)0;
								
								//IN REAL LOCATION
								//int index = nRows*w;
								
								//SHIFTED TO ZERO
								int index = nRows*w;
								
								if(y>=y2){  //Copy pixels cell : CELL
									 
									 int nCols = 0;
									 for (int x = 0; x < w; x++){
										 cell_pixels[index+nCols] =(byte)(ipz.getPixel(x,y));  
										 
										 nCols++;
									 }
									 nRows++;
								}	 
					   	   }
					   	   
						   //NOW ADD MOTHER TO STACK
						  ByteProcessor proc = new ByteProcessor(w, h, cell_pixels, null);
						  ImagePlus imp_cell = new ImagePlus("Cell", proc); 
						  ImageProcessor ip_cell = imp_cell.getProcessor();
						  
						  if(gfp && intensities_cell.length>=slice){
						  
						  	byte this_color =(byte)(255*(intensities_cell[slice-1]-15)/95); //Is normalized
						  	ip_cell.setRoi(0, box_gap, w, box_height);
						  	ip_cell.setColor(Math.abs(this_color));
            			  	ip_cell.fill();
            			  	
            			  	ip_cell.setRoi(0, 0, w, 1);
            			  	ip_cell.setColor(Color.white);
            			  	ip_cell.fill();
            			  }
						  
						  
						  //Labels
						  if((slice-1)==frameBorn && this_cell.get_id_cell()>1){
						  	 
						  	 ip_cell.setColor(new Color(255,255,255));  
							
							/*
							 if(this_cell.get_up()){
							   ip_cell.drawString(toBinary((long)this_cell.get_id_cell()),(int)(box_gap/2),100);
							 }else{
							   ip_cell.drawString(toBinary((long)this_cell.get_id_cell()),(int)(box_gap/2),100);
							 }
							*/
							   ip_cell.drawString(""+slice,(int)(box_gap/2),100);
							
							
						  }						  
						  
						  
						  /*
						  if((slice-1)==frameBorn && this_cell.get_id_cell()>1){
						  	
						  	ip_cell.setRoi(0, 0, 1, h);
            			  	ip_cell.setColor(Color.white);
            			  	ip_cell.fill();
						  }
						  */
						  
						  stack_cell.addSlice("Cell "+id_channel,ip_cell);	
					   
					   }else{
					   
						   byte[] cell_pixels = new byte[w*h];
						   for(int c=0; c<cell_pixels.length; c++){
							   cell_pixels[c] =(byte)0;
						   }
						   //NOW ADD CELL TO STACK
						  ByteProcessor proc = new ByteProcessor(w, h, cell_pixels, null);
						  ImagePlus imp_cell = new ImagePlus("Cell", proc); 
						  ImageProcessor ip_cell = imp_cell.getProcessor();
						  stack_cell.addSlice("Cell "+id_channel,ip_cell);	
					   }
					   
					}
					ImagePlus imp = new ImagePlus("Cell "+id_channel, stack_cell);
					String str="";
					if(gfp){
						str="gfp_";
					}else{
						str="phase_";
					}
					
					
					int rows=1;
					int columns=sizeStack;
					int first=1;
					int last=sizeStack;
					int scale=1;
					int inc=1;
					ImagePlus imp_montage=makeMontage2(imp, columns, rows, scale, first, last, inc);
					String frame0=""+(this_cell.get_frame_born()+1);
					for(int f0=frame0.length(); f0<3; f0++){
						frame0="0"+frame0;
					}
					String fileName=dir_channel+"cells"+"/"+str+id_channel+"-"+frame0+"-"+(this_cell.get_id_cell());
       				
       				IJ.saveAs(imp_montage, "tif", fileName+".tif"); 
					if(DEBUG) IJ.log("Saved in "+fileName+".tif");
					
					//RECURSIVE
					ArrayList<mmJCell> cells_daughters=this_cell.get_daughters();
					Iterator<mmJCell> icell_daughters=cells_daughters.iterator();
					while(icell_daughters.hasNext()) {
						mmJCell this_cell_daughters=icell_daughters.next();
						
						int this_level=this_cell_daughters.get_level();
						if(this_level<=max_levels || max_levels<0){ //JUST ONE LEVEL
							
							export_cells(this_cell_daughters, imp_original, dir_channel, gfp); 
						}
					}
					
				}
			
			
	}
	
	public ImagePlus cropAndResize(ImagePlus imp, int x1, int x2, int y1, int y2){
		int wcrop=Math.abs(x1-x2);
		int hcrop=Math.abs(y1-y2);
	
    	ImageProcessor ip = imp.getProcessor();
    	ip.setRoi(x1, y1, wcrop, hcrop);
    	ip = ip.crop();
    	BufferedImage croppedImage = ip.getBufferedImage();

    	ImagePlus imp_cropped=new ImagePlus("croppedImage", croppedImage);

		return imp_cropped;
    
  }
	
	/***********************  Gets/Sets */
	public int get_id(){
		return id_channel;
	}

	public void set_id(int id_channel){
		this.id_channel=id_channel;
	}

	public int get_w(){
		return w;
	}

	public void set_w(int w){
		this.w=w;
	}

	public int get_h(){
		return h;
	}

	public void set_h(int h){
		this.h=h;
	}
	
	public int get_num_slices(){
		return istack.getSize();
	}
	
	public void setStack(ImageStack istack){
		this.istack=istack;
	}
	
	public ImageStack getImageStack(){
		return istack;
	}
	
	public ImagePlus getImagePlus(){
		ImagePlus imp = new ImagePlus("Channel "+id_channel, istack);
		return imp;
	}
	
	public int[] getOffset(int frame){
		int offset [] = new int[2];
		if(frame<=offset_x.size() && frame<=offset_y.size()){
			offset[0]=((Integer)offset_x.get(frame-1)).intValue();
			offset[1]=((Integer)offset_y.get(frame-1)).intValue();
		}
	    return offset;
	}
	
	public void setOffset(int frame, int [] offs){
		offset_x.set(frame-1, (new Integer(offs[0])));
		offset_y.set(frame-1, (new Integer(offs[1])));
	}	
	
	public Vector<Point> getListOffsets(){
		Vector<Point> listOffsets = new Vector<Point>(0, 16);
		for(int z=0; z<istack.getSize(); z++){
			int [] offset = getOffset(z+1);
			Point p = new Point(offset[0], offset[1]);
			listOffsets.addElement(p);
		}
		return listOffsets;
	}
	
	public Vector<Integer> getListBreakpoints(int slice){
		Vector<Integer> listBreakpoints = new Vector<Integer>(0, 16);
		int [] this_breakpoints=get_breakpoints(slice+1);
		for(int i=0; i<this_breakpoints.length; i++){
			listBreakpoints.addElement(this_breakpoints[i]);
		}
		return listBreakpoints;
	}
	
	public boolean contains(int x, int y, int frame){
		boolean contained = false;
		int [] offset = getOffset(frame+1);
		
		if(DEBUG) IJ.log("mmJChannel ("+x+","+y+") frame:"+(frame+1)+" offsets:("+offset[0]+","+offset[1]+")");
		Roi roi = new Roi(0, 0, w, h);
		
		if(roi.contains(x-offset[0],y-offset[1])){
			if(DEBUG) IJ.log("Is here! "+id_channel);
			contained=true;
		}
		
		
		return contained;
	}
	
	
	public boolean close(int x, int y, int frame){
		boolean close = false;
		int [] offset = getOffset(frame+1);
		
		if(DEBUG) IJ.log("mmJChannel ("+x+","+y+") frame:"+(frame+1)+" offsets:("+offset[0]+","+offset[1]+")");
		if(Math.abs(offset[0]-x)<50){
			
			close=true;
		}
		return close;
	}	
		
	/************* Track */
	
	public void trackChannel(){
	
		int fromSlice=0;  //param?
		cells = new ArrayList<mmJCell>(); //Re-do everything
		int id_cell=id_channel+1;
		mmJCell mother=new mmJCell(id_cell, w, h);
		mother.set_level(0);
		mother.set_breakpoints(breakpoints);
		mother.track(max_levels);
		
		cells.add(mother);
		
		ArrayList<mmJCell> daughters= mother.get_daughters();
		
	}
	
	public void log_cells(){
		Iterator<mmJCell> icell=cells.iterator();
		while(icell.hasNext()) {
			mmJCell this_cell=icell.next();
			if(this_cell!=null){
				this_cell.log_cell();
			}
		}
	}	
	
	
	public void determine_intensity(ImagePlus imp_original){
			ImageStack istack_original = imp_original.getImageStack();
			Iterator<mmJCell> icell=cells.iterator();
			while(icell.hasNext()) {
				mmJCell this_cell=icell.next();
				if(this_cell!=null){
					this_cell.determine_intensity(offset_x, offset_y, analyzer, imp_original);
				}
			}
	}
	
	public void results_data(ResultsTable results_table){
			Iterator<mmJCell> icell=cells.iterator();
			while(icell.hasNext()) {
			
				mmJCell this_cell=icell.next();
				if(this_cell!=null){
					this_cell.export_data(max_levels, results_table);
				}
			}
			
	}
	
	public double min_intensity(){
			double min=255;
	
			Iterator<mmJCell> icell=cells.iterator();
			while(icell.hasNext()) {
				mmJCell this_cell=icell.next();
				if(this_cell!=null){
					double this_min=this_cell.min_intensity();
					if(min>this_min){
						min=this_min;
					}
				
				}
			}
			if(DEBUG) IJ.log(id_channel+" min:"+min);
			return min;
	}
	
	public double max_intensity(){
			double max=0;
	
			Iterator<mmJCell> icell=cells.iterator();
			while(icell.hasNext()) {
				mmJCell this_cell=icell.next();
				if(this_cell!=null){
					double this_max=this_cell.max_intensity();
					if(max<this_max){
						max=this_max;
					}
				
				}
			}
			if(DEBUG) IJ.log(id_channel+" max:"+max);
			return max;
	}	
	
	
	public void normalize_intensities(double this_min, double this_max){
			Iterator<mmJCell> icell=cells.iterator();
			while(icell.hasNext()) {
				mmJCell this_cell=icell.next();
				if(this_cell!=null){
					this_cell.normalize_intensities(this_min, this_max);
				}
			}
	}
	
	/*************** Correct */
	
	public void refine_segmentation(){
			mmJDetector detector = new mmJDetector(w, h);
	
			Iterator<mmJCell> icell=cells.iterator();
			while(icell.hasNext()) {
				mmJCell this_cell=icell.next();
				if(this_cell!=null){
				
					for(int z=0; z<this_cell.get_numFrames(); z++){
					
					
					  ArrayList<mmJBreakpoints> old_breakpoints=this_cell.get_breakpoints();
					  mmJBreakpoints this_breakpoints=(old_breakpoints.get(z));
					
					  
					  //CHECK DIVISION
					  int division_breakpoint=this_cell.find_division_breakpoints(z);
					  if(division_breakpoint>0 && z>0){
						  //Now find the missing breakpoint
						  ImageProcessor ipz=istack.getProcessor(z);  //Previous frame
						  int found_breakpoint=detector.find_missing_breakpoint(ipz, division_breakpoint);
						  if(found_breakpoint>0){
						  	mmJBreakpoints previous_breakpoints=(old_breakpoints.get(z-1));
						  
						  	previous_breakpoints.add_breakpoint(found_breakpoint);
						  	if(DEBUG) IJ.log("Cell "+this_cell.get_id_cell()+" actually divided at frame "+(z)+" in "+found_breakpoint);
						  
						  	//Re-track
						  	trackChannel();
						  	
						 	 //Recursively
						  	//refine_segmentation();
						  	
						  }
					  }
					  
					  
					}
					
				}
			}
	}
	
	
	public void correct_segmentation(){
		
			mmJDetector detector = new mmJDetector(w, h);
	
			Iterator<mmJCell> icell=cells.iterator();
			while(icell.hasNext()) {
			
				mmJCell this_cell=icell.next();
				//this_cell.log_cell();
				 
				if(this_cell!=null){
				
				
					for(int z=0; z<this_cell.get_numFrames(); z++){
					
					  ArrayList<mmJBreakpoints> old_breakpoints=this_cell.get_breakpoints();
					  mmJBreakpoints this_breakpoints=(old_breakpoints.get(z));
					
					  //EXTRA BREAKPOINTS 
					  /*
					  int extra_breakpoint=this_cell.find_extra_breakpoints(z);
					  if(extra_breakpoint>0){
					  		IJ.log("Cell "+this_cell.get_id_cell()+" had an extra breakpoint: "+extra_breakpoint+" in frame "+z);
					  		this_breakpoints.remove_breakpoint(extra_breakpoint);
					  		
					  		//Re-track
						  	trackChannel();
					  }
					  */
					  /*
					  //REPEATED BREAKPOINTS 
					  int repeated_breakpoint=this_cell.find_repeated_breakpoints(z);
					  if(repeated_breakpoint>0){
					  		IJ.log("Cell "+this_cell.get_id_cell()+" had a repeated breakpoint: "+extra_breakpoint+" in frame "+z);
					  		this_breakpoints.remove_breakpoint(repeated_breakpoint);
					  		
					  		//Re-track
						  	trackChannel();
					  }
					  */
				
					  //MISSED BREAKPOINTS
					  int missed_breakpoint=this_cell.find_missed_breakpoints(z);
					 
					  if(missed_breakpoint>0){
						  //Now find the missing breakpoint
						  ImageProcessor ipz=istack.getProcessor(z+1);
						  int found_breakpoint=detector.find_missing_breakpoint(ipz, missed_breakpoint);
						  if(found_breakpoint>0){
						  	this_breakpoints.add_breakpoint(found_breakpoint);
						  	if(DEBUG) IJ.log("Cell "+this_cell.get_id_cell()+" missed a breakpoint near "+missed_breakpoint+" in frame "+z+" --> found it in "+found_breakpoint);
						  	
						  
						  	//Re-track
						  	trackChannel();
						  
						 	 //Recursively
						  	//correct_segmentation();
						  	
						  }
					  }
					 
					  
					  
					}
					
				}
			}
	}

public String toBinary(long longNumber) {
    StringBuilder builder = new StringBuilder();
    long temp = 0l;
    while (longNumber>0) {
        temp = longNumber;
        longNumber = (temp>>1);
        builder.append(temp%2);
    }
    String ret =builder.reverse().toString();
    if(ret.length()==0){
    	ret="0";
    }
    return ret;
}
	
	
/** Creates a montage and returns it as an ImagePlus. */
public ImagePlus makeMontage2(ImagePlus imp, int columns, int rows, double scale, int first, int last, int inc) {
            int stackWidth = imp.getWidth();
            int stackHeight = imp.getHeight();
            int nSlices = imp.getStackSize();
            int width = (int)(stackWidth*scale);
            int height = (int)(stackHeight*scale);
            int montageWidth = width*columns;
            int montageHeight = height*rows;
            ImageProcessor ip = imp.getProcessor();
            ImageProcessor montage = ip.createProcessor(montageWidth, montageHeight);
            Color fgColor=Color.white;
            Color bgColor = Color.black;
           
                  boolean whiteBackground = false;
                  if ((ip instanceof ByteProcessor) || (ip instanceof ColorProcessor)) {
                        ip.setRoi(0, stackHeight-12, stackWidth, 12);
                        ImageStatistics stats = ImageStatistics.getStatistics(ip, Measurements.MODE, null);
                        ip.resetRoi();
                        whiteBackground = stats.mode>=200;
                        if (imp.isInvertedLut())
                              whiteBackground = !whiteBackground;
                  }
                  if (whiteBackground) {
                        fgColor=Color.black;
                        bgColor = Color.white;
                  }
            
            montage.setColor(bgColor);
            montage.fill();
            montage.setColor(fgColor);
            Dimension screen = IJ.getScreenSize();
            ImageStack stack = imp.getStack();
            int x = 0;
            int y = 0;
            ImageProcessor aSlice;
          int slice = first;
            while (slice<=last) {
                  aSlice = stack.getProcessor(slice);
                  if (scale!=1.0)
                        aSlice = aSlice.resize(width, height);
                  montage.insert(aSlice, x, y);
                  String label = stack.getShortSliceLabel(slice);
                  x += width;
                  if (x>=montageWidth) {
                        x = 0;
                        y += height;
                        if (y>=montageHeight)
                              break;
                  }
                  IJ.showProgress((double)(slice-first)/(last-first));
                  slice += inc;
            }
        
            IJ.showProgress(1.0);
            ImagePlus imp2 = new ImagePlus("Montage", montage);
            imp2.setCalibration(imp.getCalibration());
            Calibration cal = imp2.getCalibration();
            if (cal.scaled()) {
                  cal.pixelWidth /= scale;
                  cal.pixelHeight /= scale;
            }
        imp2.setProperty("Info", "xMontage="+columns+"\nyMontage="+rows+"\n");
            return imp2;
      }
	
	
}

