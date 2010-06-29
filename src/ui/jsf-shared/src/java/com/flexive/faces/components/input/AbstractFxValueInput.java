/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
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
package com.flexive.faces.components.input;

import com.flexive.faces.FxJsfComponentUtils;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.FxContext;
import com.flexive.shared.XPathElement;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.cmis.search.CmisResultSet;
import com.flexive.shared.cmis.search.CmisResultRow;
import com.flexive.shared.structure.*;
import com.flexive.shared.value.FxReference;
import com.flexive.shared.value.FxValue;
import com.flexive.shared.value.FxString;
import com.flexive.shared.value.ReferencedContent;
import com.flexive.shared.value.mapper.IdentityInputMapper;
import com.flexive.shared.value.mapper.InputMapper;
import com.flexive.shared.value.mapper.NumberQueryInputMapper;
import com.flexive.shared.value.mapper.FxPkSelectOneInputMapper;
import com.flexive.shared.value.renderer.FxValueFormatter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import com.flexive.war.servlet.DownloadServlet;

import javax.faces.component.UIInput;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.validator.Validator;
import java.io.IOException;
import javax.faces.component.html.HtmlGraphicImage;

/**
 * Input fields for FxValue variables, including multi-language support.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public abstract class AbstractFxValueInput extends UIInput {
    private static final Log LOG = LogFactory.getLog(AbstractFxValueInput.class);
    
    /**
     * The JSF component type for a FxValue component.
     */
    public static final String COMPONENT_TYPE = "flexive.FxValueInput";
    
    private static final String REQ_ID_COUNTER = "REQ_FXVALUEINPUT_ID";
    /** Request context key for caching the list of allowed reference values */
    private static final String REQ_REFERENCE_ITEMS = "EditModeHelper_References_";

    private Boolean disableMultiLanguage;
    private Long externalId;   // an optional external id for identifying this input (ignoring the clientId)
    private Boolean readOnly;
    private Boolean readOnlyShowTranslations; //show translations in readOnly mode or just default?
    private Boolean forceLineInput;
    private Boolean filter;
    private String containerDivClass; //if present a div is created to surround the FxValueInput using this CSS class
    private InputMapper inputMapper;
    private String autocompleteHandler;
    private String onchange;
    private FxValueFormatter valueFormatter;
    private Boolean disableLytebox;
    private String downloadServletPath;

    private int configurationMask = -1;

    /**
     * Default constructor
     */
    protected AbstractFxValueInput() {
        setRendererType("flexive.FxValueInput");
        applyComponentId();
        addValidator(new FxValueInputValidator());
    }

    /**
     * Return the {@link RenderHelper} for rendering this component.
     *
     * @param context   the Faces context
     * @param value     the value to be rendered
     * @param editMode  if the component should be rendered in edit mode
     * @return          a RenderHelper instance
     */
    public abstract RenderHelper getRenderHelper(FacesContext context, FxValue value, boolean editMode);

    /**
     * Use a packaged image for the given image component.
     *
     * @param imagePath the image path in our component package (e.g. a path served by Weblets)
     */
    protected abstract void setPackagedImageUrl(HtmlGraphicImage imageComponent, String imagePath);


    /**
     * Apply an autogenerated ID - don't use JSF's sequencers since we need the id to be rendered to the client
     */
    private void applyComponentId() {
        final FxContext ctx = FxContext.get();
        if (ctx.getAttribute(REQ_ID_COUNTER) == null) {
            ctx.setAttribute(REQ_ID_COUNTER, 0);
        }
        final Integer idCounter = (Integer) ctx.getAttribute(REQ_ID_COUNTER);
        setId("fvi" + idCounter);
        ctx.setAttribute(REQ_ID_COUNTER, idCounter + 1);
    }

    @Override
    public void validate(FacesContext context) {
        AbstractFxValueInputRenderer.buildComponent(context, this);
        super.validate(context);
    }

    /**
     * Disables multi language support even if the FxValue
     * object is multilingual (e.g. for search query editors).
     *
     * @return if multi language support should be disabled
     */
    public boolean isDisableMultiLanguage() {
        if (disableMultiLanguage == null) {
            return FxJsfComponentUtils.getBooleanValue(this, "disableMultiLanguage", false);
        }
        return disableMultiLanguage;
    }

    public void setDisableMultiLanguage(boolean disableMultiLanguage) {
        this.disableMultiLanguage = disableMultiLanguage;
    }

    /**
     * Sets the component to read-only mode. In read-only mode, the embedded FxValue
     * is written directly to the response.
     *
     * @return true if read-only mode is enabled
     */
    public boolean isReadOnly() {
        if (readOnly == null) {
            return FxJsfComponentUtils.getBooleanValue(this, "readOnly", false);
        }
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    /**
     * Show translations in readOnly mode?
     *
     * @return true if translations are shown
     */
    public boolean isReadOnlyShowTranslations() {
        if (readOnlyShowTranslations == null) {
            return FxJsfComponentUtils.getBooleanValue(this, "readOnlyShowTranslations", false);
        }
        return readOnlyShowTranslations;
    }

    public void setReadOnlyShowTranslations(boolean readOnlyShowTranslations) {
        this.readOnlyShowTranslations = readOnlyShowTranslations;
    }

    /**
     * Get the div CSS Class for the container of this FxValueInput. If <code>null</code> no container div
     * will be created.
     *
     * @return container div CSS class
     */
    public String getContainerDivClass() {
        if (containerDivClass == null) {
            return FxJsfComponentUtils.getStringValue(this, "containerDivClass");
        }
        return containerDivClass;
    }

    public void setContainerDivClass(String containerDivClass) {
        this.containerDivClass = containerDivClass;
    }

    /**
     * Return an arbitrary external ID assigned to this input component. This field
     * can be used for identifying a component independent of its client ID.
     * If the external ID is set (i.e. != -1), it replaces the clientId in the
     * {@link com.flexive.faces.messages.FxFacesMessage#getId()} property when
     * a validation error message is created.
     *
     * @return the external ID assigned to this component, or -1 if there is none
     */
    public long getExternalId() {
        if (externalId == null) {
            externalId = FxJsfComponentUtils.getLongValue(this, "externalId");
        }
        return externalId == null ? -1 : externalId;
    }

    /**
     * Set an external ID used for identifying this component.
     *
     * @param externalId an external ID
     * @see #getExternalId()
     */
    public void setExternalId(long externalId) {
        this.externalId = externalId;
    }

    /**
     * Return true if the input field must be rendered in a single line. For example,
     * HTML editors are disabled by this setting.
     *
     * @return true if the input field must be rendered in a single line.
     */
    public boolean isForceLineInput() {
        if (forceLineInput == null) {
            return FxJsfComponentUtils.getBooleanValue(this, "forceLineInput", false);
        }
        return forceLineInput;
    }

    /**
     * Force single-line rendering for the input component. For example,
     * HTML editors are disabled by this setting.
     *
     * @param forceLineInput true if the component must be rendered in a single line
     */
    public void setForceLineInput(boolean forceLineInput) {
        this.forceLineInput = forceLineInput;
    }

    /**
     * Return true if the output filter is enabled (usually only applies
     * to the component in read-only mode). If enabled, text will be filtered
     * and HTML entities will be used for sensitive characters (e.g. "&amp;gt;" instead of "&gt;").
     *
     * @return true if the output filter is enabled
     */
    public boolean isFilter() {
        if (filter == null) {
            return FxJsfComponentUtils.getBooleanValue(this, "filter", true);
        }
        return filter;
    }

    public void setFilter(boolean filter) {
        this.filter = filter;
    }

    /**
     * Returns true if the <a href="http://www.dolem.com/lytebox/">Lytebox</a> javascript library
     * used for inline previews of images should not be used.
     *
     * @return  true if Lytebox is disabled
     */
    public boolean isDisableLytebox() {
        if (disableLytebox == null) {
            return FxJsfComponentUtils.getBooleanValue(this, "disableLytebox", false);
        }
        return disableLytebox;
    }

    public void setDisableLytebox(boolean disableLytebox) {
        this.disableLytebox = disableLytebox;
    }

    /**
     * Return the input mapper to be used. If no input mapper was defined for the component,
     * the identity mapper is used.
     *
     * @return the input mapper
     */
    public InputMapper getInputMapper() {
        if (inputMapper == null) {
            inputMapper = (InputMapper) FxJsfComponentUtils.getValue(this, "inputMapper");
            if (inputMapper == null) {
                final FxValue fxValue = AbstractFxValueInputRenderer.getFxValue(FacesContext.getCurrentInstance(), this);
                if (fxValue instanceof FxReference && StringUtils.isNotBlank(fxValue.getXPath())) {
                    // fetch property from assignment
                    final FxProperty property;
                    final boolean referenceSelectOne;
                    if (!XPathElement.isValidXPath(fxValue.getXPath())) {
                        //if not a valid xpath, we might have a property
                        if (CacheAdmin.getEnvironment().propertyExists(fxValue.getXPath())) {
                            property = CacheAdmin.getEnvironment().getProperty(fxValue.getXPath());
                            referenceSelectOne = property.getOption(FxStructureOption.OPTION_REFERENCE_SELECTONE).isValueTrue();
                        } else {
                            inputMapper = new IdentityInputMapper();    // use dummy mapper
                            return inputMapper;
                        }
                    } else {
                        final FxPropertyAssignment assignment = (FxPropertyAssignment) CacheAdmin.getEnvironment().getAssignment(fxValue.getXPath());
                        property = assignment.getProperty();
                        referenceSelectOne = assignment.getOption(FxStructureOption.OPTION_REFERENCE_SELECTONE).isValueTrue();
                    }


                    if (referenceSelectOne && !isReadOnly()) {
                        // render a select list with *all* valid choices (i.e. all instances visible to the user)
                        inputMapper = new FxPkSelectOneInputMapper(getValidReferenceList(property));
                    } else {
                        // render a text input with an auto suggest box
                        inputMapper = new NumberQueryInputMapper.ReferenceQueryInputMapper(property);
                    }
                } else {
                    inputMapper = new IdentityInputMapper();    // use dummy mapper
                }
            }
        }
        return inputMapper;
    }

    /**
     * Return a select list with all valid references for a reference property.
     *
     * @param property  a reference property (with datatype FxReference)
     * @return  a select list with all valid references for a reference property.
     */
    private FxSelectList getValidReferenceList(FxProperty property) {
        if (property.getDataType() != FxDataType.Reference) {
            throw new IllegalArgumentException("Invalid data type for reference selectlist: " + property.getDataType());
        }
        
        // cache list for the given type in request since this involves an EJB call
        final String cacheId = REQ_REFERENCE_ITEMS + property.getReferencedType().getName();
        final FxEnvironment env = CacheAdmin.getEnvironment();
        if (FxContext.get().getAttribute(cacheId) == null) {
            // retrieve items
            final FxSelectList list = new FxSelectList("");
            // add empty item
            new FxSelectListItem(-1, "", list, -1, new FxString(false, ""));
            try {
                final CmisResultSet result = EJBLookup.getCmisSearchEngine().search(
                        "SELECT id, cmis:Name, cmis:ObjectTypeId FROM " + property.getReferencedType().getName()
                        + " ORDER BY cmis:Name"
                );
                // create selectlist items
                for (CmisResultRow row : result) {
                    final String label;
                    final String name = row.getColumn(2).getString();
                    if (StringUtils.isBlank(name)) {
                        // caption not defined or not set - use dummy label
                        label = env.getType(row.getColumn(3).getLong()).getDisplayName()
                                + "[id=" + row.getColumn(1).getLong() + "]";
                    } else {
                        label = name;
                    }
                    new FxSelectListItem(
                            row.getColumn(1).getLong(),
                            label, list,
                            -1,
                            new FxString(false, label)
                    );
                }
            } catch (FxApplicationException e) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Failed to determine list of references for " + property.getName() + ": " + e.getMessage(), e);
                }
            }

            // cache in request
            FxContext.get().setAttribute(cacheId, list);
        }

        final FxSelectList list = (FxSelectList) FxContext.get().getAttribute(cacheId);

        // check if the current value is in the list. If not, create a dummy element to avoid accidental change of the value
        final FxReference fxValue = (FxReference) AbstractFxValueInputRenderer.getFxValue(FacesContext.getCurrentInstance(), this);
        if (!fxValue.isEmpty()) {
            for (long languageId : fxValue.getTranslatedLanguages()) {
                final ReferencedContent reference = fxValue.getTranslation(languageId);
                boolean found = false;
                for (FxSelectListItem item : list.getItems()) {
                    if (item.getId() == reference.getId()) {
                        found = true;
                        break;
                    }
                }
                // reference not visible to current user, add dummy entry. This changes the cache list instance too,
                // so it *may* affect other reference inputs that actually have a valid value.
                if (!found) {
                    new FxSelectListItem(
                            reference.getId(),
                            String.valueOf(reference.getId()),
                            list,
                            -1,
                            new FxString(false, String.valueOf(reference.getId()))
                    );
                }
            }
        }
        return list;
    }

    public void setInputMapper(InputMapper inputMapper) {
        this.inputMapper = inputMapper;
    }

    /**
     * Return the (optional) onchange javascript to be called when an
     * input element changed its value.
     *
     * @return the (optional) onchange javascript to be called if an input element changed its value.
     */
    public String getOnchange() {
        if (onchange == null) {
            onchange = FxJsfComponentUtils.getStringValue(this, "onchange");
        }
        return onchange;
    }

    public void setOnchange(String onchange) {
        this.onchange = onchange;
    }

    public String getAutocompleteHandler() {
        if (autocompleteHandler == null) {
            autocompleteHandler = FxJsfComponentUtils.getStringValue(this, "autocompleteHandler");
        }
        if (autocompleteHandler == null && getInputMapper() != null) {
            autocompleteHandler = getInputMapper().getAutocompleteHandler();
        }
        return autocompleteHandler;
    }

    public void setAutocompleteHandler(String autocompleteHandler) {
        this.autocompleteHandler = autocompleteHandler;
    }

    /**
     * Return the (optional) {@link FxValueFormatter} to be used for formatting the output
     * in read-only mode.
     *
     * @return the (optional) {@link FxValueFormatter} to be used for formatting the output
     *         in read-only mode.
     */
    public FxValueFormatter getValueFormatter() {
        if (valueFormatter == null) {
            valueFormatter = (FxValueFormatter) FxJsfComponentUtils.getValue(this, "valueFormatter");
        }
        return valueFormatter;
    }

    public void setValueFormatter(FxValueFormatter valueFormatter) {
        this.valueFormatter = valueFormatter;
    }

    /**
     * Returns the download servlet path ("{@link com.flexive.war.servlet.DownloadServlet#BASEURL}" by default).
     * (The download servlet is used to download binaries
     * from content instances, that are stored in the DB.)
     *
     * @return  the download servlet path.
     */
    public String getDownloadServletPath() {
        if (downloadServletPath == null) {
            downloadServletPath = FxJsfComponentUtils.getStringValue(this, "downloadServletPath", DownloadServlet.BASEURL);
        }
        return downloadServletPath;
    }

    /**
     * Sets the download servlet path (i.e "/download/"). (allows for custom configuration of the
     * download servlet in web.xml)
     *
     * @param downloadServletPath download servlet path
     */
    public void setDownloadServletPath(String downloadServletPath) {
        this.downloadServletPath = downloadServletPath;
    }

    @Override
    public void encodeBegin(FacesContext facesContext) throws IOException {
        super.encodeBegin(facesContext);
        // TODO move to FxValueInputRenderer
        if (StringUtils.isNotBlank(getContainerDivClass())) {
            final ResponseWriter writer = facesContext.getResponseWriter();
            writer.startElement("div", null);
            writer.writeAttribute("class", getContainerDivClass(), null);
            writer.writeAttribute("readonly", String.valueOf(isReadOnly()), null);
        }
    }

    @Override
    public void encodeEnd(FacesContext facesContext) throws IOException {
        if (StringUtils.isNotBlank(getContainerDivClass())) {
            final ResponseWriter writer = facesContext.getResponseWriter();
            writer.endElement("div");
        }
        super.encodeEnd(facesContext);
    }

    @Override
    public String getClientId(FacesContext facesContext) {
        if (getId() != null && getId().startsWith(UIViewRoot.UNIQUE_ID_PREFIX)) {
            // use autogenerated ID - don't use JSF's sequencers since we need the id to be rendered to the client
            applyComponentId();
        }
        return super.getClientId(facesContext);
    }

    /**
     * Return the input's current value and include the specified inputMapper, if present.
     * If an input mapper was used, this component's value is mapped and returned. Otherwise,
     * the component's current value is returned.
     *
     * @return the current value as rendered in the UI
     */
    public FxValue getUIValue() {
        if (getInputMapper() != null) {
            //noinspection unchecked
            return getInputMapper().encode((FxValue) getValue());
        } else {
            return (FxValue) getValue();
        }
    }

    /**
     * Returns the configuration bitmask of the current component.
     *
     * @return  the configuration bitmask of the current component.
     * @see #calcConfigurationMask()
     */
    public int getConfigurationMask() {
        return configurationMask;
    }

    /**
     * Calculates a bitmask for the current component configuration (i.e. read-only mode,
     * line input mode). Used to detect configuration changes between page views by the
     * {@link com.flexive.faces.components.input.FxValueInputRenderer}.
     *
     * @return  the bitmask of the current component configuration
     */
    public int calcConfigurationMask() {
        return (isReadOnly() ? 1 : 0)
                + ((isForceLineInput() ? 1 : 0) << 2)
                + ((isFilter() ? 1 : 0) << 4);
    }

    /**
     * Resets the configuration bitmask.
     *
     * @see #calcConfigurationMask()
     */
    public void resetConfigurationMask() {
        this.configurationMask = calcConfigurationMask();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        int index = 0;
        super.restoreState(context, values[index++]);
        this.disableMultiLanguage = (Boolean) values[index++];
        this.externalId = (Long) values[index++];
        this.readOnly = (Boolean) values[index++];
        this.readOnlyShowTranslations = (Boolean) values[index++];
        this.containerDivClass = (String) values[index++];
        this.forceLineInput = (Boolean) values[index++];
        this.onchange = (String) values[index++];
        this.filter = (Boolean) values[index++];
        this.configurationMask = (Integer) values[index++];
        this.autocompleteHandler = (String) values[index++];
        //noinspection UnusedAssignment
        this.downloadServletPath = (String) values[index++];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object saveState(FacesContext context) {
        // remove our dynamically attached validator before saving, it will be added
        // in the constructor on the next invocation anyway
        for (Validator validator : getValidators()) {
            if (validator instanceof FxValueInputValidator) {
                removeValidator(validator);
            }
        }
        
        // save state
        Object[] values = new Object[12];
        int index = 0;
        values[index++] = super.saveState(context);
        values[index++] = disableMultiLanguage;
        values[index++] = externalId;
        values[index++] = readOnly;
        values[index++] = readOnlyShowTranslations;
        values[index++] = containerDivClass;
        values[index++] = forceLineInput;
        values[index++] = onchange;
        values[index++] = filter;
        values[index++] = configurationMask >= 0 ? configurationMask : calcConfigurationMask();
        values[index++] = autocompleteHandler;
        //noinspection UnusedAssignment
        values[index++] = downloadServletPath;
        return values;
	}
	
	
}
