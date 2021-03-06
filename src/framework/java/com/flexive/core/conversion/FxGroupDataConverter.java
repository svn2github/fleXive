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
package com.flexive.core.conversion;

import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxData;
import com.flexive.shared.content.FxGroupData;
import com.flexive.shared.content.FxPropertyData;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * XStream converter for FxGroupData
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxGroupDataConverter implements Converter {

    /**
     * {@inheritDoc}
     */
    @Override
    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext ctx) {
        FxGroupData gd = (FxGroupData) o;
        gd.compact(); //make sure all gaps are closed
        writer.startNode(ConversionEngine.KEY_GROUP);
        writer.addAttribute("xpath", gd.getXPathFull());
        if( gd.getPos() >= 0 ) //no position for root group
            writer.addAttribute("pos", String.valueOf(gd.getPos()));

        /* do {
          boolean removed;
          removed = false;
          for (FxData data : gd.getChildren())
              if (data.isEmpty() && data.isRemoveable()) {
                  try {
                      gd.removeChild(data);
                  } catch (FxApplicationException e) {
                      throw e.asRuntimeException();
                  }
                  removed = true;
                  break;
              }
      } while (removed);*/
        for (FxData data : gd.getChildren())
            if (!data.isSystemInternal())
                ctx.convertAnother(data);
        writer.endNode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext ctx) {
        FxContent co = (FxContent)ctx.get(ConversionEngine.KEY_CONTENT);
        String xp = reader.getAttribute("xpath");
        int pos = -1;
        if( reader.getAttribute("pos") != null )
            pos = Integer.valueOf(reader.getAttribute("pos"));
        while( reader.hasMoreChildren() ) {
            reader.moveDown();
            if( ConversionEngine.KEY_PROPERTY.equals(reader.getNodeName())) {
                ctx.convertAnother(this, FxPropertyData.class);
            } else if (ConversionEngine.KEY_GROUP.equals(reader.getNodeName())) {
                unmarshal(reader, ctx);
            }
            reader.moveUp();
        }
        if( pos >= 0 )
            co.getGroupData(xp).setPos(pos);
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canConvert(Class aClass) {
        return FxGroupData.class.isAssignableFrom(aClass);
    }
}
