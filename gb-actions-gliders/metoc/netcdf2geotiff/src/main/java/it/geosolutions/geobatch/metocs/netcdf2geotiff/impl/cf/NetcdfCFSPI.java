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
package it.geosolutions.geobatch.metocs.netcdf2geotiff.impl.cf;

import it.geosolutions.geobatch.metocs.netcdf2geotiff.spi.dict.DefaultDictionaryLoader;
import it.geosolutions.geobatch.metocs.netcdf2geotiff.spi.DefaultSPI;
import it.geosolutions.geobatch.metocs.netcdf2geotiff.spi.dict.MetocsBaseDictionary;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author etj
 */
public class NetcdfCFSPI extends DefaultSPI {

    private final static Logger LOGGER = LoggerFactory.getLogger(NetcdfCFSPI.class);

    @Override
    public MetocsBaseDictionary buildDictionary(final File dictionaryFile) {
        return new DefaultDictionaryLoader()
        {
            @Override
            protected Map<String, Map<String, String>> getDefaultDictionary() {
                Map<String,Map<String,String>> dictionary=new HashMap<String, Map<String,String>>();
                Map<String,String> root=new HashMap<String,String>();
                dictionary.put(MetocsBaseDictionary.ROOT_SECTION_KEY, root);
//                root.put(MetocsBaseDictionary.BASETIME_KEY,"base_time");
                root.put(MetocsBaseDictionary.TAU_KEY,"tau");
                root.put(MetocsBaseDictionary.NODATA_KEY,"nodata");
                return dictionary;
            }
        }.loadDictionary(dictionaryFile);
    }

}
