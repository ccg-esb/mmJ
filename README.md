mmJ [mother-machine-analyzer]
=======================

##  General Description


This ImageJ plugin is a semi-automatic tool to track cells and measure the fluorescent protein expression level from time-lapse movies of a microfluidic continuous culture device.

![](http://penamiller.com/mmj/lib/exe/fetch.php?w=500&tok=219a99&media=mmj_panel.png)
Image credit: Markus Arnoldini and Martin Ackermann (ETH Zurich/EAWAG)

##  I. Download 

This distribution is dated December 11th, 2013.  It includes the source files, along with a jar file that contains the precompiled classes.

MMJ has been written as a plugin for ImageJ. Please read the [documentation](http://imagejdocu.tudor.lu/doku.php?id=howto:plugins:how_to_install_a_plugin) to learn how to install plugins.

##  II. Technical Explanations

MMJ segments and track cells growing in a microfluidic device known as a mother machine.  

##  III. How to run MMJ

In short, the steps to follow to detect cells and extract fluorescent intensities from the GFP channel are the following:

### 1. Detect cells on phase channel
  - Open stack containing phase channel and run plugin **MMJ>Mother Machine Analyzer**.
  - Define parameters (channel width/height).
  - Identify channels to be analyzed (adding, moving and removing channels).
  - Detect channels.   This step performs a rigid motion correction and automatically detects breakpoints.
  - Manually correct channel position on each frame.
  - Manually correct cell segmentation by adding, moving and removing breakpoints (can also be done with macro described in 2).
  - Save segmentation
  - Close image and return to ImageJ.

### 2. Manually correct segmentation

  - The detection algorithm is not perfect.  The segmentation can be manually corrected with the plugin, but it is sometimes faster to do it using [[http://www.penamiller.com/mmj/public/MMJ_correct_segmentation.ijm | this]] ImageJ macro.  This macro parses the segmentation file and generates a montage for a specific channel where breakpoints can be added or removed.

![](http://penamiller.com/mmj/lib/exe/fetch.php?media=1p_2.jpg)

Before running this macro define the directory and file name for both stacks (the gfp and phase channels) and the segmentation file.  Then run the macro and correct the segmentation using the following commands:

  * Left click: adds breakpoint
  * Right click: deletes breakpoint
  * Ctrl+Left click:  deletes all breakpoints in this slice
  * Ctrl+Right click:  deletes all breakpoints in this slice, then adds a breakpoint
  * Shift+Left click:  defines empty space as cell
  * Shift+Right click: defines cell as empty space
  * Close log window to exit

### 3. Track cells and extract fluorescent intensities

  - Open stack containing fluorescent channel and run plugin **MMJ>Mother Machine Analyzer**
  - Define parameters (channel width/height).
  - Load segmentation 
  - Manually correct cell segmentation (division events are sometimes easier to identify in the fluorescent channel)
  - Save segmentation
  - Track cells
  - Extract GFP values and save results as tab-separated text file.
  - Export cells (click checkbox 'Fluorescent channel', images are saved in the directory cells/)

### Notes:
  * Microfluidic channels have to be perpendicular to the horizontal axis, so rotate image if necessary.
  * Position and dimensions of the microfluidic channels have to be the same in both the phase and fluorescent stacks, so scale images if necessary.
  * The image should contain only the microfluidic channels, so crop image if necessary.
  * Cell detection is performed using a contrast-based algorithm, so contrast (phase) image if necessary.


## IV. User Manual 

The plugin requests at least one open image at launch.  

Segmentation is best if done on Phase channel, and once it is finished it can be saved and then loaded into the GFP channel to extract fluorescent intensity values.

To magnifiy an image, select the magnification tool and click in the image. To minify an image, use the same tool with the <CNTL> or <ALT> keyboard modifier.

![](http://penamiller.com/mmj/lib/exe/fetch.php?media=mmj.jpg)

Note: If every breakpoint in a given channel is deleted and there is another click with the delete breakpoint tool inside this channel, then all breakpoints are deleted in subsequent frames.  This is very useful in case the cell disappears or dies.

## V. License

This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt). 

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. 

## VI. Changelog

  * 2011/12/04: Detect and track mother cells
  * 2011/12/12: Extract GFP intensities
  * 2012/05/23: Track daughter cells
  * 2012/06/05: Release of beta_v2.0
  * 2012/07/24: Cells and channels are drawn in colour, and therefore the image can now be RGB.  Toggle overlay swaps between: original image/channel and breakpoints/cell bounding boxes
  * 2012/07/25: Correct bug that crashed measuring intensities when cells disappeared.
  * 2012/07/25: Temporarily disabled track daughters - only tracks mothers.
  * 2012/08/02: Corrected bug that didn't draw channels when it was too close to the image boundary.
  * 2012/08/14: Corrected bug that didn't undraw channels correctly when manually re-defining their offset.
  * 2012/08/20: Implemented a "delete all breakpoints of this channel from this frame onwards", useful when cells disappear from the channel.
  * 2012/08/02: Corrected bug that crashed the plugin after converting String ID into numerical index.  Cell index is now just an increasing number.
  * 2013/12/11: Implemented manual correction of segmentation in a time-series montage of each channel.
  * 2013/12/11: Now a mother cell doesn't need to be in the bottom of the channel. Empty cells can be defined in the segmentation file by including an '*' between breakpoints.
