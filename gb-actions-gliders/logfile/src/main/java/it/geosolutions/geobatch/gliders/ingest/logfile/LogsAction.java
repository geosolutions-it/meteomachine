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
package it.geosolutions.geobatch.gliders.ingest.logfile;

import it.geosolutions.filesystemmonitor.monitor.FileSystemEvent;
import it.geosolutions.filesystemmonitor.monitor.FileSystemEventType;
import it.geosolutions.geobatch.flow.event.action.ActionException;
import it.geosolutions.geobatch.flow.event.action.BaseAction;
import it.geosolutions.geostore.core.model.enums.DataType;
import it.geosolutions.geostore.services.dto.ShortAttribute;
import it.geosolutions.geostore.services.rest.GeoStoreClient;
import it.geosolutions.geostore.services.rest.model.RESTCategory;
import it.geosolutions.geostore.services.rest.model.RESTResource;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EventObject;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Scanner;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Class LogsAction.
 * 
 * @author Tobia Di Pisa - tobia.dipisa@geo-solutions.it
 *
 */
public class LogsAction extends BaseAction<EventObject>
{
	private static final Logger LOGGER = LoggerFactory.getLogger(LogsAction.class);
	
    /**
     * configuration
     */
    private final LogsConfiguration conf;

	private GeoStoreClient geostoreClient;

    public LogsAction(LogsConfiguration configuration)
    {
        super(configuration);
        conf = configuration;
    }

    /**
     * Removes TemplateModelEvents from the queue and put 
     */
    public Queue<EventObject> execute(Queue<EventObject> events) throws ActionException
    {

        final Queue<EventObject> ret = new LinkedList<EventObject>();
        
        if(LOGGER.isInfoEnabled()){
        	LOGGER.info("");
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
                        LOGGER.trace("Gliders Logs action.execute(): working on incoming event: " +
                            ev.getSource());
                    }

                    FileSystemEvent fileEvent = (FileSystemEvent) ev;

                    File logFile = fileEvent.getSource();   
                    
                    // ///////////////////////////////
                    // Parse the log file
                    // ///////////////////////////////

                    Date curr_time = null;
            		String vehicle_name = null;
            		String mission_name = null;
            		String mission_number = null;
            		String cruise_name = conf.getCruiseName();
            		String cruise_dir = conf.getCruiseDir();
            		
            		String keywords = conf.getKeywords();
            		String[] keywordsArray = keywords.split(",");             		
            		List<String> keywordsList = Arrays.asList(keywordsArray);
            		
            		String metadata = "";

            		Scanner scanner =  null;
            		try{
            			scanner = new Scanner(logFile);
            			while (scanner.hasNextLine()){
            				String line = scanner.nextLine();
            				
            				// ///////////////////////////
            				// Parse Mission Name
            				// ///////////////////////////
                        	if(line.contains("Mission Name:") && mission_name == null){
                        		String mname = line.split(":")[1];
                        		
                        		mname = mname.trim();
                        		
                        		mission_name = mname;
                        	}else
                        	
            				// ///////////////////////////
            				// Parse Mission Number
            				// ///////////////////////////
                        	if(line.contains("Mission Number:") && mission_number == null){
                        		String number = line.split(":")[1];
                        		
                        		while(number.startsWith(" ")){
                        			number = number.replaceFirst(" ", "");
                        		}
                        		
                        		while(number.endsWith(" ")){
                        			number = number.substring(0, number.length() - 1);
                        		}
                        		
                        		mission_number = number;
                        	}else
                        	
            				// ///////////////////////////
            				// Parse Vehicle Name
            				// ///////////////////////////
                        	if(line.contains("Vehicle Name:") && vehicle_name == null){
                        		String name = line.split(":")[1];

                        		name = name.trim();
                        		
                        		vehicle_name = name;
                        	}else
                        	
            				// ///////////////////////////
            				// Parse Curr Time
            				// ///////////////////////////
                        	if(line.contains("Curr Time:") && curr_time == null){                        		
                        		String time = line.split("Curr Time:")[1];
                        		
                        		String[] time_curr_props = time.split("MT:");

                        		// --- Parsing Curr Time --- //
                        		String ctime = time_curr_props[0];
                        		
                        		while(ctime.startsWith(" ")){
                        			ctime = ctime.replaceFirst(" ", "");
                        		}
                        		
                        		while(ctime.endsWith(" ")){
                        			ctime = ctime.substring(0, ctime.length() - 1);
                        		}
                        		
                        		String pattern = conf.getTimePattern();
    					        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.US);
    					        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
    					        
                        		curr_time = sdf.parse(ctime);
                        	}
                        	
            				// ///////////////////////////
            				// Parse LOG Keywords
            				// ///////////////////////////
                        	if(keywordsList != null){
                               	for(int i=0, size = keywordsList.size(); i<size; i++){
                               		String key = keywordsList.get(i);
                        			if(line.contains(key)){
                        				if(metadata.isEmpty()){
                        					metadata = metadata.concat(key);
                        				}else if(!metadata.contains(key)){
                        					metadata = metadata.concat("," + key);
                        				}
                        			}else{
                        				continue;
                        			}
                            	}
                        	}
                        	
                        	if(curr_time != null && vehicle_name != null && 
                        			mission_number != null && mission_name != null){
                        		break;
                        	}
            			}   
            		}finally{
            			try {
                			if(scanner != null){
                				scanner.close();
                			}
            			} catch (Exception e) {
            				if (LOGGER.isErrorEnabled())
            					LOGGER.error("Error closing file scanner stream ", e);
            				throw new Exception(e.getMessage());
            			}
            		}
   
                	if(LOGGER.isInfoEnabled()){
                		LOGGER.info("Build the GeoStore request ...");
                	}
                	
                    // //////////////////////////////////
                    // Create a new GeoStore resources
                    // //////////////////////////////////
                    RESTResource  resource = new RESTResource();
                    resource.setName(logFile.getName());
                    
                    // ///////////////////////////////////////
                    // Check if the LOGFILE category exists 
                    // ///////////////////////////////////////
                    RESTCategory category = new RESTCategory(conf.getLogfileCategoryName());
                    resource.setCategory(category);
                    
                    List<ShortAttribute> attributes = new ArrayList<ShortAttribute>();
                    
                    // ////////////////////////////////////
                    // Time begin/end Attributes definition
                    // ////////////////////////////////////
                    if(curr_time != null){
	                    ShortAttribute startTime = new ShortAttribute();
	                    startTime.setName("time_begin");
	                    startTime.setType(DataType.DATE);
	                    
	                    DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	                    startTime.setValue(format.format(curr_time));
	                    
	                    attributes.add(startTime);
	                    
	                    ShortAttribute endTime = new ShortAttribute();
	                    endTime.setName("time_end");
	                    endTime.setType(DataType.DATE);
	                    endTime.setValue(format.format(curr_time));
	                    
	                    attributes.add(endTime);
                    }
                    
                    // ////////////////////////////////////
                    // Vehicle Name Attribute definition
                    // ////////////////////////////////////
                    if(vehicle_name != null){
                        ShortAttribute vName = new ShortAttribute();
                        vName.setName("vehicle_name");
                        vName.setType(DataType.STRING);                    
                        vName.setValue(vehicle_name);
                        
                        attributes.add(vName);
                    }
                    
                    // ////////////////////////////////////
                    // Mission Name Attribute definition
                    // ////////////////////////////////////
                    if(mission_name != null){
                        ShortAttribute mName = new ShortAttribute();
                        mName.setName("mission_name");
                        mName.setType(DataType.STRING);                    
                        mName.setValue(mission_name);
                        
                        attributes.add(mName);
                    }
                    
                    // ////////////////////////////////////
                    // Mission Number Attribute definition
                    // ////////////////////////////////////
                    if(mission_number != null){
                        ShortAttribute mNumber = new ShortAttribute();
                        mNumber.setName("mission_number");
                        mNumber.setType(DataType.STRING);                    
                        mNumber.setValue(mission_number);
                        
                        attributes.add(mNumber);
                    }
                    
                    // ////////////////////////////////////
                    // Cruise Name Attribute definition
                    // ////////////////////////////////////
                	if(cruise_name != null){
                        ShortAttribute cName = new ShortAttribute();
                        cName.setName("cruise_name");
                        cName.setType(DataType.STRING);                    
                        cName.setValue(cruise_name);
                        
                        attributes.add(cName);
                	}
                	
                	if(cruise_dir != null){
                        ShortAttribute cDir = new ShortAttribute();
                        cDir.setName("logfile_path");
                        cDir.setType(DataType.STRING);                    
                        cDir.setValue(cruise_dir);
                        
                        attributes.add(cDir);
                	}
                	
                	if(metadata != null && !metadata.isEmpty()){
                		resource.setMetadata(metadata);
                	}
                	
                    if(attributes.size() > 0 && curr_time != null && vehicle_name != null && 
                    		cruise_name != null && cruise_dir != null){
                    	resource.setAttribute(attributes);
                    }else{
                    	if(LOGGER.isWarnEnabled()){
                    		LOGGER.warn("WARNING the logfile: " + logFile.getName() + " has no required attributes defined !");
                    	}
                    }
                    
                    geostoreClient.insert(resource);
        			
                	if(LOGGER.isInfoEnabled()){
                		LOGGER.info("Moving the file inside the defined location ...");
                	}
                	
                    FileSystemEvent event = new FileSystemEvent(logFile, FileSystemEventType.FILE_ADDED);
                    ret.add(event);
                }
                else
                {
                    if (LOGGER.isErrorEnabled())
                    {
                        LOGGER.error("Gliders Logs action.execute(): Encountered a NULL event: SKIPPING...");
                    }

                    continue;
                }
            }
            catch (Exception ioe)
            {
                if (LOGGER.isErrorEnabled()){
                    LOGGER.error( "Gliders Logs action.execute(): Unable to produce the output: ", ioe.getLocalizedMessage(), ioe);
                }
                throw new ActionException(this,ioe.getLocalizedMessage(),ioe );
            }
        }

        return ret;
    }

	/**
	 * @param client
	 */
	public void setGeoStoreClient(GeoStoreClient client) {
		this.geostoreClient = client;
	}

}
