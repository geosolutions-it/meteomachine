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

import it.geosolutions.geobatch.catalog.Configuration;
import it.geosolutions.geobatch.configuration.event.action.ActionConfiguration;

/**
 * Class LogsConfiguration.
 * 
 * @author Tobia Di Pisa - tobia.dipisa@geo-solutions.it
 *
 */
public class LogsConfiguration extends ActionConfiguration implements Configuration
{
	private String cruiseName = null;
	private String geostoreURL = null;
	private String geostoreUs = null;
	private String geostorePw = null;
	private String timePattern = null;
	private String logfileCategoryName = null;
	private String cruiseDir = null;
	private String keywords = null;
	
	/**
	 * @param id
	 * @param name
	 * @param description
	 */
	public LogsConfiguration(String id, String name, String description)
    {
        super(id, name, description);

        // TODO INITIALIZE MEMBERS
    }

    // TODO ADD YOUR MEMBERS

	
	/**
	 * @return the geostoreURL
	 */
	public String getGeostoreURL() {
		return geostoreURL;
	}

	/**
	 * @return the cruiseName
	 */
	public String getCruiseName() {
		return cruiseName;
	}

	/**
	 * @param cruiseName the cruiseName to set
	 */
	public void setCruiseName(String cruiseName) {
		this.cruiseName = cruiseName;
	}

	/**
	 * @param geostoreURL the geostoreURL to set
	 */
	public void setGeostoreURL(String geostoreURL) {
		this.geostoreURL = geostoreURL;
	}


	/**
	 * @return the geostoreUs
	 */
	public String getGeostoreUs() {
		return geostoreUs;
	}


	/**
	 * @param geostoreUs the geostoreUs to set
	 */
	public void setGeostoreUs(String geostoreUs) {
		this.geostoreUs = geostoreUs;
	}


	/**
	 * @return the geostorePw
	 */
	public String getGeostorePw() {
		return geostorePw;
	}


	/**
	 * @param geostorePw the geostorePw to set
	 */
	public void setGeostorePw(String geostorePw) {
		this.geostorePw = geostorePw;
	}


	/**
	 * @return the timePattern
	 */
	public String getTimePattern() {
		return timePattern;
	}


	/**
	 * @param timePattern the timePattern to set
	 */
	public void setTimePattern(String timePattern) {
		this.timePattern = timePattern;
	}


	/**
	 * @return the logfileCategoryName
	 */
	public String getLogfileCategoryName() {
		return logfileCategoryName;
	}


	/**
	 * @param logfileCategoryName the logfileCategoryName to set
	 */
	public void setLogfileCategoryName(String logfileCategoryName) {
		this.logfileCategoryName = logfileCategoryName;
	}

	/**
	 * @return the cruiseDir
	 */
	public String getCruiseDir() {
		return cruiseDir;
	}

	/**
	 * @param cruiseDir the cruiseDir to set
	 */
	public void setCruiseDir(String cruiseDir) {
		this.cruiseDir = cruiseDir;
	}
	
	/**
	 * @return the keywords
	 */
	public String getKeywords() {
		return keywords;
	}

	/**
	 * @param keywords the keywords to set
	 */
	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}

	/* (non-Javadoc)
	 * @see it.geosolutions.geobatch.configuration.event.action.ActionConfiguration#clone()
	 */
	@Override
    public LogsConfiguration clone()
    {
        final LogsConfiguration ret = new LogsConfiguration(this.getId(), this.getName(), this.getDescription());

        // TODO CLONE YOUR MEMBERS
        ret.setCruiseName(cruiseName);
        ret.setGeostorePw(geostorePw);
        ret.setGeostoreUs(geostoreUs);
        ret.setGeostoreURL(geostoreURL);
        ret.setLogfileCategoryName(logfileCategoryName);
        ret.setTimePattern(timePattern);
        ret.setCruiseDir(cruiseDir);
        ret.setKeywords(keywords);
        
        ret.setServiceID(this.getServiceID());

        return ret;
    }

}
