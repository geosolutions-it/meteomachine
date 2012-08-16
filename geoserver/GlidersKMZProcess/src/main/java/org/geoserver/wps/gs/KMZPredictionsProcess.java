/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.TimeSpan;

/**
 * 
 * 
 * @author Alessio Fabiani - GeoSolutions
 * 
 */
@DescribeProcess(title = "kmzPredictionsProcess", description = "Ingest Predictions KMZ input files.")
public class KMZPredictionsProcess extends KMZProcess implements GSProcess {

	public static final String CRUISE_NAME 			= "cruise_name";
	public static final String GLIDER_NAME 			= "glider_name";

	public static final String TYPE_PREDICTED_TRACKS		= "Predicted Track";
	public static final String TYPE_PREDICTED_CURRENT 		= "Predicted Current";
	
	public static final String TYPE_ERROR_ELLIPSES 			= "Error Ellipses";
	public static final String TYPE_ERROR_ELLIPSE_ENVELOPE	= "Error Ellipse Envelope";

	public static final String TYPE_ERROR_ELLIPSE_CENTERS	= "Ellipse Centers";
	
	final static String[] typeNames = new String[]{"GlidersPredictedTracks", "GlidersPredictedCurrent", "GlidersErrorEllipses", "GlidersErrorEllipseEnvelopes", "GlidersErrorEllipseCenters"};
	final static Map<String, List<String>> typeSchema = new HashMap<String, List<String>>();
	final static Map<String, List<String>> typeSchemaTypes = new HashMap<String, List<String>>();
	final static Map<String, Class<?>> typeGeometries = new HashMap<String, Class<?>>();

	static
	{
		typeSchema.put(typeNames[0], Arrays.asList(KMZPredictionsProcess.GEOMETRY, KMZPredictionsProcess.CRUISE_NAME, KMZPredictionsProcess.GLIDER_NAME, KMZPredictionsProcess.DESCRIPTION, KMZPredictionsProcess.NAME, KMZPredictionsProcess.TIME_BEGIN, KMZPredictionsProcess.TIME_END));
		typeSchema.put(typeNames[1], Arrays.asList(KMZPredictionsProcess.GEOMETRY, KMZPredictionsProcess.CRUISE_NAME, KMZPredictionsProcess.GLIDER_NAME, KMZPredictionsProcess.DESCRIPTION, KMZPredictionsProcess.NAME, KMZPredictionsProcess.TIME_BEGIN, KMZPredictionsProcess.TIME_END));
		typeSchema.put(typeNames[2], Arrays.asList(KMZPredictionsProcess.GEOMETRY, KMZPredictionsProcess.CRUISE_NAME, KMZPredictionsProcess.GLIDER_NAME, KMZPredictionsProcess.DESCRIPTION, KMZPredictionsProcess.TYPE, KMZPredictionsProcess.NAME, KMZPredictionsProcess.TIME_BEGIN, KMZPredictionsProcess.TIME_END));
		typeSchema.put(typeNames[3], Arrays.asList(KMZPredictionsProcess.GEOMETRY, KMZPredictionsProcess.CRUISE_NAME, KMZPredictionsProcess.GLIDER_NAME, KMZPredictionsProcess.DESCRIPTION, KMZPredictionsProcess.TYPE, KMZPredictionsProcess.NAME));
		typeSchema.put(typeNames[4], Arrays.asList(KMZPredictionsProcess.GEOMETRY, KMZPredictionsProcess.CRUISE_NAME, KMZPredictionsProcess.GLIDER_NAME, KMZPredictionsProcess.DESCRIPTION, KMZPredictionsProcess.NAME, KMZPredictionsProcess.TIME_BEGIN, KMZPredictionsProcess.TIME_END));

		typeSchemaTypes.put(typeNames[0], Arrays.asList(KMZPredictionsProcess.TYPE_PREDICTED_TRACKS));
		typeSchemaTypes.put(typeNames[1], Arrays.asList(KMZPredictionsProcess.TYPE_PREDICTED_CURRENT));
		typeSchemaTypes.put(typeNames[2], Arrays.asList(KMZPredictionsProcess.TYPE_ERROR_ELLIPSES));
		typeSchemaTypes.put(typeNames[3], Arrays.asList(KMZPredictionsProcess.TYPE_ERROR_ELLIPSE_ENVELOPE));
		typeSchemaTypes.put(typeNames[4], Arrays.asList(KMZPredictionsProcess.TYPE_ERROR_ELLIPSE_CENTERS));

		typeGeometries.put(typeNames[0], MultiLineString.class);
		typeGeometries.put(typeNames[1], MultiLineString.class);
		typeGeometries.put(typeNames[2], Polygon.class);
		typeGeometries.put(typeNames[3], Polygon.class);
		typeGeometries.put(typeNames[4], Point.class);
	}

	public KMZPredictionsProcess(GeoServer geoServer) {
		super(geoServer);
	}

	@DescribeResult(name = "result", description = "Name of the Cruise processed")
	public String execute(
			@DescribeParameter(name = "input Predictions KMZ file", description = "The feature collection to be updated", min = 1) Kml file,
			@DescribeParameter(name = "cruise", description = "The Cruise name", min = 1) String cruiseName,
			@DescribeParameter(name = "glider", description = "The Glider name", min = 1) String gliderName,
			@DescribeParameter(name = "workspace", description = "The workSpace (must exist in the catalog)", min = 1) String workspace,
			@DescribeParameter(name = "store", description = "The dataStore (must exist in the catalog)", min = 1) String store,
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

		final List<Folder> predictions = new ArrayList<Folder>();
		
		for(Object folder : ((Document)file.getFeature()).getFeature())
		{
			if(folder instanceof Folder)
			{
				predictions.add((Folder) folder);
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
					if(attType.equals(KMZPredictionsProcess.GEOMETRY))
					{
						tb.add(KMZPredictionsProcess.GEOMETRY, typeGeometries.get(typeName), crs);
					}//the_geom::if
					else if(attType.equals(KMZPredictionsProcess.DESCRIPTION))
					{
						tb.add(KMZPredictionsProcess.DESCRIPTION, String.class);
					}//description::if
					else if(attType.equals(KMZPredictionsProcess.TIME_BEGIN) || attType.equals(KMZPredictionsProcess.TIME_END))
					{
						hasTimeDimension=true;
						tb.add(attType, Date.class);
					}//description::if
					else
					{
						tb.add(attType, String.class);
					}//if::else
				}//schemaBuild::for
				
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
					for(Folder folder : predictions)
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
					        Filter filterGlider = ff.equals(ff.property("glider_name"), ff.literal(gliderName));

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
						for(Folder folder : predictions)
						{
							if(folder.getName().equals(kmlType))
							{
								for(Object feature : folder.getFeature())
								{
									Object element = null;

									if(feature instanceof Placemark)
									{
										element = feature;
									}//Placemark::if
									else if(feature instanceof Document)
									{
										for(Object subFt : ((Document) feature).getFeature())
										{
											if(subFt instanceof Placemark)
											{
												element = subFt;
											}
										}//for
									}//Document::else

									if(element != null)
									{
										fb.reset();
										fb.set(KMZPredictionsProcess.CRUISE_NAME, cruiseName);
										fb.set(KMZPredictionsProcess.GLIDER_NAME, gliderName);

										Placemark pm = (Placemark) element;
										for (String attType : typeSchema.get(typeName)) {
											if (attType.equals(KMZPredictionsProcess.NAME)) {
												fb.set(KMZPredictionsProcess.NAME, pm.getName());
											}// KMZGlidersProcess.NAME::if
											if (attType.equals(KMZPredictionsProcess.TYPE)) {
												fb.set(KMZPredictionsProcess.TYPE, kmlType);
											}// KMZGlidersProcess.NAME::if
											if (attType.equals(KMZPredictionsProcess.DESCRIPTION) && pm.getDescription() != null) {
												String desc = pm.getDescription().trim();

												desc = desc.replaceAll("<TABLE border=\"1\"><TR><TD><B>Variable</B></TD><TD><B>Value</B></TD></TR>","");
												desc = desc.replaceAll("</TR></TABLE>","");
												desc = desc.replaceAll("</TD><TD>","=");
												desc = desc.replaceAll("</TD></TR><TR><TD>",";");
												desc = desc.replaceAll("<TR><TD>", "");
												desc = desc.replaceAll("</TD>", "");

												desc = ( desc.length()>=255 ? desc.substring(0, 254) : desc );
												
												fb.set(KMZPredictionsProcess.DESCRIPTION, desc);

											}// KMZGlidersProcess.DESCRIPTION::if
											if (attType.equals(KMZPredictionsProcess.GEOMETRY)) {
												if (typeGeometries.get(typeName) == Point.class && pm.getGeometry() instanceof de.micromata.opengis.kml.v_2_2_0.Point) {
													de.micromata.opengis.kml.v_2_2_0.Point thePoint = (de.micromata.opengis.kml.v_2_2_0.Point) pm.getGeometry();
													fb.set(KMZPredictionsProcess.GEOMETRY, geomBuilder
															.point(
																	thePoint.getCoordinates().get(0).getLongitude(),
																	thePoint.getCoordinates().get(0).getLatitude())
													);
												}// Point::if

												if (typeGeometries.get(typeName) == MultiLineString.class && pm.getGeometry() instanceof de.micromata.opengis.kml.v_2_2_0.LineString) {
													de.micromata.opengis.kml.v_2_2_0.LineString theLine = (de.micromata.opengis.kml.v_2_2_0.LineString) pm.getGeometry();

													double[] coords = new double[theLine.getCoordinates().size() * 2];
													int c = 0;
													if (theLine.getCoordinates().size() > 2) {
														for (Coordinate coord : theLine.getCoordinates()) {
															coords[c++] = coord.getLongitude();
															coords[c++] = coord.getLatitude();
														}// for
													}// if
													else {
														for (Coordinate coord : theLine.getCoordinates()) {
															coords[c++] = theLine.getCoordinates().get(0).getLongitude();
															coords[c++] = theLine.getCoordinates().get(0).getLatitude();
														}// for
													}// else

													fb.set(KMZPredictionsProcess.GEOMETRY, geomBuilder.multiLineString(geomBuilder.lineString(coords)));
												}// LineString::if

												if (typeGeometries.get(typeName) == Polygon.class && pm.getGeometry() instanceof de.micromata.opengis.kml.v_2_2_0.Polygon) {
													de.micromata.opengis.kml.v_2_2_0.Polygon thePoly = (de.micromata.opengis.kml.v_2_2_0.Polygon) pm.getGeometry();

													if(thePoly.getOuterBoundaryIs() != null && thePoly.getOuterBoundaryIs().getLinearRing() != null && thePoly.getOuterBoundaryIs().getLinearRing().getCoordinates().size() > 0)
													{
														double[] coords = new double[thePoly.getOuterBoundaryIs().getLinearRing().getCoordinates().size() * 2];
														int c = 0;
														for(Coordinate coord : thePoly.getOuterBoundaryIs().getLinearRing().getCoordinates())
														{
															coords[c++] = coord.getLongitude();
															coords[c++] = coord.getLatitude();
														}

														fb.set(KMZPredictionsProcess.GEOMETRY, geomBuilder.polygon(geomBuilder.linearRing(coords)));
													}//if
												}// Polygon::if

											}// KMZGlidersProcess.GEOMETRY::if
											if (attType.equals(KMZPredictionsProcess.TIME_BEGIN)) {
												if (pm.getTimePrimitive() instanceof TimeSpan) {
													fb.set(KMZPredictionsProcess.TIME_BEGIN, ((TimeSpan) pm.getTimePrimitive()).getBegin());
												}// TimePrimitive::if
											}// KMZGlidersProcess.TIME_BEGIN::if
											if (attType.equals(KMZPredictionsProcess.TIME_END)) {
												if (pm.getTimePrimitive() instanceof TimeSpan) {
													fb.set(KMZPredictionsProcess.TIME_END, ((TimeSpan) pm.getTimePrimitive()).getEnd());
												}// TimePrimitive::if
											}// KMZGlidersProcess.TIME_END::if
										}// schemaBuild::for

										SimpleFeature target = fb.buildFeature(null);
										fstore.addFeatures(DataUtilities.collection(target));
									}//if

								}//element::for
							}//prediction.name::if
						}//predictions::for
						t.commit();
					}//try 
					catch(Exception e)
					{
						t.rollback();
						throw new ProcessException("Failed to import data into the target store", e);
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
				throw new ProcessException("Failed to import data into the target store", e);
			}//try:catch
		}//typeNames::for

		return cruiseName;
	}

}