/*
 *  GeoBatch - Open Source geospatial batch processing system
 *  http://code.google.com/p/geobatch/
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
package it.geosolutions.geobatch.metocs.netcdf2geotiff;

import it.geosolutions.filesystemmonitor.monitor.FileSystemEvent;
import it.geosolutions.geobatch.actions.tools.adapter.EventAdapter;
import it.geosolutions.geobatch.flow.event.action.ActionException;
import it.geosolutions.geobatch.flow.event.action.BaseAction;
import it.geosolutions.geobatch.metocs.netcdf2geotiff.spi.NetcdfLoader;
import it.geosolutions.geobatch.metocs.netcdf2geotiff.spi.NetcdfSPILoader;
import it.geosolutions.geobatch.metocs.netcdf2geotiff.spi.NetcdfSPI;
import it.geosolutions.geobatch.metocs.netcdf2geotiff.spi.NetcdfVariable;
import it.geosolutions.geobatch.metocs.netcdf2geotiff.spi.geotiff.GeoTiffNameBuilder;
import it.geosolutions.geobatch.metocs.netcdf2geotiff.spi.output.OutputQueueHandler;
import it.geosolutions.geobatch.metocs.utils.io.METOCSActionsIOUtils;
import it.geosolutions.geobatch.metocs.utils.io.Utilities;
import it.geosolutions.tools.commons.file.Path;
import it.geosolutions.tools.dyntokens.DynTokenResolver;
import it.geosolutions.tools.dyntokens.model.DynTokenList;

import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.EventObject;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FilenameUtils;
import org.geotools.geometry.GeneralEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.ma2.Array;
import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.units.Converter;

/**
 *
 * Public class to split NetCDF_CF Geodetic to GeoTIFFs and consequently send them to GeoServer along with their basic metadata.
 *
 * For the NetCDF_CF Geodetic file we assume that it contains georectified geodetic grids and therefore has a maximum set of dimensions as follows:
 *
 * lat { lat:long_name = "Latitude" lat:units = "degrees_north" }
 * lon { lon:long_name = "Longitude" lon:units = "degrees_east" }
 * time { time:long_name = "time" time:units = "seconds since 1980-1-1 0:0:0" }
 * depth { depth:long_name = "depth"; depth:units = "m"; depth:positive = "down"; }
 * height { height:long_name = "height"; height:units = "m"; height:positive = "up"; }
 *
 */
public class Netcdf2GeotiffAction
    extends BaseAction<EventObject>
    implements EventAdapter<NetcdfEvent> {

    protected final static Logger LOGGER = LoggerFactory.getLogger(Netcdf2GeotiffAction.class);

    /**
     * GeoTIFF Writer Default Params
     */
    private static final int DEFAULT_TILE_SIZE = 256;
    private static final double DEFAULT_COMPRESSION_RATIO = 0.75;
    private static final String DEFAULT_COMPRESSION_TYPE = "LZW";

    private final Netcdf2GeotiffConfiguration configuration;

    /**
     * Static DateFormat Converter
     */
//    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmssSSS'Z'");

    public Netcdf2GeotiffAction(Netcdf2GeotiffConfiguration configuration) throws IOException {
        super(configuration);
        this.configuration = configuration;
//        sdf.setTimeZone(TimeZone.getTimeZone("GMT+0"));
    }

    /**
     * EXECUTE METHOD
     */
    public Queue<EventObject> execute(Queue<EventObject> events) throws ActionException {

        // checks
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Starting checks...");
        }

        listenerForwarder.started();
        listenerForwarder.setTask("Starting checks");

        File outputBaseDir = buildOutputDir();

        listenerForwarder.setTask("Starting extracting variables");
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Starting extracting variables");
        }

        // the output
        final Queue<EventObject> ret = new LinkedList<EventObject>();

        final int eventSize = events.size();
        // looking for file
        while (!events.isEmpty()) {
            NetcdfFile ncFileIn = null;
            try {
                final EventObject event = events.remove();
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Starting processing event: " + event.toString());
                }
                // adapt the input event
                final NetcdfEvent netcdfEvent = adapter(event);

                ncFileIn = netcdfEvent.getSource();

                final String inputFileBaseName;
                final String originalInputFileBaseName;

                if (ncFileIn != null) {
//                    inputFileBaseName = FilenameUtils.getBaseName(ncFileIn.getLocation()).replaceAll("_", "");
                    originalInputFileBaseName = FilenameUtils.getBaseName(ncFileIn.getLocation());
                    inputFileBaseName = originalInputFileBaseName.replaceAll("_", "");
                } else {
                    final String message = "Unable to locate event file sources for event: " + netcdfEvent.getPath();
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn(message);
                    }
                    if (configuration.isFailIgnored()) {
                        continue;
                    } else {
                        final ActionException e = new ActionException(this, message);
                        listenerForwarder.failed(e);
                        throw e;
                    }
                }
                final File layerOutputBaseDir = new File(outputBaseDir, new Date().getTime() + "_" + inputFileBaseName);

                DynTokenList tokenList = configuration.getDynamicTokens();
                Map<String, Object> tokens = new HashMap<String, Object>();
                if (tokenList != null && !tokenList.isEmpty()) {
                    DynTokenResolver tokenResolver = new DynTokenResolver(tokenList);
                    tokenResolver.setBaseToken("FILENAME", originalInputFileBaseName);
                    tokenResolver.resolve();
                    tokens.putAll(tokenResolver.getResolvedTokens());
                }

                // ----------------------------------------------------------------------------------

//    System.out.println("DEEP:"+ncFileIn.getIosp().getDetailInfo());
//    System.out.println("TypeID:"+ncFileIn.getIosp().toString());

                // SPI LOADING
                final SPIObjects spi = loadSPI(ncFileIn);
                if(spi == null)
                    continue; // logs already performed in method

                // VARIABLES
                final List<Variable> foundVariables = ncFileIn.getVariables();
                final Set<String> readVariables = checkVariables(foundVariables);

                // PROGRESS
                float progress = 0;
                // foundVariables.size>0
                final float step = configuration.getVariables() != null?
                    100 / (configuration.getVariables().size() * eventSize) : // TODO check progress for multiple input files
                    100 / (foundVariables.size() * eventSize); // TODO check progress for multiple input files


                // ----------------------------------------------------------------------------------

//                final String runTime = checker.getRunTime();
//                final String tau = checker.getTAU();

                for (Variable var : foundVariables) {
                    if (var == null) {
                        if (LOGGER.isWarnEnabled()) {
                            LOGGER.warn("Skipping NULL variable");
                        }
                        continue;
                    }

                    if (readVariables.size() > 0) {
                        if (!readVariables.contains(var.getName())) {
                            if (LOGGER.isTraceEnabled()) {
                                LOGGER.trace("Skipping variable named: \'" + var.getName() + "\'");
                            }
                            continue;
                        }
                    }

                    final String task = "Extracting variable named \'"
                            + var.getFullName() + "\' with dimensions: " + var.getDimensionsString();
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info(task);
                    }
                    listenerForwarder.setProgress((++progress) * step);
                    listenerForwarder.setTask(task);

                    // INITIALIZE checker variables
                    NetcdfVariable nvar = new NetcdfVariable(spi.loader, var);

                    if ( ! nvar.getStatus() ) {
                        if (LOGGER.isErrorEnabled()) {
                            LOGGER.error("Failed to initialize cache for this variable.");
                        }
                        continue;
                    }

                    /*
                     * VARIABLE (DIRECTORY) NAMING CONVENTION
                     * build the output layer directory using the getVarName implementation for
                     */
                    final File layerOutputVarDir = new File(layerOutputBaseDir, spi.nameBuilder.getDirName(nvar, tokens));

                    if (!layerOutputVarDir.exists()) {
                        if (!layerOutputVarDir.mkdirs()) {
                            if (LOGGER.isWarnEnabled()) {
                                LOGGER.warn("Unable to build build the output dir: \'"
                                        + layerOutputVarDir.getAbsolutePath() + "\'");
                            }
                            continue;
                        }
                    }

                    // building Envelope
                    final GeneralEnvelope envelope = nvar.getEnvelope();
                    if (envelope == null) {
                        if (LOGGER.isWarnEnabled()) {
                            LOGGER.warn("No envelope found, skipping var " + var.getFullName());
                        }
                        continue;
                    }

                    ////
                    // defining the SampleModel data type
                    // //
                    if(LOGGER.isDebugEnabled())
                        LOGGER.debug("Creating sample model "
                                + "type:" + var.getDataType()
                                + " lon:" + nvar.getLonSize()
                                + " lat:" + nvar.getLatSize());
                    
                    final SampleModel outSampleModel = Utilities.getSampleModel(
                            var.getDataType(), nvar.getLonSize(), nvar.getLatSize(), 1);

//    /*
//     * Creating a new ImageMosaicCommand to add a layer using this geotiff
//     * (variable)
//     */
//    final List<File> addList = new ArrayList<File>();
//    final ImageMosaicCommand cmd = new ImageMosaicCommand(layerOutputVarDir, addList, null);

                    final WritableRaster userRaster = Raster.createWritableRaster(outSampleModel, null);

                    final int[] shape = var.getShape();
                    final Section section = new Section(shape);
                    final int rank = section.getRank();

                    if(LOGGER.isDebugEnabled())
                        LOGGER.debug("Var '"+var.getFullName()+"', shape dim is " + shape.length + ", rank is " + rank);

                    //TODO
                    final Section section2d = new Section();
                    if (rank == 4) {
                        // z,t,y,x
                        section2d.appendRange();
                        section2d.appendRange();
                        section2d.appendRange(shape[shape.length - 2]);
                        section2d.appendRange(shape[shape.length - 1]);
                    } else if (rank >= 3) {
                        // t,y,x
                        section2d.appendRange();
                        section2d.appendRange(shape[shape.length - 2]);
                        section2d.appendRange(shape[shape.length - 1]);
                    } else if (rank > 1) {
                        // y,x
                        section2d.appendRange(shape[shape.length - 2]);
                        section2d.appendRange(shape[shape.length - 1]);
                    } else {
                        if (LOGGER.isWarnEnabled()) {
                            LOGGER.warn("Can't handle rank " + rank + ". Skipping var " + var.getFullName());
                        }
                        continue;//TODO log
                    }
                    final Number fillValue = nvar.getFillValue();

                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("Missing value is \'" + fillValue.toString() + "\'");
                    }

                    final Converter converter = nvar.getConverter();

                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("Loading converter for this variable is \'" + converter + "\'");
                    }


                    for (int z = 0; z < nvar.getZetaSize(); z++) {

                        if (rank == 4) {
                            section2d.setRange(1, new Range(z, z));
                        }

                        for (int t = 0; t < nvar.getTimeSize(); t++) {

                            if (rank >= 3) {
                                section2d.setRange(0, new Range(t, t));
                            }

                            final Array originalVarArray = var.read(section2d); // TODO MOVE INTO THE FOR USING section2d)

                            METOCSActionsIOUtils.<Integer>gWrite2DData(userRaster, var, originalVarArray, converter, fillValue, configuration.isFlipY());

//                                    METOCSActionsIOUtils.write2DData(userRaster, var, originalVarArray,
//                                            false, false, section2d.getShape(),
////                                            (hasLocalZLevel ? new int[] { t, z, nLat,
////                                                    nLon } : new int[] { t, nLat, nLon }),
//                                            configuration.isFlipY());

                            // ////
                            // producing the Coverage here...
                            // ////
                            tokens.put("zeta", z);
                            tokens.put("time", t);
                            final String coverageName = spi.nameBuilder.buildName(nvar, tokens);

                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("Writing GeoTiff named \'" + coverageName + "\'");
                            }

                            // Storing variables Variables as GeoTIFFs
                            final File gtiffFile = Utilities.storeCoverageAsGeoTIFF(layerOutputVarDir,
                                    coverageName, nvar.getFullName(), userRaster, Double.NaN, //Double.parseDouble(checker.getFillValue(var).toString()),
                                    envelope, DEFAULT_COMPRESSION_TYPE,
                                    DEFAULT_COMPRESSION_RATIO, DEFAULT_TILE_SIZE);

                            spi.outputHandler.addOutput(gtiffFile);
                        } // FOR
                    }     // FOR

                    //set ouptut
                    final EventObject ev = spi.outputHandler.writeOutput(layerOutputVarDir, nvar);
                    if (ev != null) {
                        if(LOGGER.isDebugEnabled())
                            LOGGER.debug("Adding output event " + ev);
                        ret.add(ev);
                    }

                    spi.outputHandler.getOutList().clear();

                } // for vars
            } catch (ActionException ae) {
                throw ae;
                
            } catch (Exception t) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error(t.getLocalizedMessage(), t);
                }
                listenerForwarder.failed(t);
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error(t.getLocalizedMessage(), t);
                }
                throw new ActionException(this, t.getLocalizedMessage());

            } finally {
                try {
                    if (ncFileIn != null) {
                        ncFileIn.close();
                    }
                } catch (IOException e) {
                    if (LOGGER.isErrorEnabled()) {
                        LOGGER.error(e.getLocalizedMessage(), e);
                    }
                }
            }
        } // while events
        return ret;
    }

    /**
     * This method define the mapping between input and output EventObject instance
     *
     * @param ieo is the object to transform
     * @return the EventObject adapted
     */
    public NetcdfEvent adapter(EventObject ieo) throws ActionException {

        if (ieo != null) {
            /**
             * Map the FileSystemEvent to a NetCDFDataset event object
             */
            if (ieo instanceof FileSystemEvent) {

                FileSystemEvent fs_event = (FileSystemEvent) ieo;
                File inputFile = fs_event.getSource();

                /**
                 * Here we assume that each FileSystemEvent file represent a valid NetcdfFile. This is done (without checks) since the specific class
                 * implementation name define the file type should be passed. Be careful when build flux
                 */
                // TODO we should check if this file is a netcdf file!
                try {
                    NetcdfFile ncFileIn = NetcdfFile.open(inputFile.getAbsolutePath());
                    NetcdfDataset d = new NetcdfDataset(ncFileIn);// TODO: add performBackup arg
                    return new NetcdfEvent(d);
                } catch (IOException ioe) {
                    throw new ActionException(this, ioe.getLocalizedMessage(), ioe.getCause());
                }
            } /**
             * if it is a NetcdfEvent we only have to return a NetcdfEvent input instance
             */
            else if (ieo instanceof NetcdfEvent) {
                return (NetcdfEvent) ieo;
            } else {
                throw new ActionException(this, "Unknown FileSystemEvent" + ieo);
            }
        } else {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Input EventObject is null");
            }
            return null;
        }
    }

    protected File buildOutputDir() throws ActionException {
        /*
         * get the output data dir
         */
        File outputBaseDir = null;
        if (configuration.getLayerParentDirectory() != null) {
            // is absolute path?
            File layerParDir = new File(configuration.getLayerParentDirectory());
            if (layerParDir.isAbsolute()) {
                if (layerParDir.exists() && layerParDir.isDirectory() && layerParDir.canWrite()) {
                    outputBaseDir = layerParDir;
                } else if (layerParDir.mkdirs()) {
                    outputBaseDir = layerParDir;
                }
            } else {
                layerParDir = new File(getTempDir(), configuration.getLayerParentDirectory());
                if (layerParDir.exists() && layerParDir.isDirectory() && layerParDir.canWrite()) {
                    outputBaseDir = layerParDir;
                } else if (layerParDir.mkdirs()) {
                    outputBaseDir = layerParDir;
                }
            }

            if(outputBaseDir != null)
                if (LOGGER.isInfoEnabled())
                    LOGGER.info("Output directory: '" + outputBaseDir.getAbsolutePath() + "'");

            if(outputBaseDir == null)
                if (LOGGER.isWarnEnabled())
                    LOGGER.warn("Requested output dir '" + layerParDir + "' can't be used. Will try using temp dir.");
        }

        // if layerDir is not a valid dir let's use the working dir
        if (outputBaseDir == null) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Unable to get a writeable output directory: "
                        + "going to use the temp dir...");
            }
            outputBaseDir = getTempDir();
            if (outputBaseDir != null && outputBaseDir.exists() && outputBaseDir.isDirectory() && outputBaseDir.canWrite()) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Netcdf2GeotiffAction.execute(): Output directory \'"
                            + outputBaseDir.getAbsolutePath() + "\'");
                }
            } else {
                final ActionException ae = new ActionException(this, "Unable to get a writeable layerDir");
                listenerForwarder.failed(ae);
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error(ae.getLocalizedMessage());
                }
                throw ae;
            }
        }
        return outputBaseDir;
    }

    private static class SPIObjects {
        NetcdfLoader loader;
        OutputQueueHandler<EventObject> outputHandler;
        GeoTiffNameBuilder nameBuilder;
    }

    private SPIObjects loadSPI(NetcdfFile ncFileIn) throws ActionException {

        String spiClassName = ncFileIn.getIosp().getClass().toString();
        NetcdfSPI spi = NetcdfSPILoader.getCheckerLoader(spiClassName);//getFileTypeId()
        if (spi != null) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Creating an instance of: " + spi.getClass());
            }
            final File dictionaryFile = Path.findLocation(configuration.getMetocDictionaryPath(), getConfigDir());
            if(dictionaryFile == null) {
                throw new ActionException(this, "Can't find dictionary file: " + configuration.getMetocDictionaryPath() + " (conf dir is "+getConfigDir()+") ");
            }

            try {
                SPIObjects ret = new SPIObjects();
                ret.loader = spi.buildLoader(ncFileIn, dictionaryFile);
                ret.outputHandler = spi.buildOutputQueueHandler(configuration.getOutputConfiguration(), ret.loader);
                ret.nameBuilder = spi.buildGeoTiffNameBuilder(ret.loader);
                return ret;

            } catch (Exception ex) {
                final String message = "Failed creating an instance of: " + spi.getClass();
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(message);
                }
                if (configuration.isFailIgnored()) {
                    return null;
                } else {
                    final ActionException e = new ActionException(this, message, ex);
                    listenerForwarder.failed(e);
                    throw e;
                }
            }
        } else {
            final String message = "Unable to get spi for " + spiClassName;
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn(message);
            }
            if (configuration.isFailIgnored()) {
                return null;
            } else {
                final ActionException e = new ActionException(this, message);
                listenerForwarder.failed(e);
                throw e;
            }
        }
    }

    protected Set<String> checkVariables(final List<Variable> foundVariables) throws ActionException {
        Set<String> readVariables;
        if (configuration.getVariables() != null) {
            readVariables = new TreeSet<String>(configuration.getVariables());
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Found " + configuration.getVariables().size() + " vars");
            }
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Selected variables: " + readVariables.toString());
            }
//                    step = 100 / (readVariables.size() * eventSize); // TODO check progress for multiple input files

            boolean atLeastOnefound = false;
            for (String readVar : readVariables) {
                boolean found = false;
                for (Variable foundVar : foundVariables) {
                    if(readVar.equals(foundVar.getFullName())) {
                        found = true;
                        atLeastOnefound = true;
                        break;
                    }
                }
                if ( ! found ) {
                    if(LOGGER.isWarnEnabled())
                        LOGGER.warn("Requested var '"+readVar+"' is not available in netcdf file");
                }
            }
            if( ! atLeastOnefound ) {
                if(LOGGER.isErrorEnabled())
                    LOGGER.warn("No requested var could be found in netcdf file");
                if(LOGGER.isDebugEnabled()) {
                    for (Variable foundVar : foundVariables) {
                        LOGGER.debug( "  Available var: " + foundVar.getFullName());
                    }
                }
                throw new ActionException(this, "No requested var could be found in netcdf file");
            }

        } else {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("No vars set in configuration -- all found vars will be processed");
            }
            readVariables = Collections.emptySet();
//                    step = 100 / (foundVariables.size() * eventSize); // TODO check progress for multiple input files
        }
        return readVariables;
    }

}