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
package it.geosolutions.geobatch.metocs.netcdf2geotiff.cf;

import it.geosolutions.geobatch.metocs.netcdf2geotiff.checker.NetcdfCheckerImpl;
import it.geosolutions.geobatch.metocs.netcdf2geotiff.checker.NetcdfCheckerSPI;

import java.io.File;
import java.util.EventObject;

import ucar.nc2.NetcdfFile;

/**
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 *
 */
public class NetcdfCFChecker extends NetcdfCheckerImpl<EventObject> {

    protected NetcdfCFChecker(final NetcdfFile ncFileIn, final File dictionaryFile, final NetcdfCheckerSPI spi) throws Exception {
        super(ncFileIn, dictionaryFile, spi);
    }
}
