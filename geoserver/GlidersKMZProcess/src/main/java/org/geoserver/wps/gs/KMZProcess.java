package org.geoserver.wps.gs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.wps.WPSException;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.GeometryBuilder;
import org.geotools.process.ProcessException;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.FilterFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class KMZProcess {

	public static final String GEOMETRY 			= "the_geom";
	public static final String TYPE 				= "type";
	public static final String NAME 				= "name";
	public static final String DESCRIPTION 			= "description";
	public static final String TIME_BEGIN 			= "time_begin";
	public static final String TIME_END 			= "time_end";

	protected static final Logger LOGGER = Logging.getLogger(KMZProcess.class);
	
	protected GeoServer geoServer;
	
	protected Catalog catalog;
	
	protected FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
	
	protected GeometryBuilder geomBuilder = new GeometryBuilder();

	protected static CoordinateReferenceSystem crs;

	static
	{
		try {
			crs = CRS.decode("EPSG:4326");
		} catch (Exception e) {
			// unable to find the CRS
			throw new ProcessException(e);
		}
	}
	
	public KMZProcess(GeoServer geoServer) {
		this.geoServer = geoServer;
		this.catalog = geoServer.getCatalog();
	}

	/**
	 * Applies a set of heuristics to find which target attribute corresponds to
	 * a certain input attribute
	 * 
	 * @param sourceType
	 * @param targetType
	 * @return
	 */
	protected Map<String, String> buildAttributeMapping(SimpleFeatureType sourceType,
			SimpleFeatureType targetType) {
		// look for the typical manglings. For example, if the target is a
		// shapefile store it will move the geometry and name it the_geom

		// collect the source names
		Set<String> sourceNames = new HashSet<String>();
		for (AttributeDescriptor sd : sourceType.getAttributeDescriptors()) {
			sourceNames.add(sd.getLocalName());
		}

		// first check if we have been kissed by sheer luck and the names are
		// the same
		Map<String, String> result = new HashMap<String, String>();
		for (String name : sourceNames) {
			if (targetType.getDescriptor(name) != null) {
				result.put(name, name);
			}
		}
		sourceNames.removeAll(result.keySet());

		// then check for simple case difference (Oracle case)
		for (String name : sourceNames) {
			for (AttributeDescriptor td : targetType.getAttributeDescriptors()) {
				if (td.getLocalName().equalsIgnoreCase(name)) {
					result.put(name, td.getLocalName());
					break;
				}
			}
		}
		sourceNames.removeAll(result.keySet());

		// then check attribute names being cut (another Oracle case)
		for (String name : sourceNames) {
			String loName = name.toLowerCase();
			for (AttributeDescriptor td : targetType.getAttributeDescriptors()) {
				String tdName = td.getLocalName().toLowerCase();
				if (loName.startsWith(tdName)) {
					result.put(name, td.getLocalName());
					break;
				}
			}
		}
		sourceNames.removeAll(result.keySet());

		// consider the shapefile geometry descriptor mangling
		if (targetType.getGeometryDescriptor() != null
				&& "the_geom".equals(targetType.getGeometryDescriptor()
						.getLocalName())
				&& !"the_geom".equalsIgnoreCase(sourceType
						.getGeometryDescriptor().getLocalName())) {
			result.put(sourceType.getGeometryDescriptor().getLocalName(),
					"the_geom");
		}

		// and finally we return with as much as we can match
		if (!sourceNames.isEmpty()) {
			LOGGER.warning("Could not match the following attributes "
					+ sourceNames + " to the target feature type ones: "
					+ targetType);
		}
		return result;
	}
	
	/**
	 * Utility method which allows to build the schemaType into the datastore and create the layer into the catalog.
	 * 
	 * @param sourceType
	 * @param name
	 * @param storeInfo
	 * @param srsHandling
	 * @return
	 * @throws Exception
	 */
	protected SimpleFeatureCollection createNewLayerOnDataStore(
			SimpleFeatureType sourceType, String name,
			DataStoreInfo storeInfo, ProjectionPolicy srsHandling, boolean hasTimeDimension) throws Exception {
		SimpleFeatureCollection result = null;
		SimpleFeatureType targetType;
		// grab the data store
		DataStore ds = (DataStore) storeInfo.getDataStore(null);

		// decide on the target ft name
		if (name != null) {
			SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
			tb.init(sourceType);
			tb.setName(name);
			sourceType = tb.buildFeatureType();
		}

		// create the schema
		ds.createSchema(sourceType);

		// try to get the target feature type (might have slightly different
		// name and structure)
		targetType = ds.getSchema(sourceType.getTypeName());
		if (targetType == null) {
			// ouch, the name was changed... we can only guess now...
			// try with the typical Oracle mangling
			targetType = ds.getSchema(sourceType.getTypeName().toUpperCase());
		}

		if (targetType == null) {
			throw new WPSException(
					"The target schema was created, but with a name "
							+ "that we cannot relate to the one we provided the data store. Cannot proceeed further");
		} else {
			// check the layer is not already there
			String newLayerName = storeInfo.getWorkspace().getName() + ":"
					+ targetType.getTypeName();
			LayerInfo layer = catalog.getLayerByName(newLayerName);
			// todo: we should not really reach here and know beforehand what
			// the targetType
			// name is, but if we do we should at least get a way to drop it
			if (layer != null) {
				throw new ProcessException("Target layer " + newLayerName
						+ " already exists in the catalog");
			}
		}

		// now import the newly created layer into GeoServer
		CatalogBuilder cb = new CatalogBuilder(catalog);
		cb.setStore(storeInfo);

		// build the typeInfo and set CRS if necessary
		FeatureTypeInfo typeInfo = cb.buildFeatureType(sourceType.getName());
		typeInfo.setSRS(CRS.lookupIdentifier(KMZProcess.crs,false));
		if (srsHandling != null) {
			typeInfo.setProjectionPolicy(srsHandling);
		}
		// compute the bounds
		cb.setupBounds(typeInfo);

		if (hasTimeDimension)
		{
			// add metadata
			DimensionInfo timeDimension = new DimensionInfoImpl();
			timeDimension.setEnabled(true);
			timeDimension.setAttribute(KMZProcess.TIME_BEGIN);
			timeDimension.setEndAttribute(KMZProcess.TIME_END);
			timeDimension.setPresentation(DimensionPresentation.LIST);
			typeInfo.getMetadata().put("time", timeDimension);
		}//if::hasTimeDimension
		
		// build the layer and set a style
		LayerInfo layerInfo = cb.buildLayer(typeInfo);

		catalog.add(typeInfo);
		catalog.add(layerInfo);
		return result;
	}
	
}