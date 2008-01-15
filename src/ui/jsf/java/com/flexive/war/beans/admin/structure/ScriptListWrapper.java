/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2007
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/copyleft/gpl.html.
 *  A copy is found in the textfile GPL.txt and important notices to the
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
package com.flexive.war.beans.admin.structure;

import com.flexive.shared.scripting.FxScriptInfo;
import com.flexive.shared.scripting.FxScriptEvent;
import com.flexive.shared.exceptions.FxEntryExistsException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.FxSharedUtils;

import java.util.*;
import java.io.Serializable;
import java.text.Collator;

/**
 * Conveniently wraps script mappings to simplify GUI Manipulaiton.
 *
 * @author Gerhard Glos (gerhard.glos@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class ScriptListWrapper {
    public final static int ID_SCRIPT_REMOVED=-1;
    public final static int ID_SCRIPT_ADDED=-2;
    public final static int SORT_STATUS_UNSORTED=0;
    public final static int SORT_STATUS_ASCENDING=1;
    public final static int SORT_STATUS_DESCENDING=2;
    private int sortStatusScriptInfo=-1;
    private int sortStatusEvent=-1;

    public class ScriptListEntry {
        private int id;
        private FxScriptInfo scriptInfo=null;
        private FxScriptEvent scriptEvent=null;

        public ScriptListEntry(int id, FxScriptInfo scriptInfo, FxScriptEvent scriptEvent) {
            this.id = id;
            this.scriptInfo = scriptInfo;
            this.scriptEvent = scriptEvent;
        }

        public int getId() {
            return id;
        }

        private void setId(int id) {
            this.id=id;
        }

        public FxScriptInfo getScriptInfo() {
            return scriptInfo;
        }

        public FxScriptEvent getScriptEvent() {
            return scriptEvent;
        }
    }

    private List<ScriptListEntry> scriptList = null;
    private int ctr=0;
    private Comparator<ScriptListEntry> scriptComparator = new ScriptInfoSorter();
    private Comparator<ScriptListEntry> eventComparator = new ScriptEventSorter();

    /* alternative Constructor for FxType.getScriptsView()
    ScriptListWrapper(Map<FxScriptInfo, List<FxScriptEvent>> scriptMap) {
        scriptList = new ArrayList<ScriptListEntry>();
        for (FxScriptInfo s: scriptMap.keySet()) {
            for (FxScriptEvent event : scriptMap.get(s)) {
                scriptList.add(new ScriptListEntry(ctr++, s,event));
            }
        }
        sortByScripts();
    }
    */

    ScriptListWrapper(Map<FxScriptEvent, long[]> scriptMapping) {
        scriptList = buildScriptList(scriptMapping);
        sortByScripts();
    }

    private List<ScriptListEntry> buildScriptList(Map<FxScriptEvent, long[]> scriptMapping) {
        List<ScriptListEntry> list = new ArrayList<ScriptListEntry>();
        for (FxScriptEvent e: scriptMapping.keySet())
            for (long scriptId: scriptMapping.get(e)) {
                list.add(new ScriptListEntry(ctr++, CacheAdmin.getEnvironment().getScript(scriptId), e));
            }
        return list;
    }

    /**
     * builds the delta to the original scriptMapping. Returns a List of ScriptListEntry objects,
     * where the id is set to ScriptListWrapper.ID_SCRIPT_REMOVED for scripts that where removed from
     * the script list and where the id is set to ScriptListWrapper.ID_SCRIPT_ADDED for scripts that
     * where added to the script list.
     *
     * @param originalScriptMapping     the original scriptMapping
     * @return  a list of ScriptEntry objects which have changed in the script list relative to
     *          the original scriptMapping.
     */
    public List<ScriptListEntry> getDelta(Map<FxScriptEvent, long[]> originalScriptMapping) {
        List<ScriptListEntry> delta = new ArrayList<ScriptListEntry>();
        List<ScriptListEntry> original = buildScriptList(originalScriptMapping);
        for (ScriptListEntry s : original) {
            if (!hasEntry(s)) {
                s.setId(ID_SCRIPT_REMOVED);
                delta.add(s);
            }
        }

        for (ScriptListEntry s : scriptList) {
            if (!hasEntry(original, s)) {
                delta.add(new ScriptListEntry(ID_SCRIPT_ADDED, s.getScriptInfo(), s.getScriptEvent()));
            }
        }

        return delta;
    }

    public List<ScriptListEntry> getScriptList() {
        return scriptList;
    }

    public Map<ScriptListEntry, Boolean> hasDoubleEntry() {
        return new HashMap<ScriptListEntry, Boolean>() {
            public Boolean getObject(Object key) {
                int ctr=0;
                for (ScriptListEntry le: scriptList) {
                    if (le.getScriptEvent().getId() == ((ScriptListEntry)key).getScriptEvent().getId()
                            && le.getScriptInfo().getId() == ((ScriptListEntry)key).getScriptInfo().getId())
                        ctr++;
                    if (ctr >1)
                        return true;
                }
                return false;
            }
        };
    }

    private boolean hasEntry(ScriptListEntry key) {
        for (ScriptListEntry le: scriptList) {
            if (le.getScriptEvent().getId() == key.getScriptEvent().getId()
                && le.getScriptInfo().getId() == key.getScriptInfo().getId())
                return true;
        }
        return false;
    }

    private boolean hasEntry(List<ScriptListEntry> scriptList, ScriptListEntry key) {
        for (ScriptListEntry le: scriptList) {
            if (le.getScriptEvent().getId() == key.getScriptEvent().getId()
                && le.getScriptInfo().getId() == key.getScriptInfo().getId())
                return true;
        }
        return false;
    }

    public void add(long scriptInfo, int scirptEvent) throws FxEntryExistsException, FxNotFoundException {
        ScriptListEntry e = new ScriptListEntry(ctr++, CacheAdmin.getEnvironment().getScript(scriptInfo), FxScriptEvent.getById(scirptEvent));
        if (!hasEntry(e)) {
            scriptList.add(e);
            sortStatusScriptInfo=SORT_STATUS_UNSORTED;
            sortStatusEvent=SORT_STATUS_UNSORTED;
        }
        else
            throw new FxEntryExistsException(e.getScriptInfo().getName()+":"+e.getScriptEvent().getName(), "ex.scriptListWrapper.entryExists",e.getScriptInfo().getName()+":"+e.getScriptEvent().getName());
    }

    public void remove(int entryId) {
        ScriptListEntry entry = null;
        for (ScriptListEntry e: scriptList) {
            if (e.getId() == entryId) {
                entry=e;
                break;
            }
        }
        scriptList.remove(entry);
    }

    public void sortByScripts() {
        Collections.sort(scriptList, scriptComparator);
        if (sortStatusScriptInfo == SORT_STATUS_UNSORTED)
            sortStatusScriptInfo=SORT_STATUS_ASCENDING;
        else if (sortStatusScriptInfo == SORT_STATUS_ASCENDING)
            sortStatusScriptInfo = SORT_STATUS_DESCENDING;
        else
            sortStatusScriptInfo = SORT_STATUS_ASCENDING;

        sortStatusEvent = SORT_STATUS_UNSORTED;
    }

    public void sortByEvents() {
        Collections.sort(scriptList, eventComparator);
        if (sortStatusEvent == SORT_STATUS_UNSORTED)
            sortStatusEvent=SORT_STATUS_ASCENDING;
        else if (sortStatusEvent == SORT_STATUS_ASCENDING)
            sortStatusEvent = SORT_STATUS_DESCENDING;
        else
            sortStatusEvent = SORT_STATUS_ASCENDING;

        sortStatusScriptInfo = SORT_STATUS_UNSORTED;
    }

    public int getSortStatusScriptInfo() {
        return sortStatusScriptInfo;
    }

    public void setSortStatusScriptInfo(int sortStatusScriptInfo) {
        this.sortStatusScriptInfo = sortStatusScriptInfo;
    }

    public int getSortStatusEvent() {
        return sortStatusEvent;
    }

    public void setSortStatusEvent(int sortStatusEvent) {
        this.sortStatusEvent = sortStatusEvent;
    }

    /**
     * compares script list entries, priorizes the script name and if equal the event name
     */
    private class ScriptInfoSorter implements Comparator<ScriptListEntry>, Serializable {
    private final Collator collator = FxSharedUtils.getCollator();

        public int compare(ScriptListEntry o1, ScriptListEntry o2) {
            int multiplicator =1;
            if (sortStatusScriptInfo == SORT_STATUS_ASCENDING)
                multiplicator =-1;
            int c1 = this.collator.compare(o1.getScriptInfo().getName(), o2.getScriptInfo().getName());
            if (c1 !=0)
                return c1*multiplicator;
            else
                return multiplicator* this.collator.compare(o1.getScriptEvent().getName(), o2.getScriptEvent().getName());
        }
    }

    /**
     * compares script list entries, priorizes the script event name and if equal the script name
     */
    private class ScriptEventSorter implements Comparator<ScriptListEntry>, Serializable {
    private final Collator collator = FxSharedUtils.getCollator();

        public int compare(ScriptListEntry o1, ScriptListEntry o2) {
            int multiplicator =1;
            if (sortStatusEvent == SORT_STATUS_ASCENDING)
                multiplicator =-1;
            int c1 = this.collator.compare(o1.getScriptEvent().getName(), o2.getScriptEvent().getName());
            if (c1 !=0)
                return c1*multiplicator;
            else
                return multiplicator*this.collator.compare(o1.getScriptInfo().getName(), o2.getScriptInfo().getName());
        }
    }
}
