/*
 *  GeoBatch - Open Source geospatial batch processing system
 *  http://geobatch.codehaus.org/
 *  Copyright (C) 2007-2008-2009 GeoSolutions S.A.S.
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
package it.geosolutions.geobatch.metocs.netcdf2geotiff;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ucar.nc2.Variable;

/**
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 *
 * @param <Type> the type of the output
 */
public abstract class Netcdf2GeotiffOutput <Type> {
	
	/**
	 * contains the list of the output files for a given variable
	 */
	private final List<File> outList = new ArrayList<File>();
    
    public void addOutput(final File file){
    	outList.add(file);
    }
    
    public List<File> getOutList(){
    	return outList;
    }

    
	/**
	 * For a certain variable we may want to add an event or collect it
	 * to generate an event
	 * @param workingDir
	 * @param var
	 * @return can return a Type object which will be appended to the
	 * queue for the next action or Null which will result in an empy queue.
	 * @note this method will be called for each variable found (and selected 
	 * from configuration) into the netcdf.
	 */
	public abstract Type writeOutput(final File workingDir, final Variable var);
}
