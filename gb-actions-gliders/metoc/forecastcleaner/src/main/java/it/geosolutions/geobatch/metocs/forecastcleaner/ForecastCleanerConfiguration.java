/*
 *  GeoBatch - Open Source geospatial batch processing system
 *  http://geobatch.geo-solutions.it/
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

import it.geosolutions.geobatch.configuration.event.action.ActionConfiguration;


public class ForecastCleanerConfiguration extends ActionConfiguration {

    private String datastoreFileName;
    private String typeName;

    private String imcRegEx;
    private String imageRegEx;

    private String forecastRegEx;
    private String runtimeRegEx;
    private String elevationRegEx;

    /**       Forecast attribute name shll be extracted from the index prop files */
    private String forecastAttribute;

    public ForecastCleanerConfiguration(String id, String name, String description) {
        super(id, name, description);
    }

    public String getDatastoreFileName() {
        return datastoreFileName;
    }

    public void setDatastoreFileName(String datastoreFileName) {
        this.datastoreFileName = datastoreFileName;
    }

    public String getImcRegEx() {
        return imcRegEx;
    }

    public void setImcRegEx(String imcRegEx) {
        this.imcRegEx = imcRegEx;
    }

    public String getImageRegEx() {
        return imageRegEx;
    }

    public void setImageRegEx(String imageRegEx) {
        this.imageRegEx = imageRegEx;
    }

    public String getForecastRegEx() {
        return forecastRegEx;
    }

    public void setForecastRegEx(String forecastRegEx) {
        this.forecastRegEx = forecastRegEx;
    }

    public String getRuntimeRegEx() {
        return runtimeRegEx;
    }

    public void setRuntimeRegEx(String runtimeRegEx) {
        this.runtimeRegEx = runtimeRegEx;
    }

    public String getElevationRegEx() {
        return elevationRegEx;
    }

    public void setElevationRegEx(String elevationRegEx) {
        this.elevationRegEx = elevationRegEx;
    }

    public String getForecastAttribute() {
        return forecastAttribute;
    }

    public void setForecastAttribute(String forecastAttribute) {
        this.forecastAttribute = forecastAttribute;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }
   
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{datastoreFileName=" + datastoreFileName + ", imcRegEx=" + imcRegEx + ", imageRegEx=" + imageRegEx + ", forecastRegEx=" + forecastRegEx + ", runtimeRegEx=" + runtimeRegEx + '}';
    }

}
