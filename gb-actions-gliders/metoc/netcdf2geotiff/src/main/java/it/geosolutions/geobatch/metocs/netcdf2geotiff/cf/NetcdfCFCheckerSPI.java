/*
 *  GeoBatch - Open Source geospatial batch processing system
 *  http://geobatch.codehaus.org/
 *  Copyright (C) 2007-2012 GeoSolutions S.A.S.
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
package it.geosolutions.geobatch.metocs.netcdf2geotiff.cf;

import it.geosolutions.geobatch.metocs.netcdf2geotiff.checker.AbsCheckerSPI;
import it.geosolutions.geobatch.metocs.netcdf2geotiff.checker.MetocsBaseDictionary;
import it.geosolutions.geobatch.metocs.netcdf2geotiff.checker.NetcdfChecker;

import java.io.File;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.nc2.NetcdfFile;

/**
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * @author etj
 *
 */
public class NetcdfCFCheckerSPI extends AbsCheckerSPI {

    private final static Logger LOGGER = LoggerFactory.getLogger(NetcdfCFCheckerSPI.class);
    
    public MetocsBaseDictionary readDictionary(final File dictionaryFile){
        Map<String,Map<String,String>> dictionary=new HashMap<String, Map<String,String>>();
        
        /*
         * adding defaults
         */
        Map<String,String> root=new HashMap<String,String>();
        dictionary.put(MetocsBaseDictionary.ROOT_SECTION_KEY, root);
        root.put(MetocsBaseDictionary.BASETIME_KEY,"base_time");
        root.put(MetocsBaseDictionary.TAU_KEY,"tau");
        root.put(MetocsBaseDictionary.TAU_KEY,"nodata"); // FIXME: same key as above!

        loadDictionary(dictionaryFile, dictionary);

        return new MetocsBaseDictionary(dictionary);
    }

    public boolean canRead(String type) {
        return type.contains("netcdf"); //TODO change with correct value!!!
    }

	public NetcdfChecker<EventObject> getChecker(NetcdfFile ncFileIn,
			File dictionary) throws Exception {
		return new NetcdfCFChecker(ncFileIn, dictionary,this);
	}


}
