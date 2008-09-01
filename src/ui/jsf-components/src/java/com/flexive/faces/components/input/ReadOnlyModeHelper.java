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
package com.flexive.faces.components.input;

import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.components.Thumbnail;
import com.flexive.shared.*;
import com.flexive.shared.media.FxMediaSelector;
import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.structure.FxStructureOption;
import com.flexive.shared.value.*;
import com.flexive.shared.value.renderer.FxValueRendererFactory;
import com.flexive.war.servlet.ThumbnailServlet;
import org.apache.commons.lang.StringUtils;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlGraphicImage;
import javax.faces.component.html.HtmlOutputLink;
import javax.faces.component.html.HtmlOutputText;
import java.io.IOException;

/**
 * Renders a FxValue component in read-only mode.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
class ReadOnlyModeHelper extends RenderHelper {

    public ReadOnlyModeHelper(FxValueInput component, String clientId, FxValue value) {
        super(component, clientId, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void encodeMultiLanguageField() throws IOException {
        if (component.isReadOnlyShowTranslations()) {
            //TODO: for now only binaries are implemented correctly
            for (FxLanguage language : FxValueInputRenderer.getLanguages()) {
                if (value.translationExists(language.getId())) {
                    encodeField(component, clientId, language);
                }
            }
        } else {
            encodeField(component, clientId, FxContext.getUserTicket().getLanguage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({"unchecked"})
    protected void encodeField(UIComponent parent, String inputId, FxLanguage language) throws IOException {
        if (value == null || value.isEmpty()) {
            return;
        }
        // TODO: optional "empty" message

        boolean useHTMLEditor;
        if (value instanceof FxString && StringUtils.isNotBlank(value.getXPath())) {
            FxPropertyAssignment pa = (FxPropertyAssignment) CacheAdmin.getEnvironment().getAssignment(value.getXPath());
            useHTMLEditor = pa.getOption(FxStructureOption.OPTION_HTML_EDITOR).isValueTrue();
        } else {
            useHTMLEditor = value instanceof FxHTML; //fallback if no xpath is known, we assume FxHTML to be rendered with an HTML editor
        }

        final FxLanguage outputLanguage = FxContext.getUserTicket().getLanguage();
        if (component.getValueFormatter() != null) {
            // use custom formatter
            addOutputComponent(component.getValueFormatter().format(value, value.getBestTranslation(language), outputLanguage), language);
        } else if (value instanceof FxBinary && !value.isEmpty()) {
            // render preview image
            renderPreviewImage(language);
        } else if (value instanceof FxReference && !value.isTranslationEmpty(language)) {
            // render reference preview
            final HtmlGraphicImage image = (HtmlGraphicImage) FxJsfUtils.addChildComponent(parent, HtmlGraphicImage.COMPONENT_TYPE);
            image.setUrl(ThumbnailServlet.getLink(
                    new FxMediaSelector(((FxReference) value).getDefaultTranslation()).
                            setSize(BinaryDescriptor.PreviewSizes.PREVIEW1).
                            setScaleHeight(16).
                            setScaleWidth(16)
            ));
            // render reference label
            addOutputComponent(FxValueRendererFactory.getInstance(outputLanguage).format(value, language), language);
        } else if (component.isFilter() && !(useHTMLEditor || value instanceof FxHTML)) {
            // escape HTML code and generate <br/> tags for newlines
            addOutputComponent(FxFormatUtils.escapeForJavaScript(FxValueRendererFactory.getInstance(outputLanguage).format(value, language), true, true), language);
        } else {
            // write the plain value
            addOutputComponent(FxValueRendererFactory.getInstance(outputLanguage).format(value, language), language);
        }
    }

    private void addOutputComponent(String value, FxLanguage language) {
        UIComponent parent = component;
        if (component.isReadOnlyShowTranslations() && component.getUIValue().isMultiLanguage()) {
            final ContainerWriter container = new ContainerWriter();
            container.setDisplayLanguage(true);
            container.setLanguage(language);
            container.setInputClientId(clientId);
            parent.getChildren().add(container);
            // use container as parent for all subsequent operations
            parent = container;
        }
        final HtmlOutputText output = (HtmlOutputText) FxJsfUtils.addChildComponent(parent, HtmlOutputText.COMPONENT_TYPE);
        output.setEscape(false);
        output.setValue(value);
    }

    private void renderPreviewImage(FxLanguage language) {
        if (value.isEmpty() || (language != null && !value.translationExists(language.getId()))) {
            return;
        }
        final BinaryDescriptor descriptor = ((FxBinary) value).getTranslation(language);
        if (component.isDisableLytebox()) {
            addImageComponent(component, descriptor, language);
            return;
        }
        final HtmlOutputLink link = (HtmlOutputLink) FxJsfUtils.addChildComponent(component, HtmlOutputLink.COMPONENT_TYPE);
        final String urlOriginal = FxJsfUtils.getServletContext().getContextPath() +
                ThumbnailServlet.getLink(XPathElement.getPK(value.getXPath()),
                        descriptor.isImage() ? BinaryDescriptor.PreviewSizes.ORIGINAL : BinaryDescriptor.PreviewSizes.PREVIEW3,
                        value.getXPath(), descriptor.getCreationTime(), language);
        link.setValue(urlOriginal);
        link.setRel("lytebox[ce]");
        addImageComponent(link, descriptor, language);
        if (component.isReadOnlyShowTranslations()) //TODO: might use another attribute to determine if this should be rendered
            addImageDescriptionComponent(component, language);

        // render lytebox include
        final WebletIncludeWriter includeWriter = new WebletIncludeWriter();
        includeWriter.setWeblets(
                "com.flexive.faces.weblets/js/lytebox.js",
                "com.flexive.faces.weblets/css/lytebox.css"
        );
        component.getChildren().add(includeWriter);
    }

    private void addImageComponent(UIComponent parent, BinaryDescriptor descriptor, FxLanguage language) {
        final HtmlGraphicImage image = (HtmlGraphicImage) FxJsfUtils.addChildComponent(parent, HtmlGraphicImage.COMPONENT_TYPE);
        image.setUrl(ThumbnailServlet.getLink(XPathElement.getPK(value.getXPath()),
                BinaryDescriptor.PreviewSizes.PREVIEW2, value.getXPath(), descriptor.getCreationTime(), language));
        if (component.isReadOnlyShowTranslations()) //TODO: might use another attribute to determine if this should be rendered
            image.setStyle("border: none; padding: 5px;");
        else
            image.setStyle("border: none;");
    }
}
