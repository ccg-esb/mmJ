
import java.awt.*;
import java.awt.event.*;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.IJ;
import ij.measure.ResultsTable; 
import java.io.*;
import java.util.*;
import java.awt.image.ColorModel;
import java.util.ArrayList;

/**

*/
public class mmJHandler {

	/*********************  PARAMS   ************************/
	
	int channel_width=20;  //Default parameters
	int channel_height=440;
	
	
	/*********************  VARIABLES   ************************/
	
	boolean DEBUG=false;
	int previousID;
	String dirName; 
	
	/*********************  CONSTRUCTORS   ************************/
	mmJDetector detector = new mmJDetector(channel_width, channel_height);
	final ArrayList<mmJChannel> mChannels = new ArrayList<mmJChannel>();	
	ResultsTable results_table = new ResultsTable();
	
	public mmJHandler(){
		if(DEBUG) IJ.log("mmJ_v2.4");
		dirName="./";
	}
	
	public mmJHandler(String dirName){
		this.dirName=dirName;
		if(DEBUG) IJ.log("mmJ_v2.4");
		if(DEBUG) IJ.log(">> "+dirName);
	}
	
	/************************ CHANNEL ************************/	
	public void setParams(int wchannel, int hchannel){
		channel_width=wchannel;
		channel_height=hchannel;
		detector = new mmJDetector(channel_width, channel_height);
		if(DEBUG) IJ.log(">> Setting parameters W:"+channel_width+" H:"+channel_height);
		
	}
	
	public int [] getParams(){
		int [] params=new int[2];
		params[0]=channel_width;
		params[1]=channel_height;
		return params;
	}

	
	public void load_channel(ImagePlus imp, int id_channel){
		
		mmJChannel this_channel=get_channel(id_channel);
		if(this_channel==null){ 
			if(DEBUG) IJ.log(">> Loading channel "+id_channel);
			this_channel=new mmJChannel(id_channel, channel_width, channel_height);
			mChannels.add(this_channel);
		}
		
	}
	
	public void save_channels(String path){
		if(DEBUG) IJ.log(">> Saving segmentation in "+path);
		try {
			final FileWriter fw = new FileWriter(path);
			
			Point p;
			String n;
			String x;
			String y;
			String z;
			String c;
			String id;
			String bp;
			fw.write("channel x y slice color ID Breakpoints\n");
			
			//For each channel
			Iterator<mmJChannel> itc=mChannels.iterator();
        	while(itc.hasNext()) {		
       			mmJChannel this_channel=(mmJChannel)itc.next();
       			if(this_channel!=null){
       				
       				//For each frame
       				for(int s=0; s<this_channel.get_num_slices(); s++){
       					int id_channel=this_channel.get_id();
       					
       					//id_channel
       					n = "" + id_channel;
						//while (n.length() < 5) {
						//	n = "" + n;
						//}
						
						//(offset_x,offset_y) 
						Vector<Point> listOffsets = getOffsets(id_channel);
						p = listOffsets.elementAt(s);
						x = "" + p.x;
						//while (x.length() < 5) {
						//	x = "" + x;
						//}
						y = "" + p.y;
						//while (y.length() < 5) {
						//	y = "" + y;
						//}
						
						
						
						//Slice
						z = "" + (s + 1);
						//while (z.length() < 5) {
						//	z = "" + z;
						//}
						/*
						//Diameter
						Vector<Integer> listDiameters = getDiameter(id_channel);
						int this_diameter = ((Integer)listDiameters.elementAt(s)).intValue();
						c = "" + this_diameter; //TMP
						//while (c.length() < 5) {
						//	c = " " + c;
						//}
						*/
						//Color
						c = "" + 0; //TMP
						//while (c.length() < 5) {
						//	c = " " + c;
						//}
						
						//Id?
						id = "" + n;  //TMP
						//while (id.length() < 5) {
						//	id = " " + id;
						//}
						
						//Breakpoints
						bp="";
						Vector<Integer> listBreakpoints = getBreakpoints(id_channel,s);
						int prev_breakpoint=0;
						for (int numBP = 0; (numBP < listBreakpoints.size()); numBP++) {
							int this_breakpoint=((Integer)listBreakpoints.elementAt(numBP)).intValue();
							if(this_breakpoint!=prev_breakpoint){
								String this_bp = " " + listBreakpoints.elementAt(numBP);
								//while (this_bp.length() < 5) {
								//	this_bp = " " + this_bp;
								//}
								bp+=this_bp;
								prev_breakpoint=this_breakpoint;
							}
						}
						if(DEBUG) IJ.log("Saving: "+n + " " + x + " " + y + " " + z	+ " " + c + " " + id + " " + bp +"\n");
						fw.write(n + " " + x + " " + y + " "+ z + " " + c + " " + id + "" + bp +"\n");
       				}
       				
       			}
       		}
			fw.close();
			
		} catch (IOException e) {
			IJ.error("IOException exception");
		} catch (SecurityException e) {
			IJ.error("Security exception");
		}
	}
	
	public void set_offset_channel(ImagePlus imp, int id_channel, int offset_x, int offset_y, int slice){
		if(DEBUG) IJ.log(">> Defining offset channel "+id_channel+" as ("+offset_x+","+offset_y+") at slice "+slice);
		mmJChannel this_channel=get_channel(id_channel);
		if(this_channel!=null){ 
			this_channel=detector.load_channel(this_channel, imp, id_channel, offset_x, offset_y, slice);
		}
		
	}	
	
	public void detect_channel(ImagePlus imp, int id_channel, int x0){
		if(DEBUG) IJ.log(">> Detecting channel "+id_channel+" near x="+x0+" (W:"+channel_width+" H:"+channel_height+")");
		//First detect channel
		mmJChannel this_channel=detector.detect_channel(imp, id_channel, x0);
		mChannels.add(this_channel);	
		detector.detect_breakpoints(this_channel);
		
		if(DEBUG) IJ.log(this_channel.toString());
		if(DEBUG) this_channel.log_all_breakpoints();
	}
	

	public mmJChannel get_channel(int id_channel){
		Iterator<mmJChannel> itc=mChannels.iterator();
        while(itc.hasNext()) {		
       		mmJChannel mchannel=(mmJChannel)itc.next();
       		if(mchannel!=null){
	       		if(id_channel==mchannel.get_id()){
	       			return mchannel;	
	       		}
       		}
        }
        return null;
	}
	
	
	public int in_channel(int x, int y, int frame){
		Iterator<mmJChannel> itc=mChannels.iterator();
		int i=0;
        while(itc.hasNext()) {		
       		mmJChannel mchannel=(mmJChannel)itc.next();
       		if(mchannel!=null){
       			boolean isHere=mchannel.contains(x,y,frame);
       			if(DEBUG) IJ.log("Checking if ("+x+","+y+") frame:"+frame+" is in channel "+i+" -> "+isHere);
	       		if(isHere){
	       			return i;	
	       		}
	       		
       		}
       		i++;
        }
		return -1;
	}
	
	public int close_channel(int x, int y, int frame){
		Iterator<mmJChannel> itc=mChannels.iterator();
		int i=0;
        while(itc.hasNext()) {		
       		mmJChannel mchannel=(mmJChannel)itc.next();
       		if(mchannel!=null){
       			boolean isHere=mchannel.close(x,y,frame);
       			
       			if(DEBUG) IJ.log("Checking if ("+x+","+y+") frame:"+frame+" is near channel "+i+" -> "+isHere);
	       		if(isHere){
	       			return i;	
	       		}
	       		
       		}
       		i++;
        }
		return -1;
	}	
	
	public void move_channel(ImagePlus imp, int id_channel, int x, int y, int z){
		int new_offset_x=(int)(x-channel_width/2);
		int new_offset_y=(int)(y-channel_height);
	
		mmJChannel this_channel=get_channel(id_channel);
		if(this_channel!=null){ 
			if(DEBUG) IJ.log(">> Moving channel "+id_channel);
			
			int [] iPoints=this_channel.get_breakpoints(z);
			//IJ.log("bpoints "+iPoints.length);
			//this_channel.set_breakpoints(int slice, int [] iPoints)	
			
			
			//detector.define_channel(imp, id_channel, new_offset_x, new_offset_y, z);
			
			
			//int [] moved_offset=moved_channel.getOffset(z);
			int [] moved_offset = new int[2];
			moved_offset[0]=new_offset_x;
			moved_offset[1]=new_offset_y;
			
			//this_channel.undrawChannel(imp, z);
			
			byte[] pixels=detector.get_pixels_channel(imp, moved_offset[0], moved_offset[1], z);
			
			//IJ.log("New offset: "+moved_offset[0]+" "+moved_offset[1]);
			
			//this_channel.undrawChannel(imp, z);
			
			this_channel.setSlice(z, pixels);
			this_channel.setOffset(z, moved_offset);	
			
			//this_channel.drawChannel(imp, z);

			//mChannels.set(id_channel, this_channel);
		}
		
		
	}
	
	public Vector<Point> getOffsets(int id_channel){
		Vector<Point> listOffsets = new Vector<Point>(0, 16);
		mmJChannel this_channel=get_channel(id_channel);
		if(this_channel!=null){
			listOffsets=this_channel.getListOffsets();
		}
		return listOffsets;
	}
	
	/*
	public Vector<Integer> getDiameter(int id_channel){
		Vector<Integer> listDiameter = new Vector<Integer>(0, 16);
		mmJChannel this_channel=get_channel(id_channel);
		if(this_channel!=null){
			listDiameter=this_channel.getListDiameter();
		}
		return listDiameter;
	}	
	*/
	
	public int get_offset_y(int id_channel, int slice){
		mmJChannel this_channel=get_channel(id_channel);
		int [] offsets=new int[2];
		offsets[1]=0;
		if(this_channel!=null){
			offsets=this_channel.getOffset(slice);
		}
		return offsets[1];
	}
	
	public int get_offset_x(int id_channel, int slice){
		mmJChannel this_channel=get_channel(id_channel);
		int [] offsets=new int[2];
		offsets[0]=0;
		if(this_channel!=null){
			offsets=this_channel.getOffset(slice);
		}
		return offsets[0];
	}	
	
	public Vector<Integer> getBreakpoints(int id_channel, int slice){
		Vector<Integer> listBreakpoints = new Vector<Integer>(0, 16);
		mmJChannel this_channel=get_channel(id_channel);
		if(this_channel!=null){
			listBreakpoints=this_channel.getListBreakpoints(slice);
		}
		return listBreakpoints;
	}
	
	public void setBreakpoints(int id_channel, int slice, Vector<Integer> listBreakpoints){
		int [] iPoints=new int[listBreakpoints.size()];
		for(int i=0; i<iPoints.length; i++){
			iPoints[i]=((Integer)listBreakpoints.elementAt(i)).intValue();
			}
		set_breakpoints_channel(id_channel, slice, iPoints);
	}
	
	
	
	/************************ BREAKPOINTS ************************/	
	
	public void delete_all_breakpoints_channel_hereafter(int id_channel, int frame){
			if(DEBUG) IJ.log(">> Deleting breakpoints in channel "+id_channel+" at frame "+frame);	
			mmJChannel this_channel=get_channel(id_channel);
			if(this_channel!=null){
				this_channel.remove_all_breakpoints_hereafter(frame);
				if(DEBUG) this_channel.log_breakpoints(frame);
			}
	}
	
	public void delete_all_breakpoints_channel(int id_channel, int frame){
			if(DEBUG) IJ.log(">> Deleting breakpoints in channel "+id_channel+" after frame "+frame);	
			mmJChannel this_channel=get_channel(id_channel);
			if(this_channel!=null){
				this_channel.remove_all_breakpoints(frame);
				if(DEBUG) this_channel.log_breakpoints(frame);
			}
	}
	
	//Returns true if it removed future breakpoints
	public boolean delete_breakpoint_channel(int id_channel, int frame, int iPoint){
		
		if(iPoint>0){
			if(DEBUG) 
				IJ.log(">> Deleting breakpoint "+iPoint+" from channel "+id_channel+" at frame "+frame);	
			mmJChannel this_channel=get_channel(id_channel);
			if(this_channel!=null){
				this_channel.remove_breakpoint(frame, iPoint);
				if(DEBUG) this_channel.log_breakpoints(frame);
			}
		}else{
			delete_all_breakpoints_channel_hereafter(id_channel, frame);
			return true;
		}
		return false;
	}
	
	public void set_breakpoints_channel(int id_channel, int frame, int [] iPoints){
			if(DEBUG) IJ.log(">> Setting "+iPoints.length+" breakpoints into channel "+id_channel+" at frame "+frame);	
			mmJChannel this_channel=get_channel(id_channel);
			
			if(this_channel!=null){
				this_channel.set_breakpoints(frame+1, iPoints);
			}
			
	}
	
	public void log_channel(int id_channel){
			mmJChannel this_channel=get_channel(id_channel);
			if(this_channel!=null){
				this_channel.log_channel();
			}
			
	}
	
	public void add_breakpoints_channel(int id_channel, int frame, int [] iPoints){
			if(DEBUG) IJ.log(">> Adding "+iPoints.length+" breakpoints into channel "+id_channel+" at frame "+frame);	
			mmJChannel this_channel=get_channel(id_channel);
			if(this_channel!=null){
				this_channel.add_breakpoints(frame, iPoints);
				if(DEBUG) this_channel.log_breakpoints(frame);
			}
	}	
	
	public void add_breakpoint_channel(int id_channel, int frame, int iPoint){
		if(iPoint>0){
			if(DEBUG) IJ.log(">> Adding "+iPoint+" into channel "+id_channel+" at frame "+frame);	
			mmJChannel this_channel=get_channel(id_channel);
			if(this_channel!=null){
				this_channel.add_breakpoint(frame, iPoint);
				if(DEBUG) this_channel.log_breakpoints(frame);
			}
		}
	}
	
	public void move_breakpoint_channel(int id_channel, int frame, int yfrom, int yto){
		if(yto>0 && yfrom>0){
			if(DEBUG) IJ.log(">> Moving "+yfrom+" to "+yto+" in channel "+id_channel+" at frame "+frame);	
			
			mmJChannel this_channel=get_channel(id_channel);
			if(this_channel!=null){
				this_channel.move_breakpoint(frame, yfrom, yto);
				if(DEBUG) this_channel.log_breakpoints(frame);
			}
			
		}
	}	
	
	public int [] get_breakpoints_channel(int id_channel, int frame){
			mmJChannel this_channel=get_channel(id_channel);
			if(this_channel!=null){ 
				int [] iPoints=this_channel.get_breakpoints(frame);
				if(DEBUG) IJ.log(">> Channel "+id_channel+" at frame "+frame+" has "+iPoints.length+" breakpoints:");				
				return iPoints;
			}
			return new int[0];
	}
	
	public int get_near_breakpoint_channel(int id_channel, int frame, int y0, int delta){
			mmJChannel this_channel=get_channel(id_channel);
			if(this_channel!=null){
				int iPoint=this_channel.get_near_breakpoint(frame, y0, delta);
				if(DEBUG){
					if(iPoint>=0){
						IJ.log(">> Breakpoint "+iPoint+" is near y="+y0+" (dist<"+delta+") in channel "+id_channel+" at frame "+frame);				
					}else{
						IJ.log(">> No breakpoint is near y="+y0+" (dist<"+delta+") in channel "+id_channel+" at frame "+frame);				
					}
				}
				return iPoint;
			}
			return -1;
	}
	
	
	/************************ Drawing functions ************************/	
	
	
	
	public void draw_everything(ImagePlus imp){
		if(DEBUG) IJ.log(">> Drawing everything ");
		
		//First draw breakpoints
		Iterator<mmJChannel> itc=mChannels.iterator();
        while(itc.hasNext()) {		
       		mmJChannel mchannel=(mmJChannel)itc.next();
       		draw_all_breakpoints(imp, mchannel.get_id());
       	}
       	
       	//Now channels
       	draw_all_channels(imp);
	}
	
	public void draw_breakpoints(ImagePlus imp, int id_channel, int frame){
		if(DEBUG) IJ.log(">> Drawing breakpoints of channel "+id_channel+" at frame "+frame);	
		mmJChannel this_channel=get_channel(id_channel);
		if(this_channel!=null) this_channel.draw_breakpoints(imp, frame, channel_width);
	}
	
	public void draw_all_breakpoints(ImagePlus imp, int id_channel){	
		mmJChannel this_channel=get_channel(id_channel);
		if(this_channel!=null) this_channel.draw_all_breakpoints(imp, channel_width);
	}
	public void export_channel(ImagePlus imp, int id_channel, boolean gfp){
		String dir_channel=dirName+"channels";
		if(DEBUG) IJ.log(">> Exporting channel "+id_channel+" into "+dir_channel);	
		mmJChannel this_channel=get_channel(id_channel);
		if(this_channel!=null) this_channel.export_channel(imp, dir_channel, gfp);
	}
	
	public void draw_channel(ImagePlus imp, int id_channel){
		if(DEBUG) IJ.log(">> Drawing channel "+id_channel);	
		mmJChannel this_channel=get_channel(id_channel);
		if(this_channel!=null) this_channel.drawChannel(imp);
	}
	
	public void draw_channel(ImagePlus imp, int id_channel, int slice){
		if(DEBUG) IJ.log(">> Drawing channel "+id_channel+" at slice "+slice);	
		mmJChannel this_channel=get_channel(id_channel);
		this_channel.drawChannel(imp, slice);
	}	
	
	public void draw_all_channels(ImagePlus imp){
		if(DEBUG) IJ.log(">> Drawing all channels ("+mChannels.size()+")");
		Iterator<mmJChannel> itc=mChannels.iterator();
        while(itc.hasNext()) {		
       		mmJChannel mchannel=(mmJChannel)itc.next();
       		if(mchannel!=null){
				mchannel.drawChannel(imp);
				draw_all_breakpoints(imp, mchannel.get_id());
			}
		}
	}	
	
	public void undraw_all_channels(ImagePlus imp){
		
		if(DEBUG) IJ.log(">> Undrawing all channels ("+mChannels.size()+")");
		Iterator<mmJChannel> itc=mChannels.iterator();
        while(itc.hasNext()) {		
       		mmJChannel mchannel=(mmJChannel)itc.next();
       		if(mchannel!=null){
				mchannel.undrawChannels(imp);
			}
		}

	}
	
	public void undraw_channel(ImagePlus imp, int id_channel, int frame){
		if(DEBUG) IJ.log(">> Undrawing channels "+id_channel+" in frame "+frame);
		mmJChannel this_channel=get_channel(id_channel);
		if(this_channel!=null) this_channel.undrawChannel(imp, frame);
		
	}		
	
	/************************** TRACK *****************************/
	
	//This could be parallel?
	public void track_all(){ 
		if(DEBUG) IJ.log(">> Tracking all channels ("+mChannels.size()+")");
		Iterator<mmJChannel> itc=mChannels.iterator();
        while(itc.hasNext()) {		
       		mmJChannel mchannel=(mmJChannel)itc.next();
       		if(mchannel!=null){
				mchannel.trackChannel();
			}
		}
	}
	
	public void track_channel(int id_channel){
		mmJChannel this_channel=get_channel(id_channel);
		if(this_channel!=null) this_channel.trackChannel();
		
	}
	
	public void log_cells(){
		Iterator<mmJChannel> itc=mChannels.iterator();
        while(itc.hasNext()) {		
       		mmJChannel mchannel=(mmJChannel)itc.next();
       		if(mchannel!=null){
				mchannel.log_cells();
			}
		}
	}
	
	public void draw_cells(ImagePlus imp){
	
		Iterator<mmJChannel> itc=mChannels.iterator();
        while(itc.hasNext()) {	
        
       		mmJChannel mchannel=(mmJChannel)itc.next();
       		if(mchannel!=null){
				mchannel.draw_cells(imp);
			}
			
		}
		
		
	}
	
	public void determine_intensity(ImagePlus imp_original){
		Iterator<mmJChannel> itc=mChannels.iterator();
        while(itc.hasNext()) {		
       		mmJChannel mchannel=(mmJChannel)itc.next();
       		if(mchannel!=null){
				mchannel.determine_intensity(imp_original);
			}
		}
		
	}
	
	public void results_data(){
		Iterator<mmJChannel> itc=mChannels.iterator();
        while(itc.hasNext()) {		
       		mmJChannel mchannel=(mmJChannel)itc.next();
       		if(mchannel!=null){
				mchannel.results_data(results_table);
			}
		}
		
	}	
	
	public void export_channels(ImagePlus imp_original, boolean gfp){
	
		if(DEBUG) IJ.log(">> Exporting channels (gfp="+gfp+") into "+dirName);
		Iterator<mmJChannel> itc=mChannels.iterator();
        while(itc.hasNext()) {		
       		mmJChannel mchannel=(mmJChannel)itc.next();
       		if(mchannel!=null){
				mchannel.export_channel(imp_original, dirName, gfp);
			}
		}
		
	}
	
	public void export_cells(ImagePlus imp_original, boolean gfp){
		if(DEBUG) IJ.log(">> Exporting cells (gfp="+gfp+") into "+dirName);
		
		Iterator<mmJChannel> itc=mChannels.iterator();
        while(itc.hasNext()) {		
       		mmJChannel mchannel=(mmJChannel)itc.next();
       		if(mchannel!=null){
       			mchannel.export_cells(null, imp_original, dirName, gfp);
			}
		}
		
	}	
	
	
	public double [] normalize_intensities(){
		double min=255;
		double max=0;
	
		Iterator<mmJChannel> itc=mChannels.iterator();
        while(itc.hasNext()) {		
       		mmJChannel mchannel=(mmJChannel)itc.next();
       		if(mchannel!=null){
				double max_channel=mchannel.max_intensity();
				if(max_channel>max){
					max=max_channel;
				}
				
				double min_channel=mchannel.min_intensity();
				if(min_channel<min){
					min=min_channel;
				}
			}
		}
		
		double [] minmax=new double[2];
		minmax[0]=min;
		minmax[1]=max;
		return minmax;
		
		/*
		if(DEBUG) IJ.log(">> Normalizing intensity values (min="+min+", max="+max+")");
		
		//NOW NORMALISE
		Iterator<mmJChannel> itc2=mChannels.iterator();
        while(itc2.hasNext()) {		
       		mmJChannel mchannel=(mmJChannel)itc2.next();
       		if(mchannel!=null){
				mchannel.normalize_intensities(min, max);
			}
		}
		*/
		
	}
	/******************** CORRECT *************************/
	
	public void correct_segmentation(){
	
		if(DEBUG) IJ.log(">> Correcting segmentation");
		Iterator<mmJChannel> itc=mChannels.iterator();
        while(itc.hasNext()) {		
       		mmJChannel mchannel=(mmJChannel)itc.next();
       		if(mchannel!=null){
       			
       			for(int i=0; i<5; i++){
       				IJ.showStatus("Correcting segmentation (round="+i+")");
       				mchannel.correct_segmentation();
       			
       				IJ.showStatus(">> Refining segmentation (round="+i+")");
       				mchannel.refine_segmentation();
       			}
			}
		}
	
	}
	
	
	
	
}