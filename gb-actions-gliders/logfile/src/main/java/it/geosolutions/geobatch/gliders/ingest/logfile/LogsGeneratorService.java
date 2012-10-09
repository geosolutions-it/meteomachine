/*
 * ====================================================================
 *
 * GeoBatch - Intersection Engine
 *
 * Copyright (C) 2007 - 2011 GeoSolutions S.A.S.
 * http://www.geo-solutions.it
 *
 * GPLv3 + Classpath exception
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.
 *
 * ====================================================================
 *
 * This software consists of voluntary contributions made by developers
 * of GeoSolutions.  For more information on GeoSolutions, please see
 * <http://www.geo-solutions.it/>.
 *
 */
package it.geosolutions.geobatch.gliders.ingest.logfile;

import it.geosolutions.geobatch.catalog.impl.BaseService;
import it.geosolutions.geobatch.flow.event.action.ActionService;

import java.util.EventObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Class LogsGeneratorService.
 * 
 * @author Tobia Di Pisa - tobia.dipisa@geo-solutions.it
 *
 */
public class LogsGeneratorService extends BaseService implements ActionService<EventObject, LogsConfiguration>
{

    private static final Logger LOGGER = LoggerFactory.getLogger(LogsGeneratorService.class);

    /**
     * @param id
     * @param name
     * @param description
     */
    public LogsGeneratorService(String id, String name, String description)
    {
        super(id, name, description);
    }

    /* (non-Javadoc)
     * @see it.geosolutions.geobatch.flow.event.action.ActionService#createAction(it.geosolutions.geobatch.configuration.event.action.ActionConfiguration)
     */
    public LogsAction createAction(LogsConfiguration configuration)
    {
        try
        {
        	LogsAction glidersLogsAction = new LogsAction(configuration);
            return glidersLogsAction;
        }
        catch (Exception e)
        {
            if (LOGGER.isInfoEnabled())
            {
                LOGGER.info(e.getLocalizedMessage(), e);
            }

            return null;
        }
    }

    /* (non-Javadoc)
     * @see it.geosolutions.geobatch.flow.event.action.ActionService#canCreateAction(it.geosolutions.geobatch.configuration.event.action.ActionConfiguration)
     */
    public boolean canCreateAction(LogsConfiguration configuration)
    {
    	if(LOGGER.isInfoEnabled()){
    		LOGGER.info("-------------------> Checking setting parameters");
    	}
        
        try
        {
            // absolutize working dir

        }
        catch (Throwable e)
        {
            if (LOGGER.isErrorEnabled())
            {
                LOGGER.error(e.getLocalizedMessage(), e);
            }

            return false;
        }

        return true;
    }
}
