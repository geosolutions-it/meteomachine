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

import it.geosolutions.geobatch.imagemosaic.ImageMosaicCommand;

//import it.geosolutions.tools.commons.time.TimeParser;
import it.geosolutions.tools.commons.time.TimeParser;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.geotools.filter.text.ecql.ECQL;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.opengis.filter.Filter;
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
    @Ignore
    public void testExtractMatchingFiles() throws Exception {
        ForecastCleanerConfiguration cfg = new ForecastCleanerConfiguration("fcid", "fcname", "fcd");
        cfg.setImcRegEx("watvel.*xml");

//    <add>/mytempdir/roms/20120821-151612-039/2_uvmerge/watvel_20100801T000000000Z/watvel_0002.000_0002.000_20100801T000000000Z_20100801T000000000Z_0_1.0E37.tiff</add>

//        cfg.setImageRegEx(".*_.*_.*_.*_.*_.*_.*.tiff");
        cfg.setImageRegEx(".*_0002.000_.*_.*_.*_.*_.*.tiff");

        cfg.setRuntimeRegEx(".*_.*_.*_(.*)_.*_.*_.*.tiff");
        cfg.setForecastRegEx(".*_.*_.*_.*_(.*)_.*_.*.tiff");
        cfg.setElevationRegEx(".*_(.*)_.*_.*_.*_.*_.*.tiff");

        cfg.setDatastoreFileName("datastore.properties");
        cfg.setTypeName("uvmerge");
        cfg.setForecastAttribute("forecast");

        File imcFile = loadFile("watvel_IMC.xml");
        assertNotNull(imcFile);

        ForecastCleanerAction action = new ForecastCleanerAction(cfg);
        action.setTempDir(getTempDir());
        action.setConfigDir(loadFile("data"));

        ImageMosaicCommand imc = ImageMosaicCommand.deserialize(imcFile);
        imc.setBaseDir(loadFile("data"));
        Set<File> fileset = action.extractMatchingFiles(imc);

        assertEquals("Bad matching files number", 25, fileset.size());

        Set<ForecastCleanerAction.GranuleFilter> filters = action.buildFilters(fileset);

        assertEquals("Bad filter number", 25, filters.size());

        action.addDeleteEntries(imc, filters);

        assertNotNull("DelFiles is null", imc.getDelFiles());
        assertEquals("Bad delfiles number", 700, imc.getDelFiles().size());
    }

    @Test
    public void regExTest() throws Exception {
        String regex1 = "(?<=\\d{8}T\\d{9}Z_)\\d{8}T\\d{9}Z";
        String name = "watvel-u_0002.000_0002.000_20100801T000000000Z_20120801T000000000Z_0_1.0E37";
        
        Pattern pattern = Pattern.compile(regex1);
        Matcher matcher = pattern.matcher(name);
        String lastMatch = null;
        while (matcher.find()) {
            lastMatch = matcher.group();
            LOGGER.info("Match found '"+lastMatch+"'");
        }
        final TimeParser parser= new TimeParser();
        List<Date> dates = parser.parse(lastMatch);
        LOGGER.info("Dates: " + dates);
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZ");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String formatted = dateFormat.format(dates.get(0));
        LOGGER.info("Formatted : " + formatted);

        String regex2 = 	"(?<=[^_]*_)(\\d+\\.\\d+)(?=_\\d+\\.\\d+_\\d{8}T)";
        pattern = Pattern.compile(regex2);
        matcher = pattern.matcher(name);
        lastMatch = null;
        while (matcher.find()) {
            lastMatch = matcher.group();
            LOGGER.info("Match found '"+lastMatch+"'");
        }
    }

    @Test
    public void cqlDateFormattingTest() {
        ForecastCleanerConfiguration cfg = new ForecastCleanerConfiguration("x", "y", "z");
        cfg.setForecastAttribute("forecast");
        cfg.setImcRegEx(".*"); // useless here
        cfg.setImageRegEx(".*"); // useless here


        ForecastCleanerAction action = new ForecastCleanerAction(cfg);

        ForecastCleanerAction.GranuleFilter gf = new ForecastCleanerAction.GranuleFilter();
        gf.setForecasttime("20120801T120000000Z");

        Filter filter = action.forecast2Filter(gf);

        LOGGER.info("Initial string:  " + gf.getForecasttime());
        LOGGER.info("Filter.toString: " + filter);
        LOGGER.info("ECQL.toCQL     : " + ECQL.toCQL(filter));    
    }


    @Test
    public void cqlDateComparisonTest() {

        parse("x after 2010-08-02T05:00:00+00:00");
        parse("x after 2010-08-02T05:00:00+0000");
        parse("x after 2010-08-02T05:00:00Z00:00");
        parse("x after 2010-08-02T05:00:00Z");
        parse("x after 2010-08-02T05:00:00Z0000");

        parse("x = 2010-08-02T05:00:00+00:00");
        parse("x is 2010-08-02T05:00:00+00:00");
        parse("x during 2010-08-02T05:00:00+00:00/2010-08-02T05:00:00+00:00");
        parse("x between 2010-08-02T05:00:00+00:00 AND 2010-08-02T05:00:00+00:00");

        parse("x = 2010-08-02T05:00:00Z");
        parse("x is 2010-08-02T05:00:00Z");
        parse("x during 2010-08-02T05:00:00Z / 2010-08-02T05:00:00Z");
        parse("x between 2010-08-02T05:00:00Z AND 2010-08-02T05:00:00Z");

    }

    protected void parse(String s) {
        try {
            ECQL.toFilter(s);
            LOGGER.info("OK " + s);
        } catch (Exception e) {
            LOGGER.info("no " + s);
        }
    }
}
