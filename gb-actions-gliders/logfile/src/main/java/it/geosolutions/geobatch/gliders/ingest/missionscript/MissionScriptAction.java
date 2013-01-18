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
package it.geosolutions.geobatch.gliders.ingest.missionscript;

import it.geosolutions.filesystemmonitor.monitor.FileSystemEvent;
import it.geosolutions.geobatch.flow.event.action.ActionException;
import it.geosolutions.geobatch.flow.event.action.BaseAction;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.micromata.opengis.kml.v_2_2_0.AltitudeMode;
import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Icon;
import de.micromata.opengis.kml.v_2_2_0.IconStyle;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.KmlFactory;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Point;
import de.micromata.opengis.kml.v_2_2_0.Style;

/**
 * Class MissionScriptAction.
 * 
 * @author Tobia Di Pisa - tobia.dipisa@geo-solutions.it
 *
 */
public class MissionScriptAction extends BaseAction<EventObject>
{
	private static final Logger LOGGER = LoggerFactory.getLogger(MissionScriptAction.class);
	
    /**
     * configuration
     */
    private final MissionScriptConfiguration conf;

    public MissionScriptAction(MissionScriptConfiguration configuration)
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
                        LOGGER.trace("Gliders Mission Script action.execute(): working on incoming event: " +
                            ev.getSource());
                    }

                    FileSystemEvent fileEvent = (FileSystemEvent) ev;

                    File scriptZip = fileEvent.getSource();   
                    String vheicleName = scriptZip.getName().split("\\.")[0];
 
                    String cruiseDirUnzipPath = conf.getUnzipPath() + File.separatorChar + conf.getCruiseName().toLowerCase();
                    File cruiseDir = new File(cruiseDirUnzipPath);
                    
                    if(!cruiseDir.exists()){
                    	cruiseDir.mkdir();
                    }
                    
                    // ////////////////////////////////
                    // Unzip the received file 
                    // ////////////////////////////////
                    String unzippedPath = cruiseDirUnzipPath + File.separatorChar + vheicleName + "-" + System.nanoTime();
                    File extractTo = new File(unzippedPath); 
                    
                    if(!extractTo.exists()){
                    	extractTo.mkdir();
                    }
                    
                    MissionScriptAction.unzip(scriptZip, extractTo);
                    
                    // /////////////////////////////////////////////////////////////////////
                    // Read the 'goto_l' and 'yo' file to extract the next WP list and args
                    // /////////////////////////////////////////////////////////////////////
                    File scriptDir = new File(unzippedPath);
                    
                    File goto_l_file = null;
                    File yo_file = null;
                    
                    File[] filelistNames = scriptDir.listFiles();
                    for(int i=0; i < filelistNames.length; i++){
                    	String file_name = filelistNames[i].getName();
                    	if(file_name.startsWith("goto_l") && file_name.endsWith(".ma")){
                    		goto_l_file = filelistNames[i];
                    		
                    		if(yo_file != null)
                    			break;
                    	}else if(file_name.startsWith("yo") && file_name.endsWith(".ma")){
                    		yo_file = filelistNames[i];
                    		
                    		if(goto_l_file != null)
                    			break;
                    	}
                    }
                    
                    // //////////////////////////////////////////////
                    // Parsing goto_l_file to extract point list
                    // //////////////////////////////////////////////
                    Map<String, String[]> args = new HashMap<String, String[]>();
                    boolean wpStart = false;
                    
                    List<String> wpList = new ArrayList<String>();
                    
                    Scanner scanner =  null;
                    
                	// ////////////////////////
                	// goto_lN.ma file  
                	// ////////////////////////
            		try{
            			scanner = new Scanner(goto_l_file);
            			while (scanner.hasNextLine()){
            				String line = scanner.nextLine();
            				
            				// ///////////////////////////
            				// Parse goto b_args
            				// ///////////////////////////
                        	if(line.contains("num_waypoints(nodim)")){
                        		args.put("num_waypoints(nodim)", new String[]{"Waypoints number", this.parseBArg(line, "num_waypoints(nodim)")});
                        	}else if(line.contains("num_legs_to_run(nodim)")){                     		
                        		args.put("num_legs_to_run(nodim)", new String[]{"Number of waypoints to track", this.parseBArg(line, "num_legs_to_run(nodim)")});
                        	}else if(line.contains("initial_wpt(enum)")){                     		
                        		args.put("initial_wpt(enum)", new String[]{"First waypoint", this.parseBArg(line, "initial_wpt(enum)")});
                        	}else 
                        	
            				// //////////////////////////////////////////////////////
            				// Setting wpStart in order to parse goto waypoints list
            				// //////////////////////////////////////////////////////
                        	
                    		if(line.contains("<start:waypoints>") && !wpStart){
                    			wpStart = true;
                    			continue;
                    		}else if(line.contains("<end:waypoints>") && wpStart){
                    			wpStart = false;
                    			continue;
                    		}else
                        	
            				// ///////////////////////////
            				// Parse goto waypoints list
            				// ///////////////////////////
            				
        					if(wpStart){
        						String coords = line;
        						
        						// Removing white spaces before
        						while(coords.startsWith(" ")){
        							coords = coords.replaceFirst(" ", "");
        						}
        						
        						// Removing white spaces after
        						while(line.endsWith(" ")){
        							coords = coords.substring(0, coords.length() - 1);
        						}
        						
        						String[] coordsArray = coords.split(" ");
        						
        						// /////////////////////////////
        						// Adjust Coords values
        						// /////////////////////////////
        						int approx = conf.getCoordsApprox() != null ?  conf.getCoordsApprox() : 1;
        						Double x = Double.valueOf(coordsArray[0])/approx, y = null;
        						
        						for(int i=1, size=coordsArray.length; i<size; i++){
        							if(coordsArray[i] != null && !coordsArray[i].isEmpty()){
        								y = Double.valueOf(coordsArray[i])/approx; 
        								break;
        							}
        						}
        						
        						if(x != null && y != null){
        							wpList.add(x.toString() + "," + y.toString());
        						}else{
        							throw new NullPointerException("X and/or Y coordinate are NULL for the WP list"); 
        						}
                    		}
            			}   
            		}catch(NumberFormatException exc){
        				if (LOGGER.isErrorEnabled())
        					LOGGER.error("An error occurred while parsing WP list", exc);
        				throw new Exception(exc.getMessage());
            		}catch(NullPointerException exc){
        				if (LOGGER.isErrorEnabled())
        					LOGGER.error("X and/or Y coordinate are NULL for the WP list", exc);
        				throw new Exception(exc.getMessage());
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
                		LOGGER.info("Finish parsing goto_l file ...");
                	}
                	
                	// ////////////////////////
                	// yoN.ma file  
                	// ////////////////////////
                	
            		try{
            			scanner = new Scanner(yo_file);
            			while (scanner.hasNextLine()){
            				String line = scanner.nextLine();
            				
            				// ///////////////////////////
            				// Parse yo b_args
            				// ///////////////////////////
                        	if(line.contains("d_target_depth(m)")){
                        		args.put("d_target_depth(m)", new String[]{"Target depth diving", this.parseBArg(line, "d_target_depth(m)")});
                        	}else if(line.contains("d_target_altitude(m)")){                     		
                        		args.put("d_target_altitude(m)", new String[]{"Target altitude diving", this.parseBArg(line, "d_target_altitude(m)")});
                        	}else if(line.contains("d_use_pitch(enum)")){                     		
                        		args.put("d_use_pitch(enum)", new String[]{"Pitch control method diving", this.parseBArg(line, "d_use_pitch(enum)")});
                        	}else if(line.contains("d_pitch_value(X)")){                     		
                        		args.put("d_pitch_value(X)", new String[]{"Desired pitch angle diving", this.parseBArg(line, "d_pitch_value(X)")});
                        	}else if(line.contains("c_target_depth(m)")){                     		
                        		args.put("c_target_depth(m)", new String[]{"Target depth climbing", this.parseBArg(line, "c_target_depth(m)")});
                        	}else if(line.contains("c_target_altitude(m)")){                     		
                        		args.put("c_target_altitude(m)", new String[]{"Target altitude climbing", this.parseBArg(line, "c_target_altitude(m)")});
                        	}else if(line.contains("c_use_pitch(enum)")){                     		
                        		args.put("c_use_pitch(enum)", new String[]{"Pitch control method climbing", this.parseBArg(line, "c_use_pitch(enum)")});
                        	}else if(line.contains("c_pitch_value(X)")){                     		
                        		args.put("c_pitch_value(X)", new String[]{"Desired pitch angle climbing", this.parseBArg(line, "c_pitch_value(X)")});
                        	}
            			}   
            		}catch(NumberFormatException exc){
        				if (LOGGER.isErrorEnabled())
        					LOGGER.error("An error occurred while parsing WP list", exc);
        				throw new Exception(exc.getMessage());
            		}catch(NullPointerException exc){
        				if (LOGGER.isErrorEnabled())
        					LOGGER.error("X and/or Y coordinate are NULL for the WP list", exc);
        				throw new Exception(exc.getMessage());
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
                		LOGGER.info("Finish parsing yo file ...");
                	}
                	
                	// /////////////////////////////
                	// Creating the KML file
                	// /////////////////////////////
                     
                	// The all encapsulating kml element.
                	Kml kml = KmlFactory.createKml();
                	
                	// //////////////////////////
                	// Setting KML Document
                	// //////////////////////////
                	Document document = KmlFactory.createDocument();
                	document.setName(vheicleName);
                	kml.setFeature(document);
                	
                	// ///////////////////////////////////////////
                	// Setting KML Style for the first point
                	// ///////////////////////////////////////////
                	Style firstPlacemarkStyle = document.createAndAddStyle();
                	firstPlacemarkStyle.setId("firstPlacemarkStyle");
                	
                	IconStyle firstIconStyle = firstPlacemarkStyle.createAndSetIconStyle();
                	firstIconStyle.setScale(conf.getKmlIconScale());                 
                	Icon firstIcon = firstIconStyle.createAndSetIcon();
                	
                	firstIcon.setHref(conf.getKmlFirstIconURLpath()); 
                	
                	// ///////////////////////////////////////////
                	// Setting KML Style for the second one
                	// ///////////////////////////////////////////
                	Style secondPlacemarkStyle = document.createAndAddStyle();
                	secondPlacemarkStyle.setId("secondPlacemarkStyle");
                	
                	IconStyle secondIconStyle = secondPlacemarkStyle.createAndSetIconStyle();
                	secondIconStyle.setScale(conf.getKmlIconScale());                 
                	Icon secondIcon = secondIconStyle.createAndSetIcon();
                	
                	secondIcon.setHref(conf.getKmlSecondIconURLpath());
                	
                	// ////////////////////////////////
                	// Preparing a list of Placemarks
                	// ////////////////////////////////
                	List<Feature> placemarks = new ArrayList<Feature>();
                	Iterator<String> iterator = wpList.iterator();
                	
                	boolean first = true;
                	while(iterator.hasNext()){
                		String coords = iterator.next();
                		
                    	// Create <Placemark> and set values.
                    	Placemark placemark = KmlFactory.createPlacemark();
                    	placemark.setName(vheicleName);
                    	placemark.setVisibility(true);
                    	
                    	if(first){
                    		placemark.setDescription(this.makeHTML(args));
                    		placemark.setStyleUrl("#firstPlacemarkStyle");
                    		first = false;
                    	}else{
                    		placemark.setStyleUrl("#secondPlacemarkStyle");
                    	}
                    	                    	
                    	// Create <Point> and set values.
                    	Point point = KmlFactory.createPoint();
                    	point.setExtrude(false);
                    	point.setAltitudeMode(AltitudeMode.RELATIVE_TO_SEA_FLOOR);
                    	point.getCoordinates().add(new Coordinate(coords + "," + "0"));

                    	placemark.setGeometry(point);      // <-- point is registered at placemark ownership.
                    	placemarks.add(placemark);
                	}
                	
                	document.setFeature(placemarks);
                	
                	//
                	// Create KML file 
                	//
                	String kmlOutputPath = conf.getKmlOutputPath();
                	String kmlPath = kmlOutputPath == null || kmlOutputPath.isEmpty() ? unzippedPath : kmlOutputPath;
                	
                	if(kmlOutputPath == null){
        				if (LOGGER.isWarnEnabled())
        					LOGGER.warn("The kmlOutputPath isnt specified inside the Action configuration! The KML file will be placed at the unzip path: " + kmlPath);
                	}

    				if (LOGGER.isInfoEnabled())
    					LOGGER.info("Generating the KML Output at the following System path: " + kmlPath);
    				
                	File kmlOutput = new File(kmlPath + File.separatorChar + extractTo.getName() + ".kml");
                	FileOutputStream os = null;
                	
                	try{
                		os = new FileOutputStream(kmlOutput);
                		kml.marshal(os);           // <-- Print the KML structure to the console.
                	}finally{
                		try{
                    		if(os != null){
                    			os.close();
                    		}	
                		}catch(IOException e){
            				if (LOGGER.isErrorEnabled())
            					LOGGER.error("Error closing the KML file output stream ", e);
            				throw new Exception(e.getMessage());
                		}
                	}
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
    
    private String makeHTML(Map<String, String[]> args) {

    	String html = 
    			"<html>" +
			    	  "<head>" +
			    	    "<title>Mission Script Descritpion</title>" +
			    	  "</head>" +
			    	  "<style type=\"text/css\">" + 
				    	  "table{" +
				    	 	"border-collapse:collapse;" +
				    	  "}" +
				    	  "table, th, td{" +
					    	  "border: 1px solid blue;" +
				    	  "}" +
			    	  "</style>" +
				      "<body>" +
					      "<div align=\"center\">" +
					    	  "<a href=\"" + conf.getPlacemarkLogoAnchor() + "\" target=\"_blank\"><img align=\"middle\" src=\"" + conf.getPlacemarkLogoURL() + "\" width=\"60\" height=\"60\" Hspace=\"10\" Vspace=\"5\"/></a>" +  
					      "</div>";
    			
    	String table = 
    			"<table cellpadding=\"5px\">";
    	
    	Set<String> argsKeys = args.keySet();
    	Iterator<String> iterator = argsKeys.iterator();
    	
    	while(iterator.hasNext()){
    		String key = iterator.next();    
    		
    		String[] obj = args.get(key);
    		
    		String th = "<tr>";
    		
    		String td = "<td>" + obj[0] + "</td>";
    		th += td;
    		
    		td = "<td>" + obj[1] + "</td>";
    		th += td;
    		
    		td = "<td>" + key + "</td>";
    		th += td;
    		
    		th += "</tr>";
    		table += th;
    	}
    			
        table += "</table>";
    	
        html += table;
        
        html += 
        		"</body>" + 
        	"</html>";
        
		return html;
	}

	/**
     * @param line
     * @param arg_name
     * @return String
     */
    private String parseBArg(String line, String arg_name){
		String argValue = line.replace("b_arg:", "");
		
		while(argValue.startsWith(" ")){
			argValue = argValue.replaceFirst(" ", "");
		}
		
		argValue = line.replace(arg_name, "");
		
		while(argValue.startsWith(" ")){
			argValue = argValue.replaceFirst(" ", "");
		}
		
		while(argValue.endsWith(" ")){
			argValue = argValue.substring(0, argValue.length() - 1);
		}
		
		return argValue;
    }
    
    /**
     * @param zip
     * @param extractTo
     * @throws IOException
     */
    public static void unzip(File zip, File extractTo) throws IOException {
    	ZipFile archive = new ZipFile(zip);
    	Enumeration<?> e = archive.entries();
    	
    	while (e.hasMoreElements()) {
    		ZipEntry entry = (ZipEntry) e.nextElement();
    		File file = new File(extractTo, entry.getName());
    		
    		if (entry.isDirectory() && !file.exists()) {
    			file.mkdirs();
    		} else {
    			if (!file.getParentFile().exists()) {
    				file.getParentFile().mkdirs();
    			}

    			InputStream in = null;
    			BufferedOutputStream out = null;
    			try{
        			in = archive.getInputStream(entry);
        			out = new BufferedOutputStream(
        					new FileOutputStream(file));
        			
        			byte[] buffer = new byte[8192];
        			int read;

        			while (-1 != (read = in.read(buffer))) {
        				out.write(buffer, 0, read);
        			}
    			}catch(IOException exc){
    				if (LOGGER.isErrorEnabled())
    					LOGGER.error("Error using the unzip output stream for the current mission script ingestion ", exc);
    				throw new IOException(exc.getMessage());
    			}finally{
            		try{
                		if(in != null){
                			in.close();
                		}	
                		
                		if(out != null){
                			out.close();
                		}	
            		}catch(IOException exc){
        				if (LOGGER.isErrorEnabled())
        					LOGGER.error("Error closing the unzip stream ", exc);
        				throw new IOException(exc.getMessage());
            		}
    			}
    		}
    	}
    }
}
