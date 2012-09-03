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
package it.geosolutions.geobatch.metocs.netcdf2geotiff.checker;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.geotools.geometry.GeneralEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.ma2.Array;
import ucar.ma2.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.units.Converter;

/**
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 */
public class NetcdfCheckerImpl extends NetcdfChecker {

    protected final Logger LOGGER = LoggerFactory.getLogger(NetcdfCheckerImpl.class);

    // obsolete: the output handler is provided by the SPI.
//    /**
//     * Define the ouptut of the Netcdf2geotiff action OVERRIDE ME!
//     *
//     * @note default output is null!
//     */
//    @Override
//    public TYPE writeOutput(final File workingDir, final Variable var) {
//        return null;
//    }


    public NetcdfCheckerImpl(final NetcdfFile ncFileIn, final File dictionaryFile, final NetcdfCheckerSPI spi)
        throws Exception {
        super(ncFileIn, dictionaryFile, spi);
        // this.checker=checker;
        // init global cache
        initGlobalCache();
    }

    // ----------------------------------------------------------------------------
    // global attributes cache
    private String noData;
    private String runTime;
    private Number tau;

    private void initGlobalCache() {
        setNoData();
        setRunTime();
        setTAU();
    }

    // RUN TIME
    private void setRunTime() {
        runTime = super.getRunTime();
    }

    @Override
    public String getRunTime() {
        return runTime;
    }

    // TAU
    public void setTAU() {
        tau = super.getTAU();
    }

    @Override
    public Number getTAU() {
        return tau;
    }

    // NODATA
    private void setNoData() {
        // final String noDataString=super.getNoData();
        // (noDataString!=null)?Double.parseDouble(noDataString):Double.NaN;
        noData = super.getNoData();
    }

    @Override
    public String getNoData() {
        return noData;
    }

    // ----------------------------------------------------------------------------
    // per variable cache

    // name
    private String varName;
    private Converter converter;
    // zeta
    private Array zeta = null;
    private int zetaSize = 1;
    private boolean zDimExists = false;
    // time
    private Array time = null;
    private int timeSize = 1;
    private Long timeConversion = null;
    private long localBaseTime;
    private boolean timeDimExists = false;
    // fillValue
    private double fillValue;
    // lat
    private Array lat;
    private int latSize;
    private boolean latDimExists = false;
    // lon
    private Array lon;
    private int lonSize;
    private boolean lonDimExists = false;
    // envelope
    private GeneralEnvelope envelope;

    @Override
    public boolean initVar(final Variable var) {
        boolean status = true;
        setFillValue(var);

        status = (setLat(var) && setLon(var));
        if (!status)
            return status;

        setEnvelope(lat, lon);

        final int[] shape = var.getShape();
        final Section section = new Section(shape);
        final int rank = section.getRank();

        if (rank == 4) {
            // TIME
            setTime(var);
            // ZETA
            setZeta(var);
        } else if (rank == 3) {
            // TIME
            setTime(var);
            // ZETA
            setZeta(false);
        } else if (rank == 2) {
            // ZETA
            setZeta(false);
            // TIME
            setTime(false);
        } else {
            if (LOGGER.isErrorEnabled())
                LOGGER.error("SKIPPING variable: \'" + varName + "\' -> Wrong shape rank: "
                             + section.getRank());
            status = false;
        }

        setVarName(var);
        setConverter(var);

        return status;
    }

    /**
     * @return the fillValue
     */
    @Override
    public Number getFillValue() {
        return fillValue;
    }

    // fillValue
    private void setFillValue(final Variable var) {
        fillValue = (noData != null) ? Double.parseDouble(noData) : Double.NaN;
        /*
         * try to get local missing value (no data)
         * 
         * NoData for local variables is called FillValue
         */
        final Number missingValue = super.getFillValue(var);
        if (missingValue != null) {
            /**
             * Transforming float to a double some data introduce errors this
             * will do the work
             */
            fillValue = Double.parseDouble(missingValue.toString());
        } else {
            final String noDataString = super.getNoData();
            if (noDataString != null)
                fillValue = Double.parseDouble(noDataString);
            else
                fillValue = Double.NaN;// Numbers.getNaN(METOCSActionsIOUtils.getClass(var));
        }

    }

    // set the converter for this var
    private void setConverter(final Variable var) {
        this.converter = super.getVarConversion(var);
    }

    /**
     * @return the converter
     */
    @Override
    public Converter getConverter() {
        // if (!status)
        // initVarCache(var);
        return converter;
    }

    /**
     * @deprecated confusing.
     */
    private void setVarName(final Variable var) {
        varName = super.getVarName(var);
    }

    /**
     * @deprecated confusing. Use {@link #composeVarName(ucar.nc2.Variable)} where needed.
     */
    @Override
    public String getVarName(final Variable var) {
        return varName;
    }

    // Zeta
    private void setZeta(final Variable var) {
        zDimExists = true;
        final Variable zetaVar = super.getZ(var);
        if (zetaVar == null) {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("Unable to get the zeta for this varibale, check your dictionary");
            zeta = null;
            zDimExists = false;
            zetaSize = 1;
        } else {
            try {
                zeta = zetaVar.read();
                zetaSize = (int)zeta.getSize();
            } catch (IOException e) {
                zDimExists = false;// TODO LOG
                zetaSize = 1;
            }
        }
    }

    private void setZeta(final boolean has) {
        zDimExists = has;
        zetaSize = 1;
        zeta = null;
    }

    /**
     * get the cached value (variable scope) for the zeta vector
     * 
     * @return an int >0
     */
    @Override
    public int getZetaSize() {
        return zetaSize;
    }

    // Time
    private void setTime(final Variable var) {
        timeDimExists = true;
        long baseTime = -1;
        final Variable timeVar = super.getTime(var);
        if (timeVar == null) {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("Unable to get the time for this variable, check your dictionary");
            time = null;
            timeDimExists = false;
        } else {
            try {
                time = timeVar.read();
                timeSize = (int)time.getSize();
                timeConversion = super.getTimeConversion(timeVar.getName());
            } catch (IOException e) {
                if(LOGGER.isInfoEnabled())
                    LOGGER.info("Can't read time var: " + e.getMessage());
                timeDimExists = false;// TODO LOG
                timeSize = 1;
            }
        }
        // base time
        if (timeDimExists) {
            baseTime = super.getBaseTime(timeVar);
            
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

            final Date date = super.getRunTimeDate();
            if (date != null) {
                localBaseTime = date.getTime();
                if (localBaseTime < 0) {
                    if (LOGGER.isErrorEnabled())
                        LOGGER.error("Unable to get the BaseTime for the varibale \'" + var.getName()
                                     + "\' setting !timeDimExists");
                    timeDimExists = false;
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
                timeDimExists = false;
                timeSize = 1;
            }
        }
    }

    private void setTime(final boolean has) {
        timeDimExists = has;
        timeSize = 1;
        time = null;
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
    private boolean setLat(final Variable var) {
        final Variable latVar = super.getLat(var);
        latDimExists = true;
        if (latVar == null) {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("Unable to get the Latitude for variable: '" + var.getName()
                            + "', check your dictionary");
            lat = null;
            latDimExists = false;
        }
        try {
            lat = latVar.read();
            latSize = (int)lat.getSize();
        } catch (IOException ioe) {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("Unable to read the Latitude array for variable: \'" + var.getName()
                            + "\', check your dictionary");
            lat = null;
            latDimExists = false;
        }
        return latDimExists;
    }

    public int getLatSize() {
        return latSize;
    }

    // lon
    private boolean setLon(final Variable var) {
        lonDimExists = true;
        final Variable lonVar = super.getLon(var);
        if (lonVar == null) {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("Unable to get the Longitude for varibale: \'" + var.getName()
                            + "\', check your dictionary");
            lon = null;
            lonDimExists = false;
        }
        try {
            lon = lonVar.read();
            lonSize = (int)lon.getSize();
        } catch (IOException ioe) {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("Unable to read the Longitude array for varibale: \'" + var.getName()
                            + "\', check your dictionary");
            lon = null;
            lonDimExists = false;
        }
        return lonDimExists;
    }

    @Override
    public int getLonSize() {
        return lonSize;
    }

    // envelope
    private void setEnvelope(final Array lat, final Array lon) {
        envelope = super.getVarEnvelope(lat, lon);
    }

    @Override
    public GeneralEnvelope getEnvelope() {
        return envelope;
    }

    /**
     * build the GeoTiff name using cached attributes
     * 
     * @param var
     * @param coords
     * @return
     */
    public String buildName(final Variable var, final int... coords) {

        int size = coords.length; // TODO checks
        final StringBuilder coverageName = new StringBuilder(getVarName(var).replaceAll("_", "")).append("_")
            // same Z since the raster is 2D
            .append(zDimExists ? elevLevelFormat(zeta.getDouble(coords[size - 1])) : "0000.000").append("_")
            // same Z since the raster is 2D
            .append(zDimExists ? elevLevelFormat(zeta.getDouble(coords[size - 1])) : "0000.000").append("_")
            .append(runTime).append("_");

        SimpleDateFormat sdf = super.getSimpleDateFormat();
        if (tau != null) {
            synchronized (sdf) {
                coverageName
                    .append(tau.intValue() == 0 ?
                            runTime :
                            timeDimExists ?
                                sdf.format(super.getTimeInstant(localBaseTime, time, coords[0], timeConversion)) :
                                "00000000T000000000Z")
                    .append("_").append(tau).append("_").append(fillValue);
            }
        } else {
            synchronized (sdf) {
                coverageName
                    .append(timeDimExists ?
                        sdf.format(super.getTimeInstant(localBaseTime, time, coords[0], timeConversion)) :
                        "00000000T000000000Z")
                    .append("_").append("0").append("_").append(fillValue);
            }
        }

        return coverageName.toString();
    }

    /**
     * 
     * @param d
     * @return
     */
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

}
