/*
 *  GeoBatch - Open Source geospatial batch processing system
 *  http://geobatch.codehaus.org/
 *  Copyright (C) 2007-2008-2009 GeoSolutions S.A.S.
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
package it.geosolutions.geobatch.metocs.utils.io.rest;

import it.geosolutions.tools.io.file.IOUtils;
import it.geosolutions.tools.io.file.Remove;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Fabiani
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class StorageCleaner extends TimerTask {
    private final static Logger LOGGER = LoggerFactory.getLogger(StorageCleaner.class);

    private long expirationDelay;

    private PublishingRestletGlobalConfig config;

    public void setConfig(PublishingRestletGlobalConfig config) {
        this.config = config;
    }

    @Override
    public void run() {
        try {

            //
            // getting base directory
            //
            final File workingDir = new File(config.getRootDirectory());
            
            // ok, now scan for existing files there and clean up those
            // that are too old
            long now = System.currentTimeMillis();
            for (File f : workingDir.listFiles()) {
                if (now - f.lastModified() > (expirationDelay * 1000)) {
                    // lock the file
                    RandomAccessFile raf = null;
                    FileChannel channel = null;
                    FileLock lock = null;
                    try {
                        raf = new RandomAccessFile(f, "rw");
                        channel = raf.getChannel();
                        lock = channel.lock();
                        Remove.deleteFile(f);
                    } catch (Throwable e) {
                    } finally {
                        try {
                            if (raf != null)
                                raf.close();
                        } catch (Throwable e) {
                            if (LOGGER.isTraceEnabled())
                                LOGGER.trace(e.getLocalizedMessage(), e);
                        }
                        try {
                            if (lock != null)
                                lock.release();
                        } catch (Throwable e) {
                            if (LOGGER.isTraceEnabled())
                                LOGGER.trace(e.getLocalizedMessage(), e);
                        }

                        try {
                            if (channel != null)
                                IOUtils.closeQuietly(channel);
                        } catch (Throwable e) {
                            if (LOGGER.isTraceEnabled())
                                LOGGER.trace(e.getLocalizedMessage(), e);
                        }

                    }
                }
            }
        } catch (Throwable e) {
            LOGGER.warn(
                    "Error occurred while trying to clean up old coverages from temp storage", e);
        }
    }

    /**
     * The file expiration delay in seconds, a file will be deleted when it's been around more than
     * expirationDelay
     * 
     * @return
     */
    public long getExpirationDelay() {
        return expirationDelay;
    }

    public void setExpirationDelay(long expirationDelay) {
        this.expirationDelay = expirationDelay;
    }

}
