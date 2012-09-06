package it.geosolutions.geobatch.metocs.netcdf2geotiff.test;


import java.io.FileNotFoundException;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import com.thoughtworks.xstream.XStream;
import it.geosolutions.filesystemmonitor.monitor.FileSystemEvent;
import it.geosolutions.filesystemmonitor.monitor.FileSystemEventType;
import it.geosolutions.geobatch.metocs.netcdf2geotiff.Netcdf2GeotiffAction;
import it.geosolutions.geobatch.metocs.netcdf2geotiff.Netcdf2GeotiffConfiguration;
import it.geosolutions.geobatch.metocs.netcdf2geotiff.spi.NetcdfSPILoader;
import it.geosolutions.geobatch.metocs.netcdf2geotiff.impl.im.NetcdfImageMosaicSPI;
import java.io.File;
import java.util.Arrays;
import java.util.EventObject;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class NCTest extends BaseTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(NCTest.class);

    private static XStream xstream;

    @Before
    public void mySetUp() throws Exception {
        
        xstream =new XStream();

        String[] paths = {"classpath:applicationContext_netcdf_spi.xml"};
        ClassPathXmlApplicationContext  ctx = new ClassPathXmlApplicationContext(paths);
        // no need to recall any beans here: we only need the static spring initialization for the SPI
    }

    /**
     * Standard case:
     * create name using netcdf internal data
     */
    @Test
    public void testStandard()  {

        try{
            NetcdfImageMosaicSPI spiClass = NetcdfSPILoader.getSPI(NetcdfImageMosaicSPI.class);
            spiClass.setPriority(1000);

            Netcdf2GeotiffConfiguration cfg = new Netcdf2GeotiffConfiguration("testid", "testname", "testdescr");
            cfg.setCrs("EPSG:4326");
            cfg.setEnvelope(null);
            cfg.setMetocDictionaryPath(loadFile("dict.xml").getAbsolutePath());
            cfg.setFlipY(true);
            cfg.setVariables(Arrays.asList("watvel-u", "watvel-v"));

            Map<String,Object> outputCfg = new HashMap<String, Object>();
            outputCfg.put("mosaicPath", getTempDir().getAbsolutePath());
            cfg.setOutputConfiguration(outputCfg);

            Netcdf2GeotiffAction action = new Netcdf2GeotiffAction(cfg);
            action.setTempDir(getTempDir());
            action.setConfigDir(getTempDir());

            File ncFile = new File(getTempDir(), "test.nc");
            LOGGER.info("Creating temp nc file " + ncFile);
            NCCreate.create(ncFile.getAbsolutePath());

            FileSystemEvent e = new FileSystemEvent(ncFile, FileSystemEventType.FILE_ADDED);
            Queue<EventObject> qin = new LinkedList<EventObject>();
            qin.add(e);
            Queue<EventObject> qout = action.execute(qin);

            LOGGER.info("" + qout);
        } catch (Exception e) {
            LOGGER.error("Exception in test: " + e.getMessage(), e);
            fail(e.getMessage());
        }

    }
    
    /**
     * Runtime is missing from internal vars:
     * create name using input filename
     */
    @Ignore
    public void testINGV() throws FileNotFoundException{
    }

    /**
     * Output file name is partly composed using the input file name.
     */
    @Ignore
    public void test2() throws FileNotFoundException{
    }


//    @After
//    public void tearDown() throws Exception {
//    }

}
