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
package com.flexive.shared.value;

import com.flexive.shared.FxFormatUtils;
import com.flexive.shared.exceptions.FxConversionException;
import com.flexive.shared.exceptions.FxInvalidStateException;
import com.flexive.shared.value.renderer.FxValueRendererFactory;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;

/**
 * A multilingual DateTime range, internally represented as java.util.Date; EMPTY is a Date range with a timestamp of <code>0</code> (usually 01/01/1970)
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxDateTimeRange extends FxValue<DateRange, FxDateTimeRange> implements Serializable {
    private static final long serialVersionUID = -6939627748513819758L;

    public final static DateRange EMPTY = new DateRange(new Date(0), new Date(0));

    /**
     * Constructor
     *
     * @param multiLanguage   multilanguage value?
     * @param defaultLanguage the default language
     */
    public FxDateTimeRange(long defaultLanguage, boolean multiLanguage) {
        super(defaultLanguage, multiLanguage);
    }

    /**
     * Constructor
     *
     * @param multiLanguage   multilanguage value?
     * @param defaultLanguage the default language
     * @param translations    HashMap containing language->translation mapping
     */
    public FxDateTimeRange(boolean multiLanguage, long defaultLanguage, Map<Long, DateRange> translations) {
        super(multiLanguage, defaultLanguage, translations);
    }

    /**
     * Constructor
     *
     * @param defaultLanguage the default language
     * @param translations    HashMap containing language->translation mapping
     */
    public FxDateTimeRange(long defaultLanguage, Map<Long, DateRange> translations) {
        super(defaultLanguage, translations);
    }

    /**
     * Constructor
     *
     * @param multiLanguage multilanguage value?
     * @param translations  HashMap containing language->translation mapping
     */
    public FxDateTimeRange(boolean multiLanguage, Map<Long, DateRange> translations) {
        super(multiLanguage, translations);
    }

    /**
     * Constructor
     *
     * @param translations HashMap containing language->translation mapping
     */
    public FxDateTimeRange(Map<Long, DateRange> translations) {
        super(translations);
    }

    /**
     * Constructor - create value from an array of translations
     *
     * @param translations HashMap containing language->translation mapping
     * @param pos          position (index) in the array to use
     */
    public FxDateTimeRange(Map<Long, DateRange[]> translations, int pos) {
        super(translations, pos);
    }

    /**
     * Constructor
     *
     * @param multiLanguage   multilanguage value?
     * @param defaultLanguage the default language
     * @param value           single initializing value
     */
    public FxDateTimeRange(boolean multiLanguage, long defaultLanguage, DateRange value) {
        super(multiLanguage, defaultLanguage, value);
    }

    /**
     * Constructor
     *
     * @param defaultLanguage the default language
     * @param value           single initializing value
     */
    public FxDateTimeRange(long defaultLanguage, DateRange value) {
        super(defaultLanguage, value);
    }

    /**
     * Constructor
     *
     * @param multiLanguage multilanguage value?
     * @param value         single initializing value
     */
    public FxDateTimeRange(boolean multiLanguage, DateRange value) {
        super(multiLanguage, value);
    }

    /**
     * Constructor
     *
     * @param value single initializing value
     */
    public FxDateTimeRange(DateRange value) {
        super(value);
    }

    /**
     * Constructor
     *
     * @param clone original FxValue to be cloned
     */
    public FxDateTimeRange(FxValue<DateRange, FxDateTimeRange> clone) {
        super(clone);
    }


    /**
     * Evaluates the given string value to an object of type T.
     *
     * @param value string value to be evaluated
     * @return the value interpreted as T
     */
    @Override
    public DateRange fromString(String value) {
        try {
            String[] dates = value.split("\\$");
            final DateFormat df = FxValueRendererFactory.getDateTimeFormat();
            return new DateRange(df.parse(dates[0]), df.parse(dates[1]));
        } catch (ParseException e) {
            throw new FxConversionException("ex.conversion.value.error", FxDateTimeRange.class.getCanonicalName(), value,
                    e.getMessage()).asRuntimeException();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStringValue(DateRange value) {
        final DateFormat df = FxValueRendererFactory.getDateTimeFormat();
        return df.format(value.getLower()) + "$" + df.format(value.getUpper());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPortableStringValue(DateRange value) {
        final DateFormat df = FxFormatUtils.getUniversalDateTimeFormat();
        return df.format(value.getLower()) + "$" + df.format(value.getUpper());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DateRange fromPortableString(String value) {
        try {
            String[] dates = value.split("\\$");
            final DateFormat df = FxFormatUtils.getUniversalDateTimeFormat();
            return new DateRange(df.parse(dates[0]), df.parse(dates[1]));
        } catch (ParseException e) {
            throw new FxConversionException("ex.conversion.value.error", FxDateTimeRange.class.getCanonicalName(), value,
                    e.getMessage()).asRuntimeException();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxDateTimeRange copy() {
        return new FxDateTimeRange(this);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected DateRange copyValue(DateRange value) {
        return new DateRange(value.getLower(), value.getUpper());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<DateRange> getValueClass() {
        return DateRange.class;
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSqlValue() {
        throw new FxInvalidStateException("ex.content.value.sql.notSupported", getClass().getSimpleName()).asRuntimeException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DateRange getEmptyValue() {
        return EMPTY.copy();
    }


    @Override
    public int compareTo(FxValue o) {
        if (o instanceof FxDateRange && !isEmpty() && !o.isEmpty()) {
            return getBestTranslation().getLower().compareTo(((FxDateRange) o).getBestTranslation().getLower());
        } else if (o instanceof FxDateTimeRange && !isEmpty() && !o.isEmpty()) {
            return getBestTranslation().getLower().compareTo(((FxDateTimeRange) o).getBestTranslation().getLower());
        } else if (o instanceof FxDate && !isEmpty() && !o.isEmpty()) {
            return getBestTranslation().getLower().compareTo(((FxDate) o).getBestTranslation());
        } else if (o instanceof FxDateTime && !isEmpty() && !o.isEmpty()) {
            return getBestTranslation().getLower().compareTo(((FxDateTime) o).getBestTranslation());
        }
        return super.compareTo(o);
    }
}