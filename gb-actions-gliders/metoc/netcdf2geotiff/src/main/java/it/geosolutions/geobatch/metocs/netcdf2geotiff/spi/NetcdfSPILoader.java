/*
 *  GeoBatch - Open Source geospatial batch processing system
 *  http://geobatch.codehaus.org/
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
package it.geosolutions.geobatch.metocs.netcdf2geotiff.spi;

import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * @author ETj
 *
 */
public class NetcdfSPILoader implements ApplicationContextAware , InitializingBean {

    protected final static Logger LOGGER = LoggerFactory.getLogger(NetcdfSPILoader.class);

    private ApplicationContext applicationContext = null;

    private static NetcdfSPILoader singleton;

    
    public NetcdfSPILoader(){
    }        
    
    public static NetcdfSPI getSPI(final String type){
        if (singleton== null) {
            if (LOGGER.isErrorEnabled())
                LOGGER.error("Underlying loader is null!");
            return null;
        }
        return singleton.getSPIInternal(type);
    }
    
    private NetcdfSPI getSPIInternal(final String type){
        if (applicationContext == null) {
            if (LOGGER.isErrorEnabled())
                LOGGER.error("Underlying applicationContext is null!");
            return null;
        }
        
        NetcdfSPI highestSpi = null;
        NetcdfSPI currentSpi = null;

        for (Entry<String, NetcdfSPI> entry : applicationContext.getBeansOfType(NetcdfSPI.class).entrySet()) {
            currentSpi = (NetcdfSPI) entry.getValue();
            if (currentSpi != null){
                if (currentSpi.canRead(type)){
                    if (LOGGER.isInfoEnabled())
                        LOGGER.info("Found compatible SPI instance "+currentSpi.getClass() + " priority " + currentSpi.getPriority());
//                        LOGGER.info("Creating an instance of: "+spi.getClass());
                    if(highestSpi == null) {
                        highestSpi = currentSpi;
                        if (LOGGER.isDebugEnabled())
                            LOGGER.info("Selecting first SPI instance "+currentSpi.getClass());
                    } else if (currentSpi.getPriority() > highestSpi.getPriority()) {
                        highestSpi = currentSpi;
                        if (LOGGER.isDebugEnabled())
                            LOGGER.info("Selecting higher SPI instance "+currentSpi.getClass());
                    }
                }
            }
        }

        if(highestSpi != null) {
            return highestSpi;
        }

        if (LOGGER.isWarnEnabled())
            LOGGER.warn("Unable to find the needed SPI for type: "+type);
        
        return null;
    }

    public static <T extends NetcdfSPI> T getSPI(final Class<T> clazz) {
        if(singleton == null)
            if (LOGGER.isErrorEnabled())
                LOGGER.error("No singleton instance");

        return singleton.getSPIInternal(clazz);

    }

    protected <T extends NetcdfSPI> T getSPIInternal(final Class<T> clazz){

        if (applicationContext == null) {
            if (LOGGER.isErrorEnabled())
                LOGGER.error("Underlying applicationContext is null!");
            return null;
        }

        for (Entry<String, NetcdfSPI> entry : applicationContext.getBeansOfType(NetcdfSPI.class).entrySet()) {
            NetcdfSPI currentSpi = (NetcdfSPI) entry.getValue();
            if (currentSpi.getClass() == clazz) {
                if (LOGGER.isInfoEnabled())
                    LOGGER.info("Found SPI instance "+currentSpi.getClass() + " priority " + currentSpi.getPriority());
                return (T)currentSpi;
            }
        }

        if (LOGGER.isWarnEnabled())
            LOGGER.warn("Unable to find the requested SPI "+ clazz.getName());

        return null;
    }

    public void afterPropertiesSet() throws Exception {
        if (applicationContext == null)
            throw new IllegalStateException("The provided applicationContext is null!");
        this.singleton=this;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext=applicationContext;
    }

}
