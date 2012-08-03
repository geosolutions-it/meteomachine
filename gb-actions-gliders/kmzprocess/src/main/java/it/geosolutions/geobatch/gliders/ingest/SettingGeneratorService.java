/*
 * ====================================================================
 *
 * GeoBatch - Intersection Engine
 *
 * Copyright (C) 2007 - 2011 GeoSolutions S.A.S.
 * http://www.geo-solutions.it
 *
 * GPLv3 + Classpath exception
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
package it.geosolutions.geobatch.gliders.ingest;

import java.util.EventObject;

import it.geosolutions.figis.requester.requester.dao.IEConfigDAO;
import it.geosolutions.geobatch.actions.tools.configuration.Path;
import it.geosolutions.geobatch.catalog.impl.BaseService;
import it.geosolutions.geobatch.flow.event.action.ActionService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 *
 */
public class SettingGeneratorService extends BaseService implements ActionService<EventObject, SettingConfiguration>
{

    private static final Logger LOGGER = LoggerFactory.getLogger(SettingGeneratorService.class);

    private IEConfigDAO ieConfigDAO = null;

    public SettingGeneratorService(String id, String name, String description)
    {
        super(id, name, description);
    }

    public SettingAction createAction(SettingConfiguration configuration)
    {
        try
        {
            SettingAction settingAction = new SettingAction(configuration);
            settingAction.setIeConfigDAO(ieConfigDAO);

            return settingAction;
        }
        catch (Exception e)
        {
            if (LOGGER.isInfoEnabled())
            {
                LOGGER.info(e.getLocalizedMessage(), e);
            }

            return null;
        }
    }

    public boolean canCreateAction(SettingConfiguration configuration)
    {
        LOGGER.info("------------------->Checking setting parameters");
        try
        {
            if (ieConfigDAO == null)
            {
                if (LOGGER.isWarnEnabled())
                {
                    LOGGER.warn("SettingGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get the ieConfigDAO.");
                }

                return false;
            }

            // absolutize working dir
//            String wd = Path.getAbsolutePath(configuration.getWorkingDirectory());
            String defaultMaskLayer = configuration.getDefaultMaskLayer();
            String host = configuration.getPersistencyHost();
//            if (wd != null)
//            {
//                configuration.setWorkingDirectory(wd);
//                // return true;
//            }
//            else
//            {
//                if (LOGGER.isWarnEnabled())
//                {
//                    LOGGER.warn("SettingGeneratorService::canCreateAction(): " +
//                        "unable to create action, it's not possible to get an absolute working dir.");
//                }
//
//                return false;
//            }

            if (host != null)
            {
                LOGGER.info("Host value is " + host);
            }
            else
            {
                if (LOGGER.isWarnEnabled())
                {
                    LOGGER.warn("SettingGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get the persistence host.");
                }

                return false;
            }

            if (defaultMaskLayer != null)
            {
                LOGGER.info("The default mask layer is " + defaultMaskLayer);

            }
            else
            {
                if (LOGGER.isWarnEnabled())
                {
                    LOGGER.warn("SettingGeneratorService::canCreateAction(): " +
                        "unable to create action, it's not possible to get the default mask layer.");
                }

                return false;
            }
        }
        catch (Throwable e)
        {
            if (LOGGER.isErrorEnabled())
            {
                LOGGER.error(e.getLocalizedMessage(), e);
            }

            return false;
        }

        return true;
    }

    /**
     * @param ieConfigDAO the ieConfigDAO to set
     */
    public void setIeConfigDAO(IEConfigDAO ieConfigDAO)
    {
        this.ieConfigDAO = ieConfigDAO;
    }

    /**
     * @return the ieConfigDAO
     */
    public IEConfigDAO getIeConfigDAO()
    {
        return ieConfigDAO;
    }

}
