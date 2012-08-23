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
package it.geosolutions.geobatch.metocs.netcdf2geotiff.grib1;

import it.geosolutions.geobatch.metocs.netcdf2geotiff.checker.NetcdfCheckerImpl;
import it.geosolutions.geobatch.metocs.netcdf2geotiff.checker.NetcdfCheckerSPI;
import it.geosolutions.geobatch.metocs.utils.converter.ConverterManager;

import java.io.File;

import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.units.ScaledUnit;
import ucar.units.UnitName;

/**
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 */
public class Grib1Checker extends NetcdfCheckerImpl {

    public Grib1Checker(final NetcdfFile ncFileIn, final File dictionaryFile, final NetcdfCheckerSPI spi)
        throws Exception {
        super(ncFileIn, dictionaryFile, spi);

        final ConverterManager manager = super.getConverterManager();

        // Carlo on 25Jul2011: added for LaMMa project
        manager.addUnit(new ScaledUnit(100, manager.get("Pa"), UnitName.newUnitName("hPa")));
    }

    /**
     * override the parent method appending the runtime to the variable name
     */
    @Override
    public String getVarName(final Variable var) {
//        return super.getVarName(var) + "_" + super.getRunTime();
        return super.composeVarName(var) + "_" + super.getRunTime();
    }

}
