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
package it.geosolutions.geobatch.gliders.ingest.missionlogfile;

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
 * Class MissionLogsAction.
 * 
 * @author Tobia Di Pisa - tobia.dipisa@geo-solutions.it
 *
 */
public class MissionLogsAction extends BaseAction<EventObject>
{
	private static final Logger LOGGER = LoggerFactory.getLogger(MissionLogsAction.class);
	

    /**
     * configuration
     */
    private final MissionLogsConfiguration conf;


	private GeoStoreClient geostoreClient;

    public MissionLogsAction(MissionLogsConfiguration configuration)
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
                        LOGGER.trace("Gliders Mission Logs action.execute(): working on incoming event: " +
                            ev.getSource());
                    }

                    FileSystemEvent fileEvent = (FileSystemEvent) ev;

                    File missionLogFile = fileEvent.getSource();   
                    
                    // ///////////////////////////////
                    // Parse the log file
                    // ///////////////////////////////

                    Date curr_time = null;
                    Date timestamp = null;
                    
                    Double log_file_opened = null;
                    Double log_file_closed = null;
                    Double curr_time_mt = null;
                    
            		String vehicle_name = null;
            		String mission_name = null;
            		String mission_number = null;
            		String cruise_name = conf.getCruiseName();
            		String cruise_dir = conf.getCruiseDir();
            		
            		String pattern = conf.getTimePattern();
			        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.US);
			        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
			        
            		Scanner scanner =  null;
            		try{
            			scanner = new Scanner(missionLogFile);
            			while (scanner.hasNextLine()){
            				String line = scanner.nextLine();
            				
            				// ///////////////////////////
            				// Parse Mission Name
            				// ///////////////////////////
                        	if(line.contains("Mission Name:") && mission_name == null){
                        		String mname = line.split(":")[1];
                        		
                        		mname = mname.trim();
                        		
                        		mission_name = mname;
                        	}
                        	
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
                        	}
                        	
            				// ///////////////////////////
            				// Parse Vehicle Name
            				// ///////////////////////////
                        	if(line.contains("Vehicle Name:") && vehicle_name == null){
                        		String name = line.split(":")[1];

                        		name = name.trim();
                        		
                        		vehicle_name = name;
                        	}
                        	
            				// ///////////////////////////
            				// Parse LOG FILE OPENED
            				// ///////////////////////////
                        	if(line.contains("LOG FILE OPENED") && log_file_opened == null){
                        		String lfo = line;
                        		
                        		while(lfo.startsWith(" ")){
                        			lfo = lfo.replaceFirst(" ", "");
                        		}
                        		
                        		lfo = lfo.split(" ")[0];
                        		
                        		try{
                        			log_file_opened = Double.parseDouble(lfo);
                        		}catch(NumberFormatException exc){
                        			log_file_opened = null;
                        			
                    				if (LOGGER.isWarnEnabled())
                    					LOGGER.warn("WARNING: Exception parsing file: " + missionLogFile + ". The LOG FILE OPENED property ", exc);
                        		}
                        	}
                        	
            				// ///////////////////////////
            				// Parse LOG FILE CLOSED
            				// ///////////////////////////
                        	if(line.contains("LOG FILE CLOSED") && log_file_closed == null){
                        		String lfc = line;
                        		
                        		while(lfc.startsWith(" ")){
                            		lfc = lfc.replaceFirst(" ", "");
                        		}
                        		
                        		lfc = lfc.split(" ")[0];
                        		
                        		try{
                        			log_file_closed = Double.parseDouble(lfc);
                        		}catch(NumberFormatException exc){
                        			log_file_closed = null;
                        			
                    				if (LOGGER.isWarnEnabled())
                    					LOGGER.warn("WARNING: Exception parsing file: " + missionLogFile + ". The LOG FILE CLOSED property ", exc);
                        		}
                        	}
                        	
                        	if(line.contains("timestamp:") && timestamp == null){
                        		
                				// ///////////////////////////
                				// Parse Timestamp
                				// ///////////////////////////
                        		
                        		String time = line.split("timestamp:")[1];
                        		
                        		while(time.startsWith(" ")){
                        			time = time.replaceFirst(" ", "");
                        		}
                        		
                        		while(time.endsWith(" ")){
                        			time = time.substring(0, time.length() - 1);
                        		}
                        		
                        		timestamp = sdf.parse(time);
                        		
                        	}else if(line.contains("Curr Time:") && curr_time == null){
                        		
                				// ///////////////////////////
                				// Parse Curr Time
                				// ///////////////////////////
                        		
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
                        		
                        		curr_time = sdf.parse(ctime);
                        		
                        		// --- Parsing MT --- //
                        		String mt = time_curr_props[1];
                        		
                        		mt = mt.trim();
                        		
                        		try{
                        			curr_time_mt = Double.parseDouble(mt);
                        		}catch(NumberFormatException exc){
                        			curr_time_mt = null;
                        			
                    				if (LOGGER.isWarnEnabled())
                    					LOGGER.warn("WARNING: Exception parsing the MT property ", exc);
                        		}
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
                    resource.setName(missionLogFile.getName());
                    
                    // ///////////////////////////////////////
                    // Check if the LOGFILE category exists 
                    // ///////////////////////////////////////
                    RESTCategory category = new RESTCategory(conf.getCategoryName());
                    resource.setCategory(category);
                    
                	List<ShortAttribute> attributes = new ArrayList<ShortAttribute>();
                	
                	// ////////////////////////////////////
                    // Time begin/end Attributes definition
                    // ////////////////////////////////////
                    
                	DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                    if(timestamp != null){
                    	
                    	// --- time begin --- //
	                    ShortAttribute startTime = new ShortAttribute();
	                    startTime.setName("time_begin");
	                    startTime.setType(DataType.DATE);	                    
	                    startTime.setValue(format.format(timestamp));
	                    
	                    attributes.add(startTime);
	                    
	                    // --- time end --- //	                    
	                    ShortAttribute endTime = new ShortAttribute();
	                    endTime.setName("time_end");
	                    endTime.setType(DataType.DATE);	
	                    
	                    if(log_file_closed != null){
	                    	long timestamp_seconds = timestamp.getTime() / 1000;
	                    	long seconds = new Double(timestamp_seconds + log_file_closed).longValue();
	                    	endTime.setValue(format.format(new Date(seconds * 1000)));
	                    }else{
	                    	endTime.setValue(format.format(timestamp));
	                    	
	                    	if (LOGGER.isWarnEnabled())
            					LOGGER.warn("WARNING: Exception setting time_end attribute: log_file_closed is null");
	                    }
	                    
	                    attributes.add(endTime);
                    	
                    }else if(curr_time != null){
                    	
                    	// --- time begin --- //
	                    ShortAttribute startTime = new ShortAttribute();
	                    startTime.setName("time_begin");
	                    startTime.setType(DataType.DATE);	       
	                    
	                    if(log_file_opened != null && curr_time_mt != null){
	                    	long curr_time_seconds = curr_time.getTime() / 1000;
	                    	long seconds = new Double(curr_time_seconds - (curr_time_mt - log_file_opened)).longValue();
	                    	startTime.setValue(format.format(new Date(seconds * 1000)));
	                    }else{
	                    	startTime.setValue(format.format(curr_time));
	                    	
	                    	if (LOGGER.isWarnEnabled())
            					LOGGER.warn("WARNING: Exception setting time_begin attribute: LOG FILE OPENED and/or MT are null");
	                    }
	                    
	                    attributes.add(startTime);
	                    
	                    // --- time end --- //
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
                	
                    if(attributes.size() > 0 && (curr_time != null || timestamp != null) &&
                    		vehicle_name != null && cruise_name != null && cruise_dir != null){
                    	resource.setAttribute(attributes);
                    }else{
                    	if(LOGGER.isWarnEnabled()){
                    		LOGGER.warn("WARNING the Mission Logfile: " + missionLogFile.getName() + " has no required attributes defined !");
                    	}
                    }
                    
                    geostoreClient.insert(resource);
        			
                	if(LOGGER.isInfoEnabled()){
                		LOGGER.info("Moving the file inside the defined location ...");
                	}
                	
                    FileSystemEvent event = new FileSystemEvent(missionLogFile, FileSystemEventType.FILE_ADDED);
                    ret.add(event);
                }
                else
                {
                    if (LOGGER.isErrorEnabled())
                    {
                        LOGGER.error("Gliders Mission Logs action.execute(): Encountered a NULL event: SKIPPING...");
                    }

                    continue;
                }
            }
            catch (Exception ioe)
            {
                if (LOGGER.isErrorEnabled()){
                    LOGGER.error( "Gliders Mission Logs action.execute(): Unable to produce the output: ", ioe.getLocalizedMessage(), ioe);
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
