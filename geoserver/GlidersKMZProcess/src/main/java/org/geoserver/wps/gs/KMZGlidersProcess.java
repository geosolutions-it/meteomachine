/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.gs.GSProcess;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;

import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Style;
import de.micromata.opengis.kml.v_2_2_0.StyleSelector;
import de.micromata.opengis.kml.v_2_2_0.TimeSpan;

/**
 * 
 * 
 * @author Alessio Fabiani - GeoSolutions
 * 
 */
@DescribeProcess(title = "kmzGlidersProcess", description = "Ingest Gliders KMZ input files.")
public class KMZGlidersProcess extends KMZProcess implements GSProcess {

	public static final String HEADING 				= "Heading";
	public static final String CRUISE_NAME 			= "cruise_name";
	public static final String GLIDER_NAME 			= "glider_name";
	public static final String OUT_AOI 			    = "out_aoi";

	public static final String TYPE_POINT 			= "Points";
	public static final String TYPE_ABORT 			= "Aborts";
	public static final String TYPE_WATER_CURRENT 	= "WaterCurrent";

	public static final String TYPE_NEXT_WPT 		= "Next WPT";

	public static final String TYPE_CURRENT_TRACK 	= "CurrentTrack";
    public static final String TYPE_OLD_TRACK 		= "OldTracks";
	
	final static String[] typeNames = new String[]{"GlidersPoints", "GlidersTracks", "GlidersNextWpts"};
	final static Map<String, List<String>> typeSchema = new HashMap<String, List<String>>();
	final static Map<String, List<String>> typeSchemaTypes = new HashMap<String, List<String>>();
	final static Map<String, Class<?>> typeGeometries = new HashMap<String, Class<?>>();

	final static List<String> gliderVariables;
	
	private static final String AOI_TABLE 			= "aoi";	
	private Map<String, Geometry> gliderAOi = new HashMap<String, Geometry>();
	
	private Date startTime;
	private Date endTime;

	static
	{
		typeSchema.put(typeNames[0], Arrays.asList(KMZGlidersProcess.GEOMETRY, KMZGlidersProcess.CRUISE_NAME, KMZGlidersProcess.GLIDER_NAME, KMZGlidersProcess.DESCRIPTION, KMZGlidersProcess.TYPE, KMZGlidersProcess.TIME_BEGIN, KMZGlidersProcess.TIME_END, KMZGlidersProcess.OUT_AOI));
		typeSchema.put(typeNames[1], Arrays.asList(KMZGlidersProcess.GEOMETRY, KMZGlidersProcess.CRUISE_NAME, KMZGlidersProcess.GLIDER_NAME, KMZGlidersProcess.DESCRIPTION, KMZGlidersProcess.TYPE));
		typeSchema.put(typeNames[2], Arrays.asList(KMZGlidersProcess.GEOMETRY, KMZGlidersProcess.CRUISE_NAME, KMZGlidersProcess.GLIDER_NAME, KMZGlidersProcess.DESCRIPTION, KMZGlidersProcess.NAME));

		typeSchemaTypes.put(typeNames[0], Arrays.asList(KMZGlidersProcess.TYPE_POINT, KMZGlidersProcess.TYPE_ABORT, KMZGlidersProcess.TYPE_WATER_CURRENT));
		typeSchemaTypes.put(typeNames[1], Arrays.asList(KMZGlidersProcess.TYPE_CURRENT_TRACK, KMZGlidersProcess.TYPE_OLD_TRACK));
		typeSchemaTypes.put(typeNames[2], Arrays.asList(KMZGlidersProcess.TYPE_NEXT_WPT));

		typeGeometries.put(typeNames[0], Point.class);
		typeGeometries.put(typeNames[1], MultiLineString.class);
		typeGeometries.put(typeNames[2], Point.class);
	
		//Points
		gliderVariables = new LinkedList<String>();
		gliderVariables.add("googleEarthTime");
		gliderVariables.add("strTime2");
		gliderVariables.add("key");
		gliderVariables.add("epoch");
		gliderVariables.add("date");
		gliderVariables.add("gmtTime");
		gliderVariables.add("lat_dd");
		gliderVariables.add("lon_dd");
		gliderVariables.add("u_zone");
		gliderVariables.add("u_lat");
		gliderVariables.add("u_lon");
		gliderVariables.add("battery_V");
		gliderVariables.add("vacuum_inHg");
		gliderVariables.add("leakdetect_V");
		gliderVariables.add("waterSpeed_m_s");
		gliderVariables.add("waterDirection_deg");
		gliderVariables.add("connectionTime");
		gliderVariables.add("disconnectionTime");
		gliderVariables.add("acquired");
		gliderVariables.add("reason");
		gliderVariables.add("mission");
		gliderVariables.add("filename");
		gliderVariables.add("filename_8_3");
		gliderVariables.add("log");
		gliderVariables.add("segment_Err_Warn_Odd");
		gliderVariables.add("mission_Err_Warn_Odd");
		gliderVariables.add("transect_Err_Warn_Odd");
		gliderVariables.add("diveTime");
		gliderVariables.add("diveDistance");
		gliderVariables.add("avgSpeed");
		gliderVariables.add("actualHeading");
		gliderVariables.add("nextWpt");
		gliderVariables.add("rangeToWpt_m");
		gliderVariables.add("bearToWpt");
		gliderVariables.add("wpt_ETA");
		gliderVariables.add("status");
		gliderVariables.add("status_code");
		gliderVariables.add("valid");
		gliderVariables.add("endTrack");
		
		//WaterCurrent
		gliderVariables.add("WaterSpeed_m_s");
		gliderVariables.add("WaterSpeed_knots");
		gliderVariables.add("WaterDir_deg");
	}

	public KMZGlidersProcess(GeoServer geoServer) {
		super(geoServer);
	}

	@DescribeResult(name = "result", description = "Name of the Cruise processed")
	public String execute(
			@DescribeParameter(name = "input Gliders KMZ file", description = "The feature collection to be updated", min = 1) Kml file,
			@DescribeParameter(name = "workspace", description = "The workSpace (must exist in the catalog)", min = 1) String workspace,
			@DescribeParameter(name = "store", description = "The dataStore (must exist in the catalog)", min = 1) String store,
			@DescribeParameter(name = "aoiKmzFormatOptions", description = "KMZ format options", min = 1) String aoiKmzFormatOptions,
			@DescribeParameter(name = "aoiKmzLayers", description = "KMZ layers list", min = 1) String aoiKmzLayers,
			@DescribeParameter(name = "aoiKmzStyles", description = "KMZ styles list", min = 1) String aoiKmzStyles,
			@DescribeParameter(name = "gsBaseUrl", description = "GS base URL for KMZ", min = 1) String gsBaseUrl,
			ProgressListener progressListener) throws ProcessException {

		// first off, decide what is the target store
		WorkspaceInfo ws;
		if (workspace != null) {
			ws = catalog.getWorkspaceByName(workspace);
			if (ws == null) {
				throw new ProcessException("Could not find workspace "
						+ workspace);
			}
		} else {
			ws = catalog.getDefaultWorkspace();
			if (ws == null) {
				throw new ProcessException(
						"The catalog is empty, could not find a default workspace");
			}
		}

		final String cruiseName = file.getFeature().getName();
		final List<Folder> gliders = new ArrayList<Folder>();
		
		for(Object folder : ((Document)file.getFeature()).getFeature())
		{
			if(folder instanceof Folder)
			{
				gliders.add((Folder) folder);
			}
		}
		
		// ok, find the target store
		DataStoreInfo storeInfo;
		if (store != null) {
			storeInfo = catalog.getDataStoreByName(ws.getName(), store);
			if (storeInfo == null) {
				throw new ProcessException("Could not find store " + store
						+ " in workspace " + workspace);
				// TODO: support store creation
			}
		} else {
			storeInfo = catalog.getDefaultDataStore(ws);
			if (storeInfo == null) {
				throw new ProcessException(
						"Could not find a default store in workspace "
								+ ws.getName());
			}
		}

		
		// ////////////////////////////////////////////////////////////////
		// Retrieving the AOI features for the current glider/cruise.
		// ////////////////////////////////////////////////////////////////
		try
		{
			// grab the data store
			DataStore ds = (DataStore) storeInfo.getDataStore(null);

			// start a transaction and fill the target with the input features
			Transaction t = new DefaultTransaction();
			SimpleFeatureStore fstore = (SimpleFeatureStore) ds.getFeatureSource(AOI_TABLE);
			fstore.setTransaction(t);
			
			try
			{
		        Filter filterAOI = ff.equals(ff.property("cruise_name"), ff.literal(cruiseName));			        
		        SimpleFeatureCollection collectionAOI = fstore.getFeatures(filterAOI);
		        
				// set outAOI attribute
				SimpleFeatureIterator features = collectionAOI.features();
				while(features.hasNext()){
					SimpleFeature feature = features.next();
					String gname = (String)feature.getAttribute("glider_name");
					Geometry g = (Geometry)feature.getDefaultGeometry();
					
					gliderAOi.put(gname, g);
				}
			}//try
			catch (IOException e) {
				throw new IOException(e);
			}//catch
			finally
			{
				t.close();
			}//finally
		}//try 
		catch (IOException e) {
			throw new ProcessException("", e);
		}//catch
		
		// ///////////////////////////////////
		// Ingestion
		// ///////////////////////////////////
		
		// check FeatureTypes
		for (String typeName : typeNames)
		{
			String tentativeTargetName = null;
			tentativeTargetName = ws.getName() + ":" + typeName;
			
			boolean createNew;
			if (catalog.getLayerByName(tentativeTargetName) == null) {
				createNew = true;
			} else {
				createNew = false;
			}

			SimpleFeatureType targetSchema;
			boolean hasTimeDimension = false;

			// build the schema type
			try {
				SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
				tb.setName(typeName);
				
				for(String attType : typeSchema.get(typeName))
				{
					if(attType.equals(KMZGlidersProcess.GEOMETRY))
					{
						tb.add(KMZGlidersProcess.GEOMETRY, typeGeometries.get(typeName), crs);
					}//the_geom::if
					else if(attType.equals(KMZGlidersProcess.DESCRIPTION))
					{
						tb.add(KMZGlidersProcess.DESCRIPTION, String.class);
					}//description::if
					else if(attType.equals(KMZGlidersProcess.TIME_BEGIN) || attType.equals(KMZGlidersProcess.TIME_END))
					{
						hasTimeDimension=true;
						tb.add(attType, Date.class);
					}//description::if
					else if(attType.equals(KMZGlidersProcess.OUT_AOI))
					{
						tb.add(attType, Boolean.class);
					}//description::if
					else
					{
						tb.add(attType, String.class);
					}//if::else
				}//schemaBuild::for
				
				/**
				 * adding gliders attributes
				 */
				for(String gliderVar : gliderVariables)
				{
					tb.add(gliderVar, String.class);
				}//for::gliderVariables
				
				/**
				 * adding heading for 'Points' types
				 */
				tb.add(KMZGlidersProcess.HEADING, Double.class);
				
				targetSchema = tb.buildFeatureType();
			} catch (Exception e) {
				throw new ProcessException(
						"Failed to import data into the target store", e);
			}//try:catch
			
			// check we can extract a code from the original data
			ProjectionPolicy srsHandling = ProjectionPolicy.FORCE_DECLARED;

			// create/update the layer
			if (createNew) 
			{
				// import the data into the target store
				try {
					createNewLayerOnDataStore(targetSchema,typeName,storeInfo,srsHandling,hasTimeDimension);
				} catch (Exception e) {
					throw new ProcessException(
							"Failed to create schema and layers into the target store", e);
				}//try:catch
			}//createNew::if
			else
			{
				try {
					for(Folder glider : gliders)
					{
						// grab the data store
						DataStore ds = (DataStore) storeInfo.getDataStore(null);

						// start a transaction and fill the target with the input features
						Transaction t = new DefaultTransaction();
						SimpleFeatureStore fstore = (SimpleFeatureStore) ds.getFeatureSource(typeName);
						fstore.setTransaction(t);
						
						try
						{
					        Filter filterCruise = ff.equals(ff.property("cruise_name"), ff.literal(cruiseName));
					        Filter filterGlider = ff.equals(ff.property("glider_name"), ff.literal(glider.getName()));

					        Filter filter = ff.and(Arrays.asList(filterCruise, filterGlider));
					        
					        fstore.removeFeatures(filter);
					        t.commit();
						}//try
						catch (IOException e) {
							t.rollback();
							throw new IOException(e);
						}//catch
						finally
						{
							t.close();
						}//finally
					}//gliders::if
				}//try 
				catch (IOException e) {
					throw new ProcessException(
							"Failed to remove data from the target store", e);
				}//catch
			}//createNew::else
			
			try
			{
				//Populate the features and update the DataStore
				for(String kmlType : typeSchemaTypes.get(typeName))
				{
					// grab the data store
					DataStore ds = (DataStore) storeInfo.getDataStore(null);

					// start a transaction and fill the target with the input features
					Transaction t = new DefaultTransaction();
					SimpleFeatureStore fstore = (SimpleFeatureStore) ds.getFeatureSource(typeName);
					fstore.setTransaction(t);

					try
					{
						SimpleFeatureBuilder fb = new SimpleFeatureBuilder(targetSchema);
						for(Folder glider : gliders)
						{
							for(Object gliderSubType : glider.getFeature())
							{
								if(gliderSubType instanceof Folder)
								{
									Folder folder = (Folder) gliderSubType;
									if(folder.getName().equals(kmlType))
									{
										for(Object element : folder.getFeature())
										{
											if(element instanceof Placemark)
											{
												fb.reset();
												fb.set(KMZGlidersProcess.CRUISE_NAME, cruiseName);
												fb.set(KMZGlidersProcess.GLIDER_NAME, glider.getName());

												Placemark pm = (Placemark) element;
												for(String attType : typeSchema.get(typeName))
												{
													if(attType.equals(KMZGlidersProcess.NAME))
													{
														fb.set(KMZGlidersProcess.NAME, pm.getName());
													}//KMZGlidersProcess.NAME::if
													if(attType.equals(KMZGlidersProcess.TYPE))
													{
														fb.set(KMZGlidersProcess.TYPE, kmlType);
													}//KMZGlidersProcess.NAME::if
													if(attType.equals(KMZGlidersProcess.DESCRIPTION) && pm.getDescription()!=null)
													{
														String desc = pm.getDescription().trim();

														desc = desc.replaceAll("<TABLE border=\"1\"><TR><TD><B>Variable</B></TD><TD><B>Value</B></TD></TR>", "");
														desc = desc.replaceAll("</TR></TABLE>","");
														desc = desc.replaceAll("</TD><TD>","=");
														desc = desc.replaceAll("</TD></TR><TR><TD>",";");
														desc = desc.replaceAll("<TR><TD>","");
														desc = desc.replaceAll("</TD>","");

														String[] variables = desc.split(";");
														if(variables != null && variables.length > 0)
														{
															for(String var : variables)
															{
																String[] varKvp = var.split("=");
																if(varKvp!=null && varKvp.length>0 && gliderVariables.contains( varKvp[0].replaceAll("\\(", "_").replaceAll("/", "_").replaceAll("\\)", "") ))
																{
																	fb.set( varKvp[0].replaceAll("\\(", "_").replaceAll("/", "_").replaceAll("\\)", "") , (varKvp.length>1 ? varKvp[1] : ""));
																}//gliderVariables::if
															}
														}//if
														else
														{
															fb.set(KMZGlidersProcess.DESCRIPTION, desc);
														}//else
													}//KMZGlidersProcess.DESCRIPTION::if
													if(attType.equals(KMZGlidersProcess.GEOMETRY))
													{
														if(typeGeometries.get(typeName) == Point.class && pm.getGeometry() instanceof de.micromata.opengis.kml.v_2_2_0.Point)
														{
															de.micromata.opengis.kml.v_2_2_0.Point thePoint = (de.micromata.opengis.kml.v_2_2_0.Point)pm.getGeometry();
															Point p = geomBuilder.point(thePoint.getCoordinates().get(0).getLongitude(), thePoint.getCoordinates().get(0).getLatitude());
															fb.set(KMZGlidersProcess.GEOMETRY, p);
															
															if(typeName.equalsIgnoreCase(typeNames[0])){
																if(gliderAOi.containsKey(glider.getName())){
														    		Geometry g = gliderAOi.get(glider.getName());
														    		if(!g.contains(p)){
														    			fb.set(KMZGlidersProcess.OUT_AOI, true);
														    		}else{
														    			fb.set(KMZGlidersProcess.OUT_AOI, false);													    			
														    		}
																}else{
																	fb.set(KMZGlidersProcess.OUT_AOI, false);	
																}
															} 													
														}//Point::if

														if(typeGeometries.get(typeName) == MultiLineString.class && pm.getGeometry() instanceof de.micromata.opengis.kml.v_2_2_0.LineString)
														{
															de.micromata.opengis.kml.v_2_2_0.LineString theLine = (de.micromata.opengis.kml.v_2_2_0.LineString)pm.getGeometry();
															
															double[] coords = new double[theLine.getCoordinates().size()*2];
															int c=0;
															if (theLine.getCoordinates().size() > 2)
															{
																for(Coordinate coord : theLine.getCoordinates())
																{
																	coords[c++] = coord.getLongitude();
																	coords[c++] = coord.getLatitude();
																}//for
															}//if
															else
															{
																for(@SuppressWarnings("unused") Coordinate coord : theLine.getCoordinates())
																{
																	coords[c++] = theLine.getCoordinates().get(0).getLongitude();
																	coords[c++] = theLine.getCoordinates().get(0).getLatitude();
																}//for
															}//else

															fb.set(KMZGlidersProcess.GEOMETRY, geomBuilder.multiLineString(geomBuilder.lineString(coords)));
														}//LineString::if

													}//KMZGlidersProcess.GEOMETRY::if
													if(attType.equals(KMZGlidersProcess.TIME_BEGIN))
													{
														if(pm.getTimePrimitive() instanceof TimeSpan)
														{
															String time = ((TimeSpan)pm.getTimePrimitive()).getBegin();
															fb.set(KMZGlidersProcess.TIME_BEGIN, time);
															
															if(typeName.equalsIgnoreCase(typeNames[0]) && time != null){
														        String pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'";
														        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
														        
														        Date start;
														        try
														        {
														            start = (Date)sdf.parse(time);
																	if(this.startTime != null){
																		if(start.before(this.startTime)){
																			this.startTime = start;
																		}
																	}else{
																		this.startTime = start;
																	}
														        } 
														        catch (ParseException e)
														        {
														            if(LOGGER.isLoggable(Level.WARNING)){
														            	LOGGER.info(e.getMessage());
														            }
														        }
															}
															
														}//TimePrimitive::if
													}//KMZGlidersProcess.TIME_BEGIN::if
													if(attType.equals(KMZGlidersProcess.TIME_END))
													{
														if(pm.getTimePrimitive() instanceof TimeSpan)
														{
															String time = ((TimeSpan)pm.getTimePrimitive()).getEnd();
															fb.set(KMZGlidersProcess.TIME_END, time);
															
															if(typeName.equalsIgnoreCase(typeNames[0]) && time != null){
														        String pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'";
														        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
														        
														        Date end;
														        try
														        {
														        	end = (Date)sdf.parse(time);
																	if(this.endTime != null){
																		if(end.after(this.endTime)){
																			this.endTime = end;
																		}
																	}else{
																		this.endTime = end;
																	}
														        } 
														        catch (ParseException e)
														        {
														            if(LOGGER.isLoggable(Level.WARNING)){
														            	LOGGER.info(e.getMessage());
														            }
														        }
															}
														}//TimePrimitive::if
													}//KMZGlidersProcess.TIME_END::if
												}//schemaBuild::for
												
												/**
												 * storing HEADING
												 */
												if(kmlType.equals(KMZGlidersProcess.TYPE_POINT) && pm.getStyleSelector() != null)
												{
													for(StyleSelector ssl : pm.getStyleSelector())
													{
														if(ssl instanceof de.micromata.opengis.kml.v_2_2_0.Style)
														{
															Style style = (de.micromata.opengis.kml.v_2_2_0.Style) ssl;
															if(style.getIconStyle() != null)
															{
																fb.set(KMZGlidersProcess.HEADING, style.getIconStyle().getHeading());
															}//if::iconStyle
														}//if::ssl
													}//for
												}//if::HEADING
												
												SimpleFeature target = fb.buildFeature(null);
												fstore.addFeatures(DataUtilities.collection(target));
											}//Placemark::if
										}//element::for
									}//folder.name::if
								}//gliderSubType.folder::if
							}//gliderSubType::for
						}//gliders::for
						t.commit();
					}//try 
					catch(Exception e)
					{
						t.rollback();
						throw new ProcessException(
								"Failed to import data into the target store", e);
					}//catch
					finally
					{
						t.close();
					}//finally
					
			        CatalogBuilder cb = new CatalogBuilder(catalog);
			        cb.setStore(storeInfo);
			        
					LayerInfo original = catalog.getLayerByName(typeName);
					original.getResource().setNativeBoundingBox(null);

					// compute the bounds
					cb.setupBounds(original.getResource());

					catalog.save(original.getResource());
					catalog.save(original);
					geoServer.reset();
				}//schemaTyapes::for
			} catch (Exception e) {
				throw new ProcessException(
						"Failed to import data into the target store", e);
			}//try:catch
		}//typeNames::for
		
		String response = cruiseName;
		
		// ////////////////////////////////////////////////
		// Produce the KML link if needed and if possible
		// ////////////////////////////////////////////////
		
		if(this.startTime != null && this.endTime != null){
			try {
				// /////////////////////////////
				// Gliders points OGC filter
				// /////////////////////////////
		        Filter filterCruise = ff.equals(ff.property("cruise_name"), ff.literal(cruiseName));
//		        Filter filterAoi = ff.equals(ff.property("out_aoi"), ff.literal(true));
		        Filter filterType = ff.equals(ff.property("type"), ff.literal("Points"));
	
		        Filter filter = ff.and(Arrays.asList(filterCruise, /*filterAoi,*/ filterType));
		        
		        org.geotools.xml.Configuration conf = new org.geotools.filter.v1_1.OGCConfiguration();
		        org.geotools.xml.Encoder encoder = new org.geotools.xml.Encoder( conf );
		        
		        String pointFilterString = encoder.encodeAsString(filter, org.geotools.filter.v1_0.OGC.Filter);
		        if(pointFilterString.startsWith("<?xml")){
		        	pointFilterString = pointFilterString.substring(pointFilterString.indexOf("<ogc:Filter"));
		        }
		        
				// /////////////////////////////
				// AOI OGC filter
				// /////////////////////////////
		        filter = ff.and(Arrays.asList(filterCruise));
		        
		        org.geotools.xml.Configuration aoiconf = new org.geotools.filter.v1_1.OGCConfiguration();
		        org.geotools.xml.Encoder aoiencoder = new org.geotools.xml.Encoder( aoiconf );
		        
		        String aoiFilterString = aoiencoder.encodeAsString(filter, org.geotools.filter.v1_0.OGC.Filter);
		        if(aoiFilterString.startsWith("<?xml")){
		        	aoiFilterString = aoiFilterString.substring(aoiFilterString.indexOf("<ogc:Filter"));
		        }
		        
				// ////////////////////////////
				// Build the KML URL
				// ////////////////////////////
				if(pointFilterString != null && aoiFilterString != null){
					String pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'";
			        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
			        
					String startDateString = sdf.format(this.startTime);
					String endDateString = sdf.format(this.endTime);
					
					String kmlURL = gsBaseUrl + "wms?" +
							"height=1024" +
							"&width=1024" +
							"&TIME=" + startDateString + "/" + endDateString +
							"&layers=" + aoiKmzLayers +
							"&request=GetMap" +
							"&service=wms" +
							"&BBOX=-180,-90,180,90" +
							"&styles=" + aoiKmzStyles +
							"&format_options=" + aoiKmzFormatOptions + 
							"&srs=EPSG:4326" +
							"&format=application/vnd.google-earth.kmz" +
							"&transparent=false" +
							"&version=1.1.1" +
							"&filter=(" + pointFilterString + ")(" + aoiFilterString + ")";
					
					response = kmlURL;
				}
				
			} catch (IOException e) {
	            if(LOGGER.isLoggable(Level.WARNING)){
	            	LOGGER.info(e.getMessage());
	            }
			}
		}
        
		return response;
	}
}