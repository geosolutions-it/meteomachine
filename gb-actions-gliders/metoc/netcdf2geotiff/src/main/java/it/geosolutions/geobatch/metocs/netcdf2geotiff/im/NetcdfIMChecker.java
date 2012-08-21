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
package it.geosolutions.geobatch.metocs.netcdf2geotiff.im;

import it.geosolutions.filesystemmonitor.monitor.FileSystemEvent;
import it.geosolutions.filesystemmonitor.monitor.FileSystemEventType;
import it.geosolutions.geobatch.imagemosaic.ImageMosaicCommand;
import it.geosolutions.geobatch.metocs.netcdf2geotiff.checker.MetocsBaseDictionary;
import it.geosolutions.geobatch.metocs.netcdf2geotiff.checker.NetcdfCheckerImpl;
import it.geosolutions.geobatch.metocs.netcdf2geotiff.checker.NetcdfCheckerSPI;
import it.geosolutions.geobatch.metocs.netcdf2geotiff.grib1.MetocsImageMosaicDictionary;

import java.io.File;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 * @author ETj
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 */
public class NetcdfIMChecker extends NetcdfCheckerImpl<EventObject> {

    protected NetcdfIMChecker(final NetcdfFile ncFileIn, final File dictionaryFile, final NetcdfCheckerSPI spi) throws Exception {
        super(ncFileIn, dictionaryFile, spi);
    }

    @Override
    public String getVarName(final Variable var) {
        return super.getVarName(var) + "_" + super.getRunTime();
    }

    @Override
    public EventObject writeOutput(final File tempDir, final Variable var) {
        /*
         * Creating a new ImageMosaicCommand to add a layer using this geotiff
         * (variable)
         */
        final ImageMosaicCommand cmd = new ImageMosaicCommand(tempDir, getOutList(), null);

        final MetocsBaseDictionary dict = this.getDictionary();

        // STYLES_KYE
        final String styles = dict.getValueFromDictionary(var.getName(),
                                                          MetocsImageMosaicDictionary.STYLES_KEY);
        if (styles != null) {
            final String[] stylesList = styles.split(",");
            if (stylesList != null) {
                List<String> list = new ArrayList<String>(stylesList.length);
                for (final String style : stylesList) {
                    list.add(style);
                }
                cmd.setStyles(list);
            }
        }

        // DEFAULT_STYLE_KEY
        final String defaultStyle = dict.getValueFromDictionary(var.getFullName(), MetocsImageMosaicDictionary.DEFAULT_STYLE_KEY);
        cmd.setDefaultStyle(defaultStyle);

        final File command;
        try {
            // serialize the output

            final File outFile = File.createTempFile(getVarName(var) + "_", "_imc.xml", tempDir);

            command = ImageMosaicCommand.serialize(cmd, outFile.getAbsolutePath());
            if (command != null) {
                // ... setting up the appropriate event for the next action
                // append the ImageMosaicCommand file to the output queue
                return new FileSystemEvent(command, FileSystemEventType.FILE_ADDED);

            } else {
                if (LOGGER.isErrorEnabled())
                    LOGGER.error("Unable to get a serialized command file");
                return null;
            }

        } catch (Exception t) {
            if (LOGGER.isErrorEnabled())
                LOGGER.error("Unable to serialize command:" + t.getLocalizedMessage(), t);
            return null;
        }
    }

}
