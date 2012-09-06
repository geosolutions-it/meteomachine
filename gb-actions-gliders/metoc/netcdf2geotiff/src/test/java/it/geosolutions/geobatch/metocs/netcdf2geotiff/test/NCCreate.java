/*
 *  Copyright (C) 2007 - 2012 GeoSolutions S.A.S.
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
package it.geosolutions.geobatch.metocs.netcdf2geotiff.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;

/**
 *
 * Create sample netcdf files.
 *
 * @see http://www.unidata.ucar.edu/software/netcdf-java/tutorial/NetcdfWriteable.html
 * 
 * @author ETj (etj at geo-solutions.it)
 */
public class NCCreate {

    static String fileName = "example.nc"; // default name of file created

    public static void main(String [] argv) throws IOException {

        String filename = "/tmp/test.nc";
        create(filename);
    }
    

    public static void create(String filename) throws IOException {

        NetcdfFileWriteable ncfile = NetcdfFileWriteable.createNew(filename, false);
        // add dimensions
        Dimension timDim = ncfile.addDimension("time", 4);
        Dimension depDim = ncfile.addDimension("depth", 3);
        Dimension latDim = ncfile.addDimension("lat", 2);
        Dimension lonDim = ncfile.addDimension("lon", 2);

        ncfile.addVariable("time", DataType.DOUBLE, new Dimension[]{timDim});
        ncfile.addVariableAttribute("time", "long_name", "time");
        ncfile.addVariableAttribute("time", "units", "seconds since 1980-1-1 0:0:0");
        ncfile.addVariableAttribute("time", "time_origin", 965088000 );
//        Array data = Array.factory(int.class, new int[]{3}, new int[]{1, 2, 3});
//        ncfile.addVariableAttribute("time", "scale", data);


        ncfile.addVariable("depth", DataType.FLOAT, new Dimension[]{depDim});
        ncfile.addVariableAttribute("depth", "long_name", "depth");
        ncfile.addVariableAttribute("depth", "units", "m");
        ncfile.addVariableAttribute("depth", "positive", "down");

        ncfile.addVariable("lat", DataType.DOUBLE, new Dimension[]{latDim});
        ncfile.addVariableAttribute("lat", "long_name", "Latitude");
        ncfile.addVariableAttribute("lat", "units", "degrees_north");

        ncfile.addVariable("lon", DataType.DOUBLE, new Dimension[]{lonDim});
        ncfile.addVariableAttribute("lon", "long_name", "Longitude");
        ncfile.addVariableAttribute("lon", "units", "degrees_east");

        
        ncfile.addVariable("watvel-u", DataType.FLOAT, new Dimension[]{timDim, depDim, latDim, lonDim});
        ncfile.addVariableAttribute("watvel-u", "long_name", "water velocity u comp");
        ncfile.addVariableAttribute("watvel-u", "units", "m/s");
        ncfile.addVariableAttribute("watvel-u", "missing_value", 1e+37);
        ncfile.addVariableAttribute("watvel-u", "_FillValue", 1e+37);

        ncfile.addVariable("watvel-v", DataType.FLOAT, new Dimension[]{timDim, depDim, latDim, lonDim});
        ncfile.addVariableAttribute("watvel-v", "long_name", "water velocity v comp");
        ncfile.addVariableAttribute("watvel-v", "units", "m/s");
        ncfile.addVariableAttribute("watvel-v", "missing_value", 1e+37);
        ncfile.addVariableAttribute("watvel-v", "_FillValue", 1e+37);

        ncfile.addVariable("wattemp", DataType.FLOAT, new Dimension[]{timDim, depDim, latDim, lonDim});
        ncfile.addVariableAttribute("wattemp", "long_name", "water temperature");
        ncfile.addVariableAttribute("wattemp", "units", "m/s");
        ncfile.addVariableAttribute("wattemp", "missing_value", 1e+37);
        ncfile.addVariableAttribute("wattemp", "_FillValue", 1e+37);



//        ncfile.addVariable("temperature", DataType.FLOAT, new Dimension[]{latDim, lonDim, timDim, depDim});
//        ncfile.addVariableAttribute("temperature", "units", "K");
        // add a 1D attribute of length 3
//        Array data2 = Array.factory(int.class, new int[]{3}, new int[]{1, 2, 3});
//        ncfile.addVariableAttribute("temperature", "scale", data2);
        // add a string-valued variable: char svar(80)
//        Dimension svar_len = ncfile.addDimension("svar_len", 80);


//        dims = new ArrayList();
//        dims.add(svar_len);
//        ncfile.addVariable("svar", DataType.CHAR, dims);
        // string array: char names(3, 80)
//        Dimension names = ncfile.addDimension("names", 3);
//        ArrayList dima = new ArrayList();
//        dima.add(names);
//        dima.add(svar_len);
//        ncfile.addVariable("names", DataType.CHAR, dima);
//        // how about a scalar variable?
//        ncfile.addVariable("scalar", DataType.DOUBLE, new ArrayList());
        // add global attributes
        ncfile.addGlobalAttribute("_FillValue", new Float(1.e+37));
        ncfile.addGlobalAttribute("nodata",     new Float(1.e+37));

        ncfile.addGlobalAttribute("base_time", "20100801T000000000Z");
        ncfile.addGlobalAttribute("type", "ROMS");
        ncfile.addGlobalAttribute("title", "ROMS");
        ncfile.addGlobalAttribute("author", "GeoSolutions, info@geo-solutions.it");
        ncfile.addGlobalAttribute("date", "03-Sep-2012 13:29:21");
        ncfile.addGlobalAttribute("tau", new Integer(3));

        // create the file
        try {
            ncfile.create();

            ncfile.write("time", Array.factory(new double[] {965088000, 965098800, 965109600, 965120400}));
            ncfile.write("depth", Array.factory(new float[] {10, 100, 1000}));
            ncfile.write("lat", Array.factory(new float[] {40.0f, 41.0f}));
            ncfile.write("lon", Array.factory(new float[] {-109.0f, -107.0f}));


            ArrayFloat wvu = new ArrayFloat.D4(timDim.getLength(), depDim.getLength(), latDim.getLength(), lonDim.getLength());

               Index ima = wvu.getIndex();
               for (int i=0; i<timDim.getLength(); i++) {
                 for (int j=0; j<depDim.getLength(); j++) {
                     for (int h = 0; h < latDim.getLength(); h++) {
                         for (int k = 0; k < lonDim.getLength(); k++) {
                            wvu.setFloat(ima.set(i,j,h,k), (float) ((i+1)*100f+(j+1)*10f+(h+1)+k/10f));
                         }
                     }
                 }
               }

               int[] origin = new int[4];
               ncfile.write("watvel-u", origin, wvu);

            ncfile.flush();
            ncfile.close();

            ncfile.writeCDL(System.out, true);
//            ncfile.writeNcML(System.out, new File("/tmp/test.ncml").toURI().toURL().toExternalForm());
        } catch (InvalidRangeException ex) {
            Logger.getLogger(NCCreate.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException e) {
            System.err.println("ERROR creating file " + ncfile.getLocation() + "\n" + e);
        }
    }

}
