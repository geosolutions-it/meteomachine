/*
 *  GeoBatch - Open Source geospatial batch processing system
 *  http://code.google.com/p/geobatch/
 *  Copyright (C) 2007-2011 GeoSolutions S.A.S.
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
package it.geosolutions.geobatch.metocs.utils;

import it.geosolutions.geobatch.metocs.utils.io.Utilities;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReaderSpi;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.TiledImage;

import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.factory.Hints;
import org.geotools.gce.geotiff.GeoTiffFormatFactorySpi;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.GeneralEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 */
public class MergeImageUtils {
	protected final static Logger LOGGER = LoggerFactory
			.getLogger(MergeImageUtils.class);

	private final GeoTiffFormatFactorySpi GEOTIFF_FORMAT_FACTORY_SPI = new GeoTiffFormatFactorySpi();
	// ImageIO
	private final TIFFImageReaderSpi TIFF_READER_SPI = new TIFFImageReaderSpi();

	private final Pattern pattern;
	private final String compressionType = "LZW";
	private final float compressionRatio = 0.7f;
	private final int tileSize = 512;

	// from the model
	private boolean inittedModel;
	private GeneralEnvelope envelope;
	private Double nodata; // TODO short uint ???
	private int iHeight;
	private int iWidth;

	public MergeImageUtils(final String regex) {
		pattern = Pattern.compile(regex);
	}

	public File mergeMaskImage(final File outDir, final List<File> files,
			final String outVarName) throws Throwable {

		// the first file is used as model for others in the list
		final File model = files.get(0);

		if (!inittedModel)
			initModel(model);

		// build an in memory image using the files list
		final RenderedImage tiledImage = mergeImage(files);

		// exclude first part of the variable (name) substituting varName
		final String name = buildName(outVarName, model.getName());

		return writeGeotiff(outDir, name, tiledImage, envelope);
	}

	private String buildName(final String nameVar, final String name) {
		final Matcher matcher = pattern.matcher(name);
		if (matcher != null && matcher.matches()) {
			return nameVar + "_" + matcher.group(1);
		} else {
            return nameVar + "_" + name;
        }
	}

	/**
	 * Merge all the passed images into a
	 * 
	 * @param files
	 * @return
	 * @throws IllegalArgumentException
	 */
	public final RenderedImage mergeImage(final List<File> files)
			throws IllegalArgumentException {
		final int listSize = files.size();
		final ImageCursor[] cursors = new ImageCursor[listSize];

		// Create a DataBuffer from the values on the image array.
		// define the type of the image DataModel
		SampleModel sampleModel = null;
		if (listSize < 8 && listSize >= 0) {
			// Create a float data sample model.
			sampleModel = RasterFactory.createBandedSampleModel(
					DataBuffer.TYPE_BYTE, 256, 256, 1);
		} else if (listSize >= 8 && listSize < 16) {
			// Create a float data sample model.
			sampleModel = RasterFactory.createBandedSampleModel(
					DataBuffer.TYPE_USHORT, 256, 256, 1);
		} else {
			// unsupported
			if (LOGGER.isErrorEnabled())
				LOGGER.error("MergeImageUtils: ERROR unsupported data type");
			return null;
		}

		// Create a compatible ColorModel.
		ColorModel colorModel = PlanarImage.createColorModel(sampleModel);

		// Create a TiledImage using the float SampleModel.
		TiledImage tiledImage = new TiledImage(0, 0, iWidth, iHeight, 0, 0,
				sampleModel, colorModel);
		try {
			// fill in the array
			for (int h = 0; h < iHeight; h++) {
				for (int w = 0; w < iWidth; w++) {
					int setValue = -1; // TODO SET TO NAN???
					for (int i = 0; i < listSize; i++) {
						if (cursors[i] == null) {
							cursors[i] = new ImageCursor();
							if (!cursors[i].isInitted()) {
								if (!cursors[i]
										.init(files.get(i), Double.class)) {
									// ERROR!!!!
									if (LOGGER.isErrorEnabled())
										LOGGER.error("ERROR for file n " + i
												+ " Named: " + files.get(i));
									return null;
								}
							}
						}

						final Number value = cursors[i].getNext();
						if (value.equals(nodata)) {
							// System.out.println("NOT NaN for file n "+i+" value is: \'"+value+"\'");
							setValue = nodata.intValue();
						} else if (value.equals(0)) {
							// System.out.println("VALUE: \'"+value+"\' for file n."+i+" Named: "+files.get(i));
							setValue = 0;
						} else {
							setValue = i;
						}
					}
					tiledImage.setSample(w, h, 0, setValue);
				}
			}
		} catch (Throwable t) {
			if (LOGGER.isErrorEnabled())
				LOGGER.error(
						"MergeImageUtils: ERROR: \'" + t.getLocalizedMessage()
								+ "\'", t);
		} finally {
			// close streams
			if (cursors != null) {
				for (int i = 0; i < listSize; i++) {
					if (cursors[i] != null && !cursors[i].isInitted()) {
						cursors[i].dispose();
						cursors[i] = null;
					}
				}
			}
		}

		// //////////////////////////////////////////////
		// TODO
		return tiledImage;
	}

	/**
	 * Read the model image file initializing all the data and metadata members
	 * of this class
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 *             , IllegalArgumentException
	 */
	private final void initModel(final File model) throws Throwable,
			IllegalArgumentException {
		inittedModel = false;
		// reading metadata from the model image

		// using geotools
		GeoTiffReader reader = null;
		try {
			final AbstractGridFormat format = GEOTIFF_FORMAT_FACTORY_SPI
					.createFormat();
			if (format.accepts(model)) {
				/*
				 * the reader can decode input let's take a reader on it
				 */
				reader = (GeoTiffReader) format.getReader(model);
				if (reader == null) {
					final IOException ioe = new IOException(
							"Unable to find a reader for the provided file: "
									+ model.getAbsolutePath());
					throw ioe;
				}
			} else {
				final IllegalArgumentException iae = new IllegalArgumentException(
						"Unable to get a reader for the image file: "
								+ model.getAbsolutePath());
				if (LOGGER.isErrorEnabled())
					LOGGER.error(
							"MergeImageUtils: ERROR: \'"
									+ iae.getLocalizedMessage() + "\'", iae);
				throw iae;
			}
			// final CoordinateReferenceSystem crs=reader.getCrs();
			envelope = reader.getOriginalEnvelope();
			nodata = reader.getMetadata().getNoData();
		} catch (Throwable t) {
			if (LOGGER.isErrorEnabled())
				LOGGER.error(
						"MergeImageUtils: ERROR: \'" + t.getLocalizedMessage()
								+ "\'", t);
			throw t;
		} finally {
			try {
				if (reader != null)
					reader.dispose();
			} catch (Throwable t) {
			}
		}

		// using ImageIO
		FileImageInputStream stream = null;
		try {
			stream = new FileImageInputStream(model);
			ImageReader reader2 = null;
			try {

				if (TIFF_READER_SPI.canDecodeInput(stream)) {
					/*
					 * the reader can decode input let's take a reader on it
					 */
					reader2 = TIFF_READER_SPI.createReaderInstance();
					reader2.setInput(stream);
					// reader = (ImageReader) new TIFFImageReader(new
					// TIFFImageReaderSpi());
				} else
					throw new IllegalArgumentException(
							"Unable to get a reader for the image file: "
									+ model.getAbsolutePath()); // TODO log

				final int index = reader2.getMinIndex();
				// final Iterator<ImageTypeSpecifier> it = reader2
				// .getImageTypes(index);
				//
				// if (it.hasNext()){
				// final ImageTypeSpecifier imgTypeSpec= it.next();
				// imgTypeSpec.getColorModel().getAlpha(pixel);
				// }

				iHeight = reader2.getHeight(index);
				iWidth = reader2.getWidth(index);

			} catch (IOException e) {
				throw e; // TODO log
			} finally {
				try {
					if (reader2 != null)
						reader2.dispose();
				} catch (Throwable t) {
				}
			}
		} catch (IOException e) {
			throw e; // TODO log
		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (IOException ioe) {
			}
		}
		inittedModel = true;
	}

	public final File writeGeotiff(final File outDir, final String name,
			final RenderedImage image, final GeneralEnvelope envelope)
			throws IllegalArgumentException, IOException {
		final Hints hints = new Hints(Hints.TILE_ENCODING, "raw");
		final GridCoverageFactory factory = CoverageFactoryFinder
				.getGridCoverageFactory(hints);

		final GridCoverage2D coverage = factory.create(name, image, envelope);

		return Utilities.storeCoverageAsGeoTIFF(outDir, name, coverage,
				compressionType, compressionRatio, tileSize);

	}

	// /////////////////////////////////////////////
	// WIND SPECIFIC
	// /////////////////////////////////////////////

	public File mergeWindImage(final File outDir, final File fileU,
			final File fileV, final String outVarName) throws Throwable {

		// the first file is used as model for others in the list
		final File model = fileU;

		if (!inittedModel)
			initModel(model);

		// build an in memory image using the files list
		final RenderedImage tiledImage = mergeWindComponents(fileU, fileV);

		// exclude first part of the variable (name) substituting varName
		final String name = buildName(outVarName, model.getName());

		return writeGeotiff(outDir, name, tiledImage, envelope);
	}

	/**
	 * Merge all the passed images into a
	 * 
	 * @param files
	 * @return
	 * @throws IllegalArgumentException
	 */
	public final RenderedImage mergeWindComponents(final File fileU,
			final File fileV) throws IllegalArgumentException {

		final ImageCursor cursorU = new ImageCursor();
		final ImageCursor cursorV = new ImageCursor();

		// Create a DataBuffer from the values on the image array.
		// Create a double sample model on 2 bands
		final SampleModel sampleModel = RasterFactory.createBandedSampleModel(
				DataBuffer.TYPE_DOUBLE, 256, 256, 2);

		// Create a compatible ColorModel.
		final ColorModel colorModel = PlanarImage.createColorModel(sampleModel);

		// Create a TiledImage using the float SampleModel.
		TiledImage tiledImage = new TiledImage(0, 0, iWidth, iHeight, 0, 0,
				sampleModel, colorModel);
		
		try {
			if (!cursorU.isInitted()) {
				if (!cursorU.init(fileU, Double.class)) {
					// ERROR!!!!
					if (LOGGER.isErrorEnabled())
						LOGGER.error("ERROR for file Named: "
								+ fileU.getAbsolutePath());
					return null;
				}
			}
			if (!cursorV.isInitted()) {
				if (!cursorV.init(fileV, Double.class)) {
					// ERROR!!!!
					if (LOGGER.isErrorEnabled())
						LOGGER.error("ERROR for file Named: "
								+ fileV.getAbsolutePath());
					return null;
				}
			}

			// fill in the array
			for (int h = 0; h < iHeight; h++) {
				for (int w = 0; w < iWidth; w++) {
					// band 1
					final double setModule;
					// band 2
					final double setDirection;

					final Number valueU = cursorU.getNext();
					final Number valueV = cursorV.getNext();
					if (valueU.equals(nodata) || valueV.equals(nodata)) {
						// System.out.println("NOT NaN for file n "+i+" value is: \'"+value+"\'");
						// unable to calculate Wind direction and speed
						setModule = Double.NaN;
						setDirection = Double.NaN;
					} else {
						// System.out.println("VALUE: \'"+value+"\' for file n."+i+" Named: "+files.get(i));
						double u=valueU.doubleValue();
						double v=valueV.doubleValue();
						setModule = getModule(u,v);
						setDirection = getDirection(u,v);
					}
					tiledImage.setSample(w, h, 0, setModule);
					tiledImage.setSample(w, h, 1, setDirection);
//ImageFrame frame = new ImageFrame(tiledImage,"IMAGE");
					 

				}
			}
		} catch (Throwable t) {
			if (LOGGER.isErrorEnabled())
				LOGGER.error(
						"MergeImageUtils: ERROR: \'" + t.getLocalizedMessage()
								+ "\'", t);
		} finally {
			// try {
			// if (tiledImage != null)
			// tiledImage.dispose();
			// } catch (Exception e) {
			// }
			try {
				// close streams
				if (cursorU != null) {
					cursorU.dispose();
				}
			} catch (Exception e) {
			}
			try {
				if (cursorV != null) {
					cursorV.dispose();
				}
			} catch (Exception e) {
			}
		}

		// //////////////////////////////////////////////
		// TODO
		return tiledImage;
	}

	public double getModule(double valueU, double valueV) {
		return Math.sqrt(Math.pow(valueU, 2) + Math.pow(valueV, 2));
	}

	/**
	 * from: http://mst.nerc.ac.uk/wind_vect_convs.html
	 * tool to check: http://cactus2000.net/uk/unit/masswin.shtml
	 * 
	 * @param valueU
	 * @param valueV
	 * @return
	 */
	public double getDirection(double valueU, double valueV) {
		final double direction=(180 / Math.PI) * Math.atan2(-valueU,-valueV);
		return (direction<0)?(360+direction):direction;
	}

	public static void main(String[] args) throws IllegalArgumentException, Throwable {
		// Store the image using the PNG format.
		File imageFile=new File("src/main/resources/glpattern.tiff");
		testWriteImage(imageFile);
		File imageFile2=new File("src/main/resources/glpattern2.tiff");
		testWriteImage(imageFile2);
		
		MergeImageUtils util=new MergeImageUtils(".*");
//		util.initModel(imageFile);
		util.iHeight=10;
		util.iWidth=25;
		util.nodata=Double.NaN;
		RenderedImage image=util.mergeWindComponents(imageFile, imageFile2);
		
		ImageIO.write(image, "TIFF", imageFile);
		
		ImageCursor ic=new ImageCursor();
		ic.init(imageFile, Integer.class);
		int val=0;
		while (ic.hasNext()){
			val=(Integer)ic.getNext();
			System.out.println(val);
		}
		
	}
	
	
	public static void testWriteImage(File imageFile) throws IOException {
		int width = 25; // Dimensions of the image
		int height = 10;
		// Let's create a BufferedImage for a gray level image.
		BufferedImage im = new BufferedImage(width, height,
				BufferedImage.TYPE_BYTE_GRAY);
		// We need its raster to set the pixels' values.
		WritableRaster raster = im.getRaster();
		long val = 0;
		// Put the pixels on the raster, using values between 0 and 255.
		for (int h = 0; h < height; h++) {
			for (int w = 0; w < width; w++) {
				System.out.println(val);
				raster.setSample(w, h, 0, val++);
			}
		}
		
		// Store the image using the TIFF format.
		ImageIO.write(im, "TIFF", imageFile);
		
	}
}
