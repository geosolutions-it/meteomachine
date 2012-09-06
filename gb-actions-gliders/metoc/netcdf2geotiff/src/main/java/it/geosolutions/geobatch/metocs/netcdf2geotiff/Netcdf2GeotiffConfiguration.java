package it.geosolutions.geobatch.metocs.netcdf2geotiff;

import it.geosolutions.geobatch.configuration.event.action.ActionConfiguration;
import it.geosolutions.tools.dyntokens.model.DynTokenList;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Netcdf2GeotiffConfiguration extends ActionConfiguration {

    private boolean packComponents; //TODO ??

    private boolean flipY;

    private String crs;

    private String envelope;

    private String storeFilePrefix;

    private String metocDictionaryPath;
    
    private List<String> variables;

    private String metocHarvesterXMLTemplatePath;

    private DynTokenList dynamicTokens;
    
    /**
     * This represents the base directory where to public layers
     */
    private String layerParentDirectory;

    /**
     * The mosaic Dir to be set in the ImageMosaicCommand.
     * (probably this info is too specific if this action is going to put on the 
     * output queue something different from the IMC.
     */
    private Map<String, Object> outputConfiguration;

    /**
     * Default logger
     */
    protected final static Logger LOGGER = LoggerFactory
            .getLogger(Netcdf2GeotiffConfiguration.class);

    protected final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddmm_HHH");

    public static final long startTime;

    static {
        GregorianCalendar calendar = new GregorianCalendar(1980, 00, 01, 00, 00, 00);
        calendar.setTimeZone(TimeZone.getTimeZone("GMT+0"));
        startTime = calendar.getTimeInMillis();
    }

    public Netcdf2GeotiffConfiguration(final String id, final String name,
            final String description) {
        super(id, name, description);

        // //
        // initialize params...
        // //
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+0"));

    }
    
    /**
     * @param queryParams
     * @return
     */
    protected static String getQueryString(Map<String, String> queryParams) {
        StringBuilder queryString = new StringBuilder();

        if (queryParams != null)
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                if (queryString.length() > 0)
                    queryString.append("&");
                queryString.append(entry.getKey()).append("=").append(entry.getValue());
            }

        return queryString.toString();
    }

    /**
     * @return the metocDictionaryPath
     */
    public String getMetocDictionaryPath() {
        return metocDictionaryPath;
    }

    /**
     * @param metocDictionaryPath
     *            the metocDictionaryPath to set
     */
    public void setMetocDictionaryPath(String metocDictionaryPath) {
        this.metocDictionaryPath = metocDictionaryPath;
    }

    /**
     * @return the metocHarvesterXMLTemplatePath
     */
    public String getMetocHarvesterXMLTemplatePath() {
        return metocHarvesterXMLTemplatePath;
    }

    /**
     * @param metocHarvesterXMLTemplatePath
     *            the metocHarvesterXMLTemplatePath to set
     */
    public void setMetocHarvesterXMLTemplatePath(String metocHarvesterXMLTemplatePath) {
        this.metocHarvesterXMLTemplatePath = metocHarvesterXMLTemplatePath;
    }

    public boolean isPackComponents() {
        return packComponents;
    }

    public void setPackComponents(boolean packComponents) {
        this.packComponents = packComponents;
    }

    /**
     * Returns true if the image should be flip by Y
     * 
     * @return boolean
     */
    public boolean isFlipY() {
        return flipY;
    }

    public void setFlipY(boolean f) {
        this.flipY = f;
    }

    public String getCrs() {
        return crs;
    }

    public void setCrs(String crs) {
        this.crs = crs;
    }

    public List<String> getVariables() {
        return variables;
    }

    public void setVariables(List<String> variables) {
        this.variables = variables;
    }

    public String getEnvelope() {
        return envelope;
    }

    public void setEnvelope(String envelope) {
        this.envelope = envelope;
    }

    public String getStoreFilePrefix() {
        return storeFilePrefix;
    }

    public void setStoreFilePrefix(String storeFilePrefix) {
        this.storeFilePrefix = storeFilePrefix;
    }

    @Override
    public Netcdf2GeotiffConfiguration clone() {
        return copy(this);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + "id:" + getId() + " name:" + getName()
                + " srvId:" + getServiceID() + "]";
    }

    /**
     * @return represents the base directory where to public layers
     */
    public String getLayerParentDirectory() {
        return layerParentDirectory;
    }

    /**
     * @param represents
     *            the base directory where to public layers
     */
    public void setLayerParentDirectory(String outputDirectory) {
        this.layerParentDirectory = outputDirectory;
    }

    public Map<String, Object> getOutputConfiguration() {
        return outputConfiguration;
    }

    public void setOutputConfiguration(Map<String, Object> outputConfiguration) {
        this.outputConfiguration = outputConfiguration;
    }

    public DynTokenList getDynamicTokens() {
        return dynamicTokens;
    }

    public void setDynamicTokens(DynTokenList dynamicTokens) {
        this.dynamicTokens = dynamicTokens;
    }
    
    
    /**
     * copy into returned object src
     * 
     * @param src
     * @return
     */
    protected Netcdf2GeotiffConfiguration copy(Netcdf2GeotiffConfiguration src) {

        final Netcdf2GeotiffConfiguration configuration = (Netcdf2GeotiffConfiguration) super.clone();

        return configuration;
    }

}
