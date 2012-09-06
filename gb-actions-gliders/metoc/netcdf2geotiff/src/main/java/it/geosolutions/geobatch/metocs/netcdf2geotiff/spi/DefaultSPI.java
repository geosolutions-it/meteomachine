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
package it.geosolutions.geobatch.metocs.netcdf2geotiff.spi;

import it.geosolutions.geobatch.metocs.netcdf2geotiff.spi.dict.MetocsBaseDictionary;
import it.geosolutions.geobatch.metocs.netcdf2geotiff.spi.dict.DefaultDictionaryLoader;
import it.geosolutions.geobatch.metocs.netcdf2geotiff.spi.geotiff.DefaultGeoTiffNameBuilder;
import it.geosolutions.geobatch.metocs.netcdf2geotiff.spi.geotiff.GeoTiffNameBuilder;
import it.geosolutions.geobatch.metocs.netcdf2geotiff.spi.output.DefaultOutputQueueHandler;
import it.geosolutions.geobatch.metocs.netcdf2geotiff.spi.output.OutputQueueHandler;

import java.io.File;
import java.util.EventObject;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.nc2.NetcdfFile;

/**
 * @author etj
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 */
public class DefaultSPI implements NetcdfSPI {

    private final static Logger LOGGER = LoggerFactory.getLogger(DefaultSPI.class);

    @Override
    public MetocsBaseDictionary buildDictionary(final File dictionaryFile) {
        return new DefaultDictionaryLoader().loadDictionary(dictionaryFile);
    }

    @Override
    public boolean canRead(String type) {
        return type.contains("netcdf"); 
    }

    @Override
	public NetcdfLoader buildLoader(NetcdfFile ncFileIn, File dictionary) throws Exception {
		return new NetcdfLoader(ncFileIn, dictionary,this);
	}

    @Override
    public OutputQueueHandler<EventObject> buildOutputQueueHandler(Map<String, Object> cfg, NetcdfLoader checker) {
        return new DefaultOutputQueueHandler(cfg, checker);
    }

    @Override
    public GeoTiffNameBuilder buildGeoTiffNameBuilder(NetcdfLoader checker) {
        return new DefaultGeoTiffNameBuilder(checker);
    }

  /**
     * Implementation priority.
     * When looking for a SPI implementation, the implementation with the highest
     * priority will be choosen.
     * <br/>
     * The priority may be set using an OverrideProperty in the custom webapp.
     */
    public int priority = -1;

    /**
     * Implementation priority.
     * When looking for a SPI implementation, the implementation with the highest
     * priority will be choosen.
     * <br/>
     * The priority may be set using an OverrideProperty in the custom webapp.
     */
    @Override
    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
}
