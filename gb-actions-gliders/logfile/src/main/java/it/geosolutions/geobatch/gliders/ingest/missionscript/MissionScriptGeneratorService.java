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
package it.geosolutions.geobatch.gliders.ingest.missionscript;

import it.geosolutions.geobatch.catalog.impl.BaseService;
import it.geosolutions.geobatch.flow.event.action.ActionService;

import java.util.EventObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Class MissionScriptGeneratorService.
 * 
 * @author Tobia Di Pisa - tobia.dipisa@geo-solutions.it
 *
 */
public class MissionScriptGeneratorService extends BaseService implements ActionService<EventObject, MissionScriptConfiguration>
{

    private static final Logger LOGGER = LoggerFactory.getLogger(MissionScriptGeneratorService.class);

    /**
     * @param id
     * @param name
     * @param description
     */
    public MissionScriptGeneratorService(String id, String name, String description)
    {
        super(id, name, description);
    }

    /* (non-Javadoc)
     * @see it.geosolutions.geobatch.flow.event.action.ActionService#createAction(it.geosolutions.geobatch.configuration.event.action.ActionConfiguration)
     */
    public MissionScriptAction createAction(MissionScriptConfiguration configuration)
    {
        try
        {
        	MissionScriptAction glidersLogsAction = new MissionScriptAction(configuration);
        	
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
    public boolean canCreateAction(MissionScriptConfiguration configuration)
    {
    	if(LOGGER.isInfoEnabled()){
    		LOGGER.info("-------------------> Checking setting parameters");
    	}
        

//    	
//    	private String placemarkLogoAnchor = null;
//    	
//    	private String placemarkLogoURL = null;
    	
    	
        try
        {
        	String cruiseName = configuration.getCruiseName();
        	
            if (cruiseName != null){
                LOGGER.info("Cruise Name value is " + cruiseName);
            }else{
                if (LOGGER.isWarnEnabled()){
                    LOGGER.warn("MissionScriptGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get the Cruise Name.");
                }

                return false;
            }
            
        	String unzipPath = configuration.getUnzipPath();
        	
            if (unzipPath != null){
                LOGGER.info("Unzip Path value is " + unzipPath);
            }else{
                if (LOGGER.isWarnEnabled()){
                    LOGGER.warn("MissionScriptGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get the Unzip Path.");
                }

                return false;
            }
            
        	Integer coordsApprox = configuration.getCoordsApprox();
        	
            if (coordsApprox != null){
                LOGGER.info("Coordinate Approximation value is " + coordsApprox);
            }
            
        	Double kmlIconScale = configuration.getKmlIconScale();
        	
            if (kmlIconScale != null){
                LOGGER.info("KML Icon Scale value is " + kmlIconScale);
            }else{
                if (LOGGER.isWarnEnabled()){
                    LOGGER.warn("MissionScriptGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get the KML Icon Scale.");
                }

                return false;
            }
            
        	String kmlFirstIconURLpath = configuration.getKmlFirstIconURLpath();
        	
            if (kmlFirstIconURLpath != null){
                LOGGER.info("First Icon URL path value is " + kmlFirstIconURLpath);
            }else{
                if (LOGGER.isWarnEnabled()){
                    LOGGER.warn("MissionScriptGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get the First Icon URL path.");
                }

                return false;
            }
            
        	String kmlSecondIconURLpath = configuration.getKmlSecondIconURLpath();
        	
            if (kmlSecondIconURLpath != null){
                LOGGER.info("Second Icon URL path value is " + kmlSecondIconURLpath);
            }else{
                if (LOGGER.isWarnEnabled()){
                    LOGGER.warn("MissionScriptGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get the Second Icon URL path.");
                }

                return false;
            }
            
        	String placemarkLogoAnchor = configuration.getPlacemarkLogoAnchor();
        	
            if (placemarkLogoAnchor != null){
                LOGGER.info("PlacemarkLogo Anchor value is " + placemarkLogoAnchor);
            }else{
                if (LOGGER.isWarnEnabled()){
                    LOGGER.warn("MissionScriptGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get the PlacemarkLogo Anchor.");
                }

                return false;
            }
            
        	String placemarkLogoURL = configuration.getPlacemarkLogoURL();
        	
            if (placemarkLogoURL != null){
                LOGGER.info("PlacemarkLogo Logo URL value is " + placemarkLogoURL);
            }else{
                if (LOGGER.isWarnEnabled()){
                    LOGGER.warn("MissionScriptGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get the PlacemarkLogo Logo URL.");
                }

                return false;
            }
            
        	String kmlOutputPath = configuration.getKmlOutputPath();
        	
            if (kmlOutputPath != null){
                LOGGER.info("KML Output path value is " + kmlOutputPath);
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
