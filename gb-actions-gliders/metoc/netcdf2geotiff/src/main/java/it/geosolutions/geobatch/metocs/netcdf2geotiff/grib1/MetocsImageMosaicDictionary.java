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

package it.geosolutions.geobatch.metocs.netcdf2geotiff.grib1;

import java.util.Map;

import it.geosolutions.geobatch.metocs.netcdf2geotiff.checker.MetocsBaseDictionary;

/**
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 *
 */
public class MetocsImageMosaicDictionary extends MetocsBaseDictionary {

	public MetocsImageMosaicDictionary(
			Map<String, Map<String, String>> dictionary) {
		super(dictionary);
	}
	
	//a list of styles comma separated
	public final static String STYLES_KEY="styles";
	
	// the default style
	public final static String DEFAULT_STYLE_KEY="defaultStyle";

	// TODO
//	backgroundValue
//	outputTransparentColor
//	inputTransparentColor
//	allowMultithreading
//	useJaiImageRead
//	tileSizeH
//	tileSizeW
//	
////	<!--NONE, REPROJECT_TO_DECLARED, FORCE_DECLARED-->
//	projectionPolicy
////	<!-- METADATA -->
//	timeRegex
//	timeDimEnabled
////	<!-- LIST, CONTINUOUS_INTERVAL, DISCRETE_INTERVAL -->
//	timePresentationMode
//	
//	elevationRegex
//	elevDimEnabled
//	elevationPresentationMode
//
//	datastorePropertiesPath
	
	
	

}
