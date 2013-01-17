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

import it.geosolutions.geobatch.catalog.Configuration;
import it.geosolutions.geobatch.configuration.event.action.ActionConfiguration;

/**
 * Class MissionScriptConfiguration.
 * 
 * @author Tobia Di Pisa - tobia.dipisa@geo-solutions.it
 *
 */
public class MissionScriptConfiguration extends ActionConfiguration implements Configuration
{
	private String cruiseName = null;

	private String unzipPath = null;
	
	private Integer coordsApprox = null;
	
	private Double kmlIconScale = null;
	
	private String kmlFirstIconURLpath = null;
	
	private String kmlSecondIconURLpath = null;
	
	private String placemarkLogoAnchor = null;
	
	private String placemarkLogoURL = null;
	
	private String kmlOutputPath = null;
	
	/**
	 * @param id
	 * @param name
	 * @param description
	 */
	public MissionScriptConfiguration(String id, String name, String description)
    {
        super(id, name, description);
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
	 * @return the unzipPath
	 */
	public String getUnzipPath() {
		return unzipPath;
	}

	/**
	 * @param unzipPath the unzipPath to set
	 */
	public void setUnzipPath(String unzipPath) {
		this.unzipPath = unzipPath;
	}
	
	/**
	 * @return the coordsApprox
	 */
	public Integer getCoordsApprox() {
		return coordsApprox;
	}

	/**
	 * @param coordsApprox the coordsApprox to set
	 */
	public void setCoordsApprox(Integer coordsApprox) {
		this.coordsApprox = coordsApprox;
	}

	/**
	 * @return the kmlIconScale
	 */
	public Double getKmlIconScale() {
		return kmlIconScale;
	}

	/**
	 * @param kmlIconScale the kmlIconScale to set
	 */
	public void setKmlIconScale(Double kmlIconScale) {
		this.kmlIconScale = kmlIconScale;
	}

	/**
	 * @return the kmlFirstIconURLpath
	 */
	public String getKmlFirstIconURLpath() {
		return kmlFirstIconURLpath;
	}

	/**
	 * @param kmlFirstIconURLpath the kmlFirstIconURLpath to set
	 */
	public void setKmlFirstIconURLpath(String kmlFirstIconURLpath) {
		this.kmlFirstIconURLpath = kmlFirstIconURLpath;
	}

	/**
	 * @return the kmlSecondIconURLpath
	 */
	public String getKmlSecondIconURLpath() {
		return kmlSecondIconURLpath;
	}

	/**
	 * @param kmlSecondIconURLpath the kmlSecondIconURLpath to set
	 */
	public void setKmlSecondIconURLpath(String kmlSecondIconURLpath) {
		this.kmlSecondIconURLpath = kmlSecondIconURLpath;
	}

	/**
	 * @return the placemarkLogoAnchor
	 */
	public String getPlacemarkLogoAnchor() {
		return placemarkLogoAnchor;
	}

	/**
	 * @param placemarkLogoAnchor the placemarkLogoAnchor to set
	 */
	public void setPlacemarkLogoAnchor(String placemarkLogoAnchor) {
		this.placemarkLogoAnchor = placemarkLogoAnchor;
	}

	/**
	 * @return the placemarkLogoURL
	 */
	public String getPlacemarkLogoURL() {
		return placemarkLogoURL;
	}

	/**
	 * @param placemarkLogoURL the placemarkLogoURL to set
	 */
	public void setPlacemarkLogoURL(String placemarkLogoURL) {
		this.placemarkLogoURL = placemarkLogoURL;
	}
	
	/**
	 * @return the kmlOutputPath
	 */
	public String getKmlOutputPath() {
		return kmlOutputPath;
	}

	/**
	 * @param kmlOutputPath the kmlOutputPath to set
	 */
	public void setKmlOutputPath(String kmlOutputPath) {
		this.kmlOutputPath = kmlOutputPath;
	}

	/* (non-Javadoc)
	 * @see it.geosolutions.geobatch.configuration.event.action.ActionConfiguration#clone()
	 */
	@Override
    public MissionScriptConfiguration clone()
    {
        final MissionScriptConfiguration ret = new MissionScriptConfiguration(this.getId(), this.getName(), this.getDescription());

        ret.setCruiseName(cruiseName);
        ret.setUnzipPath(unzipPath);
        ret.setCoordsApprox(coordsApprox);
        ret.setKmlFirstIconURLpath(kmlFirstIconURLpath);
        ret.setKmlSecondIconURLpath(kmlSecondIconURLpath);
        ret.setKmlIconScale(kmlIconScale);
        ret.setPlacemarkLogoAnchor(placemarkLogoAnchor);
        ret.setPlacemarkLogoURL(placemarkLogoURL);
        ret.setKmlOutputPath(kmlOutputPath);
        
        ret.setServiceID(this.getServiceID());

        return ret;
    }

}
