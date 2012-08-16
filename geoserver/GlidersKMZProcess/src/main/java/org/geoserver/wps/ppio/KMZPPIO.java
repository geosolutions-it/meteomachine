/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.geoserver.data.util.IOUtils;
import org.geoserver.wps.resource.WPSResourceManager;

import de.micromata.opengis.kml.v_2_2_0.Kml;

/**
 * Handles input and output of KML as zipped KMZ
 * 
 * @author Alessio Fabiani - GeoSolutions
 */
public class KMZPPIO extends BinaryPPIO {

	WPSResourceManager resources;

	protected KMZPPIO(WPSResourceManager resources) {
		super(Kml.class, Kml.class, "application/vnd.google-earth.kmz");
		this.resources = resources;
	}

	@Override
	public void encode(Object value, OutputStream os) throws Exception {
		// create the temp directory and register it as a temporary resource
		File tempDir = IOUtils.createTempDirectory("kmztemp");
		File tempFile = File.createTempFile("kmztemp", ".tmp", tempDir);
		Kml kmlDocument = (Kml) value;
		kmlDocument.marshalAsKmz(tempFile.getAbsolutePath());
		org.apache.commons.io.IOUtils.copy(new FileReader(tempFile), os);
	}

	@Override
	public Object decode(InputStream input) throws Exception {
		// create the temp directory and register it as a temporary resource
		File tempDir = IOUtils.createTempDirectory("kmztemp");

		// unzip to the temporary directory
		ZipInputStream zis = null;
		File kmlFile = null;
		try {
			zis = new ZipInputStream(input);
			ZipEntry entry = null;

			while ((entry = zis.getNextEntry()) != null) {
				String name = entry.getName();
				File file = new File(tempDir, entry.getName());
				if (entry.isDirectory()) {
					file.mkdir();
				} else {
					if (file.getName().toLowerCase().endsWith(".kml")) {
						kmlFile = file;

						int count;
						byte data[] = new byte[4096];
						// write the files to the disk
						FileOutputStream fos = null;
						try {
							fos = new FileOutputStream(file);
							while ((count = zis.read(data)) != -1) {
								fos.write(data, 0, count);
							}
							fos.flush();
						} finally {
							if (fos != null) {
								fos.close();
							}
						}
					}
				}
				zis.closeEntry();
			}
		} finally {
			if (zis != null) {
				zis.close();
			}
		}

		if (kmlFile == null) {
			FileUtils.deleteDirectory(tempDir);
			throw new IOException(
					"Could not find any file with .kml extension in the kmz file");
		} else {
			BufferedReader in = null;
			PrintWriter out = null;
			File tempFile = File.createTempFile("kmztemp", ".tmp", tempDir);
			try {
				in = new BufferedReader(new FileReader(kmlFile));
				out = new PrintWriter(tempFile);

				String line; // a line in the file
				while ((line = in.readLine()) != null) {
					if (line.startsWith("<kml")){
						out.println("<kml xmlns=\"http://www.opengis.net/kml/2.2\" xmlns:ns2=\"http://www.w3.org/2005/Atom\" xmlns:ns3=\"urn:oasis:names:tc:ciq:xsdschema:xAL:2.0\" xmlns:ns4=\"http://www.google.com/kml/ext/2.2\">");
					} else {
						out.println(line);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (in != null)
					in.close();
				if (out != null) {
					out.flush();
					out.close();
				}
			}

			return Kml.unmarshal(tempFile);
		}

	}

	@Override
	public String getFileExtension() {
		return "kmz";
	}

}
