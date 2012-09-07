/*
 *  GeoBatch - Open Source geospatial batch processing system
 *  http://geobatch.codehaus.org/
 *  Copyright (C) 2007-2008-2009 GeoSolutions S.A.S.
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
 * 
 */
public class MetocsBaseDictionary extends
		MetocsDictionary<String, Map<String, String>> {
	private final static Logger LOGGER = LoggerFactory.getLogger(MetocsBaseDictionary.class);

	public MetocsBaseDictionary(Map<String, Map<String, String>> dictionary) {
		super(dictionary);
	}

	/**
	 * The prefix to place in the front of the variable name
	 */
	public static final String PREFIX_KEY = "PREFIX";

	/**
	 * The suffix to append to the variable name
	 */
	public static final String SUFFIX_KEY = "SUFFIX";

	public static final String LATITUDE_KEY = "LAT";

	public static final String LONGITUDE_KEY = "LON";

	public static final String Z_KEY = "Z";

	public static final String FILLVALUE_KEY = "FILLVALUE";

	public static final String NODATA_KEY = "NODATA";

	public static final String TAU_KEY = "TAU";

	public static final String TIME_KEY = "TIME";

	public static final String RUNTIME_KEY = "RUNTIME";

    public static final String TIMEUNITS_KEY = "TIMEUNITS";

    /** Expected a fieldname containing any datetime format (not a long) */
	public static final String TIMEORIGIN_KEY = "TIMEORIGIN";

    /** Expected a fixed string with any datetime format (not a long) */
 	public static final String FIXEDTIMEORIGIN_KEY = "FIXEDTIMEORIGIN";

	public static final String FIXEDTAU_KEY = "FIXEDTAU";

	/**
	 * Specify the conversion VALUE (as parsable Long) from the specified value
	 * (extracted from the referenced time vector) from units to millisec(s)<br>
	 * F.E.: if time variable contains [1, 2, 3, ... 12] in hours <br>
	 * you may to specify: 3600000
	 * 
	 * @note 3600000 is the default conversion value (do you don't need to
	 *       specify it).
	 * @note if the vector is specified as string(s) you may to override
	 *       NetcdfChecker.getTimeInstant() which uses this value (returned by
	 *       the getTimeConversion())
	 */
	public static final String TIME_CONVERSION_KEY = "TIMECONVERSION";

	/**
	 * Specify the name of the unit to convert to this name represent a unit
	 * into the standard UnitDB of the NetCDF library.
	 * 
	 * @see CONVERSION_SECTION_KEY
	 */
	public static final String CONVERSION_KEY = "CONVERT_TO";

	public static final String UOM_KEY = "UOM";

	/**
	 * the variable to use to append global/root attribute name into the
	 * dictionary
	 */
	public static final String ROOT_SECTION_KEY = "ROOT";

	/**
	 * use this to open a global section to set aliases for units (aliases must
	 * match units into the UnitDB used) the map should be formed as following:<br>
	 * key - is the new alias to add to the unit into UnitDB<br>
	 * value - is the referenced unit into the UnitDB<br>
	 */
	public static final String CONVERSION_SECTION_KEY = "UNIT_ALIAS_MAP";

	/**
	 * Search into the dictionary the key passed in 'key' parameter into the
	 * ROOT
	 * 
	 * @param key
	 * @note can return null
	 * @return
	 */
	@Override
	public String getValueFromRootDictionary(final String key) {
		// search into the dictionary ROOT section
		final Map<String, String> rootDictionary = getVal(MetocsBaseDictionary.ROOT_SECTION_KEY);

		if (rootDictionary != null) {
			return rootDictionary.get(key);
		} else {
			if (LOGGER.isErrorEnabled())
				LOGGER.error("Unable to find a key named: \'" + key
						+ "\' into the dictionary.");
			return null;
		}
	}

}
