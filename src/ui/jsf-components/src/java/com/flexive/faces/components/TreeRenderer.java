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
package com.flexive.faces.components;

import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.JsfRelativeUriMapper;
import com.flexive.faces.beans.PluginRegistryBean;
import com.flexive.faces.javascript.FxJavascriptUtils;
import com.flexive.faces.javascript.RelativeUriMapper;
import com.flexive.faces.javascript.menu.DojoMenuWriter;
import com.flexive.faces.javascript.tree.TreeNodeWriter;
import com.flexive.faces.plugin.ExtensionPoint;
import com.flexive.faces.plugin.TreePluginExecutor;
import org.apache.commons.lang.StringUtils;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;
import java.io.IOException;
import java.util.*;

public class TreeRenderer extends Renderer {
    /**
     * Property set for dynamic javascript includes - enables embedded tree
     * renderers to use the main tree nodewriter.
     */
    public static final String PROP_NODEWRITER = "treeNodeWriter";

    /**
     * Extended tree writer that injects tree nodes generated by plugins
     * in the rendered tree.
     */
    private static class PluginAwareTreeWriter extends TreeNodeWriter {
        private final Map<String, List<Node>> injectedNodes;
        private final Stack<String> nodeStack = new Stack<String>();

        private PluginAwareTreeWriter(java.io.Writer out, RelativeUriMapper uriMapper, NodeFormatter formatter, Map<String, List<Node>> injectedNodes) throws IOException {
            super(out, uriMapper, formatter);
            this.injectedNodes = new HashMap<String, List<Node>>(injectedNodes);
        }

        @Override
        public void startNode(Node node) throws IOException {
            super.startNode(node);
            nodeStack.push(node.getId());
        }

        @Override
        public void closeChildren() throws IOException {
            writeInjectedNodes();
            super.closeChildren();
        }

        @Override
        public void closeNode() throws IOException {
            if (hasInjectedNodes()) {
                // if there are nodes available, closeChildren() has not been called on this
                // node, thus no children have been rendered yet
                startChildren();
                writeInjectedNodes();
                closeChildren();
            }
            nodeStack.pop();
            super.closeNode();
        }

        @Override
        public void finishResponse() throws IOException {
            writeInjectedNodes();
            super.finishResponse();
        }

        private void writeInjectedNodes() throws IOException {
            final String parent = getCurrentParent();
            if (injectedNodes.get(parent) != null) {
                final List<Node> injected = injectedNodes.remove(parent);
                for (Node node : injected) {
                    writeNode(node);
                }
            }
        }

        private boolean hasInjectedNodes() {
            return injectedNodes.containsKey(getCurrentParent());
        }

        private String getCurrentParent() {
            return nodeStack.isEmpty() ? null : nodeStack.peek();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getRendersChildren() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        Tree tree = (Tree) component;
        ResponseWriter writer = context.getResponseWriter();
        FxJavascriptUtils.beginJavascript(writer);
        final String treeControllerWidget = (tree.isContentTree()
                ? "flexive.widget.FxTreeRpcController" : "dojo.widget.TreeBasicControllerV3");
        writer.write("dojo.addOnLoad(function() {\n");
        FxJavascriptUtils.writeDojoRequires(writer, "dojo.widget.TreeV3",
                StringUtils.replace(treeControllerWidget, "flexive:", ""),
                "dojo.widget.TreeSelectorV3", "dojo.widget.TreeEmphasizeOnSelect",
                "dojo.widget.TreeNodeV3", "dojo.widget.TreeContextMenuV3",
                "dojo.widget.TreeLinkExtension", "dojo.widget.TreeDndControllerV3",
                "dojo.widget.TreeEditor", "dojo.widget.TreeDocIconExtension");
        writer.write("var controller = dojo.widget.createWidget(\""
                + FxJavascriptUtils.getWidgetName(treeControllerWidget) + "\""
                + ",{RpcUrl: \"" + FxJsfUtils.getJsonServletUri() + "\"});\n"
                + "var _listeners = [controller.widgetId];\n"
        );
        addTreeExtension(writer, "selector", "TreeSelectorV3", true, tree.isSelector());
        addTreeExtension(writer, "emphasize", "TreeEmphasizeOnSelect", false, tree.isSelector(), "{selector: selector.widgetId}");
        addTreeExtension(writer, "dnd", "TreeDndControllerV3", true, tree.isDragAndDrop(), "{controller: controller}");
        addTreeExtension(writer, "docIcons", "TreeDocIconExtension", true, tree.isDocIcons());
        if (tree.isEditor()) {
            writer.write("var editor = dojo.widget.createWidget(\"TreeEditor\");\n"
                    + "controller.editor = editor;\n"
                    + "editor.controller = controller;\n");

        }

        final Map<String, List<TreeNodeWriter.Node>> injectedNodes = new HashMap<String, List<TreeNodeWriter.Node>>();
        if (StringUtils.isNotBlank(tree.getExtensionPoint())) {
            // execute plugins, populate injectedNodes map
            TreePluginExecutor executor = new TreePluginExecutor() {
                public void addNode(String parentNode, TreeNodeWriter.Node node) {
                    if (!injectedNodes.containsKey(parentNode)) {
                        injectedNodes.put(parentNode, new ArrayList<TreeNodeWriter.Node>());
                    }
                    injectedNodes.get(parentNode).add(node);
                }
            };
            PluginRegistryBean.getInstance().execute(new ExtensionPoint<TreePluginExecutor>(tree.getExtensionPoint()) {
            }, executor);
        }

        writer.write("var children = ");
        // Tree JSON representation follows
        TreeNodeWriter nodeWriter = new PluginAwareTreeWriter(context.getResponseWriter(), new JsfRelativeUriMapper(),
                tree.isContentTree()
                        ? TreeNodeWriter.FORMAT_CONTENTTREE : tree.isDocIcons()
                        ? TreeNodeWriter.FORMAT_PLAIN : TreeNodeWriter.FORMAT_ADMINTREE, injectedNodes);
        tree.setNodeWriter(nodeWriter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void encodeChildren(FacesContext context, UIComponent component) throws IOException {
        Tree tree = ((Tree) component);
        TreeNodeWriter nodeWriter = tree.getNodeWriter();
        for (Object child : component.getChildren()) {
            if (child instanceof TreeNode) {
                TreeNode treeNode = (TreeNode) child;
                treeNode.writeTreeNodes(nodeWriter);
            } else if (child instanceof JsonRpcCall) {
                // indirect rendering through json-rpc call 
                FxJsfUtils.getRequest().setAttribute(TreeRenderer.PROP_NODEWRITER, nodeWriter);
                JsonRpcCall rpcCall = (JsonRpcCall) child;
                rpcCall.encodeBegin(context);
                rpcCall.encodeEnd(context);
            } else if (child instanceof UIComponent) {
                UIComponent childComponent = (UIComponent) child;
                childComponent.encodeBegin(context);
                childComponent.encodeChildren(context);
                childComponent.encodeEnd(context);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        Tree tree = (Tree) component;
        ResponseWriter writer = context.getResponseWriter();
        TreeNodeWriter nodeWriter = ((Tree) component).getNodeWriter();

        nodeWriter.finishResponse();

        // close "tree.setChildren"
        writer.write(";\n");

        // init tree reporter (ignoring the context menu listener)
        writer.write("initializeTreeReporter(_listeners, "
                + StringUtils.defaultString(tree.getClickHandler(), "null") + ");\n");
        String widgetId = tree.getName();

        if (tree.getContextMenu() != null) {
            DojoMenuWriter.writeMenu(writer, widgetId + "Menu", "contextMenu", "TreeContextMenuV3", "TreeMenuItemV3",
                    tree.getContextMenu(), new JsfRelativeUriMapper(), null, tree.getContextMenu().getShowHandler());
            // add to tree listeners
            writer.write("_listeners.push(contextMenu.widgetId);\n");
        }

        writer.write(
                "var tree = dojo.widget.createWidget(\"TreeV3\", "
                        + "{widgetId: \"" + widgetId + "\", listeners: _listeners\n"
                        + (tree.isDragAndDrop() ? ", DndAcceptTypes:\"" + widgetId + "\", DndMode:\"between;onto\"" : "")
                        + "});\n"
                        + "tree.fxController = controller;\n"
                        + (tree.isSelector() ? "tree.fxSelector = selector;\n" : "")
                        + (tree.isDragAndDrop() ? "tree.dndController = dnd;\n" : "")
                        + "tree.setChildren(children);\n"
                        + (tree.isExpandFirstNode() ? "if (children.length > 0) tree.children[0].expand();\n" : "")
        );
        writer.write("document.getElementById(\"" + tree.getTargetId() + "\").appendChild(tree.domNode);\n");
        // close "addOnLoad"
        writer.write("});\n");
        FxJavascriptUtils.endJavascript(writer);

    }

    private void addTreeExtension(ResponseWriter writer, String varName, String widget, boolean addToListeners,
                                  boolean rendered, String... args) throws IOException {
        if (!rendered) {
            return;
        }
        writer.write("var " + varName + " = dojo.widget.createWidget(\"" + widget + "\""
                + (args.length > 0 ? "," + StringUtils.join(args, ',') : "") + ");\n");
        if (addToListeners) {
            writer.write("_listeners.push(" + varName + ".widgetId);\n");
        }
    }
}
