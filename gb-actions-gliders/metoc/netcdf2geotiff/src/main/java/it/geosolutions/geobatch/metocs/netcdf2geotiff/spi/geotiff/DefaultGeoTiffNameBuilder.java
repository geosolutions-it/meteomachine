/*
 *  Copyright (C) 2007 - 2012 GeoSolutions S.A.S.
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
package it.geosolutions.geobatch.metocs.netcdf2geotiff.spi.geotiff;

import it.geosolutions.geobatch.metocs.netcdf2geotiff.spi.NetcdfLoader;
import it.geosolutions.geobatch.metocs.netcdf2geotiff.spi.NetcdfVariable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.nc2.Variable;

/**
 *
 * @author ETj (etj at geo-solutions.it)
 */
public class DefaultGeoTiffNameBuilder implements GeoTiffNameBuilder {
    private final static Logger LOGGER = LoggerFactory.getLogger(DefaultGeoTiffNameBuilder.class);

    private final NetcdfLoader loader;
    private final long DEFAULT_TIME_MULTIPLIER = 3600000;



    public DefaultGeoTiffNameBuilder(NetcdfLoader checker) {
        this.loader = checker;
    }

    /**
     * build the GeoTiff name using cached attributes
     */
    @Override
    public String buildName(final NetcdfVariable var, Map<String, Object> tokens) {

        Integer zetaIndex = 0;
        Object zetaO = tokens.get("zeta");
        if (zetaO != null && zetaO instanceof Integer) {
            zetaIndex = (Integer) zetaO;
        }
        Integer timeIndex = 0;
        Object timeO = tokens.get("time");
        if (timeO != null && timeO instanceof Integer) {
            timeIndex = (Integer) timeO;
        }
        Object runTimeString = tokens.get("runtime");
        String rt = var.getGlobal().getRuntime();
        if (runTimeString != null && runTimeString instanceof String) {
            rt = (String) runTimeString;
        }

        final String baseName = composeVarName(var).replaceAll("_", "");
        final String zetaString = var.iszDimDefined() ? elevLevelFormat(var.getZetaArray().getDouble(zetaIndex)) : "0000.000";

        final StringBuilder retName = new StringBuilder()
            .append(baseName).append("_")
            // same Z since the raster is 2D
            .append(zetaString).append("_")
            // same Z since the raster is 2D
            .append(zetaString).append("_")
            .append(rt).append("_");

        SimpleDateFormat sdf = var.getSimpleDateFormat();

        Number tau = var.getGlobal().getTau();
        Number fillValue = var.getFillValue();
        
        String timeApp = "00000000T000000000Z";
        if(var.isTimeDimDefined())
            synchronized (sdf) {
                LOGGER.debug("Formatting time origin:" + var.getTimeOrigin() + " idx:"+timeIndex + " tconv:" + var.getTimeConversion());
                timeApp = sdf.format(getTimeInstant(var.getTimeOrigin(), var.getTimeArray(), timeIndex, var.getTimeConversion()));
            }

        if (tau != null) {            
                retName
                    .append(tau.intValue() == 0 ? rt : timeApp)
                    .append("_").append(tau);            
        } else {
                retName
                    .append(timeApp)
                    .append("_").append("0");
        }

        retName.append("_").append(fillValue);

        return retName.toString();
    }

    /**
	 * The output variable directory name.
     */
    @Override
    public String getDirName(final NetcdfVariable var, Map<String, Object> tokens) {
        String runtime = var.getGlobal().getRuntime();

        if (runtime!= null) {
            return var.getFullName() + "_" + runtime;
        } else if (tokens != null && tokens.containsKey("runtime")) {
            Object runTimeString = tokens.get("runtime");
            if (runTimeString != null && runTimeString instanceof String) {
                runtime = (String) runTimeString;
                return var.getFullName() + "_" + runtime;
            }
        }
        return var.getFullName();
    }

	/**
	 * Return the t^th time in millisecs
	 *
	 * @note you may override this method if the time array is of String type
	 *
	 * @param timeOrigin
	 *            the BaseTime in milliseconds
	 * @param timeVar
	 *            the variable representing the time vector
	 * @param timeIndex
	 *            the t^th time to calculate
	 * @return
	 */
	public long getTimeInstant(final long timeOrigin, final Array timeArr,
			final int timeIndex, final Long conversion) {

		long timeValue = timeArr.getLong(timeIndex);
		if (timeValue < 0) {
			if (LOGGER.isWarnEnabled())
				LOGGER.warn("The time TAU is: " + timeValue);
		} else {
            long mult = conversion == null? DEFAULT_TIME_MULTIPLIER : conversion;

			timeValue =  timeOrigin + timeValue * mult;
		}

		final Calendar roundedTimeInstant = new GregorianCalendar();//UTC_TZ);
		roundedTimeInstant.setTimeInMillis(timeValue);

		return roundedTimeInstant.getTimeInMillis();
	}

//	/**
//	 * Return the DELTA (TAU) in milliseconds for the specified time variable at
//	 * the specified index
//	 *
//	 * @param time
//	 * @param t
//	 *
//	 * @return
//	 */
//	private long getDeltaTime(final Array time, final int t, final Long conversion) {
//
//		final long deltaValue;
////		if (t > 0) {
////			deltaValue = Math.abs(time.getLong(t - 1) - time.getLong(t));
////		} else
//			deltaValue = time.getLong(t);
//
//		if (conversion == null) {
//			// apply standard conversion from hour
//			return DELTA * deltaValue;
//		} else {
//			return conversion * deltaValue; // from hour to millisec(s)
//		}
//	}

    private static String elevLevelFormat(double d) {
        String[] parts = String.valueOf(d).split("\\.");

        String integerPart = parts[0];
        String decimalPart = parts[1];

        while (integerPart.length() % 4 != 0)
            integerPart = "0" + integerPart;

        decimalPart = decimalPart.length() > 3 ? decimalPart.substring(0, 3) : decimalPart;

        while (decimalPart.length() % 3 != 0)
            decimalPart = decimalPart + "0";

        return integerPart + "." + decimalPart;
    }

	/**
	 * return the variable name with prefix and suffix in the form:<br>
	 *
	 * PREFIXVariableNameSUFFIX<br>
	 * <br>
	 * The SUFFIX and the PREFIX variables can be defined into the dictionary as
	 * root (global) or section (per variable) attributes.
	 *
	 * @param var
	 *            the variable to use to getName
	 * @return a string representing the name in the form described above
     *
	 */
	public String composeVarName(final NetcdfVariable var) {
		return var.getPrefix() + var.getFullName() + var.getSuffix();
	}

}
