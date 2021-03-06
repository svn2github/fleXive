/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2014
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

/**
 * Miscellaneous classes and functions used by flexive JSF components.
 */

fxExtend = function(dest, source) {
       for (var p in source) {
         dest[p] = source[p];
       }
       return dest;
};

var flexive = flexive || {};

fxExtend(flexive, new function() {
    /**
     * Enumeration of valid thumbnail preview sizes - see BinaryDescriptor#PreviewSizes
     */
    this.PreviewSizes = {
        PREVIEW1: {id: 1, size: 42},
        PREVIEW2: {id: 2, size: 85},
        PREVIEW3: {id: 3, size: 232},
        SCREENVIEW: {id: 4, size: 1024},
        ORIGINAL: {id: 0, size: -1}
    };

    /** Absolute application base URL (set by fx:includes) */
    this.baseUrl = null;
    /** Application context path */
    this.contextPath = null;
    /** Weblets resource provider root URL. */
    this.componentsWebletUrl = null;
});

// miscellaneous utility functions
flexive.util = flexive.util || {};
fxExtend(flexive.util, new function() {
    
    /**
     * Initialize the resource URL mapping for JSF2 from a known resource path.
     * Currently you have to map the Faces Servlet to /faces/* in order to use JS/CSS resources
     * like TinyMCE or YUI.
     *
     * @param   url             the resource URL as returned by the container
     * @param   resourcePath    the actual resource name (e.g. js/flexiveComponents.js)
     * @since   3.1.3
     */
    this.initJsf2ResourceMapping = function(/* String */ url, /* String */ resourcePath) {

        // Due to issues with postfix mapping (e.g. .xhtml), we currently require that /faces/ is mapped
        // to the FacesServlet and serve subsequent resource requests (e.g. for TinyMCE) from that path
        flexive.componentsWebletUrl = flexive.contextPath 
            + (url.indexOf("/faces/") == -1 ? "/faces" : "")
            + url.substring(flexive.contextPath.length, url.indexOf(resourcePath))
            // always include library name. Some libraries dump the query string including the library.
            + "flexive-faces/";
        flexive.yuiBase = flexive.componentsWebletUrl + "js/yui/";
    }


    /**
     * Return the absolute URL for the thumbnail of the given PK.
     *
     * @param pk    the object PK
     * @param size  the thumbnail size (one of FxComponents.PreviewSizes)
     * @param includeTimestamp  if true, the current timestamp will be included (disables caching)
     */
    this.getThumbnailURL = function(/* String */ pk, /* flexive.PreviewSizes */ size, /* boolean */ includeTimestamp) {
        return flexive.baseUrl + "thumbnail/pk" + pk
                + (size != null ? "/s" + size.id : "")
                + (includeTimestamp ? "/ts" + new Date().getTime() : "");
    };

    /**
     * Returns the preview size with the given ID.
     *
     * @param id    the preview size ID (0-4)
     */
    this.getPreviewSize = function(/* int */ id) {
        for (var ps in flexive.PreviewSizes) {
            if (flexive.PreviewSizes[ps].id == id) {
                return flexive.PreviewSizes[ps];
            }
        }
        return null;
    };

    /**
     * Parses the given PK and returns a PK object.
     *
     * @param pk     a primary key (e.g. "125.1")
     * @returns      the parsed PK, e.g. { id: 125, version: 1 }
     */
    this.parsePk = function(/* String */ pk) {
        if (pk == null || pk.indexOf(".") == -1) {
            throw "Not a valid PK: " + pk;
        }
        return {id: parseInt(pk.substr(0, pk.indexOf("."))),
                 version: parseInt(pk.substr(pk.indexOf(".") + 1)), 
                 toString: function() {return this.id + "." + this.version}
        };
    };

    /**
     * Extracts the object IDs of the given PK array.
     *
     * @param pks   the PKs
     * @return  the IDs of the given PKs
     */
    this.getPkIds = function(/* Array[PK] */ pks) {
        var result = [];
        for (var i = 0; i < pks.length; i++) {
            result[i] = pks[i].id;
        }
        return result;
    };

    this.JSON_RPC_CLIENT = null;
    this.getJsonRpc = function() {
        if (this.JSON_RPC_CLIENT == null) {
            this.JSON_RPC_CLIENT = new JSONRpcClient(flexive.baseUrl + "adm/JSON-RPC");
        }
        return this.JSON_RPC_CLIENT;
    };

    /**
     * Adds "zero padding" to a number, e.g.: flexive.util.zeroPad(51, 4) --> "0051"
     *
     * @param number    the number to be formatted
     * @param count     the desired input width
     */
    this.zeroPad = function(number, count) {
        var result = "" + number;
        while (result.length < count) {
            result = "0" + result;
        }
        return result;
    };


    /**
     * Escapes single and double quotes of the given string.
     * @param string    the input value
     * @return  the value with escaped quotes
     * @since 3.1
     */
    this.escapeQuotes = function(string) {
        return string.replace("'", "\\'").replace('"', '\\"');
    };
    
});

// Yahoo UI (YUI) helper methods and classes
flexive.yui = flexive.yui || {};
fxExtend(flexive.yui, new function() {
    /** A list of all required components in the current page. Evaluated at the end of the page to initialize Yahoo.*/
    this.requiredComponents = [];
    /** A list of callback functions to be called when YUI has been fully loaded */
    this.onYahooLoadedFunctions = [];

    /**
     * Loads the required Yahoo components of the current page.
     */
    this.load = function() {
        if (this.requiredComponents.length > 0) {
            // load Yahoo UI only if at least one component is required

            // if you want to load the debug version of YUI, you have to change flexive.yuiBase to the
            // path of a full YUI installation - the flexive JAR includes only the minimized versions 
            try {
                var loader = new YAHOO.util.YUILoader({
                            base: flexive.yuiBase,
                            require: this.requiredComponents,
                            loadOptional: true,
                            onSuccess: function() {
                                flexive.yui.processOnYahoo();
                            }
                    });
                loader.insert();
            } catch (e) {
                // exception during initialization, probably YUI hasn't been included on the page
            }
        } else {
            flexive.yui.processOnYahoo();   // FX-428
        }
    };

    /**
     * Adds the given function to flexive.yui.onYahooLoaded.
     *
     * @param fn    the function to be called when Yahoo has been loaded
     */
    this.onYahooLoaded = function(fn) {
        this.onYahooLoadedFunctions.push(fn);
    };

    /**
     * Adds the given Yahoo component to the required components of this page.
     *
     * @param component the Yahoo component (e.g. "button")
     */
    this.require = function(component) {
        var found = false;
        for (var i = 0; i < this.requiredComponents.length; i++) {
            if (this.requiredComponents[i] == component) {
                found = true;
                break;
            }
        }
        if (!found) {
            this.requiredComponents.push(component);
        }
    };

    /**
     * Call setup methods of flexive components after yahoo has been initialized.
     */
    this.processOnYahoo = function() {
        this.requiredComponents = [];
        for (var i = 0; i < this.onYahooLoadedFunctions.length; i++) {
            this.onYahooLoadedFunctions[i]();
        }
        this.onYahooLoadedFunctions = [];
    };

    /**
     * Updates a property of a menu item.
     *
     * @param id    the menu item ID
     * @param property  the property (e.g. "disabled")
     * @param value the new value
     */
    this.setMenuItem = function(/* String */ id, /* String */ property, value) {
        var item = YAHOO.widget.MenuManager.getMenuItem(id);
        if (item == null) {
            alert("Menu item not found: " + id);
            return;
        }
        item.cfg.setProperty(property, value);
    };

    /**
     * Updates a property of one or more menu items.
     *
     * @param ids    the menu items ID(s)
     * @param property  the property (e.g. "disabled")
     * @param value the new value
     * @since 3.1
     */
    this.setMenuItems = function(/* String[] */ ids, /* String */ property, value) {
        for (var i = 0; i < ids.length; i++) {
            this.setMenuItem(ids[i], property, value);
        }
    };
});

flexive.yui.datatable = flexive.yui.datatable || {};
fxExtend(flexive.yui.datatable, new function() {
    /**
     * Return the datatable wrapper for the correct view (list or thumbnails).
     *
     * @param result    the search result object as returned by fxSearchResultBean.jsonResult
     */
    this.getViewWrapper = function(result, actionColumnHandler, previewSizeName) {
        var previewSize = flexive.PreviewSizes[previewSizeName];
        if (previewSize == null) {
            previewSize = flexive.PreviewSizes.PREVIEW2;
        }
        return result.viewType == "THUMBNAILS"
                ? new flexive.yui.datatable.ThumbnailView(result, actionColumnHandler, previewSize)
                : new flexive.yui.datatable.ListView(result, actionColumnHandler);
    };

    /**
     * <p>Return the primary key of the result table row/column of the given element (e.g. an event target).
     * Supports both list and thumbnail view.</p>
     *
     * <p>Note that the primary key is only available if it was selected in the FxSQL query (@pk).</p>
     *
     * @param dataTable the datatable variable (set with the 'var' attribute of fx:resultTable)
     * @param element   the table element (i.e. an element nested in a table cell/row)
     * @return  a PK object, e.g. {id: 21, version: 10}
     */
    this.getPk = function(/* YAHOO.widget.DataTable */ dataTable, /* Element */ element) {
        return flexive.util.parsePk(this.getRecordValue(dataTable, element, "pk"));
    };

    /**
     * <p>Return the permissions object of the result table row/column of the given element (e.g. an event target).
     * Supports both list and thumbnail view.</p>
     *
     * <p>Note that the permissions are only available if they were selected in the FxSQL query (@permissions).</p>
     *
     * @param dataTable the datatable variable (set with the 'var' attribute of fx:resultTable)
     * @param element   the table element (i.e. an element nested in a table cell/row)
     * @return  <p>a permissions object with the following boolean properties: <ul>
     * <li>read</li>
     * <li>edit</li>
     * <li>relate</li>
     * <li>delete</li>
     * <li>export</li>
     * <li>create</li>
     * </ul>
     * A property is set if the current user has the appropriate rights to perform the action.</p>
     */
    this.getPermissions = function(/* YAHOO.widget.DataTable */ dataTable, /* Element */ element) {
        return this.decodePermissions(this.getRecordValue(dataTable, element, "permissions"));
    };

    /**
     * Decode the permissions bitmask as returned by the search result.
     *
     * @param permissions   the permissions bitmask as returned by the search result.
     * @return  <p>a permissions object with the following boolean properties: <ul>
     * <li>read</li>
     * <li>edit</li>
     * <li>relate</li>
     * <li>delete</li>
     * <li>export</li>
     * <li>create</li>
     * </ul>
     * A property is set if the current user has the appropriate rights to perform the action.</p>
     * @since 3.1
     */
    this.decodePermissions = function(/* Integer */ permissions) {
        return {
            "read": (permissions & 1)  > 0,
            "create": (permissions & 2) > 0,
            "delete": (permissions & 4) > 0,
            "edit": (permissions & 8) > 0,
            "export": (permissions & 16) > 0,
            "relate": (permissions & 32) > 0,
            encode: function() {return permissions;}
        };
    };

    /**
     * Extracts a property from the data record of the row/column indicated by the given element
     * (e.g. the target element of a "click" or "contextmenu" event).
     *
     * @param dataTable the Yahoo datatable widget
     * @param element   the source element (must be inside the table)
     * @param property  the property value to be looked up
     * @return  the property value, or null if no data record was found
     */
    this.getRecordValue = function(/* YAHOO.widget.DataTable */ dataTable, /* Element */ element, /* String */ property) {
        var elRow = dataTable.getTrEl(element);
        var record = dataTable.getRecord(elRow);
        if (record == null) {
            YAHOO.log("No primary key found for element " + element, "warn");
            return null;
        }
        var data = record.getData();
        if (data[property] && !YAHOO.lang.isArray(data[property])) {
            // only one property for this column, return it
            return data[property];
        } else if (YAHOO.lang.isArray(data[property])) {
            // one property per column, get column index and return PK
            var elCol = dataTable.getTdEl(element);
            var col = dataTable.getColumn(elCol);
            return data[property][col.getKeyIndex()];
        } else {
            return null;
        }
    };

    /**
     * <p>Returns the currently selected PKs of the given datatable.</p>
     *
     * <p><i>
     * Implementation note: selections over page boundaries are only supported in list view.
     * This appears to be a limitation of the YUI datatable widget, and may work in future versions.
     * </i></p>
     *
     * @param dataTable the datatable instance.
     * @return  a PK object, e.g. {id: 21, version: 10}
     */
    this.getSelectedPks = function(/* YAHOO.widget.DataTable */ dataTable) {
        var selectedPks = [];
        var tdEls = dataTable.getSelectedTdEls();
        if (tdEls.length > 0) {
            // column selection mode - works only on current page anyway
            for (var i = 0; i < tdEls.length; i++) {
                selectedPks.push(flexive.yui.datatable.getPk(dataTable, tdEls[i]));
            }
        } else {
            // get all selected rows (may include rows outside the current page)
            var rows = dataTable.getSelectedRows();
            for (var i = 0; i < rows.length; i++) {
                selectedPks.push(flexive.util.parsePk(dataTable.getRecord(rows[i]).getData()["pk"]));
            }
        }
        return selectedPks;
    };

    /**
     * Delete all selected rows from the datatable.
     *
     * @param dataTable the datatable instance
     * @return the number of rows deleted
     * @since 3.1
     */
    this.deleteSelectedRows = function(/* YAHOO.widget.DataTable */ dataTable) {
        var recordSet = dataTable.getRecordSet();
        var rows = dataTable.getSelectedRows();
        for (var i = 0; i < rows.length; i++) {
            dataTable.deleteRow(recordSet.getRecordIndex(recordSet.getRecord(rows[i])));
        }
        return rows.length;
    };

    /**
     * Delete all rows where the given function returns true.
     *
     * @param dataTable the datatable
     * @param predicateFn a function that takes the record as an argument and returns "true" if the row matches
     * @return the number of rows deleted
     * @since 3.1
     */
    this.deleteMatchingRows = function(/* YAHOO.widget.DataTable */ dataTable, predicateFn) {
        var recordSet = dataTable.getRecordSet();
        var records = recordSet.getRecords();
        var indices = [];
        for (var i = 0; i < records.length; i++) {
            var record = records[i];
            if (predicateFn(record)) {
                indices.push(recordSet.getRecordIndex(record));
            }
        }
        // delete in reversed row index order
        indices.sort(function(a, b) {return b-a;});
        for (i = 0; i < indices.length; i++) {
            dataTable.deleteRow(indices[i]);
        }
        return indices.length;
    };

    /**
     * Selects all rows of the given datatable.
     *
     * @param dataTable the datatable widget
     * @since 3.1
     */
    this.selectAllRows = function(/* YAHOO.widget.DataTable */ dataTable) {
        var records = dataTable.getRecordSet().getRecords();
        for (var i = 0; i < records.length; i++) {
            dataTable.selectRow(records[i]);
        }
    };
    
    /**
     * Selects all rows of the given datatable on the current page.
     *
     * @param dataTable the datatable widget
     * @since 3.1
     */
    this.selectAllPageRows = function(/* YAHOO.widget.DataTable */ dataTable) {
        var trEl = dataTable.getFirstTrEl();
        dataTable.unselectAllRows();
        while (trEl != null) {
            dataTable.selectRow(trEl);
            trEl = trEl.nextSibling;
        }
    };

    /**
     * Selects all cells of the given datatable on the current page.
     * As of 2.7.0, the YUI datatable does not support selection of off-page cells.
     *
     * @param dataTable the datatable widget
     * @since 3.1
     */
    this.selectAllPageCells = function(/* YAHOO.widget.DataTable */ dataTable) {
        var trEl = dataTable.getFirstTrEl();
        dataTable.unselectAllCells();
        while (trEl != null) {
            var tdEl = dataTable.getFirstTdEl(trEl);
            while (tdEl != null) {
                dataTable.selectCell(tdEl);
                tdEl = tdEl.nextSibling;
            }
            trEl = trEl.nextSibling;
        }
    };

});

/**
 * List result view - returns the linear result list of the search result
 * @param result    the search result object as returned by fxSearchResultBean.jsonResult
 * @param actionColumnHandler    - an optional handler for generating the action column
 */
flexive.yui.datatable.ListView = function(result, actionColumnHandler) {
    this.result = result;
    this.rowsPerPage = 25;
    this.actionColumnHandler = actionColumnHandler;
    this.responseSchema = this.result.responseSchema;
    this.rows = this.result.rows;
    this.columns = this.result.columns;
    if (this.actionColumnHandler != null) {
        // add action column to column headers
        this.columns.push({
            key: "__actions",
            label: "",
            sortable: false
        });

        // extend response schema with action column
        this.responseSchema.fields.push({
            key: "__actions",
            parser: "string"
        });

        // generate action column markup
        for (var i = 0; i < this.rows.length; i++) {
            var row = this.rows[i];
            row.__actions = this.actionColumnHandler.getActionColumn(
                    row["pk"] != null ? flexive.util.parsePk(row["pk"]) : null,
                    row["permissions"] != null ? flexive.yui.datatable.decodePermissions(row["permissions"]) : null
            );
        }
    }
};

flexive.yui.datatable.ListView.prototype = {
    getColumns: function() {
        return this.columns;
    },

    getResponseSchema: function() {
        return this.responseSchema;
    },

    getRows: function() {
        return this.rows;
    }
};

/**
 * Thumbnail view - projects the linear result list to a thumbnail grid
 * @param result    the search result object as returned by fxSearchResultBean.jsonResult
 * @param actionColumnHandler    - an optional handler for generating the action column
 * @param previewSize           - the preview size to be used for thumbnails (see flexive.PreviewSizes)
 */
flexive.yui.datatable.ThumbnailView = function(result, actionColumnHandler, previewSize) {
    this.result = result;
    this.previewSize = previewSize;
    this.gridColumns = Math.min(
            Math.max(1, parseInt((YAHOO.util.Dom.getViewportWidth() - 50) / (this.previewSize.size + 20))),
            result.rows.length
    );
    this.rowsPerPage = 5;
    this.actionColumnHandler = actionColumnHandler;
};

flexive.yui.datatable.ThumbnailView.prototype = {
    // return the columns of the thumbnail grid
    getColumns: function() {
        var columns = [];
        for (var i = 0; i < this.gridColumns; i++) {
            columns.push({key: "c" + i, label: ""});
        }
        return columns;
    },

    getRows: function() {
        // transpose the linear result rows according to the grid size
        var grid = [];
        var virtualFields = this.getVirtualFields();
        var currentRow = {};
        for (var i = 0; i < virtualFields.length; i++) {
            currentRow[virtualFields[i]] = [];
        }
        var currentColumn = 0;
        var textColumnKey = this.result.columns.length > 0 ? this.result.columns[0].key : null;
        for (i = 0; i < this.result.rowCount; i++) {
            var resultRow = this.result.rows[i];
            var data = "";
            if (resultRow.pk != null) {
                data = "<div class=\"thumbnailContainer\"><img src=\""
                        + flexive.util.getThumbnailURL(resultRow.pk, this.previewSize, true)
                        + "\" class=\"resultThumbnail\"/></div>";
            }
            if (textColumnKey != null) {
                var text = resultRow[textColumnKey];
                data += "<br/><div class=\"resultThumbnailCaption\" title=\""
                        + flexive.util.escapeQuotes(text) + "\">"
                        + resultRow[textColumnKey]
                        + "</div>";
            }
            if (this.actionColumnHandler != null) {
                data += "<div class=\"resultThumbnailAction\">"
                        + this.actionColumnHandler.getActionColumn(resultRow.pk, resultRow.permissions)
                        + "</div>";
            }
            // add lytebox links
            if (resultRow.pk != null) {
                data += "<div style=\"display:none\">"
                        + " <a rel=\"lytebox[sr]\" href=\"" + flexive.baseUrl + "thumbnail/pk" + resultRow.pk + "/s4"
                        + "\" title=\"Screenview " + resultRow.pk + "\">Screenview</a></div";
            }
            if (currentColumn >= this.gridColumns) {
                // grid row completed
                grid.push(currentRow);
                currentRow = {};
                for (var j = 0; j < virtualFields.length; j++) {
                    currentRow[virtualFields[j]] = [];
                }
                currentColumn = 0;
            }
            // store column
            currentRow["c" + currentColumn] = data;
            currentRow.pk.push(resultRow.pk);
            currentRow.permissions.push(resultRow.permissions);
            currentRow.hasBinary.push(resultRow.hasBinary);
            currentRow.mayLock.push(resultRow.mayLock);
            currentRow.mayUnlock.push(resultRow.mayUnlock);
            currentRow.locked.push(resultRow.locked);
            currentColumn++;
        }
        if (currentColumn > 0) {
            grid.push(currentRow);
        }
        return grid;
    },

    getVirtualFields: function() {
        return ["pk", "permissions", "hasBinary", "mayLock", "mayUnlock", "locked"];
    },

    getResponseSchema: function() {
        var fields = this.getVirtualFields();
        for (var i = 0; i < this.gridColumns; i++) {
            fields.push("c" + i);
        }
        return {"fields": fields};
    }
};

/**
 * A callback handler that generates the "Action" column (e.g. edit links) of a result row.
 *
 * @since 3.1
 */
flexive.yui.datatable.ActionColumnHandler = function() {
};

flexive.yui.datatable.ActionColumnHandler.prototype = {
    /**
     * Return the HTML for the result column of the given PK.
     *
     * @param pk    the primary key of the column, as returned by flexive.util.parsePk(String)
     * @param permissions the row permissions, as returned by flexive.yui.datatable.decodePermissions
     */
    getActionColumn: function(/* PK */ pk, /* Permissions */ permissions) {
    }
};

flexive.yui.AutoCompleteHandler = function(queryFn) {
    if (queryFn != null) {
        this.query = queryFn;
    }
};

flexive.yui.AutoCompleteHandler.prototype = {
    /**
     * <p>Return a datasource for the autocomplete handler.</p>
     * <p>Usually <code>YAHOO.widget.DS_JSArray</code> for data that can be sent to the client
     * or <code>YAHOO.widget.DS_JSFunction</code> for a JSON/RPC/Java wrapper.</p>
     *
     * @return The YAHOO.widget.DataSource to be used for the autocomplete. The default implementation
     * returns a JSFunction datasource that uses <code>this.query()</code> to determine the
     * valid choices.
     */
    getDataSource: function() {
        return new YAHOO.widget.DS_JSFunction(this.query);
    },

    /**
     * Implement the query logic here.
     *
     * @param query the query from the autocomplete field
     * @return the autocomplete choices (format determined by the data source)
     */
    query: function(query) {
        return [["Java 1.5", "Java"], ["Groovy 1.5", "Groovy"], ["Scala 2.7", "Scala"], ["JRuby 1.0", "JRuby"], ["JavaScript 2", "Javascript"]];
    },

    /**
     * Formats an item returned by query().
     *
     * @param item  the item to be formatted
     * @param query the current user query
     * @return  the formatted markup for the item
     */
    formatResult: function(/* Object */ item, /* String */ query) {
        return item[1];
    }
};

/**
 * Yahoo UI replacements for standard javascript dialogs (confirm, alert, prompt). These dialogs are
 * shown in a DHTML layer and do not cause issues with popup blockers, in contrast to plain
 * javascript dialogs (e.g. in IE7).
 *
 * <p>To use them, you need to include the 'container' module of YUI:</p>
 * <code>flexive.yui.require("container");</code>
 */
flexive.yui.dialogs = flexive.yui.dialogs || {};
fxExtend(flexive.yui.dialogs, new function() {

    /**
     * DHTML replacement for Javascript's confirm() dialog. Note that this dialog returns immediately,
     * any actions that should be executed after the user pressed a button need to be passed
     * in the function parameter(s).
     *
     * @param message       the message to be displayed
     * @param title         the dialog title
     * @param yesButton     the text to be displayed for the "yes" button
     * @param noButton      the text to be displayed for the "no" button
     * @param onConfirmed   the function to be executed when the user confirmed the message
     * @param onCancel      the function to be executed when the user did not confirm (optional)
     */
    this.showConfirmDialog = function(/* String */ message, /* String */ title, /* String */ yesButton, /* String */ noButton,
                                /* fn */ onConfirmed, /* fn */ onCancel) {
        var handleConfirm = function() {
            dialog.disableButtons();
            if (onConfirmed) onConfirmed();
            dialog.hide();
            dialog.destroy();
        };
        var handleCancel = function() {
            dialog.disableButtons();
            if (onCancel) onCancel();
            dialog.hide();
            dialog.destroy();
        };
        var dialog = new YAHOO.widget.SimpleDialog("dlg", {
            width: Math.min(message.length + 5, 40) + "em",
            fixedcenter:true,
            modal:true,
            visible:false,
            draggable:true,
            constraintoviewport: true,
            buttons:  [   {text: yesButton,
                            handler: handleConfirm,
                            isDefault:true},
                          {text: noButton,
                            handler: handleCancel}
                      ]
        });
        dialog.disableButtons = function() {
            this.getButtons()[0].disabled = true;
            this.getButtons()[1].disabled = true;
        };
        dialog.setHeader(title);
        dialog.setBody(message);
        dialog.cfg.setProperty("icon",YAHOO.widget.SimpleDialog.ICON_WARN);
        dialog.cfg.queueProperty("keylisteners", [
            // bind escape key to cancel button
            new YAHOO.util.KeyListener(document, {keys: 27}, {fn: handleCancel, scope: dialog, correctScope:true})
        ]);
        dialog.render(document.body);
        dialog.show();
    };

    /**
     * DHTML replacement for Javascript's alert() dialog. Note that this dialog returns immediately,
     * any actions that should be executed after the user pressed a button need to be passed
     * in the function parameter(s).
     *
     * @param message   the message to be displayed
     * @param title     the dialog title
     * @param okButton  the OK button text
     */
    this.showAlertDialog = function(/*String */ message, /* String */ title, /* String */ okButton, /* fn */ onConfirm) {
        var dialog = new YAHOO.widget.SimpleDialog("dlg", {
            width: Math.min(message.length + 5, 40) + "em",
            fixedcenter:true,
            modal:true,
            visible:false,
            draggable:true,
            constraintoviewport: true,
            buttons:  [   {text: okButton,
                            handler: function() {dialog.hide();dialog.destroy();if (onConfirm) onConfirm();},
                            isDefault:true} ]
        });
        dialog.setHeader(title);
        dialog.setBody(message);
        dialog.cfg.setProperty("icon",YAHOO.widget.SimpleDialog.ICON_INFO);
        dialog.render(document.body);
        dialog.show();
    };

    /**
     * DHTML replacement for Javascript's prompt() dialog. Note that this dialog returns immediately,
     * any actions that should be executed after the user pressed a button need to be passed
     * in the function parameter(s).
     *
     * @param message       the message to be displayed
     * @param defaultValue  the default input value
     * @param title         the dialog title
     * @param submitButton  the text to be displayed for the "submit" button
     * @param cancelButton  the text to be displayed for the "cancel" button
     * @param onSuccess     the function to be executed when the user confirmed the input (takes input as first parameter)
     * @param onCancel      the function to be executed when the user did not confirm (optional)
     */
    this.showPromptDialog = function(/* String */ message, /* String */ defaultValue, /* String */ title, /* String */ submitButton, /* String */ cancelButton,
                                     /* fn */ onSuccess, /* fn */ onCancel) {
        var e = document.getElementById("__fxPromptDialog");
        if (e == null) {
            // create dialog
            e = document.createElement("div");
            e.setAttribute("id", "__fxPromptDialog");
            e.setAttribute("style", "display: none");
            e.innerHTML = "<div id=\"__fxPromptTitle\" class=\"hd\"></div>"
                    + "<div class=\"bd\"><form name=\"__promptForm\" action=\"#\">"
                    + "<label for=\"__fxPromptInput\"><span id=\"__fxPromptLabel\"> </span></label>"
                    + "<input id=\"__fxPromptInput\" name=\"promptInput\" type=\"text\" size=\"25\"/>"
                    + "</form></div>";
            document.body.appendChild(e);
        }
        document.getElementById("__fxPromptLabel").innerHTML = message;
        document.getElementById("__fxPromptInput").value = defaultValue != null ? defaultValue : "";
        document.getElementById("__fxPromptTitle").innerHTML = title;

        if (e.dialog == null) {
            var handleSubmit = function() {
                if (e.onSuccess) {
                    e.onSuccess(document.getElementById("__fxPromptInput").value);
                }
                e.dialog.hide();
            };
            e.dialog = new YAHOO.widget.Dialog("__fxPromptDialog", {
                width: "40em",
                fixedcenter:true,
                modal:true,
                visible:false,
                draggable:true,
                postmethod: "none",
                constraintoviewport: true,
                buttons:  [   {text: submitButton,
                                handler: handleSubmit,
                                isDefault:true},
                              {text: cancelButton,
                                handler:function() {e.dialog.hide();if (e.onCancel) e.onCancel();}
                              }
                          ]
            });
            e.dialog.cfg.queueProperty("keylisteners", [
                // bind enter key to submit button
                new YAHOO.util.KeyListener(e, {keys: 13}, {fn: handleSubmit, scope: e.dialog, correctScope:true}),
                // bind escape key to cancel button
                new YAHOO.util.KeyListener(e, {keys: 27}, {fn: function() {e.dialog.hide();}, scope: e.dialog, correctScope:true})
            ]);


            document.getElementById("__fxPromptDialog").style.display = "block";
            e.dialog.render();
        }
        e.onSuccess = onSuccess;
        e.onCancel = onCancel;
        e.dialog.show();
    };
});

flexive.input = flexive.input || {};
fxExtend(flexive.input, new function() {
    this.fxValueInputList = [];   // a global list of all registered FxValueInput elements on the current page

    /**
     * Updates the current language of all multilanguage inputs on the current page.
     *
     * @param languageId    the language ID
     */
    this.setLanguageOnAllInputs = function(languageId) {
        var newList = [];
        var oldList = flexive.input.fxValueInputList;
        for (var i = 0; i < oldList.length; i++) {
            if (oldList[i].isValid()) {
                oldList[i].showLanguage(languageId);
                newList.push(oldList[i]);
            }
        }
        flexive.input.fxValueInputList = newList;     // store new list without defunct inputs
        //flexive.input._fixHtmlEditorsIE();
    };

    // trigger tinyMCE repaint on IE
    this._fixHtmlEditorsIE = function() {
        if (typeof(window.tinyMCE) != 'undefined' && tinyMCE.isIE) {
            for (var i = 0; i < tinyMCE.editors.length; i++) {
                try {
                    tinyMCE.editors[i].mceCleanup();
                } catch (e) {
                    // ignore
                }
            }
        }
    };

    this.htmlEditorConfigs = fxExtend(flexive.input.htmlEditorConfigs || {}, {
        "default-basic": {
            mode: "exact",
            dialog_type: "modal",
            editor_selector: "fxValueTextAreaHtml",
            theme: "advanced",
            plugins: "paste,fullscreen,inlinepopups,searchreplace,advlink",
            language: flexive.guiTranslation,
            entity_encoding: "raw",     // don't replace special characters (like umlaut) with HTML entities

            // general layout options
            theme_advanced_layout_manager: "SimpleLayout",
            theme_advanced_toolbar_location : "top",
            theme_advanced_buttons1: "undo,redo,removeformat,replace"
            + ",separator,link,unlink,image,charmap,separator,bullist,numlist,separator,code,fullscreen",
            theme_advanced_buttons2: "bold,italic,underline,separator,forecolor,formatselect,fontselect,fontsizeselect",
            theme_advanced_buttons3: "",
            theme_advanced_toolbar_align: "left",
            theme_advanced_resizing : true,
            theme_advanced_statusbar_location: "bottom",

            // fix alert when switching between multiple editors
            focus_alert: false,
            strict_loading_mode: true,  /* Fixes Firefox HTML encoding problem with multiple editors */
            relative_urls: false,        /* don't force relative URLs for images and links */
            convert_urls : false,
            forced_root_block : false    /* Don't add surrounding p (FX-836) */
            /*width: "100%"*/
        },

        "default-full": {
            mode: "exact",
            dialog_type: "modal",
            editor_selector: "fxValueTextAreaHtml",
            theme: "advanced",
            plugins: "paste,fullscreen,inlinepopups,searchreplace,advlink",
            language: flexive.guiTranslation,
            entity_encoding: "raw",     // don't replace special characters (like umlaut) with HTML entities

            // general layout options
            theme_advanced_layout_manager: "SimpleLayout",
            theme_advanced_toolbar_location : "top",
            theme_advanced_buttons1: "paste,pastetext,pasteword,separator,undo,redo,removeformat,replace"
            + ",separator,link,unlink,image,charmap,separator,bullist,numlist,separator,justifyleft,justifycenter,justifyright,justifyfull,separator,code,fullscreen",
            theme_advanced_buttons2: "bold,italic,underline,strikethrough,separator,forecolor,formatselect,fontselect,fontsizeselect",
            theme_advanced_buttons3: "",
            theme_advanced_toolbar_align: "left",
            theme_advanced_resizing : true,
            theme_advanced_statusbar_location: "bottom",

            // fix alert when switching between multiple editors
            focus_alert: false,
            strict_loading_mode: true,  /* Fixes Firefox HTML encoding problem with multiple editors */
            relative_urls: false,        /* don't force relative URLs for images and links */
            convert_urls : false,
            forced_root_block : false    /* Don't add surrounding p (FX-836) */
            /*width: "100%"*/
        }
    });
    
    // initialize the TinyMCE HTML editor
    this.initHtmlEditor = function(autoPopulate, configName) {
        if (this.initHtmlEditorCalled) {
            return;
        }
        try {
            tinymce.baseURL = flexive.componentsWebletUrl + "js/tiny_mce";
            tinymce.query = "";
            tinyMCE.init(this.htmlEditorConfigs[configName == null ? "default-basic" : configName]);
            this.initHtmlEditorCalled = true;
        } catch (e) {
            alert("initHtml exception: " + e);
            // HTML editor component not configured
        }
    };

    this.openReferenceQueryPopup = function(xpath, updateInputId, formName) {
        var win = window.open(flexive.baseUrl + "jsf-components/browseReferencesPopup.jsf?xPath=" + xpath + "&inputName="
                + updateInputId + "&formName=" + formName,
                "searchReferences", "scrollbars=yes,width=800,height=600,toolbar=no,menubar=no,location=no");
        win.focus();
    };

    /**
     * Implement a tristate checkbox for FxValueInputRenderer.
     * The third state means "empty" and alters the checkbox visibility with the CSS style "fxValueEmpty".
     *
     * @param inputId   the checkbox input ID
     */
    this.onTristateCheckboxChanged = function(inputId, tooltips)  {
        // get checkbox
        var checkbox = document.getElementById(inputId);
        // get hidden input for setting the empty state
        var hidden = document.getElementById(inputId + "_empty");
        var empty = hidden.value == "true";
        
        if (checkbox.checked && !empty) {
            // user toggled checkbox from "false", but empty not set - goto empty state
            hidden.value = "true";
            checkbox.checked = false;
            if (checkbox.className.indexOf("fxValueEmpty") == -1) {
                checkbox.className += " fxValueEmpty";
            }
            checkbox.title=tooltips[2];
        } else {
            // normal state
            hidden.value = "false";
            checkbox.className = checkbox.className.replace("fxValueEmpty", "");
            checkbox.title=checkbox.checked ? tooltips[0] : tooltips[1];
        }
    };
});

/**
 * JS object for fx:fxValueInput components.
 *
 * @param id            the fx:fxValueInput input ID
 * @param baseRowId     the row prefix, a valid row ID is constructed by appending the language ID
 * @param rowInfos      a map of type <code>{ languageId: { rowId: String, inputId: String } }</code>
 * @param languageSelectId  the input ID of the language select listbox
 * @param defaultLanguageId the default language (may be -1 if no default language is selected)
 */
flexive.input.FxMultiLanguageValueInput = function(id, baseRowId, rowInfos, languageSelectId, defaultLanguageId) {
    this.id = id;
    this.baseRowId = baseRowId;
    this.rowInfos = rowInfos;
    this.languageSelectId = languageSelectId;
    this.defaultLanguageId = defaultLanguageId;
    flexive.input.fxValueInputList.push(this);
    this._attachInputListeners();
};

flexive.input.FxMultiLanguageValueInput.prototype = {
    onLanguageChanged: function(languageSelect) {
        // save scrollbar position for IE
        //var scrollY = window.pageYOffset || document.documentElement.scrollTop || document.body.scrollTop || -1;

        this.showLanguage(languageSelect.options[languageSelect.selectedIndex].value);
        
        //flexive.input._fixHtmlEditorsIE();

        // restore scrollbar position in IE
        //if (document.all && scrollY != -1) {
        //    window.scrollTo(0, scrollY);
        //}
    },

    showRow: function(showRowId) {
        for (var i in this.rowInfos) {
            var rowId = this.rowInfos[i].rowId;
            var enabled = showRowId == null || rowId == showRowId;
            document.getElementById(rowId).style.display = enabled ? "block" : "none";
            // all languages / show language icon for each row
            document.getElementById(rowId + "_language").style.display = (enabled && showRowId == null) ? "inline" : "none";
        }
    },

    /**
     * Returns the id of the first shown input element
     */
    getFirstInputId: function() {
        for (var i in this.rowInfos) {
            var rowId = this.rowInfos[i].rowId;
            if (document.getElementById(rowId).style.display != "none") {
                var inputId =this.id+'_input_'+i;
                //eliminate form id from element id
                var formIdx = inputId.indexOf(":");
                if (formIdx >0)
                    return inputId.substring(formIdx+1);
                return inputId;
            }
        }
    },

    showLanguage: function(languageId) {
        // show row for this language
        this.showRow(languageId >= 0 ? this.baseRowId + languageId : null);
        // update language select
        var options = document.getElementById(this.languageSelectId).options;
        for (var i = 0; i < options.length; i++) {
            options[i].selected = (options[i].value == languageId);
        }
    },

    onDefaultLanguageChanged: function(inputCheckbox, languageId) {
        if (inputCheckbox.checked) {
            // uncheck other languages since only one language may be the default
            var languageCheckboxes = document.getElementsByName(inputCheckbox.name);
            for (var i = 0; i < languageCheckboxes.length; i++) {
                if (languageCheckboxes[i] != inputCheckbox) {
                    languageCheckboxes[i].checked = false;
                }
            }
            this.defaultLanguageId = languageId;
        }
    },

    /**
     * Returns true if the input elements assigned to this object are still present.
     */
    isValid: function() {
        return document.getElementById(this.languageSelectId) != null;
    },

    /**
     * Attaches an input listener to the given row IDs
     */
    _attachInputListeners: function() {
        // set value from current default language in all empty inputs
        for (var i in this.rowInfos) {
            var rowInfo = this.rowInfos[i];
            var input = document.getElementById(rowInfo.inputId);
            if (input != null && input.type == "text" && this.defaultLanguageId > 0 && input.value == "") {
                //input.value = document.getElementById(this.rowInfos[this.defaultLanguageId].inputId).value;
                // TODO: implement "grey default language values" (must not be submitted to the server)
                input.setAttribute("defaultLanguageSet", true);
            } else if (input != null) {
                input.removeAttribute("defaultLanguageSet");
            }
        }
    }
};

flexive.dojo = flexive.dojo || {};
fxExtend(flexive.dojo, new function() {
    /** Creates a DOJO menu from a JSON object. Based on Dojo example code. */
    this.makeMenu = function(id, menuClass, itemClass, items, isTop, contextMenuTarget) {
        var options = {
            templateCssPath: dojo.uri.dojoUri("../../css/dojoCustom/Menu2.css"),
            contextMenuForWindow: isTop,
            toggle: "fade"
        };
        if (id != null) {
            options.widgetId = id;
        }
        if (contextMenuTarget != null) {
            options.targetNodeIds = [contextMenuTarget];
        }
        var menu2 = dojo.widget.createWidget(menuClass, options);
        dojo.lang.forEach(items, function(itemJson){
            // if submenu is specified, create the submenu and then make submenuId point to it
            if( itemJson.submenu){
                var submenu = flexive.dojo.makeMenu(null, menuClass, itemClass, itemJson.submenu, false);
                itemJson.submenuId = submenu.widgetId;
            }
            var item = dojo.widget.createWidget(itemJson.createAsType == null ? itemClass : itemJson.createAsType,  itemJson);
            menu2.addChild(item);
        });
        return menu2;
    };
});

flexive.contentEditor = flexive.contentEditor || {};
fxExtend(flexive.contentEditor, new function() {
    var activeMenu = null;
    var activationTime = -1;
    var toggledGroups = null;
    var allOpened = null;
    var groupIds = null;
    var editorIds=null;

    /** save variables relavant for group folding **/
    this.saveGroupFolding=function(formPrefix) {
        if (editorIds != null) {
            var tgString ="";
            var rowArray = new Array();
            if (toggledGroups != null) {
                for (var i=0; i<editorIds.length;i++) {
                    if (toggledGroups[editorIds[i]] != null)
                        rowArray.push(editorIds[i] + ":"+toggledGroups[editorIds[i]]);
                }
            }
            tgString=rowArray.join(";");

            var aoString ="";
            rowArray = new Array();
            if (allOpened != null) {
                for (var i=0; i<editorIds.length;i++) {
                     if (allOpened[editorIds[i]] != null)
                        rowArray.push(editorIds[i] + ":"+allOpened[editorIds[i]]);
                }
            }
            aoString=rowArray.join(";");
            document.getElementById(formPrefix+":"+"__foldedGroups_toggledGroups").value=tgString;
            document.getElementById(formPrefix+":"+"__foldedGroups_allOpened").value=aoString;
        }
    };

     /** restore saved group folding **/
    this.restoreGroupFolding=function(formPrefix) {
        if (allOpened == null && toggledGroups == null) {
            var tgString = document.getElementById(formPrefix+":"+"__foldedGroups_toggledGroups").value;
            if (tgString != "") {
                var editorRowArray=tgString.split(';');
                for (var i=0;i<editorRowArray.length;i++) {
                    var keyVal=editorRowArray[i].split(':');
                    if (keyVal.length == 2) {
                        var values = keyVal[1].split(',');
                        if (toggledGroups == null) {
                            toggledGroups=new Array();
                        }
                        if (values.length>0) {
                            toggledGroups[keyVal[0]]=new Array();
                        }
                        for (var j=0;j<values.length;j++) {
                            if (values[j] !="") {
                                toggledGroups[keyVal[0]].push(values[j]);
                            }
                        }
                    }
                }
            }
            var aoString=document.getElementById(formPrefix+":"+"__foldedGroups_allOpened").value;
            if (aoString != "") {
                var editorRowArray=aoString.split(';');
                for (var i=0;i<editorRowArray.length;i++) {
                    var keyVal=editorRowArray[i].split(':');
                    if (keyVal.length == 2) {
                        var values = keyVal[1].split(',');
                        if (allOpened == null) {
                            allOpened=new Array();
                        }
                        if (values.length>0) {
                            allOpened[keyVal[0]]=new Array();
                        }
                        for (var j=0;j<values.length;j++) {
                            if (values[j] !="") {
                                allOpened[keyVal[0]].push(values[j]);
                            }
                        }
                    }
                }
            }
        }
    };   

    this.applyGroupFolding=function(formPrefix, editorId) {
        flexive.contentEditor.restoreGroupFolding(formPrefix);
        if (groupIds != null && groupIds[editorId] != null) {
            for (var i=0;  i<groupIds[editorId].length;i++) {
                if (flexive.contentEditor.isGroupFolded(editorId,groupIds[editorId][i]))
                    var ele = document.getElementById(groupIds[editorId][i]);
                    if (ele != null) {
                        ele.style.display= "none";
                    }
            }
        }
    };

    this.registerEditorIds=function(editorId) {
        if (editorIds == null)
            editorIds=new Array();
        editorIds.push(editorId);
    };

    this.clearGroupIds=function(editorId) {
        flexive.contentEditor.registerEditorIds(editorId);
        if (groupIds != null && groupIds[editorId] != null)
            groupIds[editorId] = null;
    };

    this.registerGroupId=function(editorId, groupId) {
        if (groupIds == null)
            groupIds = new Array();
        if (groupIds[editorId] == null)
            groupIds[editorId] = new Array();
        groupIds[editorId].push(groupId);
    };

    this.isGroupFolded=function(editorId, groupId) {
        if (allOpened == null)
            allOpened = new Array();
        if (allOpened[editorId]==null)
            allOpened[editorId]=true;
        if (allOpened[editorId])
            return flexive.contentEditor.isGroupToggled(editorId, groupId);
        else
            return !flexive.contentEditor.isGroupToggled(editorId, groupId);
    };

    this.isGroupToggled=function(editorId, groupId) {
        if (toggledGroups == null)
            return false;
        if (toggledGroups[editorId] != null) {
            for (var i=0; i<toggledGroups[editorId].length;i++) {
                if (toggledGroups[editorId][i] == groupId)
                    return true;
            }
        }
        return false;
    };

    this.toggleGroups=function(editorId, open) {
        if (allOpened == null)
            allOpened = new Array();
        allOpened[editorId]=open;

        if (toggledGroups != null && toggledGroups[editorId]!=null)
            toggledGroups[editorId]=null;

        if (groupIds != null && groupIds[editorId] != null) {
            for (var i=0; i<groupIds[editorId].length;i++) {
                var ele = document.getElementById(groupIds[editorId][i]);
                if (ele != null) {
                    ele.style.display= open ? "block" :"none";
                }
            }
        }
    };

    this.toggleGroup=function(editorId, groupId) {
        var wasToggled=flexive.contentEditor.isGroupToggled(editorId, groupId);
        if (toggledGroups == null) {
            toggledGroups = new Array();
        }
        if (toggledGroups[editorId] == null) {
            toggledGroups[editorId] = new Array();
        }
        if (!wasToggled) {
            toggledGroups[editorId].push(groupId);
        }
        else {
            for (var i=0; i<toggledGroups[editorId].length;i++) {
                if (toggledGroups[editorId][i] == groupId) {
                    toggledGroups[editorId].splice(i,1);
                    break;
                }
            }
        }
        var ele = document.getElementById(groupId);
        if (ele != null) {
            ele.style.display= flexive.contentEditor.isGroupFolded(editorId,groupId) ? "none" : "block";
        }
    };

    this.ignoreEvent=function(e) {
        if (!e) return;
        if (e == null) return;
        try {
            e.cancelBubble = true;
            return;
        } catch(e) {
            /* ignore, bubble is IE specific */
        }
        e.stopPropagation();
    };

    this.closeMenu =function(e,force) {
        flexive.contentEditor.ignoreEvent(e);
        var delta = ((new Date().getTime())-activationTime);
        if (activeMenu!=null) {
            if (force || delta>200) {
                activeMenu.style.display='none';
                activeMenu=null;
                activationTime=-1;
            }
        }
    };

    this.showMenu = function(e,caller,menuId) {
        flexive.contentEditor.ignoreEvent(e);
        this.closeMenu(e,true);
        try {
            activeMenu = document.getElementById(menuId);
        } catch(e) {
            activeMenu = null;
        }
        if (activeMenu==null) {
            //TODO:migrate alertDialog
            alertDialog("Menu is missing for id:"+menuId);
            return;
        }

        activeMenu.style.top=caller.offsetTop+"px";
        activeMenu.style.left=caller.offsetLeft+"px";
        activeMenu.style.display='inline';
        activationTime = new Date().getTime();
    };

    this.prepareAdd =function(formPrefix, listBox) {
        if (formPrefix == null)
            formPrefix ="";

        var ele = document.getElementById(formPrefix+":"+listBox);
        var value = "";
        for (var i = 0; i < ele.length; i++) {
            if (ele.options[i].selected) {
                value+=((value=="")?"":",")+ele.options[i].value;
            }
        }
        document.getElementById(formPrefix +":"+"__ceElements").value=value;
        return true;
    };

    var dirtyFileInputs = false;
    this.fileInputChanged =function() {
        dirtyFileInputs = true;
    };

    this.saveHtmlEditors =function() {
        try {
            tinyMCE.triggerSave();
        } catch (e) {
            alert ('Failed to save HTML editors: ' + e);
        }
        var i = 10000;   // avoid looping forever if tinyMCE fails to clear the editor array
        while (tinyMCE.editors.length > 0 && i-- > 0) {
            var editor = tinyMCE.editors[0];
            try {
                editor.remove();
            } catch (e) {
                alert("Failed to remove editor " + editor + ": " + e);
            }
        }
    };

    this.preSubmit =function() {
        this.saveHtmlEditors();
    };

    this.preA4jAction =function(formPrefix,storageKey,xpath,action) {
        //try {
            this.preSubmit();
        //}
        /*
        catch (e) {
            alert("saveHtmlEditors() Error: "+e+
                  "\nHint: most likey flexive.contentEditor.saveHtmlEditors() " +
                  "was not called before sending an Ajax Request " +
                  "\nor <fx:includes htmlEditor='true'> is missing in document head");
        }
        */
        if (formPrefix == null)
            formPrefix ="";

        if (dirtyFileInputs) {
            // If a file input was changed we need to submit the whole form, since a4j XhtmlHttpRequests
            // are not able to process binaries. The intended a4j action is stored in form variables
            document.getElementById(formPrefix+":"+"__ceStorageKey").value=storageKey;
            document.getElementById(formPrefix+":"+"__ceNextA4jAction").value=action;
            document.getElementById(formPrefix+":"+"__ceActionXpath").value=xpath;
            flexive.contentEditor.saveGroupFolding(formPrefix);
            // To submit the whole form a hidden command button is pressed
            document.getElementById(formPrefix+":"+"__ceResolveA4jAction").click();
            return false;
        } else {
            // If no file input was changed we can use the a4j XhtmlHttpRequest for submiting the
            // action and data.
            return true;
        }
    };

    this.highlightAffectedXpath=function(formPrefix,editorId) {
        var encXpath = document.getElementById(formPrefix+":"+"__affectedXPath").value;
        if (encXpath != null && encXpath !="") {
            var xpelements=encXpath.split(":");
            if (xpelements.length ==2) {
               var eId=xpelements[0];
               var xpath=xpelements[1];
               if (eId == editorId) {
                   var affectedDiv = document.getElementById(formPrefix+":"+editorId+"_"+xpath);
                    if (affectedDiv != null) {
                        affectedDiv.className= "fxContentEditor_xpathError";
                    }
                   //document.getElementById(formPrefix+":"+"__affectedXPath").value=null;
               }
            }
        }
    };
});
