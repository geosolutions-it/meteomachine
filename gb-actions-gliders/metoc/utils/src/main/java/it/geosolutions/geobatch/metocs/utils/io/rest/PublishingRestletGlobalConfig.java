/*
 * $Header: it.geosolutions.geobatch.nurc.tda.jgsflodess.config.global.JGSFLoDeSSGlobalConfig,v. 0.1 04/dic/2009 17:50:01 created by Fabiani $
 * $Revision: 0.1 $
 * $Date: 04/dic/2009 17:50:01 $
 *
 * ====================================================================
 *
 * Copyright (C) 2007-2008 GeoSolutions S.A.S.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. 
 *
 * ====================================================================
 *
 * This software consists of voluntary contributions made by developers
 * of GeoSolutions.  For more information on GeoSolutions, please see
 * <http://www.geo-solutions.it/>.
 *
 */
package it.geosolutions.geobatch.metocs.utils.io.rest;

import it.geosolutions.geobatch.catalog.file.FileBaseCatalog;
import it.geosolutions.geobatch.global.CatalogHolder;
import it.geosolutions.tools.commons.file.Path;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Alessio Fabiani, GeoSolutions SAS
 * @autor Simone Giannecchini, GeoSolutions SAS
 */
public final class PublishingRestletGlobalConfig {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(PublishingRestletGlobalConfig.class.toString());

    String rootDirectory;

    public PublishingRestletGlobalConfig(String rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public String getRootDirectory() {
        return rootDirectory;
    }

    public void setRootDirectory(String rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public void init() throws Exception {
        File workingDir = null;

        workingDir = Path.findLocation(rootDirectory,
                ((FileBaseCatalog) CatalogHolder.getCatalog()).getConfigDirectory());
        if (workingDir == null || !workingDir.exists() || !workingDir.canRead()
                || !workingDir.isDirectory()) {
            final String message = "Unable to work with the provided working directory:"
                    + (workingDir != null ? workingDir : "<NULL>");
            LOGGER.error(message);
            throw new IllegalArgumentException(message);
        }
        rootDirectory = workingDir.getAbsolutePath();
    }

}
