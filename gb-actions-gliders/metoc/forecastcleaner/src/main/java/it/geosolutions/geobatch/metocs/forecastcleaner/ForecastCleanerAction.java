/*
 *  GeoBatch - Open Source geospatial batch processing system
 *  http://code.google.com/p/geobatch/
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
package it.geosolutions.geobatch.metocs.forecastcleaner;

import it.geosolutions.filesystemmonitor.monitor.FileSystemEvent;
import it.geosolutions.filesystemmonitor.monitor.FileSystemEventType;
import it.geosolutions.geobatch.flow.event.action.ActionException;
import it.geosolutions.geobatch.flow.event.action.BaseAction;
import it.geosolutions.geobatch.imagemosaic.ImageMosaicCommand;
import it.geosolutions.tools.commons.time.TimeParser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.gce.imagemosaic.Utils;
import org.geotools.temporal.object.DefaultInstant;
import org.geotools.temporal.object.DefaultPosition;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.temporal.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * Clean a mosaic from older forecast.
 *
 * Takes N ImageMosaicCommand as input.
 * Only imc matching a regex are processed, the others are passed to the output queue unchanged.
 * Only images matching a regex are processed.
 * For each matching image, the mosaic is searched for granules matching the same forecast time and older runtime.
 * Such granules are added in the IMC as "delete" tasks.
 *
 * @author ETj
 */
public class ForecastCleanerAction
    extends BaseAction<FileSystemEvent> {

	/** {@link ShapefileDataStoreFactory} singleton for later usage.*/
    private static final ShapefileDataStoreFactory SHAPEFILE_DATA_STORE_FACTORY = new ShapefileDataStoreFactory();

	protected final static Logger LOGGER = LoggerFactory.getLogger(ForecastCleanerAction.class);

    private final ForecastCleanerConfiguration configuration;

    private final Pattern imageNamePattern;
    private final Pattern imcNamePattern;

    /**
     * Static DateFormat Converter
     */
//    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmssSSS'Z'");

    protected ForecastCleanerAction(ForecastCleanerConfiguration configuration) {
        super(configuration);

        this.configuration = configuration;

        imcNamePattern = Pattern.compile(configuration.getImcRegEx());
        imageNamePattern = Pattern.compile(configuration.getImageRegEx());

//        sdf.setTimeZone(TimeZone.getTimeZone("GMT+0"));
    }

    /**
     * EXECUTE METHOD
     */
    public Queue<FileSystemEvent> execute(Queue<FileSystemEvent> events) throws ActionException {

        // checks
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Starting checks...");
        }

        listenerForwarder.started();
        listenerForwarder.setTask("Starting checks");


        final Queue<FileSystemEvent> ret = new LinkedList<FileSystemEvent>();

        for (FileSystemEvent event : events) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Scanning event: " + event.toString());
            }

            // Only imc matching a regex are processed,            
            if(imcNamePattern.matcher(event.getSource().getName()).matches()) {
                ret.add(enrichImc(event.getSource()));
            } else { // the others are passed to the output queue unchanged.
                ret.add(event);
            }
        }

        return ret;
    }

    /**
     * Enrich IMC by adding "delete" entries for old forecasts.
     * Creates a new file with the new serialized IMC and return a related
     * FileSystemEvent.
     */
    protected FileSystemEvent enrichImc(File imcFile) throws ActionException {
        try {
            ImageMosaicCommand imc = ImageMosaicCommand.deserialize(imcFile);
            Set<File> fileset = extractMatchingFiles(imc);
            Set<GranuleFilter> filters = buildFilters(fileset);

            addDeleteEntries(imc, filters);

            String basename = FilenameUtils.getBaseName(imcFile.getAbsolutePath());
            File outImc = File.createTempFile(basename, ".xml", getTempDir());
            ImageMosaicCommand.serialize(imc, outImc.getAbsolutePath());
            return new FileSystemEvent(outImc, FileSystemEventType.FILE_ADDED);
        } catch (Exception ex) {
            throw new ActionException(this, "Error processing IMC " + imcFile, ex);
        }
    }

    /**
     * Extracts "add" entries matching the imageFilter.
     */
    protected Set<File> extractMatchingFiles(ImageMosaicCommand imc) {
        Set<File> fileset = new HashSet<File>();
        for (File file : imc.getAddFiles()) {
            Matcher imgMatcher = imageNamePattern.matcher(file.getName());
            if(imgMatcher.matches())
                fileset.add(file);
        }
        if(LOGGER.isInfoEnabled())
            LOGGER.info("Found " + fileset.size() + " files matching out of " + imc.getAddFiles().size());
        return fileset;
    }

    /**
     * Builds a set of filters, with constrains on runtime, forecasttime and elevation.
     * These filters will be used to filter out outdated granules.
     *
     * @param fileset the set of input files with up-to-date forecasts.
     */
    protected Set<GranuleFilter> buildFilters(Set<File> fileset) {
        Set<GranuleFilter> ret = new HashSet<GranuleFilter>();
        GranuleFilterBuilder builder = new GranuleFilterBuilder(
                    configuration.getForecastRegEx(),
                    configuration.getRuntimeRegEx(),
                    configuration.getElevationRegEx());

        for (File file : fileset) {
            String filename = file.getName();
            GranuleFilter gf = builder.build(filename);
            ret.add(gf);

            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug(gf.toString());
            }        
        }

        return ret;
    }

    protected void addDeleteEntries(ImageMosaicCommand imc, Set<GranuleFilter> filters) throws IOException {
        File datastoreProps = new File(imc.getBaseDir(), configuration.getDatastoreFileName());
        if(LOGGER.isDebugEnabled())
            LOGGER.debug("Looking for datastore property file " + datastoreProps);

        if( ! datastoreProps.exists()) {
            if(LOGGER.isErrorEnabled()) {
                LOGGER.error("Datastore file could not be found. The mosaic may be still not initialized. Skipping forecast cleanup.");
                LOGGER.info("Datastore file: " + datastoreProps );
            }
            return;
        }

        DataStore granuleDataStore = openDataStore(datastoreProps.toURI().toURL());
        if(LOGGER.isDebugEnabled())
            LOGGER.debug("Got datastore " + granuleDataStore);

        if(imc.getDelFiles() == null)
            imc.setDelFiles(new ArrayList<File>());

        Set<File> filesToRemove = new HashSet<File>();
        Set<Filter> cqlExamined = new HashSet<Filter>();

        for (GranuleFilter granuleFilter : filters) {
            Filter cqlFilter = filter2cql(granuleFilter);
            if(cqlFilter == null) // parsing error. should we bail out?
                continue;

            if(cqlExamined.contains(cqlFilter))
                continue;
            else
                cqlExamined.add(cqlFilter);

            Set<File> oldFiles = selectOldForecast(granuleDataStore, imc.getBaseDir(), configuration.getTypeName(), cqlFilter);
            filesToRemove.addAll(oldFiles);
        }
        imc.getDelFiles().addAll(filesToRemove);
    }

    private DataStore openDataStore(final URL propsURL) throws IOException {
        // TODO!!!
        // datastore params should be read from the datastore file in the mosaic dir
        if(propsURL == null)
            throw new NullPointerException("Datastore URL is null");
        
        // load the datastore.properties file
        final Properties properties = Utils.loadPropertiesFromURL(propsURL);
        if(properties!=null){
        	
    		// SPI
    		final String SPIClass = properties.getProperty("SPI");
    		try {
    			// create a datastore as instructed
    			final DataStoreFactorySpi spi = (DataStoreFactorySpi) Class.forName(SPIClass).newInstance();
    			return spi.createDataStore(Utils.createDataStoreParamsFromPropertiesFile(properties, spi));
    		} catch (Exception e) {
    			throw new IOException(e);
    		}
        } else {
        	// try for a shapefile store
        	if(ForecastCleanerAction.SHAPEFILE_DATA_STORE_FACTORY.canProcess(propsURL)){
        		return ForecastCleanerAction.SHAPEFILE_DATA_STORE_FACTORY.createDataStore(propsURL);
        	}
        }
        return null;
    }

    private final static SimpleDateFormat UTCFORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    static {
        UTCFORMATTER.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

//    private final static String reformatDate(String dateS) {
//        try {
//            final TimeParser parser = new TimeParser();
//            List<Date> dates = parser.parse(dateS);
//            if(dates.isEmpty()) {
//                return null;
//            }
//            String utcForecast = UTCFORMATTER.format(dates.get(0));
//            // now we have somethig in the form 2010-08-02T05:00:00+0000
//            // but we need                      2010-08-02T05:00:00+00:00
//            // or                               2010-08-02T05:00:00Z
////            String utc2 = utcForecast.substring(0,22)+":"+utcForecast.substring(22);
//            String utc2 = utcForecast.substring(0,20)+"Z";
//            return utc2;
//        } catch (ParseException ex) {
//            LOGGER.error("Error parsing " + dateS+ ": " + ex.getMessage(), ex);
//            return null;
//        }
//    }

    protected Filter forecast2Filter(GranuleFilter filter) {
        try {
            final TimeParser parser = new TimeParser();
            List<Date> dates = parser.parse(filter.getForecasttime());
            if(dates.isEmpty()) {
                return null;
            }

            String utc = UTCFORMATTER.format(dates.get(0));

            LOGGER.debug(" Date " + filter.getForecasttime() 
                    + " parsed as " + utc +" -- "
                    + dates.get(0));

            FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

            // checkme: it seems there is not equal comparator for dates
            // checkme2: we have to convert the date to UTC ourselves, bc filters suffer from timezone conversion
            Filter out = ff.and(
                    ff.greaterOrEqual(
                        ff.property(configuration.getForecastAttribute()),
//                        ff.literal(dates.get(0))),
                        ff.literal(utc)),
                    ff.lessOrEqual(
                        ff.property(configuration.getForecastAttribute()),
                        ff.literal(utc)) );
//                        ff.literal(dates.get(0))) );

            return out;
        } catch (ParseException ex) {
            LOGGER.error("Error parsing " + filter.getForecasttime()+ ": " + ex.getMessage(), ex);
            return null;
        }
    }

    private Filter filter2cql(GranuleFilter filter) {

        // TODO!!!
        // attribute names could be read from the regex prop files, but we could
        // redefine them in the config file in order to save some implementation time

       Filter forecastFilter = forecast2Filter(filter);
       // not filtering by elevation, since all ingestion always have the same set of elevations

       return forecastFilter;
    }

    /**
     * 
     * @param granuleDataStore
     * @param typeName
     * @param cqlFilter
     * @return
     * @throws IOException
     */
    private Set<File> selectOldForecast(DataStore granuleDataStore, File mosaicDir, String typeName, Filter cqlFilter) throws IOException {

        if(LOGGER.isDebugEnabled())
            LOGGER.debug("Filtering " + typeName + " using filter \""+cqlFilter +"\"");

    	SimpleFeatureIterator iterator=null;
    	final Set<File> retValue= new HashSet<File>();
    	try {            
			final SimpleFeatureCollection features = granuleDataStore.getFeatureSource(typeName).getFeatures(cqlFilter);
			iterator = features.features();
			while(iterator.hasNext()){
				
				// get feature
				SimpleFeature granule = iterator.next();

				// get attribute location
				// TODO make the attribute parametric by inspecting the mosaic properties file
				String location = (String) granule.getAttribute("location"); // I am using the default name for the attribute.
//				String location = (String) granule.getAttribute(Utils.Prop.LOCATION_ATTRIBUTE); // I am using the default name for the attribute.
				
                File file = new File(location);
                if(file.isAbsolute())
                    retValue.add(file);
                else
                    retValue.add(new File(mosaicDir, location));
			}
		} finally {
			// release resources
			if(iterator!=null){
				iterator.close();
			}
		}
        if(LOGGER.isInfoEnabled())
            LOGGER.info("Found " + retValue.size() + " old granules.");

//        if(LOGGER.isDebugEnabled()) {
//            for (File file : retValue) {
//                LOGGER.debug("  granule " + file.getName());
//            }
//        }
        return retValue;
    }

    //=========================================================================

    public static class GranuleFilterBuilder {
        private final Pattern forecasttimePattern;
        private final Pattern runtimePattern;
        private final Pattern elevationPattern;

        public GranuleFilterBuilder(String forecastRegex, String runtimeRegex, String elevRegEx) {
            forecasttimePattern = Pattern.compile(forecastRegex);
            runtimePattern =      Pattern.compile(runtimeRegex);
            elevationPattern =    elevRegEx == null ? null : Pattern.compile(elevRegEx);
        }

        public GranuleFilter build(String filename) {
            Matcher m;

            m = forecasttimePattern.matcher(filename);
            final String forecasttime = m.matches() ? m.group(1) : null;

            m = runtimePattern.matcher(filename);
            final String runtime = m.matches() ? m.group(1) : null;

            String elevation = null;
            if(elevationPattern != null) {
                m = elevationPattern.matcher(filename);
                elevation = m.matches() ? m.group(1) : null;
            }

            GranuleFilter gf = new GranuleFilter();
            gf.setRuntime(runtime);
            gf.setForecasttime(forecasttime);
            gf.setElevation(elevation);

            return gf;
        }
    }

    //=========================================================================

    public static class GranuleFilter {
        String runtime;
        String forecasttime;
        String elevation;

        public String getRuntime() {
            return runtime;
        }

        public void setRuntime(String runtime) {
            this.runtime = runtime;
        }

        public String getForecasttime() {
            return forecasttime;
        }

        public void setForecasttime(String forecasttime) {
            this.forecasttime = forecasttime;
        }

        public String getElevation() {
            return elevation;
        }

        public void setElevation(String elevation) {
            this.elevation = elevation;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName()
                    +"[runtime=" + runtime
                    + ", forecasttime=" + forecasttime
                    + ", elevation=" + elevation + ']';
        }

    }

}