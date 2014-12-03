import java.awt.*;
import java.util.*;
import java.util.List;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.IJ;
import ij.measure.ResultsTable; 

public class mmJCell{

	boolean DEBUG =false;
	
	/***********************  Tracking parameters */
	double div_thres=0.7;
	int min_diameter=30;
	int max_growth_rate=20;  //Maximum allowed increase in diameter in consecutive frames
	int delta=2;
	
	
	/***********************  Variables */
	int channel_height;
	int channel_width;
	int id_cell;
	int id_parent;
	int frameBorn;
	
	int numColors=20;
	List<Color> list_colors=pick(numColors);
	
	int level=0;  //Default level (mother)
	boolean left_pole=true;  //Default orientation (up)
	int this_i=1;  
	
	Vector<Integer> y1s = new Vector<Integer>(0, 16);
	Vector<Integer> y2s = new Vector<Integer>(0, 16);
	Vector<Integer> ys = new Vector<Integer>(0, 16);  //CENTER
	
	Vector<Double> intensities = new Vector<Double>(0, 16);
	Vector<Boolean> Divs = new Vector<Boolean>(0, 16);
	
	ArrayList<mmJCell> daughters = new ArrayList<mmJCell>();
	int numDaughters=0;

	ArrayList<mmJBreakpoints> breakpoints;
			
	
	/*********************** Constructors */
	public mmJCell(int z, int id_cell, int w, int h){
		this.frameBorn=z;
		this.id_cell=id_cell;
		this.channel_height=h;
		this.channel_width=w;
		this.id_parent=0;
		breakpoints = new ArrayList<mmJBreakpoints>();
	}

	public mmJCell(int id_cell, int w, int h){
		this.id_cell=id_cell;
		this.channel_height=h;
		this.channel_width=w;
		this.id_parent=0;
		this.frameBorn=0;
		breakpoints = new ArrayList<mmJBreakpoints>();
	}
	
	
	/***********************  Segmentation functions */

	public int find_missed_breakpoints(int this_z){
		if(DEBUG) IJ.log("Correcting segmentation cell "+id_cell+" frame "+(this_z+frameBorn));
		
		int prev_diameter=0;
		int prev_y1=0;
		int prev_y2=0;
		for(int z=0; z<y1s.size(); z++){
			int this_y1=y1s.elementAt(z);
			int this_y2=y2s.elementAt(z);
			int this_diameter=this_y1-this_y2;
			boolean this_div=Divs.elementAt(z);
			
			if(z==this_z){  //ONLY CHECK FOR THIS FRAME
				 if(this_diameter-prev_diameter>max_growth_rate && prev_y2>0){
						return prev_y2;
				 }
			}
			prev_diameter=this_diameter;
			prev_y1=this_y1;
			prev_y2=this_y2;
			
		}
		return 0;
	}
	
	public int find_division_breakpoints(int this_z){
		for(int z=0; z<y1s.size(); z++){
			int this_y1=y1s.elementAt(z);
			int this_y2=y2s.elementAt(z);
			boolean this_div=Divs.elementAt(z);
			
			if(z==this_z && this_div){  
				return this_y2;
			}
		}
		return 0;
	}	
	
	public int find_extra_breakpoints(int this_z){

		for(int z=0; z<y1s.size(); z++){
			int this_y1=y1s.elementAt(z);
			int this_y2=y2s.elementAt(z);
			int this_diameter=this_y1-this_y2;
			boolean this_div=Divs.elementAt(z);
			
			if(z==this_z){  //ONLY CHECK FOR THIS FRAME
				 if(this_diameter<min_diameter){
						return this_y2;
				 }
			}
		}
		return 0;
	}
	
	public int find_repeated_breakpoints(int this_z){
		int prev_diameter=0;
		for(int z=0; z<y1s.size(); z++){
			int this_y1=y1s.elementAt(z);
			int this_y2=y2s.elementAt(z);
			int this_diameter=this_y1-this_y2;
			boolean this_div=Divs.elementAt(z);
			
			if(z==this_z){  //ONLY CHECK FOR THIS FRAME
				 if(Math.abs(this_diameter-prev_diameter)<min_diameter){
						return this_y2;
				 }
			}
			prev_diameter=this_diameter;
		}
		return 0;
	}
	
	
	public int previous_breakpoint(int [] breakpoints, int this_point){
		int prev_point=0;
		for(int i=0; i<breakpoints.length; i++){
			if(breakpoints[i]<this_point){
				prev_point=breakpoints[i];
			}else{
				return prev_point;
			}
		}
		return 0;
	}
	
	public int next_breakpoint(int [] breakpoints, int this_point){
		int prev_point=0;
		for(int i=0; i<breakpoints.length; i++){
			if(breakpoints[i]<=this_point){
				prev_point=breakpoints[i];
			}else{
				return breakpoints[i];
			}
		}
		return 0;
	}


	public void track(int max_levels){   //JUST THE MOTHER!
		//String binary_cell=toBinary((long)id_cell);
		if(DEBUG) IJ.log("\nTracking mother cell "+id_cell+" from frame "+(frameBorn));
		
		//First define -1 the frames before the cell was born
		for(int z=0; z<frameBorn; z++){
					y1s.addElement(-1);
					y2s.addElement(-1);
					ys.addElement(-1);
					Divs.addElement(false);
		}
		
		//Now track the cell during its lifespan
		int this_center=-1;
		int prev_center=-1;
		
		int this_diameter=0;
		int prev_diameter=0;
		
		for(int z=frameBorn; z<breakpoints.size(); z++){
			//IJ.log("\nSlice "+(z+1));
			mmJBreakpoints bPoint=breakpoints.get(z);
			int [] yz0 = bPoint.get_breakpoints();
			if(yz0.length>0){
				//ADD LAST POINT
				 
				
				int [] yz = new int[yz0.length+1];
				for(int i=0; i<yz0.length; i++){
					yz[i]=yz0[i];
					if(DEBUG) IJ.log("... "+(yz[i]));
				}
				yz[yz0.length]=channel_height;
				if(DEBUG) IJ.log("... "+(yz[yz0.length]));
			
				boolean division=false;
			
				int y1=0;
				int y2=0;	
				
				for(int i=1;i<yz.length; i++){
					if(yz[i-1]>0 && yz[i]>0){
						y2=yz[i];
						y1=yz[i-1];
					}
				}
				
				//temp
				this_diameter=Math.abs(y1-y2);
				this_center=y1+this_diameter/2;
				division=false;
				
				//DIVISION? *******
		
				  	if(this_diameter<0.8*prev_diameter){ 
				  	
				  		division=true;
						
						
						 if(DEBUG) IJ.log("["+y1 +", "+y2+"] *");	
				  	}else{
				  		if(DEBUG) IJ.log("["+y1 +", "+y2+"]");
				  	} 
				  	prev_diameter=this_diameter;
				  	
					 
					if(this_i>0){
						 //Now save values
						  
						 y1s.addElement(y1);
						 y2s.addElement(y2);
						 ys.addElement(this_center);
						 Divs.addElement(division);
					   
						 if(DEBUG) IJ.log("\n"+(z+1)+": "+logTrack(yz, this_center)+" ");
						 if(DEBUG) IJ.log("i="+this_i+" diameter:"+this_diameter);
						 
						 
						 if(division)
							 if(DEBUG) IJ.log("***");
					}else{
						 if(DEBUG) IJ.log((z+1)+" ---");
						 
						 y1s.addElement(-1);
						 y2s.addElement(-1);
						 ys.addElement(-1);
						 Divs.addElement(false);
						 
					}
				}
			}	
				 
				  	
				  				 
	}

	
	/***********************  Tracking functions */
	/*
	public void track(int max_levels){
		String binary_cell=toBinary((long)id_cell);
		if(DEBUG) IJ.log("\nTracking cell "+id_cell+" ("+binary_cell+") from frame "+(frameBorn+1)+" left_pole="+left_pole);
		
		//First define -1 the frames before the cell was born
		for(int z=0; z<frameBorn; z++){
					y1s.addElement(-1);
					y2s.addElement(-1);
					ys.addElement(-1);
					Divs.addElement(false);
		}
		
		//Now track the cell during its lifespan
		int this_center=-1;
		int prev_center=-1;
		
		int this_diameter=0;
		int prev_diameter=0;
		
		for(int z=frameBorn; z<breakpoints.size(); z++){
		
			mmJBreakpoints bPoint=breakpoints.get(z);
			int [] yz = bPoint.get_breakpoints();
			int [] y=new int[yz.length+1];
			int [] em=new int[yz.length+1];
			boolean division=false;
			boolean ignore_cell=false;
			if(yz.length>0){
		
				  //Flip vertically
				  for(int i=0; i<yz.length; i++){
				  	 
					  y[yz.length-i]=channel_height-(yz[i]);
					  if(y[yz.length-i]>channel_height){
				  		if(DEBUG) IJ.log("Empty Space: "+(y[yz.length-i]));
				  		 em[yz.length-i]=1;
					  }else{
					  	 em[yz.length-i]=0;
					  }
					  if(DEBUG) IJ.log("... "+(y[yz.length-i])+" "+em[yz.length-i]);
				  }
				 
				
				 
				  if(this_i<=yz.length && this_i>0 ){
				
					
				
					this_i=1;
				  	this_diameter=y[this_i]-y[this_i-1];
				  	this_center=y[this_i-1]+delta;
				  	
				  	//DIVISION? *******
				  	division=false;
				  	if(this_diameter<prev_diameter-10){ 
				  	
				  		division=true;
						numDaughters++;
						String str_cell="1";
						for(int s=0; s<numDaughters-1; s++){
							str_cell="0"+str_cell;
						}
						
						int id_daughter_cell=numDaughters+1;	  	  
							  
						mmJCell this_daughter = new mmJCell(z, id_daughter_cell, channel_width, channel_height);
						this_daughter.set_level(level+1);
						this_daughter.set_left_pole(!left_pole);
						this_daughter.set_parent(id_cell);
						
						
						if(left_pole){
							this_daughter.set_i(this_i+1);  //AND IF RIGHT POLE????
						}else{
							this_daughter.set_i(this_i);  //AND IF RIGHT POLE????
						}
						daughters.add(this_daughter);
						
						if(left_pole){
				  			this_center=y[this_i-1]+delta;
						}else{
							this_i=this_i+1;  //RIGHT POLE
							if(this_i<y.length){		
								this_center=y[this_i]-delta;
							}else{
								this_i=-1;
							}
						}
						
						 if(DEBUG) IJ.log("*");	
				  	} 
				  	
				  	prev_center=this_center;
				  	prev_diameter=this_diameter;
				  
					if(this_i>0 && em[this_i]==0){
						 //Now save values
						 int y1=channel_height-y[this_i];
						 int y2=channel_height-y[this_i-1];
						 IJ.log("y1("+z+")="+y[this_i] +"\t y2("+z+")="+y[this_i-1]);
						   
						 y1s.addElement(y1);
						 y2s.addElement(y2);
						 ys.addElement(this_center);
						 Divs.addElement(division);
					   
						 if(DEBUG) IJ.log("\n"+(z+1)+": "+logTrack(y, this_center)+" ");
						 if(DEBUG) IJ.log("i="+this_i+" diameter:"+this_diameter);
						 
						 
						 if(division)
							 if(DEBUG) IJ.log("***");
					}else{
						 if(DEBUG) IJ.log((z+1)+" ---");
						 
						 y1s.addElement(-1);
						 y2s.addElement(-1);
						 ys.addElement(-1);
						 Divs.addElement(false);
						 
					}
				  		
				  		
				  }else{
				  	this_i=-1;
				  	if(DEBUG) IJ.log((z+1)+" ---");
				  	
				  	y1s.addElement(-1);
					y2s.addElement(-1);
					ys.addElement(-1);
					Divs.addElement(false);
					
				  	
				  }
			}
		}
		
		
		if(DEBUG) log_cell();
		
		//RECURSIVELY	
		for(int k=0; k<numDaughters; k++){
			mmJCell d = (mmJCell)daughters.get(k);
			String binary_cell_daughter=toBinary((long)d.get_id_cell());
			d.set_breakpoints(breakpoints);
			
			if(level<max_levels  || max_levels<0){ //JUST ONE LEVEL
				d.track(max_levels);
			}
		}
	}
	*/
	
	
	public int get_next_i(int [] y, int center){
		for(int i=0; i<y.length-1; i++){
			if(center>=y[i] && center<=y[i+1]){
				return i;
			}
		}
		return -1;
	}
	
	
	/***********************  Drawing functions */
	public void draw_cell(ImagePlus imp_original, ArrayList<Object> listOffset_x, ArrayList<Object> listOffset_y){
		
		int [] offset_x=convert_integers(listOffset_x);
		int [] offset_y=convert_integers(listOffset_y);
		
		ImageStack istack_original = imp_original.getImageStack();
		for (int z = frameBorn; z < y1s.size(); z++){
				ImageProcessor ipz=istack_original.getProcessor(z+1);
				
				int y1=((Integer)y1s.elementAt(z)).intValue();
    			int y2=((Integer)y2s.elementAt(z)).intValue();
    			
    			if(y1>=0 && y2>=0){
				   
				   int colorCell = id_cell;
				   ipz.setValue(colorCell); 
				   int rho=5;
				   int cell_height=y2-y1;
				   
				   Color c=list_colors.get(id_cell);
				   //float alpha=(float)0.1;
    			   
				   //DRAW CELL MARGIN
				   /*
				   if(left_pole){
					    Roi oroi = new Roi(offset_x[z]+rho+channel_width-2*rho,y1+offset_y[z]+1, rho, cell_height-2);
				  		ipz.setRoi(oroi);  
    					ipz.setColor(c); 
    					ipz.fill(); 
    						
				  }else{
				  		Roi oroi = new Roi(offset_x[z],y2+offset_y[z]-cell_height+1, rho, cell_height-2);
				  		ipz.setRoi(oroi);  
    					ipz.setColor(c);
    					ipz.fill();
				   }
				   
					  
				  //DRAW DIVISION LINE	
				  if(((Boolean)Divs.elementAt(z)).booleanValue()){
					 if(left_pole){
					 	  Roi roi = new Roi(offset_x[z], y1+offset_y[z]-1, channel_width, 4); // x, y, width, height of the rectangle  
						  ipz.setRoi(roi);  
						  ipz.setColor(new Color(255,255,255));  
						  ipz.fill(); 
					 }else{
						  Roi roi = new Roi(offset_x[z], y2+offset_y[z]-1, channel_width, 4); // x, y, width, height of the rectangle  
						  ipz.setRoi(roi);  
						  ipz.setColor(new Color(122,122,122));  
						  ipz.fill(); 
					 }
				  } 
				  */
				  
				  //DRAW CELL BOX
				  Roi oroi = new Roi(offset_x[z]+1,y1+offset_y[z]+1, channel_width-2, cell_height-2);
				  ipz.setLineWidth(2);
    			  ipz.setColor(c);  
				  
				  Color clabel=c;
				  if(((Boolean)Divs.elementAt(z)).booleanValue()){
				  	ipz.fill(oroi); 
				  	c=new Color(255,255,255);
				  }else{

    			  	ipz.draw(oroi); 
    			  }
				  
				  //DRAW LABEL
				  int xshift=3;
				  if(id_cell<10){
				  	xshift+=3;
				  }
				  
				  if(left_pole){ 
    					ipz.setColor(c);
    					ipz.drawString(""+id_cell,offset_x[z]+xshift,y2+offset_y[z]);
				  }else{ 
    					ipz.setColor(c);
					    ipz.drawString(""+id_cell,offset_x[z]+xshift,y2+offset_y[z]-cell_height+15);
				   }
				  
    			}
    	}	
    	imp_original.updateAndDraw();
    	
    	//RECURSIVE
    	/*  THERE IS AN ERROR!!!!!!  ONLY DRAWS FIVE FIRST CHANNELS?
		for(int k=0; k<numDaughters; k++){
			mmJCell d = (mmJCell)daughters.get(k);
			d.draw_cell(imp_original, listOffset_x, listOffset_y); 
		}
		*/
    	
	}
	
	public Roi get_roi(ImagePlus imp_original, int z, ArrayList<Object> listOffset_x, ArrayList<Object> listOffset_y){
				ImageStack istack_original = imp_original.getImageStack();
				ImageProcessor ipz=istack_original.getProcessor(z+1);
				
				int [] offset_x=convert_integers(listOffset_x);
				int [] offset_y=convert_integers(listOffset_y);
				
				int y2=((Integer)y1s.elementAt(z)).intValue();
    			int y1=((Integer)y2s.elementAt(z)).intValue();
				
				Roi roi = new Roi(offset_x[z], y2+offset_y[z], channel_width, (y1-y2)); // x, y, width, height of the rectangle  
				ipz.setRoi(roi);  
				ipz.setValue(122);  
				ipz.draw(roi); 
				
    			return roi;
	}	
	
	
	/***********************  Log functions */
	public void log_cell(){
		IJ.log("\n Cell: "+id_cell+" ("+left_pole+") Level:" + level);
		for (int i = 0; i < y1s.size(); i++){
    			int y1=((Integer)y1s.elementAt(i)).intValue();
    			int y2=((Integer)y2s.elementAt(i)).intValue();
    			int y=((Integer)ys.elementAt(i)).intValue();
    			if(y1>=0 && y2>=0 && y>=0){
    			
				   String strdiv="";
				   if(((Boolean)Divs.elementAt(i)).booleanValue()){
					   strdiv="***";
				   }    			
				   IJ.log("     Frame "+(i+1)+": y="+y+" y1="+y1+"  y2="+y2+" height="+(y2-y1)+"    "+strdiv);
    			}else{
    			IJ.log("     Frame "+(i+1));
    			
    			}
    	}
    	IJ.log("____");
	}
	
	public String logTrack(int [] y, int center){
		String ret="";
		for(int i=0; i<y.length; i++){
			if(center>=y[i] && center<=y[i+1]){
				if(left_pole){
					ret+="\t"+y[i]+" (<"+center+")";
				}else{
					ret+="\t"+y[i]+" ("+center+">)";
				}
			}else{
				ret+="\t"+y[i];
			}
		}
		return ret;
	}
	
	/***********************  Data analysis functions */
	public void set_intensities(double [] intensity){
		intensities = new Vector<Double>(0, 16);
		for(int i=0; i<intensity.length; i++){
			intensities.addElement(intensity[i]);
		}
	}
	
	public double [] get_intensities(){
		double [] ret=new double[intensities.size()];
		for(int i=0; i<intensities.size(); i++){
			ret[i]=((Double)intensities.elementAt(i)).doubleValue();
		}
		return ret;
	}
	
	public void determine_intensity(ArrayList<Object>  offset_x, ArrayList<Object>  offset_y, mmJAnalyzer analyzer, ImagePlus imp_original){
			
			ImageStack istack_original = imp_original.getImageStack();
			double [] inten = new double[istack_original.getSize()];
			
			for(int slice=1; slice<=istack_original.getSize(); slice++){
				int y1=get_y1(slice-1);
				int y2=get_y2(slice-1);
				if(y1>=0 && y2>=0){
					ImageProcessor ipz=istack_original.getProcessor(slice);
					Roi this_roi=get_roi(imp_original, slice-1, offset_x, offset_y);
					double this_intensity=analyzer.intensity(ipz, this_roi);
					inten[slice-1]=this_intensity;
				}else{
					inten[slice-1]=-1;
				}
			}
			set_intensities(inten);
			
			//RECURSIVE
			for(int k=0; k<numDaughters; k++){
				mmJCell d = (mmJCell)daughters.get(k);
				d.determine_intensity(offset_x, offset_y, analyzer,imp_original); 
			}
			
	}
	
	public double min_intensity(){
		double candidate=255;
		for(int i=0; i<intensities.size(); i++){
			double this_intensity=((Double)intensities.elementAt(i)).doubleValue();
			if(candidate>this_intensity){
				candidate=this_intensity;
			}
		}
		return candidate;
	}
	
	public double max_intensity(){
		double candidate=0;
		for(int i=0; i<intensities.size(); i++){
			double this_intensity=((Double)intensities.elementAt(i)).doubleValue();
			if(candidate<this_intensity){
				candidate=this_intensity;
			}
		}
		return candidate;
	}
	
	public void normalize_intensities(double this_min, double this_max){
		double [] new_intensity=new double[intensities.size()];
		for(int i=0; i<intensities.size(); i++){
			double this_intensity=((Double)intensities.elementAt(i)).doubleValue();
			new_intensity[i]=(this_intensity-this_min)/this_max;
		}
		set_intensities(new_intensity);
	}	
	
	
	/***********************  Export data */
	public void export_data(int max_levels, ResultsTable rt){
		
		for(int i=0; i<intensities.size(); i++){
			
			double this_intensity=((Double)intensities.elementAt(i)).doubleValue();	
			if(this_intensity>=0){
				boolean this_division=((Boolean)Divs.elementAt(i)).booleanValue();
				int this_center=((Integer)ys.elementAt(i)).intValue();
				if(this_center>=0){
					 
					 int this_diameter=Math.abs(((Integer)y1s.elementAt(i)).intValue()-((Integer)y2s.elementAt(i)).intValue());
					 int this_parent=get_parent();
					 if(DEBUG) IJ.log(id_cell+" frame="+(i+1)+" intensity="+this_intensity);
						rt.incrementCounter();
						rt.addValue("id_cell", (double)(id_cell));
						rt.addValue("Frame", (double)(i+1));
						rt.addValue("Level:", (double)level);
						rt.addValue("Parent:", (double)this_parent);
						rt.addValue("Intensity:", (double)this_intensity);
						rt.addValue("Center:", (double)this_center);
						rt.addValue("Diameter:", (double)this_diameter);
						if(this_division){
							rt.addValue("Division:", 1);
						}else{
							rt.addValue("Division:", 0);
						}
					}
			 }
		}
		
		for(int k=0; k<numDaughters; k++){
			mmJCell d = (mmJCell)daughters.get(k);
			//String binary_cell_daughter=toBinary((long)d.get_id_cell());
			if(level<max_levels  || max_levels<0){ 
				d.export_data(max_levels, rt);
			}
		}
		
		rt.show("mmJ"); 	
	}	
	
	/***********************  Get/set functions */
	
	public void set_birthframe(int fB){
		frameBorn=fB;
	}
	
	public void set_breakpoints(ArrayList<mmJBreakpoints> breakpoints){
		this.breakpoints=breakpoints;
	}
	
	public ArrayList<mmJBreakpoints> get_breakpoints(){
		return breakpoints;
	}
	
	public int get_width(int z){
		return channel_width; 
	}
	
	public int get_id_cell(){
		return id_cell;
	}
	
	public void set_left_pole(boolean b){
		left_pole=b;
	}
	
	public boolean get_left_pole(){
		return left_pole;
	}
	
	public int get_level(){
		return level;
	}
	
	public void set_level(int l){
		level=l;
	}
	
	public int get_i(){
		return this_i;
	}
	
	public void set_i(int i){
		this_i=i;
	}
	
	public int get_height(int z){
		int y1=get_y1(z);
		int y2=get_y1(z);
		return Math.abs(y1-y2);
	}
	
	public Vector<Integer> get_y1s(){
		return y1s;
	}
	
	public Vector<Integer> get_y2s(){
		return y2s;
	}
	
	public Vector<Integer> get_ys(){
		return ys;
	}
	
	public int get_y1(int z){
		if(z<y1s.size()){
			return ((Integer)y1s.elementAt(z)).intValue();
		}
		return -1;
	}
	
	public int get_y2(int z){
		if(z<y2s.size()){
			return ((Integer)y2s.elementAt(z)).intValue();
		}
		return -1;
	}
	
	public int get_y(int z){
		if(z<ys.size()){
			return ((Integer)ys.elementAt(z)).intValue();
		}
		return -1;
	}
	
	public void set_y1(int z, int y1){
		if(z<y1s.size()){
			y1s.set(z,y1);
		}
	}
	
	public void set_y2(int z, int y2){
		if(z<y2s.size()){
			y2s.set(z,y2);
		}
	}	

	public void set_y(int z, int y){
		if(z<ys.size()){
			ys.set(z,y);
		}
	}	
	
	public int get_numFrames(){
		return y2s.size();
	}
	
	public int get_frame_born(){
		return frameBorn;
	}
	
	public int get_parent(){
		return id_parent;
	}
	
	public void set_parent(int p){
		id_parent=p;
	}
	
	public ArrayList<mmJCell> get_daughters(){
		return daughters;
	}
	
	
	/***********************  Auxiliary functions */
	public int[] convert_integers(ArrayList<Object> integers){
    	int[] ret = new int[integers.size()];
    	Iterator<Object> iterator = integers.iterator();
    	for (int i = 0; i < ret.length; i++){
        	ret[i] = ((Integer)iterator.next()).intValue();
    	}
    	return ret;
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
   


public static List<Color> pick(int num) {
	List<Color> colors = new ArrayList<Color>();
	if (num < 2)
		return colors;
	float dx = 1.0f / (float) (num - 1);
	for (int i = 0; i < num; i++) {
		colors.add(get(i * dx));
	}
	return colors;
}

public static Color get(float x) {
	float r = 0.0f;
	float g = 0.0f;
	float b = 1.0f;
	if (x >= 0.0f && x < 0.2f) {
		x = x / 0.2f;
		r = 0.0f;
		g = x;
		b = 1.0f;
	} else if (x >= 0.2f && x < 0.4f) {
		x = (x - 0.2f) / 0.2f;
		r = 0.0f;
		g = 1.0f;
		b = 1.0f - x;
	} else if (x >= 0.4f && x < 0.6f) {
		x = (x - 0.4f) / 0.2f;
		r = x;
		g = 1.0f;
		b = 0.0f;
	} else if (x >= 0.6f && x < 0.8f) {
		x = (x - 0.6f) / 0.2f;
		r = 1.0f;
		g = 1.0f - x;
		b = 0.0f;
	} else if (x >= 0.8f && x <= 1.0f) {
		x = (x - 0.8f) / 0.2f;
		r = 1.0f;
		g = 0.0f;
		b = x;
	}
	return new Color(r, g, b);
}

	
}

