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

import it.geosolutions.tools.netcdf.UnitsParser;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.geotools.geometry.GeneralEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.ma2.Array;
import ucar.ma2.Section;
import ucar.nc2.Variable;
import ucar.units.Converter;

/**
 *
 * Caches variable information.
 *
 * @author ETj
 */
public class NetcdfVariable {

	private final static Logger LOGGER = LoggerFactory.getLogger(NetcdfVariable.class);

    // ----------------------------------------------------------------------------
    // per variable cache

    // name
    private String varName;
    private Converter converter;
    // zeta
    private Array zetaArray = null;
    private int zetaSize = 1;
    private boolean zDimDefined = false;
    // time
    private Array timeArray = null;
    private int timeSize = 1;
    private Long timeConversion = null;
    private long localBaseTime;
    private long timeOrigin = -1;

    private boolean timeDimDefined = false;
    // fillValue
    private double fillValue;
    // lat
    private Array lat;
    private int latSize;
    private boolean latDimDefined = false;
    // lon
    private Array lon;
    private int lonSize;
    private boolean lonDimDefined = false;
    // envelope
    private GeneralEnvelope envelope;

    private final NetcdfLoader loader;
    private final Variable var;

    private final CachedGlobal cachedGlobal;
    private final boolean status;

    private String prefix;
    private String suffix;

    public NetcdfVariable(NetcdfLoader checker, Variable var) {
        this.loader = checker;
        this.var = var;

        cachedGlobal = initGlobalCache();
        status = initVar(var);
    }

    // global attributes cache
    public class CachedGlobal {
        private String noData;
        private String runtime;
        private Number tau;

        public String getNoData() {
            return noData;
        }

        public String getRuntime() {
            return runtime;
        }

        public Number getTau() {
            return tau;
        }     
    }


    private CachedGlobal initGlobalCache() {
        CachedGlobal ret = new CachedGlobal();
        ret.noData = loader.getNoData();
        ret.runtime = loader.getRunTime();
        ret.tau = loader.getTAU();
        return ret;
    }

    public String getFullName() {
        return var.getFullName();
    }

    public CachedGlobal getGlobal() {
        return cachedGlobal;
    }

    private boolean initVar(final Variable var) {
        boolean status = true;
        initFillValue(var);

        status = (initLat(var) && initLon(var));
        if (!status)
            return status;

        initEnvelope(lat, lon);

        final int[] shape = var.getShape();
        final Section section = new Section(shape);
        final int rank = section.getRank();

        if (rank == 4) {
            // TIME
            initTime(var);
            // ZETA
            initZeta(var);
        } else if (rank == 3) {
            // TIME
            initTime(var);
            // ZETA
            initZeta(null);
        } else if (rank == 2) {
            // ZETA
            initZeta(null);
            // TIME
            initTime(null);
        } else {
            if (LOGGER.isErrorEnabled())
                LOGGER.error("SKIPPING variable: \'" + varName + "\' -> Wrong shape rank: "
                             + section.getRank());
            status = false;
        }

//        setVarName(var);
        initConverter(var);

        prefix = loader.getPrefix(var.getFullName());
        suffix = loader.getSuffix(var.getFullName());

        return status;
    }

    /**
     * @return the fillValue
     */
    public Number getFillValue() {
        return fillValue;
    }

    // fillValue
    private void initFillValue(final Variable var) {
        fillValue = (loader.getNoData() != null) ? Double.parseDouble(cachedGlobal.getNoData()) : Double.NaN;
        /*
         * try to get local missing value (no data)
         *
         * NoData for local variables is called FillValue
         */
        final Number missingValue = loader.getFillValue(var);
        if (missingValue != null) {
            /**
             * Transforming float to a double some data introduce errors this
             * will do the work
             */
            fillValue = Double.parseDouble(missingValue.toString());
        } else {
            final String noDataString = cachedGlobal.getNoData();
            if (noDataString != null)
                fillValue = Double.parseDouble(noDataString);
            else
                fillValue = Double.NaN;// Numbers.getNaN(METOCSActionsIOUtils.getClass(var));
        }

    }

    // set the converter for this var
    private void initConverter(final Variable var) {
        this.converter = loader.getVarConversion(var);
    }

    /**
     * @return the converter
     */
    public Converter getConverter() {
        return converter;
    }

    // Zeta
    private void initZeta(final Variable var) {
        if(var == null) {
            zDimDefined = false;
            zetaArray = null;
            zetaSize = 1;
            return;
        }

        zDimDefined = true;
        final Variable zetaVar = loader.getZ(var);
        if (zetaVar == null) {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("Unable to get the zeta for this varibale, check your dictionary");
            zetaArray = null;
            zDimDefined = false;
            zetaSize = 1;
        } else {
            try {
                zetaArray = zetaVar.read();
                zetaSize = (int)zetaArray.getSize();
            } catch (IOException e) {
                zDimDefined = false;// TODO LOG
                zetaSize = 1;
            }
        }
    }

    /**
     * get the cached value (variable scope) for the zeta vector
     *
     * @return an int >0
     */
    public int getZetaSize() {
        return zetaSize;
    }

    // Time
    private void initTime(final Variable var) {
        if(var == null) {
            timeDimDefined = false;
            timeArray = null;
            timeSize = 1;
            return;
        }

        timeDimDefined = true;
        long baseTime = -1;
        final Variable timeVar = loader.getTime(var);
        if (timeVar == null) {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("Unable to get the time for this variable, check your dictionary");
            timeArray = null;
            timeDimDefined = false;
        } else {
            try {
                timeArray = timeVar.read();
                timeSize = (int)timeArray.getSize();
                timeConversion = loader.getTimeConversion(timeVar.getFullName());
            } catch (IOException e) {
                if(LOGGER.isInfoEnabled())
                    LOGGER.info("Can't read time var: " + e.getMessage());
                timeDimDefined = false;// TODO LOG
                timeSize = 1;
            }
        }
        // base time
        if (timeDimDefined) {
            baseTime = loader.getBaseTime(timeVar);

            UnitsParser parser = loader.getTimeOriginParser(timeVar);
            if (parser != null) {
                timeOrigin = parser.getDate().getTime();
                timeConversion = parser.getSecondsMultiplier() * 1000;
            }
        }

        if (baseTime != -1) {
//            if (timeConversion != null) {
//                baseTime *= timeConversion;
//            }
            // local base time is set to global base time
            localBaseTime = baseTime;
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("Setting the BaseTime for the variable \'" + var.getFullName()
                            + "' to " + baseTime);
        } else {
            // ???

            final Date date = loader.getRunTimeDate();
            if (date != null) {
                localBaseTime = date.getTime();
                if (localBaseTime < 0) {
                    if (LOGGER.isErrorEnabled())
                        LOGGER.error("Unable to get the BaseTime for the varibale \'" + var.getName()
                                     + "\' setting !timeDimExists");
                    timeDimDefined = false;
                    timeSize = 1;
                } else {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Unable to get the BaseTime for the varibale \'" + var.getName()
                                    + "\' setting to RunTime");
                    }
                }
            } else {
                if (LOGGER.isErrorEnabled())
                    LOGGER.error("Unable to get the BaseTime for the varibale \'" + var.getName()
                                 + "\' setting !timeDimExists");
                timeDimDefined = false;
                timeSize = 1;
            }
        }
    }

    /**
     * get the cached value (variable scope) for the time vector
     *
     * @return an int >0
     */
    public int getTimeSize() {
        return timeSize;
    }

    // lat
    private boolean initLat(final Variable var) {
        final Variable latVar = loader.getLat(var);
        latDimDefined = true;
        if (latVar == null) {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("Unable to get the Latitude for variable: '" + var.getFullName()
                            + "', check your dictionary");
            lat = null;
            latDimDefined = false;
        }
        try {
            lat = latVar.read();
            latSize = (int)lat.getSize();
        } catch (IOException ioe) {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("Unable to read the Latitude array for variable: \'" + var.getFullName()
                            + "\', check your dictionary");
            lat = null;
            latDimDefined = false;
        }
        return latDimDefined;
    }

    public int getLatSize() {
        return latSize;
    }

    // lon
    private boolean initLon(final Variable var) {
        lonDimDefined = true;
        final Variable lonVar = loader.getLon(var);
        if (lonVar == null) {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("Unable to get the Longitude for varibale: \'" + var.getFullName()
                            + "\', check your dictionary");
            lon = null;
            lonDimDefined = false;
        }
        try {
            lon = lonVar.read();
            lonSize = (int)lon.getSize();
        } catch (IOException ioe) {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("Unable to read the Longitude array for varibale: \'" + var.getFullName()
                            + "\', check your dictionary");
            lon = null;
            lonDimDefined = false;
        }
        return lonDimDefined;
    }

    public int getLonSize() {
        return lonSize;
    }

    // envelope
    private void initEnvelope(final Array lat, final Array lon) {
        envelope = loader.getVarEnvelope(lat, lon);
    }

    public GeneralEnvelope getEnvelope() {
        return envelope;
    }

    public boolean iszDimDefined() {
        return zDimDefined;
    }

    public boolean isTimeDimDefined() {
        return timeDimDefined;
    }

    public Array getZetaArray() {
        return zetaArray;
    }

    public Array getTimeArray() {
        return timeArray;
    }

    public long getTimeOrigin() {
        return timeOrigin;
    }

    public Long getTimeConversion() {
        return timeConversion;
    }

    public SimpleDateFormat getSimpleDateFormat() {
        // at the moment it's static in NetcdfLoader, but may be moved at variable level
        return loader.getSimpleDateFormat();
    }

    public boolean getStatus() {
        return status;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getSuffix() {
        return suffix;
    }
}
