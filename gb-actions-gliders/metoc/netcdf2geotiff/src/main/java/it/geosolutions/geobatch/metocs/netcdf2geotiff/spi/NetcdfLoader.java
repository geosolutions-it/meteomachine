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

import it.geosolutions.geobatch.metocs.netcdf2geotiff.spi.dict.MetocsBaseDictionary;
import it.geosolutions.geobatch.metocs.utils.converter.ConverterManager;
import it.geosolutions.geobatch.metocs.utils.io.METOCSActionsIOUtils;
import it.geosolutions.tools.commons.time.TimeParser;
import it.geosolutions.tools.netcdf.UnitsParser;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.geotools.geometry.GeneralEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.units.Converter;
import ucar.units.UnitDBException;

/**
 * Load information from a netcdf file.
 * A dictionary is used to map names and behaviour to nc variables.
 *
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * @author ETj
 *
 */
public class NetcdfLoader { // <OutputType> { extends OutputQueueHandler<OutputType>{

	private final static Logger LOGGER = LoggerFactory.getLogger(NetcdfLoader.class);

	private final NetcdfFile ncFileIn;

	private final MetocsBaseDictionary dictionary;

	private final ConverterManager converterManager;

	private final SimpleDateFormat sdf;
	private static final TimeZone UTC_TZ = TimeZone.getTimeZone("UTC");
	private static final String TIME_FORMAT = "yyyyMMdd'T'HHmmssSSS'Z'"; // TODO move into the dictionary

	/**
	 * Constructor
	 *
	 * @param ncFileIn
	 * @throws UnitDBException
	 */
	public NetcdfLoader(final NetcdfFile ncFileIn,
			final File dictionaryFile, final NetcdfSPI spi) throws Exception {

		if (ncFileIn == null)
			throw new NullPointerException("Input file is null");

    	this.ncFileIn = ncFileIn;

		dictionary = spi.buildDictionary(dictionaryFile);

        sdf = new SimpleDateFormat(getTimeFormat());
		sdf.setTimeZone(getTimeZone());

		converterManager=new ConverterManager();
		if (LOGGER.isTraceEnabled()){
			LOGGER.trace(converterManager.toString());
		}

		// load global conversion alias
		final Map<String, String> alias_section=dictionary.getVal(MetocsBaseDictionary.CONVERSION_SECTION_KEY);
		if (alias_section!=null){
			converterManager.addAlias(alias_section);
		}

	}

	/**
	 * calculate general envelop
	 * @param lat
	 * @param lon
	 * @return
	 */
    public GeneralEnvelope getVarEnvelope(final Array lat, final Array lon){
        final double[] bbox = METOCSActionsIOUtils.computeExtrema(lat,lon);
        // building Envelope
        final GeneralEnvelope envelope = new GeneralEnvelope(METOCSActionsIOUtils.WGS_84);

        envelope.setRange(0, bbox[0], bbox[2]);
        envelope.setRange(1, bbox[1], bbox[3]);
        return envelope;
    }

    /**
     * @return the dictionary
     * @note the dictionary may never be null!
     */
	public MetocsBaseDictionary getDictionary() {
		return dictionary;
	}

	/**
     * @return the converterManager
     * @note the converterManager may never be null!
     */
	public ConverterManager getConverterManager(){
		return converterManager;
	}

	// //////////////////////////////
	// GLOBAL Attributes
	// //////////////////////////////
	public String getRunTime() {
		final Date date = getRunTimeDate();
		if (date != null) {
			try {
				synchronized (sdf) {
					return sdf.format(date);
				}
			} catch (Exception e) {
				if (LOGGER.isWarnEnabled())
					LOGGER.warn(
							"Unable to format the RunTime date attribute string: "
									+ e.getMessage(), e);
			}
		}
		return null;
	}

	/**
	 * Return the runtime date as Date object.<br>
	 * - First the dictionary is scanned searching at root level for the
	 * attribute matching the RUNTIME_KEY<br>
	 * - If it is found the netcdf file is queried and the runtime attribute is
	 * read (else null is returned)<br>
	 * - The attribute is parsed using the gb-tools TimeParser to get the date
	 * from the String<br>
	 * - If success the date is returned (else null is returned)<br>
	 *
	 * @see TimeParser
	 * @see MetocsBaseDictionary.RUNTIME_KEY
	 * @note you may override this method if the runtime attribute is not an
	 *       iso801 compatible String.
	 * @return
	 */
	public Date getRunTimeDate() {
		final Attribute attr = getGlobalAttrByKey(MetocsBaseDictionary.RUNTIME_KEY);
		if (attr != null) {
			final TimeParser parser = new TimeParser();
			final List<Date> dates;
			try {
				dates = parser.parse(attr.getStringValue());
				if (dates.size() > 0) {
					return dates.get(0);
				}
			} catch (ParseException e) {
				if (LOGGER.isWarnEnabled())
					LOGGER.warn(
							"Unable to parse the string "
									+ attr.getStringValue() + " as date: "
									+ e.getMessage(), e);
			}
		}
		return null;
	}

	/**
	 * Search into the dictionary the TAU value for the ROOT node
	 *
	 * @return
	 */
    public Number getTAU() {
        final Attribute attr = getGlobalAttrByKey(MetocsBaseDictionary.TAU_KEY);
        if (attr != null) {
            return attr.getNumericValue();
        } else {
            final String fixedTau = getDictionary().getValueFromRootDictionary(
                    MetocsBaseDictionary.FIXEDTAU_KEY);
            if (fixedTau != null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Found FixedTau: " + fixedTau);
                }
                return Integer.parseInt(fixedTau);
            }
        }
        return null;
    }

	/**
	 * Search into the dictionary the NODATA value for the ROOT node
	 *
	 * @return
	 */
	public String getNoData() {
		final Attribute attr = getGlobalAttrByKey(MetocsBaseDictionary.NODATA_KEY);
		if (attr != null)
			return attr.getStringValue();
		else
			return null;
	}


	/**
	 *
	 * @param varName
	 * @return
	 */
	public String getPrefix(final String varName) {
		final String prefix = getDictionary().getValueFromDictionary(varName,
				MetocsBaseDictionary.PREFIX_KEY);
		if (prefix != null)
			return prefix;
		else
			return "";
	}

	/**
	 *
	 * @param varName
	 * @return
	 */
	public String getSuffix(final String varName) {
		final String suffix = getDictionary().getValueFromDictionary(varName,
				MetocsBaseDictionary.SUFFIX_KEY);
		if (suffix != null)
			return suffix;
		else
			return "";
	}

	/**
	 * Try to search for the latitude dimension for the passed variable. The
	 * used name for the latitude dimension will be read from the dictionary.
	 * Return null if no latitude dimension is assigned.
	 *
	 * @see NetcdfLoader.getDimVar()
	 * @param var
	 *            the variable to query for the latitude variable
	 * @return the variable representing the latitude dimension or null
	 */
	public Variable getLat(final Variable var) {
		return getDimVar(var, MetocsBaseDictionary.LATITUDE_KEY);
	}

	/**
	 * Try to search for the longitude dimension for the passed variable. The
	 * used name for the longitude dimension will be read from the dictionary.
	 * Return null if no longitude dimension is assigned.
	 *
	 * @see NetcdfLoader.getDimVar()
	 * @param var
	 *            the variable to query for the longitude variable
	 * @return the variable representing the longitude dimension or null
	 */
	public Variable getLon(final Variable var) {
		return getDimVar(var, MetocsBaseDictionary.LONGITUDE_KEY);
	}

	/**
	 * Try to search for the elevation/depth ('Z') dimension for the passed
	 * variable. The used name for the elevation/depth ('Z') dimension will be
	 * read from the dictionary. Return null if no elevation/depth ('Z')
	 * dimension is assigned.
	 *
	 * @see NetcdfLoader.getDimVar()
	 * @param var
	 *            the variable to query for the elevation/depth ('Z') variable
	 * @return the variable representing the elevation/depth ('Z') dimension or
	 *         null
	 */
	public Variable getZ(final Variable var) {
		return getDimVar(var, MetocsBaseDictionary.Z_KEY);
	}

	/**
	 * Try to search for the time dimension for the passed variable. The used
	 * name for the time dimension will be read from the dictionary. Return null
	 * if no time dimension is assigned.
	 *
	 * @see NetcdfLoader.getDimVar()
	 * @param var
	 *            the variable to query for the time variable
	 * @return the variable representing the time dimension or null
	 */
	public Variable getTime(final Variable var) {
		return getDimVar(var, MetocsBaseDictionary.TIME_KEY);
	}

	/**
	 * Return the _fillValue attribute (as Number) for the specified variable
	 *
	 * @param time
	 * @return the _fillValue attribute (as Number), can return null.
	 */
	public Number getFillValue(final Variable var) {
		final Attribute attr = getVarAttrByKey(var,
				MetocsBaseDictionary.FILLVALUE_KEY);
		if (attr != null)
			return attr.getNumericValue();
		else
			return null;
	}

	/**
	 *
	 * Try to parse the dictionary to get the specified alias for a unit to convert to
	 * if it is found try to get the converter from the UnitDB using the variable unit
	 * as starting unit and the alias found into the dictionary as unit to convert to.
	 *
	 * @param var the variable to try to convert
	 * @return the converter or null (if no converter is found)
	 */
	public Converter getVarConversion(final Variable var) {
		// try to parse conversion value from the dictionary
		final String conversionVal = getDictionary().getValueFromDictionary(var.getName()
				, MetocsBaseDictionary.CONVERSION_KEY);
		if (conversionVal!=null)
			return converterManager.getConverter(var.getUnitsString(), conversionVal);
		else{
			if (LOGGER.isInfoEnabled())
				LOGGER.info("No converter specified for this variable");
			return null;
		}

	}



	/**
	 * Try to parse the dictionary to get the value of the time unit conversion
	 * (from unknown milliseconds) can return null.
	 *
	 * @param timeVarName
	 * @return a Long representing the conversion constant to convert the time
	 *         of the passed timeVarName variable into milliseconds
	 */
	public Long getTimeConversion(final String timeVarName) {
		// try to parse conversion value from the dictionary
		final String conversionVal = getDictionary().getValueFromDictionary(
				timeVarName, MetocsBaseDictionary.TIME_CONVERSION_KEY);
		if (conversionVal != null) {
			try {
				return Long.parseLong(conversionVal);
			} catch (NumberFormatException e) {
				if (LOGGER.isErrorEnabled())
					LOGGER.error("Unable to parse time conversion value for the variable '"
							+ timeVarName + "' with value '"+conversionVal+"'.");
			}
		}
		return null;
	}

	/**
	 * Dim return a long representing the time in milliseconds from the BaseTime
	 * for the specified variable
	 *
	 * @param ncFileIn
	 * @param time
	 * @param index
	 * @return
	 * @throws InvalidRangeException
	 * @throws IOException
	 */
	public long getTime(final long baseTime, final Variable time,
			final int index) throws InvalidRangeException, IOException {
		final Section section = new Section(time.getShape());
		section.setRange(0, new Range(index, index));
		return (baseTime + time.read(section).getLong(0));
	}

	/**
	 * return a long representing the BaseTime in milliseconds for the specified
	 * variable
	 *
	 * @param ncFileIn
	 * @param time
	 * @param index
	 * @return
	 */
	public long getTimeOrigin(final Variable time) {
        long ret = getIndirectTimeOrigin(time);
        return ret != -1 ? ret : getFixedTimeOrigin(time);
    }

	public long getFixedTimeOrigin(final Variable time) {
		final String fixedto = getDictionary().getValueFromDictionary(time.getFullName(), MetocsBaseDictionary.FIXEDTIMEORIGIN_KEY);

        if (fixedto != null) {
            if(LOGGER.isDebugEnabled())
                LOGGER.debug("Found FixedTimeOrigin: " + time);

			try {
				final TimeParser parser = new TimeParser();
				final List<Date> dates = parser.parse(fixedto);
				if (dates.size() > 0) {
					return dates.get(0).getTime();
				} else {
                    if (LOGGER.isWarnEnabled())
                        LOGGER.warn("No date returned from FixedTimeOrigin value '"+fixedto+"'");
				}
			} catch (ParseException e) {
				if (LOGGER.isWarnEnabled())
					LOGGER.warn("Unable to parse the FixedTimeOrigin '"+fixedto+"': " + e.getMessage(), e);
			}
		}
		return -1;
	}

	public long getIndirectTimeOrigin(final Variable time) {
		final Attribute attr = getVarAttrByKey(time, MetocsBaseDictionary.TIMEORIGIN_KEY);
		if (attr != null) {
            if(LOGGER.isDebugEnabled())
                LOGGER.debug("Found TimeOrigin: " + time);
			try {
                // try to get the value as a string
                final String sval = attr.getStringValue();
                if(sval != null) {
                    final TimeParser parser = new TimeParser();
                    final List<Date> dates = parser.parse(sval);
                    if (dates.size() > 0) {
                        return dates.get(0).getTime();
                    } else {
                        if (LOGGER.isWarnEnabled())
                            LOGGER.warn("No date returned from TimeOrigin string value '"+sval+"'");
                    }
                } else if(attr.getNumericValue() != null) { // try to get the value as a numeric value
                    if(LOGGER.isDebugEnabled())
                        LOGGER.debug("Numeric TimeOrigin value from "+time.getFullName()+"."+attr.getName() +": " + attr.getNumericValue());
                    return attr.getNumericValue().longValue();
                }
			} catch (ParseException e) {
				if (LOGGER.isWarnEnabled())
					LOGGER.warn("Unable to parse the " + attr.getName()
							+ " attribute string '"+attr.getStringValue()+"': " + e.getMessage(), e);
			}
		}
		return -1;
	}

	public UnitsParser getTimeOriginParser (final Variable time) {
            final Attribute attr = getVarAttrByKey(time, MetocsBaseDictionary.TIMEUNITS_KEY);
            if (attr != null) {
                if(LOGGER.isDebugEnabled())
                    LOGGER.debug("Found timeUnits");
                    // try to get the value as a string
                final String sval = attr.getStringValue();
                if (sval != null) {
                    UnitsParser unitsParser = new UnitsParser();
                    boolean parsed = unitsParser.parse(sval);
                    if (parsed) {
                        return unitsParser;
                    }
                    else {
                        if(LOGGER.isWarnEnabled()) {
                          LOGGER.warn("Unable to parse the timeUnits attribute.");
                        }
                    }
                }
//                else if(attr.getNumericValue() != null) { // try to get the value as a numeric value
//                    if(LOGGER.isDebugEnabled())
//                        LOGGER.debug("Numeric value from "+time.getFullName()+"."+attr.getName() +": " + attr.getNumericValue());
//                    return null;
//                }
            }
            return null;
    }

	// //////////////////////////////
	// Protected
	// //////////////////////////////

	public final SimpleDateFormat getSimpleDateFormat(){
		return sdf;
	}

	protected String getTimeFormat(){
		return TIME_FORMAT;
	}

	protected TimeZone getTimeZone(){
		return UTC_TZ;
	}


	/**
	 * return the global attribute matching the attrKey into the dictionary or
	 * null.
	 *
	 * @param var
	 * @param attrKey
	 * @return
	 */
	protected Attribute getGlobalAttrByKey(final String attrKey) {
		final String name = getDictionary().getValueFromRootDictionary(attrKey);
		if (name == null)
			return null;
		else
			return getGlobalAttr(name);
	}

	/**
	 * return the attribute associated to the passed variable and matching the
	 * attrKey into the dictionary or null.
	 *
	 * @param var
	 * @param attrKey
	 * @return
	 */
	protected Attribute getVarAttrByKey(final Variable var, final String attrKey) {

		final String name = getDictionary().getValueFromDictionary(
				var.getName(), attrKey);
		if (name == null) {
			return null;
		}

		return var.findAttributeIgnoreCase(name);
	}

	/**
	 * Get the variable representing a dimension for the passed variable ('var')
	 * trying to parse the vocabulary using the 'VarName' as key.<br>
	 * Search is performed first at var level, if no result is found a ROOT
	 * (dictionary) level search is performed. If no result is found (into
	 * dictionary or into the dataset) null is returned.
	 *
	 * @param var
	 * @param VarNameKey
	 * @return
	 */
	protected Variable getDimVar(final Variable var, final String varName) {
		final String name = getDictionary().getValueFromDictionary(
				var.getName(), varName);
		if (name == null) {
			return null;
		}
		// verify if the found variable is really a dimension for the variable 'var'
		final int dimIndex = var.findDimensionIndex(name);
		if (dimIndex < 0) {
			if (LOGGER.isErrorEnabled())
				LOGGER.error("Unable to find " + name
						+ " dimension into the variable named: "
						+ var.getName());
			return null;
		} else {
			return ncFileIn.findVariable(name);
		}
	}

	/**
	 * Search a Variable (by name) into the dataset using the name found into
	 * the ROOT node of the dictionary.
	 *
	 * @param dimName
	 * @return
	 */
	protected Variable getVarByKey(final String varNameKey) {

		final String name = getDictionary().getValueFromRootDictionary(
				varNameKey);
		if (name == null) {
			return null;
		}

		return findVariable(name);
	}

	// /////////////////////////
	// PRIVATE
	// /////////////////////////



	private Attribute getVarAttr(final Variable var, final String name) {
		final Attribute attr = var.findAttribute(name);
		if (attr != null)
			return attr;
		else {
			if (LOGGER.isErrorEnabled())
				LOGGER.error("Unable to find attribute named: \'" + name
						+ "\' associated to the variable: \'" + var.getName()
						+ "\'.");
			return null;
		}
	}

	/**
	 * search into the passed (opened and !null) netcdf file the attribute name
	 * string (not null) as global attribute and return the found Attribute
	 * object (or null if not found)
	 *
	 * @param ncFileIn
	 *            (must be not null and opened) the netcdf object representing
	 *            the reading dataset
	 * @param attrNameKey
	 *            (must be not null) the name representing the attribute to
	 *            search for
	 * @return the searched global Attribute.
	 */
	protected Attribute getGlobalAttr(final String attrName) {
		Attribute attr;
		final Group grp;
		if ((attr = ncFileIn.findGlobalAttribute(attrName)) != null) {
			return attr;
		}
		/*
		 * @note Carlo Cancellieri 16 Dec 2010 Search the global attributes as
		 * global attributes or as attributes of the root group.
		 */
		else if ((grp = ncFileIn.getRootGroup()) != null) {
			if ((attr = grp.findAttribute(attrName)) != null) {
				return attr;
			} else
				return null;
		} else {
			final String message = "NetcdfChecker.getGlobalAttr(): Unable to find \'"
					+ attrName + "\' global variable in the source file";
			if (LOGGER.isWarnEnabled())
				LOGGER.warn(message);
			return null;
		}
	}

	/**
	 * look into the dataset for a 'name' named variable
	 *
	 * @param name
	 * @return
	 */
	private Variable findVariable(final String name) {

		final Variable var = ncFileIn.findVariable(name);
		final Group grp;
		if (var != null) {
			return var;
		}
		/*
		 * @note Carlo Cancellieri 16 Dec 2010 Search the global attributes as
		 * global attributes or as attributes of the root group.
		 */
		else if ((grp = ncFileIn.getRootGroup()) != null) {
			// TODO search in ROOT group and test findTopVariable()
			return grp.findVariable(name);
		} else {
			if (LOGGER.isErrorEnabled())
				LOGGER.error("Unable to find \'" + name
						+ "\' variable into the dataset.");
			return null;
		}
	}

}
