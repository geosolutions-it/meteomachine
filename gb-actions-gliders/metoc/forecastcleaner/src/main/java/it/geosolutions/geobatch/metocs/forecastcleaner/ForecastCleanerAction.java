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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.gce.imagemosaic.Utils;
import org.opengis.feature.simple.SimpleFeature;
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
            if(imcNamePattern.matcher(event.getSource().toString()).matches()) {
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
            Matcher imgMatcher = imageNamePattern.matcher(file.getAbsolutePath());
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

    private void addDeleteEntries(ImageMosaicCommand imc, Set<GranuleFilter> filters) {
        DataStore granuleDataStore = openDataStore();

        if(imc.getDelFiles() == null)
            imc.setDelFiles(new ArrayList<File>());

        for (GranuleFilter granuleFilter : filters) {
            String cqlFilter = filter2cql(granuleFilter);
            Set<File> oldFiles = selectOldForecast(granuleDataStore, cqlFilter);
            imc.getDelFiles().addAll(oldFiles);
        }
    }

    private DataStore openDataStore(final URL propsURL) throws IOException {
        // TODO!!!
        // datastore params should be read from the datastore file in the mosaic dir

        
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

    private String filter2cql(GranuleFilter filter) {
        // TODO!!!
        // attribute names could be read from the regex prop files, but we could
        // redefine them in the config file in order to save some implementation time
        LOGGER.error("NOT IMPLEMENTED YET");
        return null;
    }

    /**
     * 
     * @param granuleDataStore
     * @param typeName
     * @param cqlFilter
     * @return
     * @throws IOException
     */
    private Set<File> selectOldForecast(DataStore granuleDataStore, String typeName, String cqlFilter) throws IOException {
        // TODO!!!
        
    	SimpleFeatureIterator iterator=null;
    	final Set<File> retValue= new HashSet<File>();
    	try {
			final SimpleFeatureCollection features = granuleDataStore.getFeatureSource(typeName).getFeatures(ECQL.toFilter(cqlFilter));
			iterator = features.features();
			while(iterator.hasNext()){
				
				// get feature
				SimpleFeature granule = iterator.next();
				
				// get attribute location
				// TODO make the attribute parametric by inspecting the mosaic properties file
				String location = (String) granule.getAttribute(Utils.Prop.LOCATION_ATTRIBUTE); // I am using the default name for the attribute.
				
				// TODO I am here assuming that the location is absolute, but it might be relative to the base dir!!!!
				retValue.add(new File(location));
			}
		} catch (CQLException e) {
			throw new IOException(e);
		} finally {
			// release resources
			if(iterator!=null){
				iterator.close();
			}
		}
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