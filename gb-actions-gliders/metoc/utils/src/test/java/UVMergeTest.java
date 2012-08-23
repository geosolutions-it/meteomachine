/*
 *  Copyright (C) 2007 - 2011 GeoSolutions S.A.S.
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

import com.thoughtworks.xstream.XStream;
import it.geosolutions.filesystemmonitor.monitor.FileSystemEvent;
import it.geosolutions.filesystemmonitor.monitor.FileSystemEventType;
import it.geosolutions.geobatch.action.scripting.ScriptingAction;
import it.geosolutions.geobatch.action.scripting.ScriptingConfiguration;
import it.geosolutions.geobatch.flow.event.action.ActionException;
import it.geosolutions.geobatch.imagemosaic.ImageMosaicCommand;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.junit.Test;
import static org.junit.Assert.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ETj (etj at geo-solutions.it)
 */
public class UVMergeTest extends BaseTest {

    private final static Logger LOGGER = LoggerFactory.getLogger(UVMergeTest.class);

    @Test
    public void testMerge() throws IOException, ActionException {

        File uvmerge = loadFile("uvmerge");
        assertNotNull("Dir uvmerge not found", uvmerge);
        LOGGER.info("UVmerge file dir: " + uvmerge);

        File dirU = new File(uvmerge, "watvel-u_20100801T000000000Z");
        File imcFileU = refreshImc(dirU);

        File dirV = new File(uvmerge, "watvel-v_20100801T000000000Z");
        File imcFileV = refreshImc(dirV);

        ScriptingConfiguration cfg = new ScriptingConfiguration("uvmerge_test", "name", "desc");
        cfg.setLanguage("groovy");

        File scriptFile = new File(uvmerge.getParentFile().getParentFile().getParentFile(), "/src/test/java/uvmerge.groovy"); // terrible hack
//        File scriptFile = new File("/home/geosol/prj/nurc/meteomachine/gb-actions-gliders/metoc/utils/src/main/java/uvmerge.groovy");
        assertNotNull(scriptFile);
        cfg.setScriptFile(scriptFile.getAbsolutePath());
        cfg.setProperties(new HashMap<String, Object>());
        cfg.getProperties().put("imcRegex", "watvel.*_imc.xml");
        cfg.getProperties().put("imageNameRegex", "watvel.._(.*).tiff");
        cfg.getProperties().put("outDirName", "_");
        cfg.getProperties().put("outFileBaseName", "watvel");
        cfg.getProperties().put("prefixRegex", "(watvel).*");
        cfg.getProperties().put("suffixRegex", ".*_([0-9]+T[0-9]+Z)");
        cfg.getProperties().put("defaultStyle", "garrows");
        cfg.getProperties().put("mosaicPath", "/tmp/mosaic");

        XStream xstream = new XStream();
        xstream.toXML(cfg, System.out);

        ScriptingAction action = new ScriptingAction(cfg);
        action.setTempDir(getTempDir());
        action.setConfigDir(getTempDir());

        Queue<FileSystemEvent> input= new LinkedList<FileSystemEvent>();
        input.add(new FileSystemEvent(imcFileU, FileSystemEventType.FILE_ADDED));
        input.add(new FileSystemEvent(imcFileV, FileSystemEventType.FILE_ADDED));

        LOGGER.info(" ==========");
        LOGGER.info(" ========== RUNNING ACTION");
        LOGGER.info(" ==========");
        action.execute(input);

    }

    protected File refreshImc(File dir) throws IOException {
        File[] listFiles = dir.listFiles((FilenameFilter)new SuffixFileFilter(".xml"));
        File imcFileU = listFiles[0];
        LOGGER.info("IMC " + imcFileU);
        ImageMosaicCommand imcu = ImageMosaicCommand.deserialize(imcFileU);
        imcu.setBaseDir(dir);

        // renew file list
        imcu.setAddFiles(new ArrayList<File>());
        for(File tiff : dir.listFiles((FilenameFilter)new SuffixFileFilter(".tiff"))) {
            LOGGER.info("Adding tiff " + tiff);
            imcu.getAddFiles().add(tiff);
        }

        ImageMosaicCommand.serialize(imcu, imcFileU.getAbsolutePath());
        return imcFileU;
    }

}
