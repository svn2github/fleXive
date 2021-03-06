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
package com.flexive.faces.plugin;

import net.java.dev.weblets.FacesWebletUtils;

import javax.faces.context.FacesContext;

/**
 * Plugin for submitting feedback.
 *
 * @author Gerhard Glos (gerhard.glos@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */

public class FeedbackPlugin implements Plugin<ToolbarPluginExecutor>{


    @Override
    public void apply(ToolbarPluginExecutor executor) {
        executor.addToolbarSeparatorButton();
        executor.addToolbarButton("*", getGoodFeedbackButton());
        executor.addToolbarButton("*", getModerateFeedbackButton());
        executor.addToolbarButton("*", getBadFeedbackButton());
    }

    private ToolbarPluginExecutor.Button getGoodFeedbackButton() {
        ToolbarPluginExecutor.Button b = new ToolbarPluginExecutor.Button("goodFeedbackButton");
        b.setBean("feedbackPluginBean");
        b.setAction("sendGoodFeedback");
        b.setIconUrl(FacesWebletUtils.getURL(FacesContext.getCurrentInstance(), "feedback_plugin.weblet", "/images/feedback_good.png"));
        b.setLabelKey("FeedbackPlugin.button.tooltip.goodFeedback");
        return b;
    }

    private ToolbarPluginExecutor.Button getModerateFeedbackButton() {
        ToolbarPluginExecutor.Button b = new ToolbarPluginExecutor.Button("moderateFeedbackButton");
        b.setBean("feedbackPluginBean");
        b.setAction("sendModerateFeedback");
        b.setIconUrl(FacesWebletUtils.getURL(FacesContext.getCurrentInstance(), "feedback_plugin.weblet", "/images/feedback_moderate.png"));
        b.setLabelKey("FeedbackPlugin.button.tooltip.moderateFeedback");
        return b;
    }

    private ToolbarPluginExecutor.Button getBadFeedbackButton() {
        ToolbarPluginExecutor.Button b = new ToolbarPluginExecutor.Button("badFeedbackButton");
        b.setBean("feedbackPluginBean");
        b.setAction("sendBadFeedback");
        b.setIconUrl(FacesWebletUtils.getURL(FacesContext.getCurrentInstance(), "feedback_plugin.weblet", "/images/feedback_bad.png"));
        b.setLabelKey("FeedbackPlugin.button.tooltip.badFeedback");
        return b;
    }
}
