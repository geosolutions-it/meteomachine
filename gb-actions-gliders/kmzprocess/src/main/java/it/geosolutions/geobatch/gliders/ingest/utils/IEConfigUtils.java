/**
 *
 */
package it.geosolutions.geobatch.gliders.ingest.utils;

import java.io.FileNotFoundException;

import it.geosolutions.figis.model.Config;
import it.geosolutions.figis.model.ConfigXStreamMapper;
import it.geosolutions.figis.model.Intersection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Alessio
 *
 */
public class IEConfigUtils
{

    private static final Logger LOGGER = LoggerFactory.getLogger(IEConfigUtils.class);

    // ////////////////////////////////////////////////////////////////////////////
    //
    // UTILITY METHODS
    //
    // ////////////////////////////////////////////////////////////////////////////

    /**
     * @throws FileNotFoundException
     *
     */
    public static Config parseXMLConfig(String configPath) throws FileNotFoundException
    {
        Config xmlConfig = null;
        try
        {
            // READ THE XML AND CREATE A CONFIG OBJECT
            xmlConfig = ConfigXStreamMapper.init(configPath);
            LOGGER.info("Managing : " + configPath);
            // READ THE COMING CONFIG (XMLConfig) AND EVENTUALLY UPDATE THE CURRENT STATUS OF BOTH THE CONFIG AND THE INTERSECTIONS
            if (!validateConfig(xmlConfig))
            {
                LOGGER.error("Some errors exist in the config file, please check the log to discover it");
                throw new IllegalStateException(
                    "Some errors exist in the config file, please check the log to discover it");
            }

            return xmlConfig;
        }
        catch (FileNotFoundException e)
        {
            LOGGER.error("Failed to convert the '" + configPath + "' configuration file: " + e.getLocalizedMessage());
            throw e;
        }
    }

    /**
     *
     * @param config
     * @return
     */
    public static boolean validateConfig(Config config)
    {
        if (config == null)
        {
            LOGGER.error("The config object cannot be null");

            return false;
        }
        if (config.getGlobal() == null)
        {
            LOGGER.error("The global configuration cannot be null");

            return false;
        }
        if (config.getGlobal().getGeoserver() == null)
        {
            LOGGER.error("The geoserver configuration cannot be null");

            return false;
        }
        if (config.getGlobal().getGeoserver().getGeoserverUrl() == null)
        {
            LOGGER.error("The geoserver url  cannot be null");

            return false;
        }
        if (config.getGlobal().getGeoserver().getGeoserverUsername() == null)
        {
            LOGGER.error("The geoserver username  cannot be null");

            return false;
        }
        if (config.getGlobal().getDb() == null)
        {
            LOGGER.error("The db configuration  cannot be null");

            return false;
        }
        if (config.getGlobal().getDb().getDatabase() == null)
        {
            LOGGER.error("The db name  cannot be null");

            return false;
        }
        if (config.getGlobal().getDb().getHost() == null)
        {
            LOGGER.error("The db host  cannot be null");

            return false;
        }
        if (config.getGlobal().getDb().getUser() == null)
        {
            LOGGER.error("The db user name  cannot be null");

            return false;
        }
        if (config.getGlobal().getDb().getPort() == null)
        {
            LOGGER.error("The db port  cannot be null");

            return false;
        }
        if (config.intersections == null)
        {
            LOGGER.error("The intersection list cannot be null");

            return false;
        }
        LOGGER.info("The config check is successfull");

        return true;
    }

    /**********
     * this method checks whether two intersections are different considering only CRS, SrcCodeField, TrgCodeField, MaskLayer, PreserveTrgGeom or isMask
     * the srcLayer and the trgLayer are not compared
     * @param srcIntersection
     * @param trgIntersection
     * @return true if one of the parameters is different, false in the other case
     */
    public static boolean areIntersectionParameterDifferent(Intersection srcIntersection, Intersection trgIntersection)
    {
        if (!(srcIntersection.getAreaCRS().equals(trgIntersection.getAreaCRS())))
        {
            return true;
        }
        if (!(srcIntersection.getSrcCodeField().equals(trgIntersection.getSrcCodeField())))
        {
            return true;
        }
        if (!(srcIntersection.getTrgCodeField().equals(trgIntersection.getTrgCodeField())))
        {
            return true;
        }
        if (!(srcIntersection.getMaskLayer().equals(trgIntersection.getMaskLayer())))
        {
            return true;
        }
        if (!(srcIntersection.isMask() == trgIntersection.isMask()))
        {
            return true;
        }
        if (!(srcIntersection.isPreserveTrgGeom() == trgIntersection.isPreserveTrgGeom()))
        {
            return true;
        }

        return false;
    }
}
