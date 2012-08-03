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
package it.geosolutions.geobatch.gliders.ingest;

import it.geosolutions.geobatch.catalog.Configuration;
import it.geosolutions.geobatch.configuration.event.action.ActionConfiguration;


/**
 *
 * @author Alessio Fabiani - alessio.fabiani@geo-solutions.it
 *
 */

public class TracksIngestConfiguration extends ActionConfiguration implements Configuration
{

    private String wpsServiceCapabilitiesURL = null;
    private String wpsProcessIdentifier = null;
    private String targetWorkspace = null;
    private String targetDataStore = null;
    
	public TracksIngestConfiguration(String id, String name, String description)
    {
        super(id, name, description);

        // TODO INITIALIZE MEMBERS
    }

    // TODO ADD YOUR MEMBERS

    /**
	 * @param wpsServiceCapabilitiesURL the wpsServiceCapabilitiesURL to set
	 */
	public void setWpsServiceCapabilitiesURL(String wpsServiceCapabilitiesURL) {
		this.wpsServiceCapabilitiesURL = wpsServiceCapabilitiesURL;
	}

	/**
	 * @return the wpsServiceCapabilitiesURL
	 */
	public String getWpsServiceCapabilitiesURL() {
		return wpsServiceCapabilitiesURL;
	}

	/**
	 * @param wpsProcessIdentifier the wpsProcessIdentifier to set
	 */
	public void setWpsProcessIdentifier(String wpsProcessIdentifier) {
		this.wpsProcessIdentifier = wpsProcessIdentifier;
	}

	/**
	 * @return the wpsProcessIdentifier
	 */
	public String getWpsProcessIdentifier() {
		return wpsProcessIdentifier;
	}

	/**
	 * @param targetWorkspace the targetWorkspace to set
	 */
	public void setTargetWorkspace(String targetWorkspace) {
		this.targetWorkspace = targetWorkspace;
	}

	/**
	 * @return the targetWorkspace
	 */
	public String getTargetWorkspace() {
		return targetWorkspace;
	}

	/**
	 * @param targetDataStore the targetDataStore to set
	 */
	public void setTargetDataStore(String targetDataStore) {
		this.targetDataStore = targetDataStore;
	}

	/**
	 * @return the targetDataStore
	 */
	public String getTargetDataStore() {
		return targetDataStore;
	}

	@Override
    public TracksIngestConfiguration clone()
    {
        final TracksIngestConfiguration ret = new TracksIngestConfiguration(this.getId(), this.getName(), this.getDescription());

        // TODO CLONE YOUR MEMBERS
        ret.setWpsProcessIdentifier(wpsProcessIdentifier);
        ret.setWpsServiceCapabilitiesURL(wpsServiceCapabilitiesURL);
        ret.setTargetDataStore(targetDataStore);
        ret.setTargetWorkspace(targetWorkspace);
        
        ret.setServiceID(this.getServiceID());
        ret.setListenerConfigurations(ret.getListenerConfigurations());

        return ret;
    }


}
