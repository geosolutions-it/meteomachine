/*
 *  GeoBatch - Open Source geospatial batch processing system
 *  http://code.google.com/p/geobatch/
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

import it.geosolutions.geobatch.catalog.impl.BaseService;
import it.geosolutions.geobatch.flow.event.action.ActionService;

import java.io.IOException;
import java.util.EventObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Public class to generate NetCDF_CF-2-GeoTIFFs Services
 * 
 */
public class Netcdf2GeotiffService extends BaseService implements
        ActionService<EventObject, Netcdf2GeotiffConfiguration> {

    private final static Logger LOGGER = LoggerFactory
            .getLogger(Netcdf2GeotiffService.class);

    public Netcdf2GeotiffService(String id, String name, String description) {
        super(id, name, description);
    }

    /**
     * Action creator
     * 
     * @param configuration
     *            The data base action configuration
     * @return new JGSFLoDeSSSWANFileConfigurator()
     */
    public Netcdf2GeotiffAction createAction(
            Netcdf2GeotiffConfiguration configuration) {
        try {
            return new Netcdf2GeotiffAction(configuration);
        } catch (IOException e) {
            if (LOGGER.isErrorEnabled())
                LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    public boolean canCreateAction(Netcdf2GeotiffConfiguration configuration) {
        return true;
    }

}