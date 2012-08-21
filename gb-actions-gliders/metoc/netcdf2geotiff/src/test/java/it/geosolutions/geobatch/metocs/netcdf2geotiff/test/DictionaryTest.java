package it.geosolutions.geobatch.metocs.netcdf2geotiff.test;


import it.geosolutions.geobatch.metocs.netcdf2geotiff.checker.MetocsBaseDictionary;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.thoughtworks.xstream.XStream;

public class DictionaryTest {
    private static XStream xstream;
    @Before
    public void setUp() throws Exception {
        
        xstream =new XStream();
    }
    
    @Test
    public void test() throws FileNotFoundException{
        Map<String, Map<String,String>> map=new HashMap<String, Map<String, String>>();
        Map<String,String> var1Map=new HashMap<String,String>(); 
        map.put("VAR_1", var1Map);
        var1Map.put(MetocsBaseDictionary.LATITUDE_KEY, "lat");
        var1Map.put(MetocsBaseDictionary.LONGITUDE_KEY, "lon");
        var1Map.put(MetocsBaseDictionary.TIME_KEY, "Time");
        var1Map.put(MetocsBaseDictionary.Z_KEY, "Elevation");
        var1Map.put(MetocsBaseDictionary.UOM_KEY, "uom");
        MetocsBaseDictionary md=new MetocsBaseDictionary(map);
        
        xstream.toXML(md, new FileOutputStream(new File("src/test/resources/dictionary_out.xml")));
        
    }
    
    @Test
    public void deserializeTest() throws FileNotFoundException{
        Map<String, Map<String,String>> map=new HashMap<String, Map<String, String>>();
        Map<String,String> var1Map=new HashMap<String,String>(); 
        map.put("VAR_1", var1Map);
        var1Map.put(MetocsBaseDictionary.LATITUDE_KEY, "lat");
        var1Map.put(MetocsBaseDictionary.LONGITUDE_KEY, "lon");
        var1Map.put(MetocsBaseDictionary.TIME_KEY, "Time");
        var1Map.put(MetocsBaseDictionary.Z_KEY, "Elevation");
        var1Map.put(MetocsBaseDictionary.UOM_KEY, "uom");
        MetocsBaseDictionary md = new MetocsBaseDictionary(
                (Map<String, Map<String, String>>) xstream.fromXML(new FileInputStream(new File(
                        "src/test/resources/dictionary.xml"))));
        
        Assert.assertNotNull(md);
        Assert.assertNotNull(md.getVal("VAR_1"));
        
    }

    @After
    public void tearDown() throws Exception {
    }

}
