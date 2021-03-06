/***************************************************************
 *  This file is part of the [fleXive](R) backend application.
 *
 *  Copyright (c) 1999-2014
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) backend application is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/licenses/gpl.html.
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

package com.flexive.faces.components.content;

import com.flexive.faces.beans.FxContentEditorBean;
import com.flexive.faces.beans.MessageBean;
import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.FxLock;
import com.flexive.shared.FxLockType;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.content.FxPermissionUtils;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLockException;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.security.UserTicket;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.value.renderer.FxValueFormatter;
import com.sun.facelets.FaceletContext;
import com.sun.facelets.FaceletException;
import com.sun.facelets.el.VariableMapperWrapper;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.tag.TagHandler;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.el.ELException;
import javax.el.VariableMapper;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

/**
 * This tag handler creates or fetches a {@link FxWrappedContent}
 * instance that was previously stored in the view-state and provides it within its
 * tag body to the content editor template. The wrapped content instance is
 * created only <b>once</b> and kept in the view-state to preserve changes over
 * multiple requests. After creation, wrapped content instances are fetched from the
 * view-state with the given <em>editorId</em> as key.
 * <p/>
 * In order to edit different contents subsequently in the <b>same</b> view
 * with the <b>same</b> editorId, the tag handler makes a "best effort" to react
 * to external changes by comparing the retrieved wrapped content instance with
 * the given attributes. If i.e. the pk is different, or the given content id
 * does not match the stored content id, the tag handler automatically reinitializes
 * the stored wrapped content instance to match the given attributes.<br/>
 * The user may also trigger a reinitialisation manually by either setting the
 * tag handler's<em>reset</em> attribute, or by calling
 * {@link FxWrappedContent#setReset(boolean)}.
 * <p/>
 * The tag handler already provides the wrapped content instance at
 * compile-time, which enables us to render the content's groups
 * and properties recursively with Facelets-templates, which would
 * be impossible at render-time.
 * <p/>
 * <p/>
 * Attributes:<br/>
 * <b>editorId</b> (required)...   Id of the content editor component (must be unique in a view,
 * used as key to retrieve a specific content instance stored in view-state).<br/>
 * <b>formPrefix</b> (required)... The form prefix of the surrounding form.<br/>
 * <b>contentId</b> (optional)...  Id of the content to edit (maximum available version is loaded).<br/>
 * <b>editMode</b> (optional, default: false) ...If true initilaizes the content editor to edit mode, otherwise to view mode.<br/>
 * <b>pk</b> (optional)...         FxPk of the content to edit.<br/>
 * <b>content</b> (optional)...    Content instance to edit.<br/>
 * <b>typeId</b> (optional)...     Initializes content editor to create a new content of the given type id.<br/>
 * <b>type</b> (optional)...       Initializes content editor to create a new content of the given type.<br/>
 * <b>var</b> (optional)...        Variable name to expose the wrapped content instance inside the tag's body.<br/>
 * <b>reset</b> (optional)...      True if the content for the given editor id should be reset (==removed from content storage and reinitilaized with given attributes, default:false).<br/>
 * <b>reRender</b> (optional)...   JSF-component id to rerender after AJAX-requests (useful for multiple content ediors in the same view).<br/>
 * <b>valueFormatter</b> (optional)...  A custom FxValueFormatter for the rendering of FxValues in the content editor template.<br/>
 * <b>disableAcl</b> (optional, default: false)... True if ACL selectbox should not be rendered.<br/>
 * <b>disableWorkflow</b> (optional, default: false)... True if workflow selct box should not be rendered.<br/>
 * <b>disableEdit</b> (optional, default: false)... True if "edit" button should not be rendered.<br/>
 * <b>disableDelete</b> (optional, default: false)... True if "delete" button should not be rendered.<br/>
 * <b>disableVersion</b> (optional, default: false)... True if "save in new version" button and "delete version" button should not be rendered.<br/>
 * <b>disableCompact</b> (optional, default: false)... True if "compact" button should not be rendered.<br/>
 * <b>disableSave</b> (optional, default: false)... True if "save" button should not be rendered.<br/>
 * <b>disableCancel</b> (optional, default: false)... True if "cancel" button should not be rendered.<br/>
 * <b>disableButtons</b> (optional, default: false)... True if no buttons should be rendered.<br/>
 * <b>disableAddAssignment</b> (optional, default: false)... True if icons for adding/inserting assignments should not be rendered.<br/>
 * <b>disableRemoveAssignment</b> (optional, default: false)... True if icons for removing assignments should not be rendered.<br/>
 * <b>disablePositionAssignment</b> (optional, default: false)... True if icons for positioning assignments should not be rendered.<br/>
 * <b>disableMessages</b> (optional, default: false)... True if the &lt;h:messages/&gt; tag inside the content editor should not be rendered.<br/>
 * <b>showLockOwner</b> (optional, default: false)...True if the owner of the lock should be shown.<br/>
 */
public class FxProvideContent extends TagHandler {
    private static final Log LOG = LogFactory.getLog(FxProvideContent.class);

    public FxProvideContent(TagConfig tagConfig) {
        super(tagConfig);
    }

    /**
     * Resolves value bindings and returns true if the attribute's value is not empty.
     *
     * @param ctx           facelet context
     * @param attributeName attribute name
     * @return true if the attribute's value is not empty.
     */
    private boolean isAttributeSet(FaceletContext ctx, String attributeName) {
        return getAttribute(attributeName) != null && !StringUtils.isEmpty(getAttribute(attributeName).getValue(ctx));
    }

    @Override
    public void apply(FaceletContext ctx, UIComponent uiComponent) throws IOException, FaceletException, ELException {
        final String id = getRequiredAttribute("editorId").getValue(ctx);
        final Long contentId = isAttributeSet(ctx, "contentId") ? Long.parseLong(getAttribute("contentId").getValue(ctx)) : null;
        final Boolean editMode = isAttributeSet(ctx, "editMode") && Boolean.valueOf(getAttribute("editMode").getValue(ctx));
        final FxPK pk = isAttributeSet(ctx, "pk") ? FxPK.fromObject(getAttribute("pk").getObject(ctx)) : null;
        final FxContent content = isAttributeSet(ctx, "content") ? (FxContent) getAttribute("content").getObject(ctx, FxContent.class) : null;
        final String formPrefix = getRequiredAttribute("formPrefix").getValue(ctx);
        final Long typeId = isAttributeSet(ctx, "typeId") ? Long.parseLong(getAttribute("typeId").getValue(ctx)) : null;
        final FxType type = isAttributeSet(ctx, "type") ? (FxType) getAttribute("type").getObject(ctx, FxType.class) : null;
        final String var = isAttributeSet(ctx, "var") ? getAttribute("var").getValue(ctx) : null;
        final Boolean reset = isAttributeSet(ctx, "reset") && Boolean.valueOf(getAttribute("reset").getValue(ctx));
        final String reRender = isAttributeSet(ctx, "reRender") ? getAttribute("reRender").getValue(ctx) : null;
        final FxValueFormatter valueFormatter = isAttributeSet(ctx, "valueFormatter") ? (FxValueFormatter) getAttribute("valueFormatter").getObject(ctx, FxValueFormatter.class) : null;
        // retrieve content editor bean
        final FxContentEditorBean contentEditor = (FxContentEditorBean) ctx.getExpressionFactory().createValueExpression(ctx, "#{" + FxContentEditorBean.getBeanName() + "}", FxContentEditorBean.class).getValue(ctx);
        //disable ACL selection
        final Boolean disableAcl = isAttributeSet(ctx, "disableAcl") && Boolean.valueOf(getAttribute("disableAcl").getValue(ctx));
        //disable WF-Step selection
        final Boolean disableWorkflow = isAttributeSet(ctx, "disableWorkflow") && Boolean.valueOf(getAttribute("disableWorkflow").getValue(ctx));
        //disable Buttons
        final Boolean disableEdit = isAttributeSet(ctx, "disableEdit") && Boolean.valueOf(getAttribute("disableEdit").getValue(ctx));
        final Boolean disableDelete = isAttributeSet(ctx, "disableDelete") && Boolean.valueOf(getAttribute("disableDelete").getValue(ctx));
        final Boolean disableVersion = isAttributeSet(ctx, "disableVersion") && Boolean.valueOf(getAttribute("disableVersion").getValue(ctx));
        final Boolean disableCompact = isAttributeSet(ctx, "disableCompact") && Boolean.valueOf(getAttribute("disableCompact").getValue(ctx));
        final Boolean disableSave = isAttributeSet(ctx, "disableSave") && Boolean.valueOf(getAttribute("disableSave").getValue(ctx));
        final Boolean disableCancel = isAttributeSet(ctx, "disableCancel") && Boolean.valueOf(getAttribute("disableCancel").getValue(ctx));
        final Boolean disableButtons = isAttributeSet(ctx, "disableButtons") && Boolean.valueOf(getAttribute("disableButtons").getValue(ctx));
        //disable assignment actions
        final Boolean disableAddAssignment = isAttributeSet(ctx, "disableAddAssignment") && Boolean.valueOf(getAttribute("disableAddAssignment").getValue(ctx));
        final Boolean disableRemoveAssignment = isAttributeSet(ctx, "disableRemoveAssignment") && Boolean.valueOf(getAttribute("disableRemoveAssignment").getValue(ctx));
        final Boolean disablePositionAssignment = isAttributeSet(ctx, "disablePositionAssignment") && Boolean.valueOf(getAttribute("disablePositionAssignment").getValue(ctx));
        //disable rendering of "h:messages" inside the template
        final Boolean disableMessages = isAttributeSet(ctx, "disableMessages") && Boolean.valueOf(getAttribute("disableMessages").getValue(ctx));
        final Boolean disableReferenceEditor = isAttributeSet(ctx, "disableReferenceEditor") && Boolean.valueOf(getAttribute("disableReferenceEditor").getValue(ctx));
        // LOCKS
        final Boolean askLockedMode = isAttributeSet(ctx, "askLockedMode") && Boolean.valueOf(getAttribute("askLockMode").getValue(ctx));
        final Boolean lockedContentOverride = isAttributeSet(ctx, "lockedContentOverride") && Boolean.valueOf(getAttribute("lockedContentOverride").getValue(ctx));
        final Boolean cannotTakeOverPermLock = isAttributeSet(ctx, "cannotTakeOverPermLock") && Boolean.valueOf(getAttribute("cannotTakeOverPermLock").getValue(ctx));
        final Boolean askCreateNewVersion = isAttributeSet(ctx, "askCreateNewVersion") && Boolean.valueOf(getAttribute("askCreateNewVersion").getValue(ctx));
        final String lockStatus = isAttributeSet(ctx, "lockStatus") ? getAttribute("lockStatus").getValue(ctx) : null;
        final String lockStatusTooltip = isAttributeSet(ctx, "lockStatusTooltip") ? getAttribute("lockStatusTooltip").getValue(ctx) : null;
        final Boolean contentLocked = isAttributeSet(ctx, "contentLocked") && Boolean.valueOf(getAttribute("contentLocked").getValue(ctx));
        final Boolean looseLock = isAttributeSet(ctx, "looseLock") && Boolean.valueOf(getAttribute("looseLock").getValue(ctx));
        final Boolean permLock = isAttributeSet(ctx, "permLock") && Boolean.valueOf(getAttribute("permLock").getValue(ctx));
        final Boolean takeOver = isAttributeSet(ctx, "takeOver") && Boolean.valueOf(getAttribute("takeOver").getValue(ctx));
        final Boolean userMayTakeover = isAttributeSet(ctx, "userMayTakeover") && Boolean.valueOf(getAttribute("userMayTakeover").getValue(ctx));
        final Boolean userMayUnlock = isAttributeSet(ctx, "userMayUnlock") && Boolean.valueOf(getAttribute("userMayUnlock").getValue(ctx));
        final Boolean userMayLooseLock = isAttributeSet(ctx, "userMayLooseLock") && Boolean.valueOf(getAttribute("userMayLooseLock").getValue(ctx));
        final Boolean userMayPermLock = isAttributeSet(ctx, "userMayPermLock") && Boolean.valueOf(getAttribute("userMayPermLock").getValue(ctx));
        final String closePanelScript = isAttributeSet(ctx, "closePanelScript") ? getAttribute("closePanelScript").getValue(ctx) : "";
        final Boolean showLockOwner = isAttributeSet(ctx, "showLockOwner") ? Boolean.valueOf(getAttribute("showLockOwner").getValue(ctx)) : true;
        @SuppressWarnings("unchecked")
        final Collection<Long> hiddenAssignments = isAttributeSet(ctx, "hiddenAssignments") ? (Collection<Long>) getAttribute("hiddenAssignments").getObject(ctx) : null;
        @SuppressWarnings("unchecked")
        final Collection<Long> hiddenProperties = isAttributeSet(ctx, "hiddenProperties") ? (Collection<Long>) getAttribute("hiddenProperties").getObject(ctx) : null;

        String lockOwner = "";

        //encapsulate gui-relevant attributes in wrapper object
        FxWrappedContent.GuiSettings guiSettings = new FxWrappedContent.GuiSettings(editMode, disableAcl,
                disableWorkflow, disableEdit, disableDelete, disableVersion, disableCompact, disableSave,
                disableCancel, disableButtons, disableAddAssignment, disableRemoveAssignment,
                disablePositionAssignment, disableMessages, formPrefix, reRender, valueFormatter,
                hiddenAssignments, hiddenProperties,
                askLockedMode,
                lockedContentOverride, cannotTakeOverPermLock, askCreateNewVersion, lockStatus, lockStatusTooltip,
                contentLocked, looseLock, permLock, takeOver, userMayTakeover, userMayUnlock, userMayLooseLock, userMayPermLock,
                disableReferenceEditor, showLockOwner, lockOwner);

        if (contentEditor.getContentStorage() == null) {
            contentEditor.setContentStorage(new HashMap<String, FxWrappedContent>(3));
        }

        FxWrappedContent wc = null;
        // boolean indicating if an error happened
        boolean error = false;

        // check reload flag. if set, remove content from storage so that it is automatically reinitialized and reloaded
        if (contentEditor.getContentStorage().containsKey(id) && (contentEditor.getContentStorage().get(id).isReset() || reset)) {
            contentEditor.getContentStorage().remove(id);
        }

        if (!contentEditor.getContentStorage().containsKey(id) && contentId == null && pk == null && content == null && typeId == null && type == null) {
            throw new FacesException("provide one of the following attributes: contentId, pk, content, typeId, type");
        }

        if (pk != null && pk.isNew()) {
            throw new FacesException("pk may not be new: only pks of existing content instances are allowed");
        }

        try {
            // if content not existing in storage,
            // either create a new one or load one and put into storage
            if (!contentEditor.getContentStorage().containsKey(id)) {
                // create new content
                if (typeId != null || type != null) {
                    wc = new FxWrappedContent(EJBLookup.getContentEngine().initialize(type != null ? type.getId() : typeId),
                            id, guiSettings, false);
                    guiSettings.setUserMayUnlock(true);
                } else {
                    // edit given content instance
                    FxContent storedCo = content;
                    if (storedCo == null) {
                        if (pk != null) {
                            storedCo = EJBLookup.getContentEngine().load(pk);
                        } else {
                            storedCo = EJBLookup.getContentEngine().load(new FxPK(contentId, FxPK.MAX));
                        }
                    }
                    wc = new FxWrappedContent(storedCo, id, guiSettings, false);
                }
                contentEditor.getContentStorage().put(id, wc);
            } else {
                wc = contentEditor.getContentStorage().get(id);
                boolean externalChanges = false;
                // react to possible external changes
                // given id doesn't match stored content id -> update to latest version of content with given id
                if (contentId != null && wc.getContent().getPk().getId() != contentId) {
                    wc = new FxWrappedContent(EJBLookup.getContentEngine().load(new FxPK(contentId, FxPK.MAX)), id, guiSettings, false);
                    externalChanges = true;
                }
                // given pk or version doesn't match stored content pk or version -> update to given pk
                else if (pk != null && !wc.getContent().matchesPk(pk)) {
                    wc = new FxWrappedContent(EJBLookup.getContentEngine().load(pk), id, guiSettings, false);
                    externalChanges = true;
                }
                // given content pk doesn't match stored content pk -> update to given content
                else if (content != null && !content.matchesPk(wc.getContent().getPk())) {
                    wc = new FxWrappedContent(content, id, guiSettings, false);
                    externalChanges = true;
                }
                // given type doesn't match content type -> reinitialize content for new type
                else if (type != null && wc.getContent().getTypeId() != type.getId()) {
                    wc = new FxWrappedContent(EJBLookup.getContentEngine().initialize(type.getId()), id, guiSettings, false);
                    guiSettings.setUserMayUnlock(true);
                    externalChanges = true;
                }
                // given type doesn't match content type -> reinitialize content for new type
                else if (typeId != null && wc.getContent().getTypeId() != typeId) {
                    wc = new FxWrappedContent(EJBLookup.getContentEngine().initialize(typeId), id, guiSettings, false);
                    guiSettings.setUserMayUnlock(true);
                    externalChanges = true;
                }
                if (externalChanges) {
                    contentEditor.getContentStorage().put(id, wc);
                }
            }
            if (wc != null && !wc.isNew() && wc.getGuiSettings().isEditMode()) {
                //lock the content is in edit mode with a loose lock
                final ContentEngine ce = EJBLookup.getContentEngine();
                final UserTicket ticket = FxContext.getUserTicket();
                final FxLock lock = wc.getContent().getLock();
                try {
                    if (!wc.getContent().isLocked()) {
                        wc.getContent().updateLock(ce.lock(FxLockType.Loose, wc.getContent().getPk()));

                    } else if (lock.getUserId() != ticket.getUserId() && !wc.getGuiSettings().isAskLockedMode()
                            && !wc.getGuiSettings().isLockedContentOverride() && !wc.getGuiSettings().isTakeOver()) {
                        new FxFacesMsgErr("ex.lock.content.locked" +
                                (lock.getLockType() == FxLockType.Permanent ? ".permanent" : ".loose")).addToContext();
                        wc.getGuiSettings().setEditMode(false);
                    }
                } catch (FxLockException e) {
                    new FxFacesMsgErr(e).addToContext();
                }

                setLockStatusMessage(wc);

            } else if ((wc != null && !wc.isNew())) {
                setLockStatusMessage(wc);
            }

        } catch (FxApplicationException e) {
            new FxFacesMsgErr(e).addToContext(formPrefix + ":" + id + "_" + FxContentEditorBean.MESSAGES_ID);
            error = true;
            //make GuiSettings available, even if an error happened
            if (wc == null)
                wc = new FxWrappedContent(null, id, guiSettings, false);
        }

        wc.setClosePanelScript(closePanelScript);

        final VariableMapper origMapper = ctx.getVariableMapper();
        final VariableMapperWrapper mapper = new VariableMapperWrapper(origMapper);
        // check if a previous content editor tag is already existing in this form and
        // put result in context map
        if (!ctx.getFacesContext().getExternalContext().getRequestMap().containsKey(formPrefix + "__ceRendered"))
            ctx.getFacesContext().getExternalContext().getRequestMap().put(formPrefix + "__ceRendered", false);
        //put wrapped conent into request map additionally
        //TODO: check possible side effects
        if (var != null) {
            ctx.getFacesContext().getExternalContext().getRequestMap().put(var, wc);
        }
        try {
            ctx.setVariableMapper(mapper);
            // make bean and id value available
            mapper.setVariable("__ceBean", ctx.getExpressionFactory().createValueExpression(ctx, "#{" + FxContentEditorBean.getBeanName() + "}", FxContentEditorBean.class));
            // make the content instance available internally
            mapper.setVariable("_ceContent", ctx.getExpressionFactory().createValueExpression(wc, FxWrappedContent.class));
            // make the content instance available for user
            if (var != null)
                mapper.setVariable(var, ctx.getExpressionFactory().createValueExpression(wc, FxWrappedContent.class));
            // signify that an error has happened while creating/loading content
            mapper.setVariable("__ceError", ctx.getExpressionFactory().createValueExpression(error, Boolean.class));
            this.nextHandler.apply(ctx, uiComponent);
        } finally {
            ctx.setVariableMapper(origMapper);
            //signify that one content editor has already been rendered in this form
            ctx.getFacesContext().getExternalContext().getRequestMap().put(formPrefix + "__ceRendered", true);
        }
    }

    /**
     * Set the lock status and the related lock status message
     *
     * @param wc the FxWrappedContent
     */
    private void setLockStatusMessage(FxWrappedContent wc) {
        final FxLock lock = wc.getContent().getLock();
        final UserTicket ticket = FxContext.getUserTicket();

        if (lock.getLockType() == FxLockType.Permanent) {
            if (ticket.getUserId() == lock.getUserId()) {
                setLockStatus(wc, false, true, true, false, "ContentEditor.msg.permLock",
                        "ContentEditor.msg.permLock.tooltip", "ContentEditor.msg.lockYou.tooltip");
            } else {
                setLockStatus(wc, false, true, true, false, "ContentEditor.msg.permLock", "ContentEditor.msg.permLock.tooltip", "ContentEditor.msg.lockOther.tooltip");
            }
            setLockOwner(wc);
        } else if (lock.getLockType() == FxLockType.Loose) {
            if (ticket.getUserId() == lock.getUserId() && !wc.getGuiSettings().isTakeOver()) {
                setLockStatus(wc, true, false, true, false, "ContentEditor.msg.looseLock", "ContentEditor.msg.looseLock.tooltip", "ContentEditor.msg.lockYou.tooltip");
            } else {
                setLockStatus(wc, true, false, true, false, "ContentEditor.msg.looseLock", "ContentEditor.msg.looseLock.tooltip", "ContentEditor.msg.lockOther.tooltip");
            }
            setLockOwner(wc);
        } else { // None
            setLockStatus(wc, false, false, false, false, "ContentEditor.msg.noLock", "ContentEditor.msg.noLock.tooltip",  "");
        }
    }

    /**
     * Set the lock owner's login name
     * @param wc FxWrappedContent instance
     */
    private void setLockOwner(FxWrappedContent wc) {
        if(wc.getGuiSettings().isShowLockOwner()) {
            String lockOwner = "";
            try {
                lockOwner = EJBLookup.getAccountEngine().load(wc.getContent().getLock().getUserId()).getLoginName();
                wc.getGuiSettings().setLockOwner(lockOwner);
            } catch (FxApplicationException e) {
                LOG.error("Could not retrieve the lock owner's login name", e.getCause());
            }
        }
    }

    /**
     * @param wc the FxWrappedContent
     * @param looseLock true if the content is loosely locked
     * @param permLock true if the content is permanently locked
     * @param contentLocked true if the content is locked (perm or loose)
     * @param takeOver true if the content was taken over by another user
     * @param messageKeyLS the message key for the lockStatus message
     * @param messageKeyLST the message key for the lockStatusTooltip message
     * @param messageKeyLOWNER the message key for the lock owner ("you" or "another user")
     */
    private void setLockStatus(FxWrappedContent wc, boolean looseLock, boolean permLock, boolean contentLocked,
                                      boolean takeOver, String messageKeyLS, String messageKeyLST,
                                      String messageKeyLOWNER) {
        wc.getGuiSettings().setLooseLock(looseLock);
        wc.getGuiSettings().setPermLock(permLock);
        wc.getGuiSettings().setContentLocked(contentLocked);
        wc.getGuiSettings().setTakeOver(takeOver);

        if (StringUtils.isBlank(messageKeyLS)) {
            wc.getGuiSettings().setLockStatus("");
        } else {
            wc.getGuiSettings().setLockStatus(MessageBean.getInstance().getMessage(messageKeyLS));
        }

        if (StringUtils.isBlank(messageKeyLST)) {
            wc.getGuiSettings().setLockStatusTooltip("");
        } else {
            String msg = MessageBean.getInstance().getMessage(messageKeyLST);
            if(!StringUtils.isBlank(messageKeyLOWNER)) {
                msg = msg + ". " + MessageBean.getInstance().getMessage(messageKeyLOWNER);
            }
            wc.getGuiSettings().setLockStatusTooltip(msg);
        }

        // set the userMayTakeover flag & button display privileges (may unlock, loose lock, permlock)
        final UserTicket ticket = FxContext.getUserTicket();
        final FxLock lock = wc.getContent().getLock();

        if ((ticket.isGlobalSupervisor() || ticket.isMandatorSupervisor()) && ticket.getUserId() != lock.getUserId()
                && lock.getLockType() != FxLockType.None) {
            wc.getGuiSettings().setUserMayTakeover(true);
            wc.getGuiSettings().setUserMayUnlock(true);
            wc.getGuiSettings().setUserMayLooseLock(true);
            wc.getGuiSettings().setUserMayPermLock(true);
            // "normal" user can only override loose locks and if in the same acl
        } else if (FxPermissionUtils.currentUserInACLList(ticket, wc.getContent().getAclIds())
                && lock.getLockType() == FxLockType.Loose && ticket.getUserId() != lock.getUserId()) {
            wc.getGuiSettings().setUserMayTakeover(true);
            wc.getGuiSettings().setUserMayUnlock(true);
            wc.getGuiSettings().setUserMayLooseLock(false);
            wc.getGuiSettings().setUserMayPermLock(true);
        } else if(FxPermissionUtils.currentUserInACLList(ticket, wc.getContent().getAclIds())
            && lock.getLockType() == FxLockType.Permanent && ticket.getUserId() != lock.getUserId()) {
            wc.getGuiSettings().setUserMayTakeover(false);
            wc.getGuiSettings().setUserMayUnlock(false);
            wc.getGuiSettings().setUserMayLooseLock(false);
            wc.getGuiSettings().setUserMayPermLock(false);
        } else { // content is not locked
            wc.getGuiSettings().setUserMayTakeover(false);
            wc.getGuiSettings().setUserMayUnlock(true);
            wc.getGuiSettings().setUserMayLooseLock(true);
            wc.getGuiSettings().setUserMayPermLock(true);
        }
    }
}
