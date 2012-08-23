/*
 *  GeoBatch - Open Source geospatial batch processing system
 *  http://code.google.com/p/geobatch/
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
package it.geosolutions.geobatch.metocs.forecastcleaner;

import it.geosolutions.filesystemmonitor.monitor.FileSystemEvent;
import it.geosolutions.geobatch.catalog.impl.BaseService;
import it.geosolutions.geobatch.configuration.event.action.ActionConfiguration;
import it.geosolutions.geobatch.flow.event.action.ActionService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Public class to generate forecast cleaner actions
 * 
 */
public class ForecastCleanerGeneratorService
    extends BaseService
    implements ActionService<FileSystemEvent, ActionConfiguration> {

    private final static Logger LOGGER = LoggerFactory.getLogger(ForecastCleanerGeneratorService.class);

    public ForecastCleanerGeneratorService(String id) {
        super(id);
    }

    /**
     * @deprecated name and description are not used
     */
    public ForecastCleanerGeneratorService(String id, String name, String description) {
        super(id, name, description);
    }

    @Override
    public boolean canCreateAction(ActionConfiguration configuration) {
        return configuration instanceof ForecastCleanerConfiguration;
    }

    @Override
    public ForecastCleanerAction createAction(ActionConfiguration configuration) {
        return new ForecastCleanerAction((ForecastCleanerConfiguration)configuration);
    }

}