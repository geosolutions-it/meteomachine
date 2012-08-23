/*
 *  Copyright (C) 2007 - 2011 GeoSolutions S.A.S.
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
package it.geosolutions.geobatch.metocs.netcdf2geotiff.output;

import it.geosolutions.geobatch.metocs.netcdf2geotiff.checker.NetcdfChecker;
import java.io.File;
import java.util.EventObject;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.Variable;

/**
 *
 * @author ETj (etj at geo-solutions.it)
 */
public class DefaultOutputQueueHandler extends OutputQueueHandler<EventObject> {

    private final static Logger LOGGER = LoggerFactory.getLogger(DefaultOutputQueueHandler.class);

    public DefaultOutputQueueHandler(Map<String, Object> configuration, NetcdfChecker checker) {
        super(configuration, checker);
    }

    @Override
    public EventObject writeOutput(File workingDir, Variable var) {
        if(LOGGER.isInfoEnabled())
            LOGGER.info("Not generating output for variable " + var.getFullName());
        return null;
    }
}
