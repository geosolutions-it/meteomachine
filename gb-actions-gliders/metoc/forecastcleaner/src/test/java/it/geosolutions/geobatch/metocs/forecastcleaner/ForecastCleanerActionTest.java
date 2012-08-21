/*
 *  Copyright (C) 2007 - 2011 GeoSolutions S.A.S.
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
package it.geosolutions.geobatch.metocs.forecastcleaner;

import it.geosolutions.filesystemmonitor.monitor.FileSystemEvent;
import it.geosolutions.geobatch.imagemosaic.ImageMosaicCommand;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Queue;
import java.util.Set;
import org.apache.commons.io.FilenameUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ETj (etj at geo-solutions.it)
 */
public class ForecastCleanerActionTest extends BaseTest {

    private final static Logger LOGGER = LoggerFactory.getLogger(ForecastCleanerActionTest.class);
    
    public ForecastCleanerActionTest() {
    }

    /**
     * Test of execute method, of class ForecastCleanerAction.
     */
    @Test
    public void testExtractMatchingFiles() throws Exception {
        ForecastCleanerConfiguration cfg = new ForecastCleanerConfiguration("fcid", "fcname", "fcd");
        cfg.setImcRegEx("watvel.*xml");

//    <add>/mytempdir/roms/20120821-151612-039/2_uvmerge/watvel_20100801T000000000Z/watvel_0002.000_0002.000_20100801T000000000Z_20100801T000000000Z_0_1.0E37.tiff</add>

//        cfg.setImageRegEx(".*_.*_.*_.*_.*_.*_.*.tiff");
        cfg.setImageRegEx(".*_0002.000_.*_.*_.*_.*_.*.tiff");

        cfg.setRuntimeRegEx(".*_.*_.*_(.*)_.*_.*_.*.tiff");
        cfg.setForecastRegEx(".*_.*_.*_.*_(.*)_.*_.*.tiff");
        cfg.setElevationRegEx(".*_(.*)_.*_.*_.*_.*_.*.tiff");

        File imcFile = loadFile("watvel_IMC.xml");
        assertNotNull(imcFile);

        ForecastCleanerAction action = new ForecastCleanerAction(cfg);
        action.setTempDir(getTempDir());

        ImageMosaicCommand imc = ImageMosaicCommand.deserialize(imcFile);
        Set<File> fileset = action.extractMatchingFiles(imc);

        assertEquals(25, fileset.size());
    }


    
}