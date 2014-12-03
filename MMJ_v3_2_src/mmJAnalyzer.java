
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.IJ;
import java.awt.*;

/**

*/
public class mmJAnalyzer{


	/**
	 * Construct a new mmjAnalyzer
	 * 
	 */
	public mmJAnalyzer(){
	}
	
	/**
	 * Returns the area of a given Roi
	 */
	public int area(Roi roi){
		Rectangle roi_rect = roi.getBounds();

		int roi_area = 0;

		// compute area of the ROI
		int start_x = (int)roi_rect.getX();
		int start_y = (int)roi_rect.getY();
		int end_x = (int)(roi_rect.getX() + roi_rect.getWidth());
		int end_y = (int)(roi_rect.getY() + roi_rect.getHeight());


		for(int x_roi = start_x; x_roi < end_x; x_roi++)
			for(int y_roi = start_y; y_roi < end_y; y_roi++)
				if(roi.contains(x_roi, y_roi))
					roi_area++;


		return roi_area;
	}
	
	/**
	 * Returns the average intensity of a given Roi
	 */
	 /*
	public double intensity(ImageProcessor ip_original, ImagePlus imp_original, Roi roi){
	
		int w = ip_original.getWidth();
		int h = ip_original.getHeight();		

		ImagePlus imp_tmp = NewImage.createRGBImage ("mmjAnalyzer:Area", w, h, 1, NewImage.FILL_BLACK);

		ImageProcessor ip_tmp = imp_tmp.getProcessor();
		ip_tmp.copyBits(ip_original,0,0,Blitter.COPY);
		
		//imp_tmp.show();
		
		
		//ip_tmp.setColor(new Color(255,255,0)); 
		roi.drawPixels(ip_tmp);		
	
	
		
		Rectangle roi_rect = roi.getBounds();

		double intensity = 0;
		int numPoints=0;

		// compute area of the ROI
		int start_x = (int)roi_rect.getX();
		int start_y = (int)roi_rect.getY();
		int end_x = (int)(roi_rect.getX() + roi_rect.getWidth());
		int end_y = (int)(roi_rect.getY() + roi_rect.getHeight());


		for(int x_roi = start_x; x_roi < end_x; x_roi++)
			for(int y_roi = start_y; y_roi < end_y; y_roi++)
				if(roi.contains(x_roi, y_roi)){
					int []  pix = imp_tmp.getPixel(x_roi, y_roi);
					//int []  pix = imp_original.getPixel(x_roi, y_roi);
		
					//IJ.log("("+x_roi+","+y_roi+") -> "+pix[0]);	
					intensity+=pix[0];
					numPoints++;
					
				}

		intensity=intensity/numPoints;
		imp_tmp.flush();		
		imp_tmp.close();
		

		return intensity;
	}	
*/

	public double intensity(ImageProcessor ipz, Roi roi){

		Rectangle roi_rect = roi.getBounds();
		double this_intensity=0;

		// compute area of the ROI
		int start_x = (int)roi_rect.getX();
		int start_y = (int)roi_rect.getY();
		int end_x = (int)(roi_rect.getX() + roi_rect.getWidth());
		int end_y = (int)(roi_rect.getY() + roi_rect.getHeight());
		

				byte[] roi_pixels = new byte[((int)roi_rect.getWidth())*((int)roi_rect.getHeight())];
			
				int nRows = 0; 
				for (int y = start_y; y < start_y+(int)roi_rect.getHeight(); y++){
            		int index = nRows*(int)roi_rect.getWidth();
                	int nCols = 0;
                	for (int x = start_x; x < start_x+(int)roi_rect.getWidth(); x++){
               			roi_pixels[index+nCols] =(byte)ipz.getPixel(x,y);
               			this_intensity+=Math.abs(roi_pixels[index+nCols]);
                		nCols++;
               		}
                nRows++;
           		}
           		
           		//NOW ADD TO CHANNEL STACK
           		ByteProcessor proc = new ByteProcessor((int)roi_rect.getWidth(), (int)roi_rect.getHeight(), roi_pixels, null);
    			ImagePlus imp_cell = new ImagePlus("Cell ", proc); 
				//ImageProcessor ip_channel = imp_cell.getProcessor();
				
				if(roi_pixels.length>0){
					this_intensity=this_intensity/roi_pixels.length;
				}
				
				//imp_cell.show();
			
				
				
				return this_intensity;
	}


}

