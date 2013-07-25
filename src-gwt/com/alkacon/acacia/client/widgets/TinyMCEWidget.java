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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.logical.shared.HasResizeHandlers;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Timer;

/**
 * This class is used to start TinyMCE for editing the content of an element.<p>
 * 
 * After constructing the instance, the actual editor is opened using the init() method, and destroyed with the close()
 * method. While the editor is opened, the edited contents can be accessed using the methods of the HasValue interface.  
 */
public final class TinyMCEWidget extends A_EditWidget implements HasResizeHandlers {

    /** The minimum editor height. */
    private static final int MIN_EDITOR_HEIGHT = 70;

    /** A flag which indicates whether the editor is currently active. */
    protected boolean m_active;

    /** The current content. */
    protected String m_currentContent;

    /** The TinyMCE editor instance. */
    protected JavaScriptObject m_editor;

    /** The DOM ID of the editable element. */
    protected String m_id;

    /** The original HTML content of the editable element. */
    protected String m_originalContent;

    /** The maximal width of the widget. */
    protected int m_width;

    /** The editor height to set. */
    int m_editorHeight;

    /** The element to store the widget content in. */
    private Element m_contentElement;

    /** Indicating if the widget has been attached yet. */
    private boolean m_hasBeenAttached;

    /** Flag indicating the editor has been initialized. */
    private boolean m_initialized;

    /** Flag indicating if in line editing is used. */
    private boolean m_inline;

    /** The editor options. */
    private JavaScriptObject m_options;

    /**
     * Creates a new instance for the given element. Use this constructor for in line editing.<p>
     * 
     * @param element the DOM element
     * @param options the tinyMCE editor options to extend the default settings
     */
    public TinyMCEWidget(Element element, JavaScriptObject options) {

        this(element, options, true);
    }

    /**
     * Creates a new instance with the given options. Use this constructor for form based editing.<p>
     * 
     * @param options the tinyMCE editor options to extend the default settings
     */
    public TinyMCEWidget(JavaScriptObject options) {

        this(DOM.createDiv(), options, false);
    }

    /**
     * Constructor.<p>
     * 
     * @param element the DOM element
     * @param options the tinyMCE editor options to extend the default settings
     * @param inline flag indicating if in line editing is used
     */
    private TinyMCEWidget(Element element, JavaScriptObject options, boolean inline) {

        super(element);
        m_originalContent = "";
        m_options = options;
        m_active = true;
        m_inline = inline;
        if (m_inline) {
            m_contentElement = element;
        } else {
            // using a child DIV as content element
            m_contentElement = getElement().appendChild(DOM.createDiv());
        }
    }

    /**
     * @see com.google.gwt.event.logical.shared.HasResizeHandlers#addResizeHandler(com.google.gwt.event.logical.shared.ResizeHandler)
     */
    public HandlerRegistration addResizeHandler(ResizeHandler handler) {

        return addHandler(handler, ResizeEvent.getType());
    }

    /**
     * @see com.alkacon.acacia.client.widgets.A_EditWidget#addValueChangeHandler(com.google.gwt.event.logical.shared.ValueChangeHandler)
     */
    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler) {

        return addHandler(handler, ValueChangeEvent.getType());
    }

    /** 
     * Gets the main editable element.<p>
     * 
     * @return the editable element 
     */
    public Element getMainElement() {

        return m_contentElement;
    }

    /**
     * @see com.google.gwt.user.client.ui.HasValue#getValue()
     */
    @Override
    public String getValue() {

        if (m_editor != null) {
            return getContent().trim();
        }
        return m_originalContent.trim();
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
        if (m_editor != null) {
            if (m_active) {
                getEditorParentElement().removeClassName(I_LayoutBundle.INSTANCE.form().inActive());
                fireValueChange(true);
            } else {
                getEditorParentElement().addClassName(I_LayoutBundle.INSTANCE.form().inActive());
            }
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

        setValue(value, false);
    }

    /**
     * @see com.google.gwt.user.client.ui.HasValue#setValue(java.lang.Object, boolean)
     */
    public void setValue(String value, boolean fireEvents) {

        if (value != null) {
            value = value.trim();
        }
        setPreviousValue(value);
        if (m_editor == null) {
            // editor has not been initialized yet
            m_originalContent = value;
        } else {
            setContent(value);
        }
        if (fireEvents) {
            fireValueChange(true);
        }
    }

    /**
     * Checks whether the necessary Javascript libraries are available by accessing them. 
     */
    protected native void checkLibraries() /*-{
        // fail early if tinymce is not available
        var w = $wnd;
        var init = w.tinyMCE.init;
    }-*/;

    /**
     * Gives an element an id if it doesn't already have an id, and then returns the element's id.<p>
     * 
     * @param element the element for which we want to add the id
     *  
     * @return the id 
     */
    protected String ensureId(Element element) {

        String id = element.getId();
        if ((id == null) || "".equals(id)) {
            id = Document.get().createUniqueId();
            element.setId(id);
        }
        return id;
    }

    /**
     * Fixes the styling of the editor widget.<p>
     */
    protected void fixStyles() {

        if (!m_inline) {
            // it may take some time until the editor has been initialized, repeat until layout fix can be applied
            Scheduler.get().scheduleFixedDelay(new RepeatingCommand() {

                /**
                 * @see com.google.gwt.core.client.Scheduler.RepeatingCommand#execute()
                 */
                public boolean execute() {

                    Element parent = getEditorParentElement();
                    if (parent != null) {
                        String cssClass = getMainElement().getClassName();
                        if ((cssClass != null) && (cssClass.trim().length() > 0)) {
                            parent.addClassName(cssClass);
                        }
                        parent.getStyle().setDisplay(Display.BLOCK);
                        getEditorTableElement().getStyle().setWidth(100, Unit.PCT);
                        return false;
                    }
                    return true;
                }
            }, 100);
        }
    }

    /**
     * Returns the editor parent element.<p>
     * 
     * @return the editor parent element
     */
    protected native Element getEditorParentElement() /*-{
        var elementId = this.@com.alkacon.acacia.client.widgets.TinyMCEWidget::m_id;
        var parentId = elementId + "_parent";
        return $doc.getElementById(parentId);
    }-*/;

    /**
     * Returns the editor table element.<p>
     * 
     * @return the editor table element
     */
    protected native Element getEditorTableElement() /*-{
        var elementId = this.@com.alkacon.acacia.client.widgets.TinyMCEWidget::m_id;
        var tableId = elementId + "_tbl";
        return $doc.getElementById(tableId);
    }-*/;

    /**
     * Gets the toolbar element.<p>
     * 
     * @return the toolbar element 
     */
    protected native Element getToolbarElement() /*-{
        var elementId = this.@com.alkacon.acacia.client.widgets.TinyMCEWidget::m_id;
        var toolbarId = elementId + "_external";
        return $doc.getElementById(toolbarId);
    }-*/;

    /**
     * @see com.google.gwt.user.client.ui.FocusWidget#onAttach()
     */
    @Override
    protected void onAttach() {

        super.onAttach();
        if (!m_hasBeenAttached) {
            m_hasBeenAttached = true;
            final boolean refocus;
            if (m_inline) {
                Element rangeParent = getCurrentRangeParent();
                refocus = (rangeParent != null) && getMainElement().isOrHasChild(rangeParent);
            } else {
                refocus = false;
            }
            Scheduler.get().scheduleDeferred(new ScheduledCommand() {

                public void execute() {

                    if (isAttached()) {
                        m_editorHeight = calculateEditorHeight();
                        m_id = ensureId(getMainElement());
                        m_width = getElement().getOffsetWidth() - 2;
                        checkLibraries();
                        initNative();
                        if (refocus) {
                            Timer focusTimer = new Timer() {

                                @Override
                                public void run() {

                                    refocusInlineEditor();
                                }

                            };
                            focusTimer.schedule(200);
                        }

                        if (!m_active) {

                            Element parent = getEditorParentElement();
                            if (parent != null) {
                                parent.addClassName(I_LayoutBundle.INSTANCE.form().inActive());
                            }
                        }
                    } else {
                        resetAtachedFlag();
                    }
                }
            });
        }
    }

    /**
     * @see com.google.gwt.user.client.ui.Widget#onDetach()
     */
    @Override
    protected void onDetach() {

        detachEditor();
        super.onDetach();
    }

    /**
     * Propagates the a focus event.<p>
     */
    protected void propagateFocusEvent() {

        NativeEvent nativeEvent = Document.get().createFocusEvent();
        DomEvent.fireNativeEvent(nativeEvent, this, getElement());
    }

    /**
     * Propagates a native mouse event.<p>
     *
     * @param eventType the mouse event type 
     * @param eventSource the event source
     */
    protected native void propagateMouseEvent(String eventType, Element eventSource) /*-{
        var doc = $wnd.document;
        var event;
        if (doc.createEvent) {
            event = doc.createEvent("MouseEvents");
            event.initEvent(eventType, true, true);
            eventSource.dispatchEvent(event);
        } else {
            eventSource.fireEvent("on" + eventType);
        }
    }-*/;

    /**
     * Sets focus to the editor. Use only when in line editing.<p>
     */
    protected native void refocusInlineEditor() /*-{
        var elem = $wnd.document
                .getElementById(this.@com.alkacon.acacia.client.widgets.TinyMCEWidget::m_id);
        elem.blur();
        elem.focus();
    }-*/;

    /**
     * Removes the editor instance.<p>
     */
    protected native void removeEditor() /*-{
        var editor = this.@com.alkacon.acacia.client.widgets.TinyMCEWidget::m_editor;
        editor.remove();
    }-*/;

    /**
     * Sets the main content of the element which is inline editable.<p>
     * 
     * @param html the new content html 
     */
    protected native void setMainElementContent(String html) /*-{
        var instance = this;
        var elementId = instance.@com.alkacon.acacia.client.widgets.TinyMCEWidget::m_id;
        var mainElement = $wnd.document.getElementById(elementId);
        mainElement.innerHTML = html;
    }-*/;

    /**
     * Calculates the needed editor height.<p>
     * 
     * @return the calculated editor height
     */
    int calculateEditorHeight() {

        int result = getElement().getOffsetHeight() + 30;
        return result > MIN_EDITOR_HEIGHT ? result : MIN_EDITOR_HEIGHT;
    }

    /**
     * Initializes the TinyMCE instance.
     */
    native void initNative() /*-{

        var self = this;
        var elementId = self.@com.alkacon.acacia.client.widgets.TinyMCEWidget::m_id;
        var iframeId = elementId + "_ifr";
        var mainElement = $wnd.document.getElementById(elementId);
        var editorHeight = self.@com.alkacon.acacia.client.widgets.TinyMCEWidget::m_editorHeight
                + "px";

        var fireChange = function() {
            self.@com.alkacon.acacia.client.widgets.TinyMCEWidget::fireChangeFromNative()();
        };
        var options = this.@com.alkacon.acacia.client.widgets.TinyMCEWidget::m_options;
        if (options != null && options.editorHeight) {
            editorHeight = options.editorHeight;
            delete options.editorHeight;
        }
        // default options:
        var defaults = {
            relative_urls : false,
            remove_script_host : false,
            entity_encoding : "raw",
            skin_variant : 'ocms',
            mode : "exact",
            theme : "modern",
            plugins : "autolink,lists,pagebreak,layer,table,save,hr,image,link,emoticons,spellchecker,insertdatetime,preview,media,searchreplace,print,contextmenu,paste,directionality,noneditable,visualchars,nonbreaking,template,wordcount,advlist",
            menubar : false,
            toolbar_items_size : 'small'
        };
        if (this.@com.alkacon.acacia.client.widgets.TinyMCEWidget::m_inline) {
            self.@com.alkacon.acacia.client.widgets.TinyMCEWidget::m_currentContent = mainElement.innerHTML;
            defaults.inline = true;
            defaults.width = this.@com.alkacon.acacia.client.widgets.TinyMCEWidget::m_width;
        } else {
            self.@com.alkacon.acacia.client.widgets.TinyMCEWidget::m_currentContent = self.@com.alkacon.acacia.client.widgets.TinyMCEWidget::m_originalContent;
            defaults.autoresize_min_height = 100;
            defaults.autoresize_max_height = editorHeight;
            defaults.autoresize_bottom_margin = 10;
            defaults.width = '100%';
            defaults.resize = 'both';
            defaults.plugins = "autoresize," + defaults.plugins;
        }
        // extend the defaults with any given options
        if (options != null) {
            if (this.@com.alkacon.acacia.client.widgets.TinyMCEWidget::m_inline) {
                delete options.content_css;
            }
            var vie = @com.alkacon.vie.client.Vie::getInstance()();
            vie.jQuery.extend(defaults, options);
        }

        // add the setup function
        defaults.setup = function(ed) {
            self.@com.alkacon.acacia.client.widgets.TinyMCEWidget::m_editor = ed;

            ed.on('SetContent', fireChange);
            ed.on('change', fireChange);
            ed.on('KeyDown', fireChange);
            ed
                    .on(
                            'LoadContent',
                            function() {
                                if (!self.@com.alkacon.acacia.client.widgets.TinyMCEWidget::m_inline) {
                                    // firing resize event on resize of the editor iframe
                                    ed.dom
                                            .bind(
                                                    ed.getWin(),
                                                    'resize',
                                                    function() {
                                                        self.@com.alkacon.acacia.client.widgets.TinyMCEWidget::fireResizeEvent()();
                                                    });
                                    var content = self.@com.alkacon.acacia.client.widgets.TinyMCEWidget::m_originalContent;
                                    if (content != null) {
                                        ed.setContent(content);
                                    }
                                    // ensure the body height is set to 'auto', otherwise the autoresize plugin will not work
                                    ed.getDoc().body.style.height = 'auto';
                                }
                                self.@com.alkacon.acacia.client.widgets.TinyMCEWidget::m_initialized = true;
                            });
            if (!self.@com.alkacon.acacia.client.widgets.TinyMCEWidget::m_inline) {
                ed
                        .on(
                                'Click',
                                function(event) {
                                    if (!self.@com.alkacon.acacia.client.widgets.TinyMCEWidget::isActive()()) {
                                        // this may be the case if the mouse-down event has not been triggered correctly yet (IE),
                                        // trigger activation through new mouse-down
                                        self.@com.alkacon.acacia.client.widgets.TinyMCEWidget::propagateMouseEvent(Ljava/lang/String;Lcom/google/gwt/user/client/Element;)('mousedown', mainElement);
                                    }
                                    self.@com.alkacon.acacia.client.widgets.TinyMCEWidget::propagateMouseEvent(Ljava/lang/String;Lcom/google/gwt/user/client/Element;)('click', mainElement);
                                });
                ed
                        .on(
                                'focus',
                                function(event) {
                                    self.@com.alkacon.acacia.client.widgets.TinyMCEWidget::propagateFocusEvent()();
                                });
                ed
                        .on(
                                'mousedown',
                                function(event) {
                                    self.@com.alkacon.acacia.client.widgets.TinyMCEWidget::propagateMouseEvent(Ljava/lang/String;Lcom/google/gwt/user/client/Element;)('mousedown', mainElement);
                                });
                ed
                        .on(
                                'mouseup',
                                function(event) {
                                    self.@com.alkacon.acacia.client.widgets.TinyMCEWidget::propagateMouseEvent(Ljava/lang/String;Lcom/google/gwt/user/client/Element;)('mouseup', mainElement);
                                });
            }
        };
        // set the edited element id
        defaults.elements = self.@com.alkacon.acacia.client.widgets.TinyMCEWidget::m_id;
        // set default zindex
        var cssConstants = @com.alkacon.acacia.client.css.I_LayoutBundle::INSTANCE.@com.alkacon.acacia.client.css.I_LayoutBundle::constants()().@com.alkacon.geranium.client.ui.css.I_ConstantsBundle::css()();
        $wnd.tinymce.ui.FloatPanel.zIndex = cssConstants.@com.alkacon.geranium.client.ui.css.I_ConstantsBundle.I_ConstantsCss::zIndexPopup()();

        // initialize tinyMCE
        $wnd.tinymce.init(defaults);
        self.@com.alkacon.acacia.client.widgets.TinyMCEWidget::fixStyles()();
    }-*/;

    /**
     * Resets the attached flag.<p> 
     */
    void resetAtachedFlag() {

        m_hasBeenAttached = false;
    }

    /**
     * Removes the editor.<p>
     */
    private native void detachEditor() /*-{
        var ed = this.@com.alkacon.acacia.client.widgets.TinyMCEWidget::m_editor;
        ed.remove();
    }-*/;

    /**
     * Used to fire the value changed event from native code.<p>
     */
    private void fireChangeFromNative() {

        if (m_initialized) {
            Scheduler.get().scheduleDeferred(new ScheduledCommand() {

                public void execute() {

                    try {
                        fireValueChange(false);
                    } catch (Throwable t) {
                        // this may happen when returning from full screen mode, nothing to be done
                    }
                }
            });
        }
    }

    /**
     * Fires the resize event.<p>
     */
    private void fireResizeEvent() {

        ResizeEvent.fire(this, getOffsetWidth(), getOffsetHeight());
    }

    /**
     * Returns the editor content.<p>
     * 
     * @return the editor content
     */
    private native String getContent() /*-{
        var editor = this.@com.alkacon.acacia.client.widgets.TinyMCEWidget::m_editor;
        return editor.getContent();
    }-*/;

    /**
     * Returns the parent element to the current selection range.<p>
     * 
     * @return the parent element
     */
    private native Element getCurrentRangeParent()/*-{
        if ($wnd.getSelection) {
            var sel = $wnd.getSelection();
            if (sel.getRangeAt && sel.rangeCount) {
                var range = sel.getRangeAt(0);
                return range.commonAncestorContainer;
            }
        } else if ($wnd.document.selection
                && $wnd.document.selection.createRange) {
            var range = $wnd.document.selection.createRange();
            return range.parentElement();
        }
        return null;
    }-*/;

    /**
     * Sets the content of the TinyMCE editor.<p>
     * 
     * @param newContent the new content 
     */
    private native void setContent(String newContent) /*-{
        var editor = this.@com.alkacon.acacia.client.widgets.TinyMCEWidget::m_editor;
        editor.setContent(newContent);
    }-*/;

}
