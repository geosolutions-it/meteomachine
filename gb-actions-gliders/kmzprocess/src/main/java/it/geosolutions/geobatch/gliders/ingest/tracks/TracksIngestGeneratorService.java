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
package it.geosolutions.geobatch.gliders.ingest.tracks;

import it.geosolutions.geobatch.catalog.impl.BaseService;
import it.geosolutions.geobatch.flow.event.action.ActionService;

import java.util.EventObject;

import org.geotools.data.ows.HTTPClient;
import org.geotools.data.ows.SimpleHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Alessio Fabiani - alessio.fabiani@geo-solutions.it
 *
 */
public class TracksIngestGeneratorService extends BaseService implements ActionService<EventObject, TracksIngestConfiguration>
{

    private static final Logger LOGGER = LoggerFactory.getLogger(TracksIngestGeneratorService.class);

    private HTTPClient wpsHTTPClient = null;
    
    public TracksIngestGeneratorService(String id, String name, String description)
    {
        super(id, name, description);
    }

    public TracksIngestAction createAction(TracksIngestConfiguration configuration)
    {
        try
        {
        	TracksIngestAction glidersTracksIngestAction = new TracksIngestAction(configuration);
        	
        	wpsHTTPClient = new SimpleHttpClient();
        	wpsHTTPClient.setConnectTimeout(configuration.getWpsHTTPClientConnectionTimeout());
        	wpsHTTPClient.setPassword(configuration.getWpsHTTPClientPassword());
        	wpsHTTPClient.setReadTimeout(configuration.getWpsHTTPClientReadTimeout());
        	wpsHTTPClient.setUser(configuration.getWpsHTTPClientUser());
        	
        	glidersTracksIngestAction.setWpsHTTPClient(wpsHTTPClient);

            return glidersTracksIngestAction;
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

    public boolean canCreateAction(TracksIngestConfiguration configuration)
    {
        LOGGER.info("------------------->Checking setting parameters");
        try
        {
            // absolutize working dir
        	String wpsServiceCapabilitiesURL = configuration.getWpsServiceCapabilitiesURL();
            String wpsProcessIdentifier = configuration.getWpsProcessIdentifier();
            String targetWorkspace = configuration.getTargetWorkspace();
            String targetDataStore = configuration.getTargetDataStore();
            String aoiKmzFormatOptions = configuration.getAoiKmzFormatOptions();
            String aoiKmzStyles = configuration.getAoiKmzLayers();
            String aoiKmzLayers = configuration.getAoiKmzStyles();
            String gsBaseUrl = configuration.getGsBaseUrl();
            
            String wpsHTTPClientUser = configuration.getWpsHTTPClientUser();
            String wpsHTTPClientPassword = configuration.getWpsHTTPClientPassword();
            Integer wpsHTTPClientConnectionTimeout = Integer.valueOf(configuration.getWpsHTTPClientConnectionTimeout());
            Integer wpsHTTPClientReadTimeout = Integer.valueOf(configuration.getWpsHTTPClientReadTimeout());

            if (wpsServiceCapabilitiesURL != null)
            {
                LOGGER.info("WPS GetCapabilities URL value is " + wpsServiceCapabilitiesURL);
            }
            else
            {
                if (LOGGER.isWarnEnabled())
                {
                    LOGGER.warn("TracksIngestGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get the WPS GetCapabilities URL.");
                }

                return false;
            }

            if (wpsProcessIdentifier != null)
            {
                LOGGER.info("WPS Process Identifier is " + wpsProcessIdentifier);

            }
            else
            {
                if (LOGGER.isWarnEnabled())
                {
                    LOGGER.warn("TracksIngestGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get the WPS Process Identifier.");
                }

                return false;
            }

            if (targetWorkspace != null && targetDataStore != null)
            {
                LOGGER.info("WPS Process target Workspace and DataStore are " + targetWorkspace+":"+targetDataStore);

            }
            else
            {
                if (LOGGER.isWarnEnabled())
                {
                    LOGGER.warn("TracksIngestGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get the WPS Process target Workspace and DataStore.");
                }

                return false;
            }
            
            if (aoiKmzFormatOptions != null)
            {
                LOGGER.info("WPS aoiKmzFormatOptions " + aoiKmzFormatOptions);

            }
            else
            {
                if (LOGGER.isWarnEnabled())
                {
                    LOGGER.warn("TracksIngestGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get the aoiKmzFormatOptions.");
                }

                return false;
            }
            
            if (aoiKmzStyles != null)
            {
                LOGGER.info("WPS aoiKmzStyles " + aoiKmzStyles);

            }
            else
            {
                if (LOGGER.isWarnEnabled())
                {
                    LOGGER.warn("TracksIngestGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get the aoiKmzStyles.");
                }

                return false;
            }
            
            if (aoiKmzLayers != null)
            {
                LOGGER.info("WPS aoiKmzLayers " + aoiKmzLayers);

            }
            else
            {
                if (LOGGER.isWarnEnabled())
                {
                    LOGGER.warn("TracksIngestGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get the aoiKmzLayers.");
                }

                return false;
            }
            
            if (gsBaseUrl != null)
            {
                LOGGER.info("WPS gsBaseUrl " + gsBaseUrl);

            }
            else
            {
                if (LOGGER.isWarnEnabled())
                {
                    LOGGER.warn("TracksIngestGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get the gsBaseUrl.");
                }

                return false;
            }
            
            if (wpsHTTPClientUser != null)
            {
                LOGGER.info("WPS wpsHTTPClientUser " + wpsHTTPClientUser);

            }
            else
            {
                if (LOGGER.isWarnEnabled())
                {
                    LOGGER.warn("TracksIngestGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get the wpsHTTPClientUser.");
                }

                return false;
            }
            
            if (wpsHTTPClientPassword != null)
            {
                LOGGER.info("WPS wpsHTTPClientPassword " + wpsHTTPClientPassword);

            }
            else
            {
                if (LOGGER.isWarnEnabled())
                {
                    LOGGER.warn("TracksIngestGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get the wpsHTTPClientPassword.");
                }

                return false;
            }
            
            if (wpsHTTPClientConnectionTimeout != null)
            {
                LOGGER.info("WPS wpsHTTPClientConnectionTimeout " + wpsHTTPClientConnectionTimeout);

            }
            else
            {
                if (LOGGER.isWarnEnabled())
                {
                    LOGGER.warn("TracksIngestGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get the wpsHTTPClientConnectionTimeout.");
                }

                return false;
            }
            
            if (wpsHTTPClientReadTimeout != null)
            {
                LOGGER.info("WPS wpsHTTPClientReadTimeout " + wpsHTTPClientReadTimeout);

            }
            else
            {
                if (LOGGER.isWarnEnabled())
                {
                    LOGGER.warn("TracksIngestGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get the wpsHTTPClientReadTimeout.");
                }

                return false;
            }

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
