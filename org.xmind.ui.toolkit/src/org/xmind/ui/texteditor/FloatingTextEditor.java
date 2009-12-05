/* ******************************************************************************
 * Copyright (c) 2006-2009 XMind Ltd. and others.
 * 
 * This file is a part of XMind 3. XMind releases 3 and
 * above are dual-licensed under the Eclipse Public License (EPL),
 * which is available at http://www.eclipse.org/legal/epl-v10.html
 * and the GNU Lesser General Public License (LGPL), 
 * which is available at http://www.gnu.org/licenses/lgpl.html
 * See http://www.xmind.net/license.html for details.
 * 
 * Contributors:
 *     XMind Ltd. - initial API and implementation
 *******************************************************************************/
package org.xmind.ui.texteditor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.TextViewerUndoManager;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;
import org.xmind.ui.viewers.SWTUtils;

public class FloatingTextEditor extends Viewer implements ITextOperationTarget {

    private static int DEFAULT_STYLE = SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL
            | SWT.H_SCROLL;

    private static class SameCompositeProvider implements ICompositeProvider {

        private Composite composite;

        public SameCompositeProvider(Composite composite) {
            this.composite = composite;
        }

        public Composite getParent() {
            return composite;
        }

    }

    private class TextViewerHooker implements ISelectionChangedListener,
            VerifyKeyListener {

        public void selectionChanged(SelectionChangedEvent event) {
            fireSelectionChanged(new SelectionChangedEvent(
                    FloatingTextEditor.this, event.getSelection()));
        }

        public void verifyKey(VerifyEvent event) {
            handleVerifyKey(event);
        }

        public void hook(ITextViewer viewer) {
            if (viewer instanceof ISelectionProvider) {
                ((ISelectionProvider) viewer).addSelectionChangedListener(this);
            }
            if (viewer instanceof IPostSelectionProvider) {
                ((IPostSelectionProvider) viewer)
                        .addPostSelectionChangedListener(this);
            }
            if (viewer instanceof ITextViewerExtension) {
                ((ITextViewerExtension) viewer).prependVerifyKeyListener(this);
            }
        }

        public void unhook(ITextViewer viewer) {
            if (viewer instanceof ITextViewerExtension) {
                ((ITextViewerExtension) viewer).removeVerifyKeyListener(this);
            }
            if (viewer instanceof IPostSelectionProvider) {
                ((IPostSelectionProvider) viewer)
                        .removePostSelectionChangedListener(this);
            }
            if (viewer instanceof ISelectionProvider) {
                ((ISelectionProvider) viewer)
                        .removeSelectionChangedListener(this);
            }
        }

    }

    private int style = DEFAULT_STYLE;

    private ICompositeProvider compositeProvider;

    private Composite control = null;

    private IDocument document = null;

    private ITextViewer textViewer = null;

    private IDocumentListener documentListener = null;

    private List<IFloatingTextEditorListener> textEditorListeners = null;

    private List<VerifyKeyListener> verifyKeyListeners = null;

    private TextViewerHooker textViewerHooker = null;

    private boolean closing = false;

    private Point initialLocation = null;

    private Point initialSize = null;

    public FloatingTextEditor(Composite parent) {
        this(new SameCompositeProvider(parent), DEFAULT_STYLE);
    }

    public FloatingTextEditor(Composite parent, int style) {
        this(new SameCompositeProvider(parent), style);
    }

    public FloatingTextEditor(ICompositeProvider compositeProvider) {
        this(compositeProvider, DEFAULT_STYLE);
    }

    public FloatingTextEditor(ICompositeProvider compositeProvider, int style) {
        Assert.isNotNull(compositeProvider);
        this.compositeProvider = compositeProvider;
        this.style = style;
    }

    protected Composite getParentComposite() {
        return compositeProvider.getParent();
    }

    /**
     * Sets the style of the editor. Has no effect after the control has been
     * created.
     * 
     * <dl>
     * <dt><b>Styles<b></dt>
     * <dd>NO_FOCUS, BORDER, SINGLE, MULTI, READ_ONLY, V_SCROLL, H_SCROLL, WRAP</dd>
     * </dl>
     * 
     * @param style
     */
    protected void setEditorStyle(int style) {
        this.style = style;
    }

    protected int getEditorStyle() {
        return style;
    }

    public boolean open() {
        return open(true);
    }

    public boolean open(boolean withFocus) {
        if (!isClosed())
            return true;

        TextEvent e = createTextEvent();
        fireEditingAboutToStart(e);
        if (e.isCanceled())
            return false;

        Composite parent = getParentComposite();
        if (parent == null)
            return false;

        if (isClosed())
            createControl(parent, getEditorStyle());

        if (isClosed())
            return false;

        control.setVisible(true);
        if (withFocus) {
            control.setFocus();
        }
        fireEditingStarted(createTextEvent());
        return true;
    }

    public boolean close() {
        return close(false);
    }

    public boolean close(boolean finish) {
        if (isClosed() || closing)
            return true;

        closing = true;
        TextEvent e = createTextEvent();
        if (finish) {
            fireEditingAboutToFinish(e);
        } else {
            fireEditingAboutToCancel(e);
        }

        if (e.isCanceled()) {
            closing = false;
            return false;
        }

        hardClose(finish);

        if (finish) {
            fireEditingFinished(createTextEvent());
        } else {
            fireEditingCanceled(createTextEvent());
        }
        closing = false;
        return true;
    }

    protected void hardClose(boolean finish) {
        if (textViewer != null) {
            unhookTextViewer(textViewer);
        }
        if (document != null) {
            unhookDocument(document);
        }
        if (!finish)
            hardCancel();
        Composite parent = control.getParent();
        control.dispose();
        if (parent.getShell() == parent.getDisplay().getActiveShell()) {
            parent.setFocus();
        }
    }

    protected void hardCancel() {
        if (document != null) {
            while (canDoOperation(UNDO)) {
                doOperation(UNDO);
            }
        }
    }

    protected TextEvent createTextEvent() {
        return new TextEvent(this, getTextContents());
    }

    public String getTextContents() {
        return document == null ? null : document.get();
    }

    private void createControl(Composite parent, int style) {
        boolean border = (style & SWT.BORDER) != 0;
        style &= ~(SWT.BORDER | SWT.NO_FOCUS);

        control = createContainer(parent, border);
        textViewer = createTextViewer(control, style);

        configureContainer(control);
        hookContainer(control);

        configureTextViewer(textViewer);
        hookTextViewer(textViewer);

        if (document != null) {
            hookDocument(document);
        }
    }

    private Composite createContainer(Composite parent, boolean border) {
        Composite composite = new Composite(parent, SWT.NO_FOCUS);
        composite.setBackground(parent.getDisplay().getSystemColor(
                SWT.COLOR_GRAY));

        GridLayout layout = new GridLayout();
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 0;
        if (border) {
            layout.marginWidth = 1;
            layout.marginHeight = 1;
        }
        composite.setLayout(layout);

        return composite;
    }

    protected void configureContainer(Composite container) {
        Point size = getInitialSize();
        Point position = getInitialPosition(size);
        container.setBounds(position.x, position.y, size.x, size.y);
    }

    protected Point getInitialSize() {
        if (initialSize != null)
            return initialSize;
        return new Point(100, 20);
    }

    protected Point getInitialPosition(Point size) {
        if (initialLocation != null)
            return initialLocation;
        return new Point(0, 0);
    }

    protected void hookContainer(Composite container) {
        container.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                handleDispose(e);
            }
        });
    }

    protected void handleDispose(DisposeEvent e) {
        if (document != null) {
            unhookDocument(document);
        }
        if (textViewer != null) {
            unhookTextViewer(textViewer);
            textViewer = null;
        }
        control = null;
    }

    protected ITextViewer createTextViewer(Composite parent, int style) {
        TextViewer viewer = new TextViewer(parent, style) {
            protected int getEmptySelectionChangedEventDelay() {
                return 300;
            }
        };
        viewer.getControl().setLayoutData(
                new GridData(GridData.FILL, GridData.FILL, true, true));
        return viewer;
    }

    protected void configureTextViewer(ITextViewer viewer) {
        if (getDocument() != null) {
            viewer.setDocument(getDocument());
        }
        viewer.setUndoManager(new TextViewerUndoManager(20));
        viewer.activatePlugins();
    }

    protected void hookTextViewer(ITextViewer viewer) {
        if (textViewerHooker == null) {
            textViewerHooker = new TextViewerHooker();
        }
        textViewerHooker.hook(viewer);
    }

    protected void unhookTextViewer(ITextViewer viewer) {
        if (textViewerHooker != null) {
            textViewerHooker.unhook(viewer);
            textViewerHooker = null;
        }
    }

    protected void handleVerifyKey(VerifyEvent event) {
        fireVerifyKey(event);
        if (!event.doit)
            return;

        int stateMask = event.stateMask;
        int keyCode = event.keyCode;
        if (SWTUtils.matchKey(stateMask, keyCode, 0, SWT.ESC)) {
            event.doit = false;
            cancelEditing();
        } else if (SWTUtils.matchKey(stateMask, keyCode, SWT.MOD2, SWT.CR)) {
            if ((getEditorStyle() & SWT.MULTI) == 0) {
                event.doit = false;
            }
        } else if (SWTUtils.matchKey(stateMask, keyCode, 0, SWT.CR)) {
            event.doit = false;
            finishEditing();
        } else if (SWTUtils.matchKey(stateMask, keyCode, SWT.MOD1, 'z')) {
            event.doit = false;
            if (canDoOperation(UNDO)) {
                doOperation(UNDO);
            }
        } else if (SWTUtils.matchKey(stateMask, keyCode, SWT.MOD1, 'y')) {
            event.doit = false;
            if (canDoOperation(REDO)) {
                doOperation(REDO);
            }
        }
    }

    protected void replaceText(int offset, int length, String text) {
        if (document == null)
            return;

        try {
            document.replace(offset, length, text);
        } catch (BadLocationException e) {
        }
    }

    protected void finishEditing() {
        close(true);
    }

    protected void cancelEditing() {
        close(false);
    }

    protected void hookDocument(IDocument document) {
        if (documentListener == null) {
            documentListener = new IDocumentListener() {
                public void documentChanged(DocumentEvent event) {
                    fireTextChanged(createTextEvent());
                }

                public void documentAboutToBeChanged(DocumentEvent event) {
                    fireTextAboutToChange(createTextEvent());
                }
            };
        }
        document.addDocumentListener(documentListener);
    }

    protected void unhookDocument(IDocument document) {
        if (document != null && documentListener != null) {
            document.removeDocumentListener(documentListener);
            documentListener = null;
        }
    }

    public boolean isClosed() {
        return control == null || control.isDisposed();
    }

    public Control getControl() {
        return control;
    }

    public Object getInput() {
        return getDocument();
    }

    protected IDocument getDocument() {
        return document;
    }

    public ISelection getSelection() {
        if (textViewer instanceof ISelectionProvider)
            return ((ISelectionProvider) textViewer).getSelection();
        return null;
    }

    public void refresh() {
        if (textViewer instanceof Viewer) {
            ((Viewer) textViewer).refresh();
        }
    }

    public void setInput(Object input) {
        if (!(input instanceof IDocument))
            return;

        IDocument newDocument = (IDocument) input;
        IDocument oldDocument = this.document;
        if (newDocument == oldDocument
                || (newDocument != null && newDocument.equals(oldDocument)))
            return;

        this.document = newDocument;
        documentChanged(oldDocument, newDocument);
    }

    protected void documentChanged(IDocument oldDocument, IDocument newDocument) {
        if (oldDocument != null) {
            unhookDocument(oldDocument);
        }
        if (textViewer != null) {
            textViewer.setDocument(newDocument);
            if (newDocument != null) {
                hookDocument(newDocument);
            }
        }
        inputChanged(newDocument, oldDocument);
    }

    public void setSelection(ISelection selection) {
        if (textViewer instanceof ISelectionProvider)
            ((ISelectionProvider) textViewer).setSelection(selection);
    }

    public void setSelection(ISelection selection, boolean reveal) {
        if (textViewer instanceof Viewer)
            ((Viewer) textViewer).setSelection(selection, reveal);
    }

    public ITextViewer getTextViewer() {
        return textViewer;
    }

    public void doOperation(int operation) {
        if (textViewer instanceof ITextOperationTarget) {
            ((ITextOperationTarget) textViewer).doOperation(operation);
        }
    }

    public boolean canDoOperation(int operation) {
        if (textViewer instanceof ITextOperationTarget) {
            return ((ITextOperationTarget) textViewer)
                    .canDoOperation(operation);
        }
        return false;
    }

    public void addFloatingTextEditorListener(
            IFloatingTextEditorListener listener) {
        if (textEditorListeners == null)
            textEditorListeners = new ArrayList<IFloatingTextEditorListener>();
        textEditorListeners.add(listener);
    }

    public void removeFloatingTextEditorListener(
            IFloatingTextEditorListener listener) {
        if (textEditorListeners == null)
            return;
        textEditorListeners.remove(listener);
    }

    protected void fireEditingAboutToStart(TextEvent e) {
        if (textEditorListeners == null)
            return;

        for (Object l : textEditorListeners.toArray()) {
            ((IFloatingTextEditorListener) l).editingAboutToStart(e);
        }
    }

    protected void fireEditingStarted(TextEvent e) {
        if (textEditorListeners == null)
            return;

        for (Object l : textEditorListeners.toArray()) {
            ((IFloatingTextEditorListener) l).editingStarted(e);
        }
    }

    protected void fireEditingAboutToCancel(TextEvent e) {
        if (textEditorListeners == null)
            return;

        for (Object l : textEditorListeners.toArray()) {
            ((IFloatingTextEditorListener) l).editingAboutToCancel(e);
        }
    }

    protected void fireEditingCanceled(TextEvent e) {
        if (textEditorListeners == null)
            return;

        for (Object l : textEditorListeners.toArray()) {
            ((IFloatingTextEditorListener) l).editingCanceled(e);
        }
    }

    protected void fireEditingAboutToFinish(TextEvent e) {
        if (textEditorListeners == null)
            return;

        for (Object l : textEditorListeners.toArray()) {
            ((IFloatingTextEditorListener) l).editingAboutToFinish(e);
        }
    }

    protected void fireEditingFinished(TextEvent e) {
        if (textEditorListeners == null)
            return;

        for (Object l : textEditorListeners.toArray()) {
            ((IFloatingTextEditorListener) l).editingFinished(e);
        }
    }

    protected void fireTextAboutToChange(TextEvent e) {
        if (textEditorListeners == null)
            return;

        for (Object l : textEditorListeners.toArray()) {
            ((IFloatingTextEditorListener) l).textAboutToChange(e);
        }
    }

    protected void fireTextChanged(TextEvent e) {
        if (textEditorListeners == null)
            return;

        for (Object l : textEditorListeners.toArray()) {
            ((IFloatingTextEditorListener) l).textChanged(e);
        }
    }

    public void addVerifyKeyListener(VerifyKeyListener listener) {
        if (verifyKeyListeners == null)
            verifyKeyListeners = new ArrayList<VerifyKeyListener>();
        verifyKeyListeners.add(listener);
    }

    public void removeVerifyKeyListener(VerifyKeyListener listener) {
        if (verifyKeyListeners == null)
            return;
        verifyKeyListeners.remove(listener);
    }

    protected void fireVerifyKey(VerifyEvent e) {
        if (verifyKeyListeners == null)
            return;

        for (Object l : verifyKeyListeners.toArray()) {
            ((VerifyKeyListener) l).verifyKey(e);
        }
    }

    public Rectangle computeTrim(int x, int y, int width, int height) {
        if (isClosed())
            return null;

        Rectangle trim = textViewer.getTextWidget().computeTrim(x, y, width,
                height);
        trim = control.computeTrim(trim.x, trim.y, trim.width, trim.height);
        Layout layout = control.getLayout();
        if (layout instanceof GridLayout) {
            GridLayout gl = (GridLayout) layout;
            trim.x -= gl.marginWidth + gl.marginLeft;
            trim.y -= gl.marginHeight + gl.marginTop;
            trim.width += gl.marginWidth * 2 + gl.marginLeft + gl.marginRight;
            trim.height += gl.marginHeight * 2 + gl.marginTop + gl.marginBottom;
        }
        return trim;
    }

    public void replaceText(String text) {
        replaceText(text, false);
    }

    public void replaceText(String text, boolean select) {
        if (isClosed() || document == null)
            return;

        Point range = textViewer.getSelectedRange();
        try {
            document.replace(range.x, range.y, text);
        } catch (BadLocationException e) {
        }
        if (select) {
            textViewer.setSelectedRange(range.x, text.length());
        } else {
            textViewer.setSelectedRange(range.x + text.length(), 0);
        }
    }

    public void setInitialLocation(Point initialLocation) {
        this.initialLocation = initialLocation;
    }

    public void setInitialSize(Point initialSize) {
        this.initialSize = initialSize;
    }

}