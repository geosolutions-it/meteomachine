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
package it.geosolutions.geobatch.metocs.netcdf2geotiff.checker;

import com.thoughtworks.xstream.XStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author etj
 */
public abstract class AbsCheckerSPI implements NetcdfCheckerSPI {

    private final static Logger LOGGER = LoggerFactory.getLogger(AbsCheckerSPI.class);

    private final static XStream xstream= new XStream();
    
    protected void loadDictionary(final File dictionaryFile, Map<String, Map<String, String>> dictionary) {
        if (dictionaryFile != null) {
            if(LOGGER.isInfoEnabled()) {
                LOGGER.info("Loading dictionary " + dictionaryFile);
            }

            // External dictionary can override standard
            if (dictionaryFile.exists() && dictionaryFile.canRead() && dictionaryFile.isFile()) {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(dictionaryFile);

                    // overwrite defaults
                    if (xstream != null) {
                    	final Object dict=xstream.fromXML(fis);
                    	if (dict instanceof Map<?, ?>){
                    		@SuppressWarnings("unchecked")
							Map<String, Map<String, String>> fromXML = (Map<String, Map<String, String>>) dict;
                    		dictionary.putAll(fromXML);
                    	}
                    	else
                    		throw new ClassCastException("The file "+dictionaryFile+" does not point to a valid dictionary.");
					}
                    else
                		throw new NullPointerException("Unable to initialize xstream deserializer, it is a NULL reference.");

                } catch (NullPointerException e) {
                	if (LOGGER.isErrorEnabled())
                        LOGGER.error(e.getLocalizedMessage(), e);
                } catch (ClassCastException e) {
                    if (LOGGER.isErrorEnabled())
                        LOGGER.error("Unable to cast to dictionary: " + e.getLocalizedMessage(), e);
                } catch (IOException e) {
                    if (LOGGER.isErrorEnabled())
                        LOGGER.error("Unable to cast to dictionary: " + e.getLocalizedMessage(), e);
                } finally {
                    IOUtils.closeQuietly(fis);
                }
            } else {
                if (LOGGER.isErrorEnabled())
                    LOGGER.error("Unable to load dictionary at " + dictionaryFile);
            }
        } else {
            LOGGER.warn("Dictionary is null");
        }
    }

    /**
     * Implementation priority.
     * When looking for a SPI implementation, the implementation with the highest
     * priority will be choosen.
     * <br/>
     * The priority may be set using an OverrideProperty in the custom webapp.
     */
    public int priority = -1;

    /**
     * Implementation priority.
     * When looking for a SPI implementation, the implementation with the highest
     * priority will be choosen.
     * <br/>
     * The priority may be set using an OverrideProperty in the custom webapp.
     */
    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

}
