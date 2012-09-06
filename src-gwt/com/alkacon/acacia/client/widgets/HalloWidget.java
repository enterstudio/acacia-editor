/*
 * This library is part of the Acacia Editor -
 * an open source inline and form based content editor for GWT.
 *
 * Copyright (c) Alkacon Software GmbH (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software, please see the
 * company website: http://www.alkacon.com
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.alkacon.acacia.client.widgets;

import com.alkacon.acacia.client.css.I_LayoutBundle;
import com.alkacon.vie.client.Vie;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * Rich text editor widget based on the hallo editor.<p>
 */
public class HalloWidget extends A_EditWidget {

    /** Indicating if the widget is active. */
    private boolean m_active;

    /** The hallo editor options. */
    private JavaScriptObject m_options;

    /**
     * Constructor.<p>
     */
    public HalloWidget() {

        this(DOM.createDiv(), null);
    }

    /**
     * Constructor wrapping a specific DOM element.<p>
     * 
     * @param element the element to wrap
     * @param options the hallo editor options
     */
    public HalloWidget(Element element, JavaScriptObject options) {

        super(element);
        m_options = options;
    }

    /**
     * @see com.alkacon.acacia.client.widgets.A_EditWidget#addValueChangeHandler(com.google.gwt.event.logical.shared.ValueChangeHandler)
     */
    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler) {

        return addHandler(handler, ValueChangeEvent.getType());
    }

    /**
     * @see com.alkacon.acacia.client.widgets.A_EditWidget#getValue()
     */
    @Override
    public String getValue() {

        return getElement().getInnerHTML();
    }

    /**
     * @see com.alkacon.acacia.client.widgets.I_EditWidget#isActive()
     */
    public boolean isActive() {

        return m_active;
    }

    /**
     * @see com.alkacon.acacia.client.widgets.I_EditWidget#setActive(boolean)
     */
    public void setActive(boolean active) {

        if (m_active == active) {
            return;
        }
        m_active = active;
        if (m_active) {
            getElement().setAttribute("contentEditable", "true");
            getElement().removeClassName(I_LayoutBundle.INSTANCE.form().inActive());
            getElement().focus();
            fireValueChange(true);
        } else {
            getElement().setAttribute("contentEditable", "false");
            getElement().addClassName(I_LayoutBundle.INSTANCE.form().inActive());
        }
    }

    /**
     * @see com.alkacon.acacia.client.widgets.I_EditWidget#setName(java.lang.String)
     */
    public void setName(String name) {

        // no input field so nothing to do

    }

    /**
     * @see com.google.gwt.user.client.ui.HasValue#setValue(java.lang.Object)
     */
    public void setValue(String value) {

        setValue(value, true);
    }

    /**
     * @see com.google.gwt.user.client.ui.HasValue#setValue(java.lang.Object, boolean)
     */
    public void setValue(String value, boolean fireEvents) {

        getElement().setInnerHTML(value);
        fireValueChange(false);
    }

    /**
     * @see com.google.gwt.user.client.ui.FocusWidget#onAttach()
     */
    @Override
    protected void onAttach() {

        super.onAttach();
        initNative(getElement(), Vie.getInstance(), m_options);
    }

    /**
     * Initializes the widget.<p>
     */
    private void init() {

        addStyleName(I_LayoutBundle.INSTANCE.form().input());
    }

    /**
     * Initializes the native java-script components of the widget.<p>
     * 
     * @param element the element
     * @param vie the VIE instance
     * @param options editor options
     */
    private native void initNative(Element element, Vie vie, JavaScriptObject options) /*-{
        var _self = this;
        var editable = vie.jQuery(element);
        var pluginSettings;
        if (typeof options != null) {
            pluginSettings = {};
            if (options.format) {
                pluginSettings['halloformat'] = {
                    'formattings' : options.format
                };
            }
            if (options.block) {
                pluginSettings['halloblock'] = {
                    'elements' : options.block
                };
            }
            if (options.justify) {
                pluginSettings['hallojustify'] = {};
            }
            if (options.lists) {
                pluginSettings['hallolists'] = {
                    'lists' : options.lists
                };
            }
            if (options.reundo) {
                pluginSettings['halloreundo'] = {};
            }
        } else {
            pluginSettings = {
                'halloformat' : {},
                'halloblock' : {},
                'hallojustify' : {},
                'hallolists' : {},
                'halloreundo' : {}
            };
        }
        editable.hallo({
            plugins : pluginSettings,
            editable : true,
            toolbar : 'halloToolbarFixed',
            cssScope : '__acacia'
        });
        editable
                .bind(
                        'hallomodified',
                        function(event, data) {
                            _self.@com.alkacon.acacia.client.widgets.HalloWidget::fireValueChange(Z)(false);
                        });
        var selection = $wnd.rangy.getSelection();
        var range = null;
        if (selection.rangeCount > 0) {
            range = selection.getRangeAt(0);
        } else {
            range = rangy.createRange();
        }
        if (editable.hallo("containsSelection")) {
            editable.hallo("turnOn");
        }
    }-*/;
}
