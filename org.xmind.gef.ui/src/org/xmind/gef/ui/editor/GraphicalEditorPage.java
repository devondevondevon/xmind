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
package org.xmind.gef.ui.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorActionBarContributor;
import org.xmind.gef.Disposable;
import org.xmind.gef.EditDomain;
import org.xmind.gef.IGraphicalViewer;
import org.xmind.gef.ui.actions.ActionRegistry;
import org.xmind.gef.ui.actions.IActionRegistry;
import org.xmind.gef.ui.actions.ISelectionAction;

/**
 * @author Brian Sun
 */
public abstract class GraphicalEditorPage extends Disposable implements
        IGraphicalEditorPage, ISelectionChangedListener {

    private IGraphicalEditor parent = null;

    private Object input = null;

    private Control control = null;

    private EditDomain domain = null;

    private IGraphicalViewer viewer = null;

    private MenuManager contentPopupMenu = null;

//    private Menu menu = null;

    private boolean active = false;

    private IActionRegistry actionRegistry = null;

    private List<ISelectionAction> selectionActions = null;

    private IPanelContributor panelContributor = null;

    protected void setPanelContributor(IPanelContributor contributor) {
        this.panelContributor = contributor;
    }

    public IPanelContributor getPanelContributor() {
        return panelContributor;
    }

    /**
     * @see org.xmind.gef.ui.editor.IGraphicalEditorPage#getIndex()
     */
    public int getIndex() {
        return parent.findPage(this);
    }

    /**
     * @see org.xmind.gef.ui.editor.IGraphicalEditorPage#getPageTitle()
     */
    public String getPageTitle() {
        return parent.getPageText(getIndex());
    }

    /**
     * @see org.xmind.gef.ui.editor.IGraphicalEditorPage#getParentEditor()
     */
    public IGraphicalEditor getParentEditor() {
        return parent;
    }

    /**
     * @see org.xmind.gef.ui.editor.IGraphicalEditorPage#getSelectionProvider()
     */
    public ISelectionProvider getSelectionProvider() {
        return viewer;
    }

    /**
     * @see org.xmind.gef.ui.editor.IGraphicalEditorPage#init(org.xmind.gef.ui.editor.GraphicalEditor,
     *      java.lang.Object)
     */
    public void init(IGraphicalEditor parent, Object input) {
        this.parent = parent;
        this.input = input;
        if (input != null) {
            installModelListeners(input);
        }
        initPageActions(getActionRegistry());
    }

    protected void installModelListeners(Object input) {
    }

    protected void uninstallModelListeners(Object input) {
    }

    public Object getInput() {
        return input;
    }

    /**
     * @see org.xmind.gef.ui.editor.IGraphicalEditorPage#isActive()
     */
    public boolean isActive() {
        return active;
    }

    /**
     * @see org.xmind.gef.ui.editor.IGraphicalEditorPage#setActive(boolean)
     */
    public void setActive(boolean active) {
        this.active = active;
        if (active) {
            setFocus();
        }
    }

    /**
     * @see org.xmind.gef.ui.editor.IGraphicalEditorPage#setIndex(int)
     */
    public void setIndex(int index) {
        parent.movePageTo(getIndex(), index);
    }

    /**
     * @see org.xmind.gef.ui.editor.IGraphicalEditorPage#setPageTitle(java.lang.String)
     */
    public void setPageTitle(String title) {
        parent.setPageText(getIndex(), title);
    }

    public EditDomain getEditDomain() {
        return domain;
    }

    public void setEditDomain(EditDomain domain) {
        if (this.domain != null && getViewer() != null) {
//            this.domain.removeViewer(getViewer());
            this.domain.setViewer(null);
        }
        this.domain = domain;
        if (domain != null && getViewer() != null) {
//            domain.addViewer(getViewer());
            domain.setViewer(getViewer());
        }
    }

    /**
     * @see org.xmind.gef.ui.editor.IGraphicalEditorPage#createPageControl(org.eclipse.swt.widgets.Composite)
     */
    public void createPageControl(Composite parent) {
        Panel panel = null;
        if (panelContributor != null) {
            panel = new Panel();
            panelContributor.init(panel, this);
            if (panel.isEmpty()) {
                panel = null;
            }
        }

        Composite container;
        if (panel == null) {
            container = parent;
        } else {
            panel.createControls(parent);
            container = panel.getContainer();
        }
        viewer = createViewer();
        initViewer(viewer);
        createViewerControl(viewer, container);
        createContentPopupMenu(viewer.getControl());

        if (panel != null) {
            panel.setContent(viewer.getControl());
            panel.update();
        }

        hookViewer(viewer);
        configureViewer(viewer);
        updateSelectionActions(viewer.getSelection());

        if (panelContributor != null) {
            panelContributor.setViewer(viewer);
        }

        if (panel != null) {
            control = panel.getContainer();
        } else {
            control = viewer.getControl();
        }
    }

    /**
     * @param parent
     * @return
     */
    protected abstract IGraphicalViewer createViewer();

    protected abstract void createViewerControl(IGraphicalViewer viewer,
            Composite parent);

    public Control getControl() {
        return control;
    }

    private void createContentPopupMenu(Control control) {
        if (contentPopupMenu == null) {
            contentPopupMenu = createContentPopupMenu();
            String menuId = getParentEditor().getSite().getId()
                    + "-" + hashCode() + ".content"; //$NON-NLS-1$ //$NON-NLS-2$
            initContentPopupMenu(contentPopupMenu);
            registerContentPopupMenu(menuId, contentPopupMenu);
        }
        Menu menu = contentPopupMenu.getMenu();
        if (menu == null || menu.isDisposed()) {
            menu = contentPopupMenu.createContextMenu(control);
        }
        control.setMenu(menu);
    }

    protected void registerContentPopupMenu(String menuId, MenuManager menu) {
        getParentEditor().getSite().registerContextMenu(menuId, menu,
                getSelectionProvider());
    }

    protected MenuManager createContentPopupMenu() {
        return new MenuManager();
    }

    protected void initContentPopupMenu(MenuManager menu) {
        if (isContentPopupMenuDynamic()) {
            menu.setRemoveAllWhenShown(true);
            menu.addMenuListener(new IMenuListener() {
                public void menuAboutToShow(IMenuManager manager) {
                    contributeToContentPopupMenu(manager);
                }
            });
        } else {
            contributeToContentPopupMenu(menu);
        }
    }

    protected boolean isContentPopupMenuDynamic() {
        return true;
    }

    protected void contributeToContentPopupMenu(IMenuManager menu) {
        IEditorActionBarContributor contributor = getParentEditor()
                .getEditorSite().getActionBarContributor();
        if (contributor instanceof GraphicalEditorActionBarContributor) {
            ((GraphicalEditorActionBarContributor) contributor)
                    .contributeToContentPopupMenu(menu);
        }
    }

    protected void initPageActions(IActionRegistry actionRegistry) {
    }

    protected void initViewer(IGraphicalViewer viewer) {
        if (domain != null) {
            //domain.addViewer(viewer);
            domain.setViewer(viewer);
        }
    }

    protected void configureViewer(IGraphicalViewer viewer) {
        viewer.setInput(createViewerInput());
    }

    protected Object createViewerInput() {
        return getInput();
    }

    protected void hookViewer(IGraphicalViewer viewer) {
        viewer.addSelectionChangedListener(this);
    }

    protected void unhookViewer(IGraphicalViewer viewer) {
        viewer.removeSelectionChangedListener(this);
    }

    /**
     * @see org.xmind.gef.ui.editor.IGraphicalEditorPage#setFocus()
     */
    public void setFocus() {
        if (viewer != null && viewer.getControl() != null
                && !viewer.getControl().isDisposed()) {
            viewer.getControl().setFocus();
        }
    }

    public IGraphicalViewer getViewer() {
        return viewer;
    }

    public Object getAdapter(Class adapter) {
        if (adapter == IActionRegistry.class)
            return getActionRegistry();
        if (adapter == IGraphicalViewer.class)
            return getViewer();
        if (adapter == EditDomain.class)
            return getEditDomain();
        return null;
    }

    protected IActionRegistry getActionRegistry() {
        if (actionRegistry == null)
            actionRegistry = new ActionRegistry();
        return actionRegistry;
    }

    protected void addSelectionAction(ISelectionAction action) {
        if (selectionActions == null)
            selectionActions = new ArrayList<ISelectionAction>();
        selectionActions.add(action);
    }

    public void selectionChanged(SelectionChangedEvent event) {
        updateSelectionActions(event.getSelection());
    }

    protected void updateSelectionActions(ISelection selection) {
        if (selectionActions != null) {
            for (ISelectionAction action : selectionActions) {
                action.setSelection(selection);
            }
        }
    }

    /**
     * @see org.xmind.util.Disposable#dispose()
     */
    @Override
    public void dispose() {
        if (panelContributor != null) {
            panelContributor.dispose();
        }
        if (selectionActions != null) {
            for (ISelectionAction action : selectionActions) {
                action.setSelection(null);
            }
            selectionActions = null;
        }
        if (actionRegistry != null) {
            actionRegistry.dispose();
            actionRegistry = null;
        }
        if (getInput() != null) {
            uninstallModelListeners(getInput());
        }
        if (contentPopupMenu != null) {
            contentPopupMenu.dispose();
            contentPopupMenu = null;
        }
//        if (menu != null) {
//            menu.dispose();
//            menu = null;
//        }
        if (viewer != null) {
            unhookViewer(viewer);
            if (domain != null) {
                domain.setViewer(null);
                //domain.removeViewer(viewer);
            }
            viewer.getControl().dispose();
            viewer = null;
        }
        super.dispose();
    }
}