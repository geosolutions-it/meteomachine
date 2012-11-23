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
import it.geosolutions.geostore.services.rest.GeoStoreClient;
import it.geosolutions.geostore.services.rest.model.RESTCategory;

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
        	
            GeoStoreClient client = new GeoStoreClient();
            client.setGeostoreRestUrl(configuration.getGeostoreURL());
            client.setUsername(configuration.getGeostoreUs());
            client.setPassword(configuration.getGeostorePw());
            
            glidersLogsAction.setGeoStoreClient(client);
            
            // ///////////////////////////////////////
            // Check if the LOGFILE category exists 
            // ///////////////////////////////////////
            
            long count = client.getCategoryCount(configuration.getLogfileCategoryName());
            RESTCategory category = new RESTCategory(configuration.getLogfileCategoryName());
            if(count < 1){
            	client.insert(category);
            }

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
        	String cruiseName = configuration.getCruiseName();
        	
            if (cruiseName != null){
                LOGGER.info("Cruise Name value is " + cruiseName);
            }else{
                if (LOGGER.isWarnEnabled()){
                    LOGGER.warn("LogsGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get the Cruise Name.");
                }

                return false;
            }
            
        	String geostoreURL = configuration.getGeostoreURL();
        	
            if (geostoreURL != null){
                LOGGER.info("GeoStore URL value is " + geostoreURL);
            }else{
                if (LOGGER.isWarnEnabled()){
                    LOGGER.warn("LogsGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get the GeoStore URL.");
                }

                return false;
            }
            
        	String geostoreUs = configuration.getGeostoreUs();
        	
            if (geostoreUs != null){
                LOGGER.info("GeoStore Us value is " + geostoreUs);
            }else{
                if (LOGGER.isWarnEnabled()){
                    LOGGER.warn("LogsGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get the GeoStore Us.");
                }

                return false;
            }
            
        	String geostorePw = configuration.getGeostorePw();
        	
            if (geostorePw != null){
                LOGGER.info("GeoStore Us value is " + geostorePw);
            }else{
                if (LOGGER.isWarnEnabled()){
                    LOGGER.warn("LogsGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get the GeoStore Pw.");
                }

                return false;
            }
            
        	String timePattern = configuration.getTimePattern();
        	
            if (timePattern != null){
                LOGGER.info("Curr. Time pattern value is " + timePattern);
            }else{
                if (LOGGER.isWarnEnabled()){
                    LOGGER.warn("LogsGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get the Curr. Time pattern.");
                }

                return false;
            }
            
        	String logfileCategoryName = configuration.getLogfileCategoryName();
        	
            if (logfileCategoryName != null){
                LOGGER.info("Logfile category name value is " + logfileCategoryName);
            }else{
                if (LOGGER.isWarnEnabled()){
                    LOGGER.warn("LogsGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get the Logfile category name.");
                }

                return false;
            }
            
        	String cruiseDir = configuration.getCruiseDir();
        	
            if (cruiseDir != null){
                LOGGER.info("Logfile cruise dir name value is " + cruiseDir);
            }else{
                if (LOGGER.isWarnEnabled()){
                    LOGGER.warn("LogsGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get the Logfile cruise dir.");
                }

                return false;
            }
        }
        catch (Throwable e){
            if (LOGGER.isErrorEnabled()){
                LOGGER.error(e.getLocalizedMessage(), e);
            }

            return false;
        }

        return true;
    }
}
