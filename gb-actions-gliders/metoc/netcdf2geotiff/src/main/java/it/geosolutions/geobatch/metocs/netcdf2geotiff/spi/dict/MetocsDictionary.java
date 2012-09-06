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
package it.geosolutions.geobatch.metocs.netcdf2geotiff.spi.dict;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * @author ETj
 *
 * @param <DICTIONARY_KEY> Type of the dictionary key into a dictionary (
 * @param <SECTION> Type of the section key (the type of the section instance extending Map<?,String>)
 */
public abstract class MetocsDictionary <DICTIONARY_KEY, SECTION extends Map<?, String>> {
	private final static Logger LOGGER = LoggerFactory.getLogger(MetocsDictionary.class);
	
    private final Map<DICTIONARY_KEY, SECTION > dictionary;

    @SuppressWarnings("unused")
	private MetocsDictionary() {
        this.dictionary = null;
    }

    public MetocsDictionary(Map<DICTIONARY_KEY, SECTION > dictionary) {
        this.dictionary = dictionary;
    }

    /**
     * returns a Section of the dictionary
     * @param key
     * @return
     */
    public SECTION getVal(final String key) { // FIXME: param should be typed DICTIONARY_KEY, not String
        return dictionary.get(key);
    }

    protected Map<DICTIONARY_KEY, SECTION> getDictionary() {
        return dictionary;
    }
    
    /**
     * Search a key into the dictionary.
     *
     * Search into the dictionary the key passed in 'key' parameter first trying to search into the
     * section passed into the 'section' parameter then, if not found, trying to search at the ROOT
     * section.
     * 
     * @note can return null.
     * @note avoid call this method using ROOT_KEY as section, for that use
     *       getValueFromRootDictionary
     * @param section
     * @param key
     * @return
     */
    public String getValueFromDictionary(final String section, final String key) {
        if(LOGGER.isDebugEnabled())
            LOGGER.debug("getValueFromDictionary(S:"+section+", K:"+ key+")");
        
        // search into the dictionary at variable section
        final SECTION varDictionary = getVal(section);
        if(varDictionary==null) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("No dictionary section called : '" + section + "'."
                        + " Looking into root dict for key: '" + key+ "'...");
            }
            return getValueFromRootDictionary(key);

        } else {
            String name = varDictionary.get(key);
            if (name != null){
                return name;

            } else {
                if (LOGGER.isWarnEnabled())
                    LOGGER.warn("Dict section '" + section + "' does not define key: '" + key
                            + "'. Looking into root dict...");
                // search into the dictionary ROOT section
                return  getValueFromRootDictionary(key);
            }
        }
    }
    
    /**
     * Method to implement to implement a dictionary
     * @param key
     * @return
     */
    public abstract String getValueFromRootDictionary(final String key);

}
