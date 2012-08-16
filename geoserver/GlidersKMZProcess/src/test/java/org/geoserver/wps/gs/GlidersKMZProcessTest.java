package org.geoserver.wps.gs;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerTestSupport;
import org.geoserver.wps.ppio.KMZPPIO;

import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.Kml;

public class GlidersKMZProcessTest extends GeoServerTestSupport {
	/**
	 * Test KMZ Binary PPIO
	 */
	public void testKMZPPIO() throws Exception {
		KMZPPIO kmzPPIO = (KMZPPIO) GeoServerExtensions.bean("kmzPPIO");

		assertNotNull(kmzPPIO);

		Kml kmlDoc = (Kml) kmzPPIO.decode(GlidersKMZProcessTest.class
				.getClassLoader().getResourceAsStream("kmzREP10_full.kmz"));

		assertNotNull(kmlDoc);

		assertEquals("Rep10", kmlDoc.getFeature().getName());

		int i = 0;
		for (Object folder : ((Document) kmlDoc.getFeature()).getFeature()) {
			if (folder instanceof Folder && i == 0) {
				assertEquals("elettra", ((Folder) folder).getName());
				break;
			}
			i++;
		}
	}
}
