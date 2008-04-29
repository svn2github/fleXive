/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License version 2.1 or higher as published by the Free Software Foundation.
 *
 *  The GNU Lesser General Public License can be found at
 *  http://www.gnu.org/licenses/lgpl.html.
 *  A copy is found in the textfile LGPL.txt and important notices to the
 *  license from the author are found in LICENSE.txt distributed with
 *  these libraries.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  For further information about UCS - unique computing solutions gmbh,
 *  please see the company website: http://www.ucs.at
 *
 *  For further information about [fleXive](R), please see the
 *  project website: http://www.flexive.org
 *
 *
 *  This copyright notice MUST APPEAR in all copies of the file!
 ***************************************************************/
package com.flexive.core.timer.jobs;

import com.flexive.core.Database;
import com.flexive.core.storage.StorageManager;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.configuration.DivisionData;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.structure.TypeStorageMode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.*;

import java.sql.Connection;

/**
 * [fleXive] maintenance
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev
 */
public class MaintenanceJob implements Job {

    private static transient Log LOG = LogFactory.getLog(MaintenanceJob.class);

    /**
     * {@inheritDoc}
     */
    public void execute(JobExecutionContext context) throws JobExecutionException {
//        System.out.println("===\nExecuting " + this.getClass().getCanonicalName() + " - " + this);
//        System.out.println("Last: " + context.getPreviousFireTime() + " Next: " + context.getNextFireTime() + " Refire count: " + context.getRefireCount());
//        System.out.println("My Thread: " + Thread.currentThread() + " I am: " + FxContext.get());
        try {
            FxContext.replace((FxContext) context.getScheduler().getContext().get("com.flexive.ctx"));
        } catch (SchedulerException e) {
            LOG.error("Failed to replace FxContext: " + e.getMessage(), e);
        }

        try {
            for (DivisionData dd : EJBLookup.getGlobalConfigurationEngine().getDivisions()) {
                if (dd.getId() <= 0 || !dd.isAvailable())
                    continue;
                if (LOG.isDebugEnabled())
                    LOG.debug("Performing maintenance for division #" + dd.getId() + " ... (times triggered: " +
                            ((SimpleTrigger) context.getTrigger()).getTimesTriggered() + ")");
                Connection con = null;
                try {
                    con = Database.getDbConnection(dd.getId());
                    for (TypeStorageMode mode : TypeStorageMode.values()) {
                        if (!mode.isSupported())
                            continue;
                        StorageManager.getContentStorage(dd, mode).maintenance(con);
                    }
                } catch (Exception e) {
                    LOG.error("Failed to perform maintenance for division #" + dd.getId() + ": " + e.getMessage(), e);
                } finally {
                    Database.closeObjects(MaintenanceJob.class, con, null);
                }
            }
        } catch (FxApplicationException e) {
            LOG.error("Maintenance error: " + e.getMessage(), e);
        }
    }
}