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
package it.geosolutions.geobatch.metocs.netcdf2geotiff.spi.output;

import it.geosolutions.filesystemmonitor.monitor.FileSystemEvent;
import it.geosolutions.filesystemmonitor.monitor.FileSystemEventType;
import it.geosolutions.geobatch.imagemosaic.ImageMosaicCommand;
import it.geosolutions.geobatch.metocs.netcdf2geotiff.impl.grib1.MetocsImageMosaicDictionary;
import it.geosolutions.geobatch.metocs.netcdf2geotiff.spi.NetcdfLoader;
import it.geosolutions.geobatch.metocs.netcdf2geotiff.spi.NetcdfVariable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ImageMosaicCommand OutputQueueHandler.
 *
 * <br/>An ImageMosaicCommand will be put on the output queue,
 * containing <code>add</code> entries for each generated geotiff file.
 * <P/>
 *
 * This handler needs a <code>mosaicPath</code> entry in the <code>outputConfiguration</code> .
 * <br/>This info is needed to create the IMC.basedir field.
 * <br/>If the info is not provided, the tempdir will be used.
 *
 * <P/>
 * If needed, you may override the method
 * {@link #buildOutputFile(it.geosolutions.geobatch.metocs.netcdf2geotiff.spi.NetcdfVariable, java.io.File) buildOutputFile}
 * to set the IMC destination file
 * 
 * @author ETj (etj at geo-solutions.it)
 */
public class IMCOutputHandler extends OutputQueueHandler<EventObject> {

    private final static Logger LOGGER = LoggerFactory.getLogger(IMCOutputHandler.class);

    public IMCOutputHandler(Map<String, Object> configuration, NetcdfLoader checker) {
        super(configuration, checker);
    }

    @Override
    public EventObject writeOutput(final File tempDir, final NetcdfVariable var) {

        final String mosaicPath = (String)configuration.get("mosaicPath");
        File mosaicPathFile;
        if(mosaicPath == null) {
            if(LOGGER.isWarnEnabled())
                LOGGER.warn("mosaicPath is not configured. Setting it to tempDir");
            mosaicPathFile = tempDir;
        } else {
            mosaicPathFile = new File(mosaicPath); // todo: add some more check here
        }

        /*
         * Creating a new ImageMosaicCommand to add a layer using this geotiff
         * (variable)
         */
        final ImageMosaicCommand cmd = new ImageMosaicCommand(mosaicPathFile, getOutList(), null);

        // STYLES_KYE
        final String styles = loader.getDictionary()
                .getValueFromDictionary(var.getFullName(), MetocsImageMosaicDictionary.STYLES_KEY);
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
        final String defaultStyle = loader.getDictionary()
                .getValueFromDictionary(var.getFullName(), MetocsImageMosaicDictionary.DEFAULT_STYLE_KEY);
        cmd.setDefaultStyle(defaultStyle);

        final File command;
        try {
            // serialize the output

            final File outFile = buildOutputFile(var, tempDir);

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

    /**
     * Creates the output file name.
     * You can override this method if you need a different name.
     */
    protected File buildOutputFile(NetcdfVariable var, File outputDir) throws IOException {
        return File.createTempFile(var.getFullName(), "_imc.xml", outputDir);
    }

}
