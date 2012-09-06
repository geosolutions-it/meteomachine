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
package it.geosolutions.geobatch.metocs.netcdf2geotiff.impl.grib1;

import it.geosolutions.geobatch.metocs.netcdf2geotiff.spi.MetocsBaseDictionary;
import it.geosolutions.geobatch.metocs.netcdf2geotiff.spi.NetcdfLoader;

import java.io.File;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.nc2.NetcdfFile;

import it.geosolutions.geobatch.metocs.netcdf2geotiff.spi.DefaultDictionaryLoader;
import it.geosolutions.geobatch.metocs.netcdf2geotiff.spi.DefaultSPI;
import it.geosolutions.geobatch.metocs.netcdf2geotiff.spi.NetcdfVariable;
import it.geosolutions.geobatch.metocs.netcdf2geotiff.spi.geotiff.DefaultGeoTiffNameBuilder;
import it.geosolutions.geobatch.metocs.netcdf2geotiff.spi.geotiff.GeoTiffNameBuilder;
import it.geosolutions.geobatch.metocs.netcdf2geotiff.spi.output.IMCOutputHandler;
import it.geosolutions.geobatch.metocs.netcdf2geotiff.spi.output.OutputQueueHandler;
import java.io.IOException;

/**
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * @author ETj
 *
 */
public class Grib1SPI extends DefaultSPI {
    protected final static Logger LOGGER = LoggerFactory.getLogger(Grib1SPI.class);

//    static{
//        try {
//            // NetcdfFile.class.getClassLoader().loadClass("ucar.grib.grib2.Grib2Input");
//            // // only load if grib.jar is present
//            NetcdfFile.class.getClassLoader().loadClass("ucar.nc2.iosp.grib.Grib2Netcdf"); // only
//            Class c=NetcdfFile.class.getClassLoader().loadClass("ucar.nc2.iosp.grib.GribGridServiceProvider");
//            NetcdfFile.registerIOProvider(c);
//        } catch (Throwable e) {
//            if (LOGGER.isErrorEnabled())
//                LOGGER.error("Cant load class: " + e);
//        }
//    }


    @Override
    public MetocsBaseDictionary buildDictionary(final File dictionaryFile) {
        return new DefaultDictionaryLoader()
        {
            public MetocsBaseDictionary loadDictionary(final File dictionaryFile) {
                Map<String, Map<String, String>> dictionary = getDefaultDictionary();
                loadDictionary(dictionaryFile, dictionary);
                return new MetocsImageMosaicDictionary(dictionary);
            }

            @Override
            protected Map<String, Map<String, String>> getDefaultDictionary() {
                Map<String,Map<String,String>> dictionary=new HashMap<String, Map<String,String>>();
                Map<String,String> root=new HashMap<String,String>();
                dictionary.put(MetocsBaseDictionary.ROOT_SECTION_KEY, root);
                root.put(MetocsBaseDictionary.RUNTIME_KEY, "_CoordinateModelRunDate");
                root.put(MetocsBaseDictionary.BASETIME_KEY, "GRIB_orgReferenceTime");
                root.put(MetocsBaseDictionary.FILLVALUE_KEY, "missing_value");

                root.put(MetocsBaseDictionary.LONGITUDE_KEY, "lon");
                root.put(MetocsBaseDictionary.LATITUDE_KEY, "lat");
                root.put(MetocsBaseDictionary.TIME_KEY, "time");
                // TODO Z check the standard elevation name...

                final Map<String, String> time = new HashMap<String, String>();
                dictionary.put("time", time);
                time.put(MetocsBaseDictionary.BASETIME_KEY, "GRIB_orgReferenceTime");
                time.put(MetocsBaseDictionary.TAU_KEY, "3600000"); // 1 hour in millisecs
                return dictionary;
            }
        }.loadDictionary(dictionaryFile);
    }

    public boolean canRead(String type) {
        return type.contains("grib"); // TODO change with a more formal value!!!
    }

    @Override
	public NetcdfLoader buildLoader(NetcdfFile ncFileIn, File dictionary) throws Exception {
			return new Grib1Loader(ncFileIn, dictionary, this);
	}

    @Override
    public OutputQueueHandler<EventObject> buildOutputQueueHandler(Map<String, Object> cfg, final NetcdfLoader loader) {
        return new IMCOutputHandler(cfg, loader) {
            @Override
            protected File buildOutputFile(NetcdfVariable var, File outputDir) throws IOException {
                String baseName =
                        var.getPrefix() + var.getFullName() + var.getSuffix()
                        + "_" + var.getGlobal().getRuntime();
                return  File.createTempFile(baseName + "_", "_ImgMscCmd.xml", outputDir);
            }
        };
    }

    @Override
    public GeoTiffNameBuilder buildGeoTiffNameBuilder(NetcdfLoader checker) {
        return new DefaultGeoTiffNameBuilder(checker) {
            @Override
            public String getDirName(final NetcdfVariable var, Map<String, Object> tokens) {
                return var.getFullName() + "_" + var.getGlobal().getRuntime();
            }        
        };
    }


}
