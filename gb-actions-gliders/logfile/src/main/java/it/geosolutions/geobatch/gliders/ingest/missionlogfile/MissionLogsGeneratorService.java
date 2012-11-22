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
package it.geosolutions.geobatch.gliders.ingest.missionlogfile;

import it.geosolutions.geobatch.catalog.impl.BaseService;
import it.geosolutions.geobatch.flow.event.action.ActionService;

import java.util.EventObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Class MissionLogsGeneratorService.
 * 
 * @author Tobia Di Pisa - tobia.dipisa@geo-solutions.it
 *
 */
public class MissionLogsGeneratorService extends BaseService implements ActionService<EventObject, MissionLogsConfiguration>
{

    private static final Logger LOGGER = LoggerFactory.getLogger(MissionLogsGeneratorService.class);

    /**
     * @param id
     * @param name
     * @param description
     */
    public MissionLogsGeneratorService(String id, String name, String description)
    {
        super(id, name, description);
    }

    /* (non-Javadoc)
     * @see it.geosolutions.geobatch.flow.event.action.ActionService#createAction(it.geosolutions.geobatch.configuration.event.action.ActionConfiguration)
     */
    public MissionLogsAction createAction(MissionLogsConfiguration configuration)
    {
        try
        {
        	MissionLogsAction glidersLogsAction = new MissionLogsAction(configuration);
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
    public boolean canCreateAction(MissionLogsConfiguration configuration)
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
                    LOGGER.warn("MissionLogsGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get the Cruise Name.");
                }

                return false;
            }
            
        	String geostoreURL = configuration.getGeostoreURL();
        	
            if (geostoreURL != null){
                LOGGER.info("GeoStore URL value is " + geostoreURL);
            }else{
                if (LOGGER.isWarnEnabled()){
                    LOGGER.warn("MissionLogsGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get the GeoStore URL.");
                }

                return false;
            }
            
        	String geostoreUs = configuration.getGeostoreUs();
        	
            if (geostoreUs != null){
                LOGGER.info("GeoStore Us value is " + geostoreUs);
            }else{
                if (LOGGER.isWarnEnabled()){
                    LOGGER.warn("MissionLogsGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get the GeoStore Us.");
                }

                return false;
            }
            
        	String geostorePw = configuration.getGeostorePw();
        	
            if (geostorePw != null){
                LOGGER.info("GeoStore Us value is " + geostorePw);
            }else{
                if (LOGGER.isWarnEnabled()){
                    LOGGER.warn("MissionLogsGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get the GeoStore Pw.");
                }

                return false;
            }
            
        	String timePattern = configuration.getTimePattern();
        	
            if (timePattern != null){
                LOGGER.info("Curr. Time pattern value is " + timePattern);
            }else{
                if (LOGGER.isWarnEnabled()){
                    LOGGER.warn("MissionLogsGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get the Curr. Time pattern.");
                }

                return false;
            }
            
        	String categoryName = configuration.getCategoryName();
        	
            if (categoryName != null){
                LOGGER.info("Mission Logfile category name value is " + categoryName);
            }else{
                if (LOGGER.isWarnEnabled()){
                    LOGGER.warn("MissionLogsGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get the Logfile category name.");
                }

                return false;
            }
            
        	String cruiseDir = configuration.getCruiseDir();
        	
            if (cruiseDir != null){
                LOGGER.info("Mission Logfile cruise dir name value is " + cruiseDir);
            }else{
                if (LOGGER.isWarnEnabled()){
                    LOGGER.warn("MissionLogsGeneratorService::canCreateAction(): " +
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
