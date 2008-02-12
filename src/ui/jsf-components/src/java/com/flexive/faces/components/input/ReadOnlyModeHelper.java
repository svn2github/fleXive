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
package com.flexive.faces.components.input;

import com.flexive.faces.FxJsfUtils;
import com.flexive.shared.FxContext;
import com.flexive.shared.FxFormatUtils;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.XPathElement;
import com.flexive.shared.value.*;
import com.flexive.shared.value.renderer.FxValueRendererFactory;
import com.flexive.war.servlet.ThumbnailServlet;

import javax.faces.component.html.HtmlGraphicImage;
import javax.faces.component.UIComponent;
import javax.faces.context.ResponseWriter;
import java.io.IOException;

/**
 * Renders a FxValue component in read-only mode.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
class ReadOnlyModeHelper extends RenderHelper {

    public ReadOnlyModeHelper(ResponseWriter writer, FxValueInput component, String clientId, FxValue value) {
        super(writer, component, clientId, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void encodeMultiLanguageField() throws IOException {
        encodeField(component, clientId, FxContext.get().getTicket().getLanguage());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({"unchecked"})
    protected void encodeField(UIComponent parent, String inputId, FxLanguage language) throws IOException {
        if (!value.isEmpty()) {
            // TODO: optional "empty" message
            final FxLanguage outputLanguage = FxContext.get().getTicket().getLanguage();
            if (component.getValueFormatter() != null) {
                // use custom formatter
                writer.write(component.getValueFormatter().format(value, value.getBestTranslation(language), outputLanguage));
            } else if (value instanceof FxReference) {
                // render preview
                if (language == null || value.getTranslation(language) != null) {
                    final HtmlGraphicImage image = (HtmlGraphicImage) FxJsfUtils.addChildComponent(component, HtmlGraphicImage.COMPONENT_TYPE);
                    image.setUrl(ThumbnailServlet.getLink(((FxReference) value).getBestTranslation(language), BinaryDescriptor.PreviewSizes.PREVIEW2));
                }
            } else if (value instanceof FxBinary && !value.isEmpty()) {
                // render preview image
                final HtmlGraphicImage image = (HtmlGraphicImage) FxJsfUtils.addChildComponent(component, HtmlGraphicImage.COMPONENT_TYPE);
                final BinaryDescriptor descriptor = ((FxBinary) value).getTranslation(language);
                image.setUrl(ThumbnailServlet.getLink(XPathElement.getPK(value.getXPath()),
                        BinaryDescriptor.PreviewSizes.PREVIEW2, value.getXPath(), descriptor.getCreationTime()));
/*
                final BinaryDescriptor descriptor = ((FxBinary) value).getTranslation(language);
                StringBuilder sb = new StringBuilder(1000);
                String urlThumb = FxJsfUtils.getServletContext().getContextPath() +
                        ThumbnailServlet.getLink(XPathElement.getPK(value.getXPath()),
                                BinaryDescriptor.PreviewSizes.PREVIEW2, value.getXPath(), descriptor.getCreationTime());
                String urlOriginal = FxJsfUtils.getServerURL() + FxJsfUtils.getServletContext().getContextPath() +
                        ThumbnailServlet.getLink(XPathElement.getPK(value.getXPath()),
                                BinaryDescriptor.PreviewSizes.ORIGINAL, value.getXPath(), descriptor.getCreationTime());
                final String text = descriptor.getName()+", "+descriptor.getSize()+" byte"+(descriptor.isImage()
                        ? ", "+descriptor.getWidth()+"x"+descriptor.getHeight()
                        : "");
                sb.append("<a title=\"").append(text).append("\" href=\"").append(urlOriginal).
                        append("\" rel=\"lytebox[ce]\"><img style=\"border-style:none;\" alt=\"").
                        append(text).
                        append("\" src=\"").
                        append(urlThumb).
                        append("\"/></a>");
                writer.write(sb.toString());
*/
            } else if (component.isFilter() && !(value instanceof FxHTML)) {
                // escape HTML code and generate <br/> tags for newlines
                writer.write(FxFormatUtils.escapeForJavaScript(FxValueRendererFactory.getInstance(outputLanguage).format(value, language), true, true));
            } else {
                // write the plain value
                FxValueRendererFactory.getInstance(outputLanguage).render(writer, value, language);
            }
//            final Object translation = language != null ? value.getBestTranslation(language) : value.getDefaultTranslation();
//            if (value instanceof FxHTML) {
//                writer.write((String) translation);
//            } else if (translation instanceof FxSelectListItem) {
//                writer.write(((FxSelectListItem) translation).getLabel().getBestTranslation());
//            } else if (translation instanceof SelectMany) {
//                writer.write(StringUtils.join(((SelectMany) translation).getSelectedLabels().iterator(), ", "));
//            } else {
//                writer.writeText(translation, null);
//            }
        }
    }
}
