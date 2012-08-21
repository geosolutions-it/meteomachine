/*
 *  GeoBatch - Open Source geospatial batch processing system
 *  http://code.google.com/p/geobatch/
 *  Copyright (C) 2007-2011 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.geosolutions.geobatch.metocs.utils;

import it.geosolutions.imageio.stream.input.FileImageInputStreamExtImpl;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReaderSpi;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;

public class ImageCursor {
	final static private TIFFImageReaderSpi TIFF_READER_SPI = new TIFFImageReaderSpi();
	final private Rectangle rect = new Rectangle();
	private RectIter iter = null;
	private ImageReader iReader = null;
	private ImageInputStream imageInputStream = null;
	private BufferedImage imageBuffer=null;
	private Class<?> clazz=null;
	// status
	private boolean initted = false;
	
	// has next status
	private boolean hasNext = false;
	private boolean start=false;
	private Number val=null;
	
	
	public boolean isInitted(){
		return initted;
	}

	public boolean init(final File imageFile, final Class<?> type){
        try {
	        imageInputStream=new FileImageInputStreamExtImpl(imageFile); 
        }
        catch (IOException e){
        	return false;
        }
        try {
	        if (TIFF_READER_SPI.canDecodeInput(imageInputStream)){
	        	iReader=TIFF_READER_SPI.createReaderInstance();
	        	iReader.setInput(imageInputStream);
		        // load into the memory the whole image
		        imageBuffer=iReader.read(iReader.getMinIndex());
	        }
	        else {
	        	//LOG
	        	return false;
	        }
        }
        catch (IOException e){
	    	//LOG
        	try{
	        	if (iReader!=null)
	    	        iReader.dispose();
	        }
	    	catch(Throwable t){
	    		//LOG
	    	}
        	try{
	        	if (imageInputStream!=null)
	        		imageInputStream.close();
        	}
        	catch(IOException ioe){/*eat*/}
	    	return false;
        }
        // set rectangle area for the iterator
		rect.x = 0;
        rect.y = 0;
        rect.width = imageBuffer.getWidth();
        rect.height = imageBuffer.getHeight();
        // init iterator
        iter=RectIterFactory.create(imageBuffer, rect);
        // set iterator to the first pixel
        iter.startBands();
        iter.startLines();
		iter.startPixels();
		// set return type
		this.clazz=type;
		// set status
		initted=true;
		
		// pixel 0,0,0
		hasNext=true;
		start=true;
		val=null;
		
		// return esit
        return true;
	}
	
	public void dispose(){
		initted=false;
		imageBuffer=null;
		//LOG
		try{
        	if (iReader!=null)
    	        iReader.dispose();
        }
    	catch(Throwable t){
    		//LOG
    	}
    	try{
        	if (imageInputStream!=null)
        		imageInputStream.close();
    	}
    	catch(IOException ioe){/*eat*/}
	}

	public boolean hasNext() {
		return hasNext;
	}
	
	public Number getVal() {
		return val;
	}
	
	private boolean next() {
		setVal();
		if (iter.nextPixelDone()){
			iter.startPixels();
			if (iter.nextLineDone()){
				iter.startPixels();
				iter.startLines();
				if (iter.nextBandDone()){
					iter.startPixels();
					iter.startLines();
					iter.startBands();
					hasNext=false;
				}
				else {
					hasNext=true;
				}
			}
			else {
				hasNext=true;
			}
		}
		else {
			hasNext=true;
		}
		
		return hasNext;
		
	}
	
	public Number getNext() {
		next();
		return getVal();
	}
	
	private void setVal(){
		if (clazz==null)
			val= iter.getSample();
		else if (clazz.isAssignableFrom(Integer.class))
			val=iter.getSample();
		else if (clazz.isAssignableFrom(Double.class))
			val=iter.getSampleDouble();
		else if (clazz.isAssignableFrom(Float.class))
			val=iter.getSampleFloat();
		else
			val=iter.getSample();
	}
}
