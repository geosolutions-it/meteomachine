/*
 * ====================================================================
 *
 * GeoBatch - Intersection Engine
 *
 * Copyright (C) 2007 - 2011 GeoSolutions S.A.S.
 * http://www.geo-solutions.it
 *
 * GPLv3 + Classpath exception
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.
 *
 * ====================================================================
 *
 * This software consists of voluntary contributions made by developers
 * of GeoSolutions.  For more information on GeoSolutions, please see
 * <http://www.geo-solutions.it/>.
 *
 */
package it.geosolutions.geobatch.gliders.ingest.predictions;

import it.geosolutions.filesystemmonitor.monitor.FileSystemEvent;
import it.geosolutions.geobatch.flow.event.action.ActionException;
import it.geosolutions.geobatch.flow.event.action.BaseAction;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.EventObject;
import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.opengis.wps10.InputReferenceType;
import net.opengis.wps10.MethodType;
import net.opengis.wps10.OutputDefinitionType;
import net.opengis.wps10.ResponseFormType;
import net.opengis.wps10.Wps10Factory;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.emf.ecore.EObject;
import org.geotools.data.ows.HTTPClient;
import org.geotools.data.wps.WebProcessingService;
import org.geotools.data.wps.request.ExecuteProcessRequest;
import org.geotools.data.wps.response.ExecuteProcessResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Alessio Fabiani - alessio.fabiani@geo-solutions.it
 *
 */
public class PredictionsIngestAction extends BaseAction<EventObject>
{
	private static final Logger LOGGER = LoggerFactory.getLogger(PredictionsIngestAction.class);
	
	private static final Wps10Factory wpsFactory = Wps10Factory.eINSTANCE;

	private String wpsServiceCapabilitiesURL = null;
    private String wpsProcessIdentifier = null;
    private String targetCruise = null;
    private String targetGliderRegex = null;
    private String targetWorkspace = null;
    private String targetDataStore = null;

    private HTTPClient wpsHTTPClient = null;

    /**
     * configuration
     */
    private final PredictionsIngestConfiguration conf;

    public PredictionsIngestAction(PredictionsIngestConfiguration configuration)
    {
        super(configuration);
        conf = configuration;
    }

    /**
     * Removes TemplateModelEvents from the queue and put
     */
    public Queue<EventObject> execute(Queue<EventObject> events) throws ActionException
    {
    	wpsServiceCapabilitiesURL = conf.getWpsServiceCapabilitiesURL();
        wpsProcessIdentifier = conf.getWpsProcessIdentifier();
        targetCruise = conf.getTargetCruise();
        targetGliderRegex = conf.getTargetGliderRegex();
        targetWorkspace = conf.getTargetWorkspace();
        targetDataStore = conf.getTargetDataStore();

        final Queue<EventObject> ret = new LinkedList<EventObject>();
        if(LOGGER.isInfoEnabled()){
        	LOGGER.info("Gliders Tracks Ingest action started with parameters " + wpsServiceCapabilitiesURL + ", " + wpsProcessIdentifier + ", " + targetWorkspace + ":" + targetDataStore);
        }
        while (events.size() > 0)
        {
            final EventObject ev;
            try
            {
                if ((ev = events.remove()) != null)
                {
                    if (LOGGER.isTraceEnabled())
                    {
                        LOGGER.trace("Gliders Tracks Ingest action.execute(): working on incoming event: " +
                            ev.getSource());
                    }

                    FileSystemEvent fileEvent = (FileSystemEvent) ev;

                    File glidersTracksKMZFile = fileEvent.getSource();
                    
                    if(glidersTracksKMZFile != null && glidersTracksKMZFile.isFile() && glidersTracksKMZFile.canRead())
                    {
                    	// Setting up WPS client
                    	WebProcessingService wps = new WebProcessingService(
                                new URL(getWPSCapabilitiesURL(this.wpsServiceCapabilitiesURL)),
                                this.wpsHTTPClient,
                                null);

                    	// Configuring the WPS Process Identifier
                        ExecuteProcessRequest execRequest = wps.createExecuteProcessRequest();
                        execRequest.setIdentifier(this.wpsProcessIdentifier);
                        
                        // reference to the File
                        EObject kmzFileReference = wpsFactory.createInputReferenceType();
                        ((InputReferenceType)kmzFileReference).setMimeType("application/vnd.google-earth.kmz");
                        ((InputReferenceType)kmzFileReference).setMethod(MethodType.GET_LITERAL);
                        ((InputReferenceType)kmzFileReference).setHref(glidersTracksKMZFile.toURI().toURL().toExternalForm());
                        
                        execRequest.addInput("input Predictions KMZ file", Arrays.asList(kmzFileReference));

                        // Cruise and Glider simple Inputs
                        execRequest.addInput("cruise", Arrays.asList(wps.createLiteralInputValue(targetCruise)));
                        final Matcher gliderNameMatcher = Pattern.compile(targetGliderRegex).matcher(FilenameUtils.getBaseName(glidersTracksKMZFile.getAbsolutePath()));
                        String gliderName = null;
						if(gliderNameMatcher.find(0))
						{
							gliderName = FilenameUtils.getBaseName(glidersTracksKMZFile.getAbsolutePath()).substring(gliderNameMatcher.start(), gliderNameMatcher.end());
						}
						else
                        {
                        	// ERROR
                        	if (LOGGER.isErrorEnabled())
                            {
                                LOGGER.error("Gliders Tracks Ingest action.execute(): Exception while executing WPS Request.");
                            }
                        	throw new ActionException(this,"Could not match Glider name on given file name: " + gliderName );
                        }
                    	execRequest.addInput("glider", Arrays.asList(wps.createLiteralInputValue(gliderName)));
                        
                        // Workspace and DataStore simple Inputs
                        execRequest.addInput("workspace", Arrays.asList(wps.createLiteralInputValue(targetWorkspace)));
                        execRequest.addInput("store", Arrays.asList(wps.createLiteralInputValue(targetDataStore)));
                        
                        OutputDefinitionType rawOutput = wps.createOutputDefinitionType("result");
						ResponseFormType responseForm = wps.createResponseForm(null, rawOutput);
                        execRequest.setResponseForm(responseForm);
                        
                        if (LOGGER.isDebugEnabled())
                        {
                            ByteArrayOutputStream out = null;
                            InputStream in = null;
                            BufferedReader reader = null;
                            try
                            {
                                out = new ByteArrayOutputStream();
                                execRequest.performPostOutput(out);

                                in = new ByteArrayInputStream(out.toByteArray());
                                reader = new BufferedReader(new InputStreamReader(in));

                                StringBuilder postText = new StringBuilder();

                                char[] cbuf = new char[1024];
                                int charsRead;
                                while ((charsRead = reader.read(cbuf)) != -1)
                                {
                                    postText = postText.append(cbuf, 0, charsRead);
                                }

                                LOGGER.debug(postText.toString());
                            }
                            catch (Exception e)
                            {
                                LOGGER.warn("Error while printing out wps exec request to DEBUG log.", e);
                            }
                            finally
                            {
                                if (reader != null)
                                {
                                    reader.close();
                                }

                                if (out != null)
                                {
                                    out.close();
                                }

                                if (in != null)
                                {
                                    in.close();
                                }
                            }
                        }

                        ExecuteProcessResponse response = wps.issueRequest(execRequest);

                        // Checking for Exceptions and Status...
                        if ((response.getExceptionResponse() == null) && (response.getRawContentType() != null))
                        {
                            // OK
                        	if (LOGGER.isDebugEnabled())
                            {
                                LOGGER.debug("Gliders Predictions Ingest action.execute(): Request Successfully send to WPS Process.");
                            }
                        }
                        else
                        {
                            // ERROR
                        	if (LOGGER.isErrorEnabled())
                            {
                                LOGGER.error("Gliders Tracks Ingest action.execute(): Exception while executing WPS Request.");
                            }
                        	throw new ActionException(this,response.getExceptionResponse().getException().get(0).toString() );
                        }
                    }
                    
                    // add the event to the return
                    // ret.add(ev);
                }
                else
                {
                    if (LOGGER.isErrorEnabled())
                    {
                        LOGGER.error("Gliders Tracks Ingest action.execute(): Encountered a NULL event: SKIPPING...");
                    }

                    continue;
                }
            }
            catch (Exception ioe)
            {
                if (LOGGER.isErrorEnabled()){
                    LOGGER.error( "Gliders Tracks Ingest action.execute(): Unable to produce the output: ",ioe.getLocalizedMessage(),ioe);
                }
                throw new ActionException(this,ioe.getLocalizedMessage(),ioe );
            }
        }

        return ret;
    }

    /**
	 * @param wpsHTTPClient the wpsHTTPClient to set
	 */
	public void setWpsHTTPClient(HTTPClient wpsHTTPClient) {
		this.wpsHTTPClient = wpsHTTPClient;
	}

	/**
	 * @return the wpsHTTPClient
	 */
	public HTTPClient getWpsHTTPClient() {
		return wpsHTTPClient;
	}

    // ------------------------------------------------------------------------------------------------------------------
	
	private static String getWPSCapabilitiesURL(String wpsServiceURL)
    {
        StringBuilder wpsGetCapabilitiesURL = new StringBuilder(wpsServiceURL);
        if (wpsGetCapabilitiesURL.indexOf("?") < 0)
        {
            wpsGetCapabilitiesURL.append("?");
        }

        if (!wpsServiceURL.toUpperCase().contains("SERVICE="))
        {
            if (wpsGetCapabilitiesURL.charAt(wpsGetCapabilitiesURL.length() - 1) != '?')
            {
                wpsGetCapabilitiesURL.append("&");
            }
            wpsGetCapabilitiesURL.append("service=wps");
        }

        if (!wpsServiceURL.toUpperCase().contains("VERSION="))
        {
            if (wpsGetCapabilitiesURL.charAt(wpsGetCapabilitiesURL.length() - 1) != '?')
            {
                wpsGetCapabilitiesURL.append("&");
            }
            wpsGetCapabilitiesURL.append("version=1.0.0");
        }

        if (!wpsServiceURL.toUpperCase().contains("REQUEST="))
        {
            if (wpsGetCapabilitiesURL.charAt(wpsGetCapabilitiesURL.length() - 1) != '?')
            {
                wpsGetCapabilitiesURL.append("&");
            }
            wpsGetCapabilitiesURL.append("request=GetCapabilities");
        }

        return wpsGetCapabilitiesURL.toString();
    }
}
