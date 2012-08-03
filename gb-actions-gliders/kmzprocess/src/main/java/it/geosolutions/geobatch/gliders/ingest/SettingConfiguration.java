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
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 *
 */

public class SettingConfiguration extends ActionConfiguration implements Configuration
{

    private String persistencyHost = null;
    private String defaultMaskLayer = null;
    private String ieServiceUsername = null;
    private String ieServicePassword = null;
    
	public SettingConfiguration(String id, String name, String description)
    {
        super(id, name, description);

        // TODO INITIALIZE MEMBERS
    }

    public String getPersistencyHost()
    {
        return persistencyHost;
    }

    public void setPersistencyHost(String persistencyHost)
    {
        this.persistencyHost = persistencyHost;
    }

    public String getDefaultMaskLayer()
    {
        return defaultMaskLayer;
    }

    public void setDefaultMaskLayer(String defaultMaskLayer)
    {
        this.defaultMaskLayer = defaultMaskLayer;
    }
    
    public String getIeServiceUsername() {
		return ieServiceUsername;
	}

	public void setIeServiceUsername(String ieServiceUsername) {
		this.ieServiceUsername = ieServiceUsername;
	}

	public String getIeServicePassword() {
		return ieServicePassword;
	}

	public void setIeServicePassword(String ieServicePassword) {
		this.ieServicePassword = ieServicePassword;
	}

    // TODO ADD YOUR MEMBERS

    @Override
    public SettingConfiguration clone()
    {
        final SettingConfiguration ret = new SettingConfiguration(this.getId(), this.getName(), this.getDescription());

        // TODO CLONE YOUR MEMBERS
        ret.setDefaultMaskLayer(defaultMaskLayer);
        ret.setPersistencyHost(persistencyHost);
        ret.setIeServiceUsername(ieServiceUsername);
        ret.setIeServicePassword(ieServicePassword);
        ret.setServiceID(this.getServiceID());
        ret.setListenerConfigurations(ret.getListenerConfigurations());

        return ret;
    }


}
