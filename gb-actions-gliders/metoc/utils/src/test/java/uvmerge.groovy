/*
 * !!!!! READ ME BEFORE EDITING THIS FILE !!!!!!
 *
 * There are 2 copies of this file:
 *   1) meteomachine/gb-actions-gliders/metoc/utils/src/test/java/uvmerge.groovy
 *   2) meteomachine/GEOBATCH_CONFIG_DIR/roms/uvmerge/uvmerge.groovy
 *
 * Editing should be made in 1) where you can also perform tests on it.
 * Once 1) is ready and working, you should copy it into 2).
 *
 */

import java.io.File;
import java.io.IOException;
import java.io.File;
import java.io.IOException;
import java.util.EventObject;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import it.geosolutions.geobatch.action.scripting.*;
import it.geosolutions.geobatch.imagemosaic.ImageMosaicCommand;

import it.geosolutions.geobatch.flow.event.ProgressListenerForwarder;
import it.geosolutions.geobatch.flow.event.IProgressListener
import it.geosolutions.geobatch.flow.event.ProgressListener
import it.geosolutions.geobatch.flow.event.ProgressListenerForwarder
import it.geosolutions.geobatch.flow.event.action.ActionException;
import it.geosolutions.geobatch.flow.event.action.BaseAction;
import it.geosolutions.filesystemmonitor.monitor.FileSystemEvent;
import it.geosolutions.filesystemmonitor.monitor.FileSystemEventType;

import it.geosolutions.tools.commons.file.Path;
import it.geosolutions.tools.io.file.Collector;
import it.geosolutions.tools.io.file.writer.*;
import it.geosolutions.tools.compress.file.Extract;

import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.FileImageInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import it.geosolutions.geobatch.lamma.misc.*;
import it.geosolutions.geobatch.metocs.utils.*;

/** 
 * Script Main "execute" function
 * @eventFilePath
 **/
public Map execute(Map argsMap) throws Exception {
    // ////////////////////////////////////////////////////////////////////
    // Initializing input variables from Flow configuration
    // ////////////////////////////////////////////////////////////////////
	
	final ScriptingConfiguration configuration=argsMap.get(ScriptingAction.CONFIG_KEY);
	final File tempDir=argsMap.get(ScriptingAction.TEMPDIR_KEY);

	final File configDir=argsMap.get(ScriptingAction.CONFIGDIR_KEY);
	final List events=argsMap.get(ScriptingAction.EVENTS_KEY);
	final ProgressListenerForwarder listenerForwarder=argsMap.get(ScriptingAction.LISTENER_KEY);

	final Logger LOGGER = LoggerFactory.getLogger("it.geosolutions.geobatch.action.scripting.ScriptingAction.class");
		
	LOGGER.info("debug is: "+LOGGER.isDebugEnabled());

    /*
    here we place the output which consist in a folder with the name of the resulting layer
    containing
    1. the collection of merged (mask) images
    2. the relative image mosaic command
    this is obtained using the incoming image mosaic command path doing:
    1. the parent of the parent of the incoming IMC is my parent
    2. my name is composed as:
    PREFIX_ -> loaded using the group 1 of the regex from the parent dir name of the IMC (the dir name)
    NAME    -> loaded from the configuration parameter (props)
    _SUFFIX -> loaded using the group 1 of the regex from the parent dir name of the IMC (the dir name)
     */
    final File outDir=null;
    final Map props=configuration.getProperties();
    final String prefixRegex=props.get("prefixRegex");
    final String suffixRegex=props.get("suffixRegex");
    final String outDirBaseName=props.get("outDirName");
    final String outFileBaseName=props.get("outFileBaseName");

    final String mosaicPath=props.get("mosaicPath");

    // regex to recognize the IMC name to merge
    final String imcRegex=props.get("imcRegex");

    listenerForwarder.started();

    // return
    final Queue<String> ret=new LinkedList<String>();
    // work
    final Queue<String> workList=new LinkedList<String>();

    final Pattern imcP = Pattern.compile(imcRegex);
    for (FileSystemEvent event in events){

		if (LOGGER.isDebugEnabled())
        LOGGER.debug("::UVMerge : event: "+event);
        //System.out.println("--------------> event: "+event);

		final File eventFile=event.getSource();
		final String fileName = eventFile.getName();
		final Matcher imcPM=imcP.matcher(fileName);
		if (imcPM.matches()) {
            //System.out.println("--------------> event matches ");
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("::UVMerge : IMC event matches");
                LOGGER.debug("::UVMerge : working on incoming event: "+fileName);
            }

            //NOT NEEDED: remove the ImageMosaicCommand from the list
            //eventList.remove(event);

            // add it to the working list
            workList.add(eventFile.getAbsolutePath());
            if (outDir==null){

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("::UVMerge : initializing outDir")
                }
                //System.out.println("--------------> initializing outDir");
                /*
                we use the first matching IMC parent dir as model name for the
                new layer to build
                 */
                final File layerModelFile=eventFile.getParentFile();
                final String layerModelFileName=layerModelFile.getName();
                final Pattern imcPrefixP = Pattern.compile(prefixRegex);
                final Matcher imcPrefixM = imcPrefixP.matcher(layerModelFileName);
                if (!imcPrefixM.matches()){
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("::UVMerge : model file name: "+layerModelFileName);
                        LOGGER.debug("::UVMerge : prefixRegex: "+prefixRegex);
                    }
                    //System.out.println("----WIND----------> model file name: "+layerModelFileName);
                    //System.out.println("----WIND----------> prefixRegex: "+prefixRegex);
                    //System.out.println("----WIND----------> ERROR: No match found");
                    final String message="::UVMerge : ERROR: No match found for prefix";
                    if (LOGGER.isErrorEnabled())
                        LOGGER.error(message);
                    final Exception e=new Exception(message);
                    listenerForwarder.failed(e);
                    throw e;
                }

                final Pattern imcSuffixP = Pattern.compile(suffixRegex);
                final Matcher imcSuffixM = imcSuffixP.matcher(layerModelFileName);
                if (!imcSuffixM.matches()){
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("::UVMerge : model file name: "+layerModelFileName);
                        LOGGER.debug("::UVMerge : suffixRegex: "+suffixRegex);
                    }
                    //System.out.println("----WIND----------> model file name: "+layerModelFileName);
                    //System.out.println("--------------> suffixRegex: "+suffixRegex);
                    final String message="::UVMerge : ERROR: No match found for suffix";
                    if (LOGGER.isErrorEnabled())
                        LOGGER.error(message);
                    final Exception e=new Exception(message);
                    listenerForwarder.failed(e);
                    throw e;
                }
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("::UVMerge : model file name: "+layerModelFileName);
                    LOGGER.debug("::UVMerge : prefixRegex: "+prefixRegex);
                    LOGGER.debug("::UVMerge : suffixRegex: "+suffixRegex);
                    LOGGER.debug("::UVMerge : preMatch: "+imcPrefixM.group(1));
                    LOGGER.debug("::UVMerge : sufMatch: "+imcSuffixM.group(1));
                }

                //System.out.println("----WIND----------> model file name: "+layerModelFileName);
                //System.out.println("--------------> prefixRegex: "+prefixRegex);
                //System.out.println("----WIND----------> suffixRegex: "+suffixRegex);
                //System.out.println("--------------> preMatch: "+imcPrefixM.group(1));
                //System.out.println("----WIND----------> sufMatch: "+imcSuffixM.group(1));

                final String outDirComposedName = imcPrefixM.group(1) + outDirBaseName + imcSuffixM.group(1);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("::UVMerge : layerName: "+outDirComposedName);
                }
                //System.out.println("----WIND----------> layerName: "+outDirComposedName);

//                outDir=new File(layerModelFile.getParentFile(),outDirComposedName);
                outDir=new File(tempDir,outDirComposedName);
            }
            //System.out.println("----WIND----------> model file name: "+outDir);
		}
		else {
            ret.add(eventFile.getAbsolutePath());
		}
    }

    if (outDir==null){
		final String message="::UVMerge : ERROR: unable to get the output directory from the incoming IMC list";
		if (LOGGER.isErrorEnabled())
            LOGGER.error(message);
		final Exception e=new Exception(message);
		listenerForwarder.failed(e);
		throw e;
        //System.out.println("ERROR: unable to get the output directory from the incoming IMC list");
    }
    else if (!outDir.mkdirs()){
		final String message="::UVMerge : ERROR: unable to create the output directory: "+outDir.getAbsolutePath();
		if (LOGGER.isErrorEnabled())
            LOGGER.error(message);
		final Exception e=new Exception(message);
		listenerForwarder.failed(e);
		throw e;
        //System.out.println("ERROR: unable to create the output directory: "+outDir.getAbsolutePath());
    }

    final int workListSize=workList.size();
    if (LOGGER.isDebugEnabled()){
		LOGGER.error("::UVMerge : Working on a list with size: "+workListSize);
    }
    //System.out.println("SIZE:"+workList.size());
    //System.out.println("PATH:"+workList.get(0).getSource().getAbsolutePath());

    final List<List<File>> imagesToMerge=new ArrayList<List<File>>(workListSize);
    int imagesListSize=-1;

    for (String fileName in workList){
        final File event=new File(fileName);
        final ImageMosaicCommand cmd=ImageMosaicCommand.deserialize(event);
        if (cmd==null){
            // ERROR
            if (LOGGER.isErrorEnabled())
			LOGGER.error("::UVMerge : SKIPPING: NULL ImageMosaicCommand");
            continue;
        }
        final List<File> list=cmd.getAddFiles();
        // check on the image list size
        if (imagesListSize<0){
            imagesListSize=list.size();
        }
        else if (imagesListSize!=list.size()){
			// here we suppose that each image mosaic command 
			// has the same number of file!!!
			final String message="::UVMerge : ERROR: file list size in image mosaic commands do not match ("+imagesListSize+"!="+list.size()+")";
			if (LOGGER.isErrorEnabled())
                LOGGER.error(message);
			final Exception e=new Exception(message);
			listenerForwarder.failed(e);
			throw e;
        }
        // add the list of images to the merge structure
        imagesToMerge.add(list);
    }
	    
    /*
     * for each t-ple of files build the args
     * and call the merge function.
     * Append the resulting image to the return queue
     */
    if(mosaicPath == null) {
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("::UVMerge : mosaicPath not set, will be set to " + outDir);
        }
        mosaicPath = outDir.absolutePath;
    }

    ImageMosaicCommand newCommand=new ImageMosaicCommand((String)mosaicPath,new ArrayList<String>(),null);
    newCommand.setDefaultStyle(props.get("defaultStyle"));
    List addFilesList=newCommand.getAddFiles();
    int i=0;
    while (i<imagesListSize){
        // list of image to merge
        final List<File> args=new ArrayList<File>(workListSize);
		int j=0;
        while (j<workListSize){
		    final List<File> imagesList=imagesToMerge.get(j);
		    if (imagesList==null){
			    // ERROR
                final String message="::UVMerge : ERROR: image list is null";
                if (LOGGER.isErrorEnabled())
                    LOGGER.error(message);
                final Exception e=new Exception(message);
                listenerForwarder.failed(e);
                throw e;
		    }
		    final File image=imagesList.get(i);
		    if (image==null){
			    // ERROR
                final String message="::UVMerge : ERROR: selected image is null";
                if (LOGGER.isErrorEnabled())
                    LOGGER.error(message);
                final Exception e=new Exception(message);
                listenerForwarder.failed(e);
                throw e;
		    }
		    // build the args
		    args.add(image);
		    j++;
        }
	    	
        final MergeImageUtils util=new MergeImageUtils(props.get("imageNameRegex"));
		
		File newCommandFile;
        try {
            final File mergedImage=util.mergeWindImage(outDir,args.get(0),args.get(1),outFileBaseName);
			addFilesList.add(mergedImage);
        } catch (Throwable t){
			final String message="::UVMerge : ERROR:"+t.getLocalizedMessage();
			if (LOGGER.isErrorEnabled())
                LOGGER.error(message);
			final Exception e=new Exception(message);
			listenerForwarder.failed(e);
			throw e;
            //System.out.println("ERROR:"+t.getLocalizedMessage());
            //t.printStackTrace();
        }
	    i++;
    }
	    
    // add the new Image Mosaic Command to the return list
    if (newCommand==null){
        //System.out.println("ERROR: unable to build the IMC...");
		final String message="::UVMerge : ERROR: unable to build the IMC...";
		if (LOGGER.isErrorEnabled())
            LOGGER.error(message);
		final Exception e=new Exception(message);
		listenerForwarder.failed(e);
		throw e;
    }
    final File newCommandFile=newCommand.serialize(newCommand, outDir.getAbsolutePath()+"/watvel_IMC.xml");
    ret.add(newCommandFile.getAbsolutePath());
    // REMOVE FILES:
/*    for (String fileName in workList){
        final File file=new File(fileName).getParentFile();
		if (LOGGER.isDebugEnabled())
            LOGGER.debug("::UVMerge : going to remove: "+file.getAbsolutePath());
        //System.out.println("--------------> going to remove: "+file.getAbsolutePath());
        // REMOVE FILES
		FileUtils.deleteQuietly(file);
    }
*/
    Map retMap=new HashMap();
    retMap.put(ScriptingAction.RETURN_KEY, ret);
    return retMap;
}
