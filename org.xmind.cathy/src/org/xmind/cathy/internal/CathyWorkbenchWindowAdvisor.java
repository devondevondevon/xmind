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
package org.xmind.cathy.internal;

import net.xmind.signin.internal.IVerificationListener;
import net.xmind.signin.internal.VerificationDelegate;

import org.eclipse.jface.action.CoolBarManager;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.xmind.cathy.internal.jobs.CheckOpenFilesJob;
import org.xmind.cathy.internal.jobs.StartupJob;
import org.xmind.ui.internal.workbench.Util;

public class CathyWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor
        implements IVerificationListener, IPartListener2, IPropertyListener {

    private boolean showPro = false;

    public CathyWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
        super(configurer);
    }

    public ActionBarAdvisor createActionBarAdvisor(
            IActionBarConfigurer configurer) {
        return new CathyWorkbenchActionBuilder(configurer);
    }

    public void preWindowOpen() {
        IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
        configurer.setInitialSize(Util.getInitialWindowSize());
        configurer.setShowCoolBar(true);
        configurer.setShowStatusLine(true);
        configurer.setShowProgressIndicator(true);
        configurer.setTitle(WorkbenchMessages.AppWindowTitle);
        VerificationDelegate.getDefault().addVerificationListener(this);
    }

    public void postWindowOpen() {
        super.postWindowOpen();
        IWorkbenchWindow window = getWindowConfigurer().getWindow();
        if (window != null) {
            CoolBarManager coolBar = ((WorkbenchWindow) window)
                    .getCoolBarManager();
            if (coolBar != null) {
                coolBar.setLockLayout(true);
            }

            window.getPartService().addPartListener(this);

            new StartupJob(window.getWorkbench()).schedule();

            Shell shell = window.getShell();
            if (shell != null && !shell.isDisposed()) {
                shell.addShellListener(new ShellAdapter() {
                    @Override
                    public void shellActivated(ShellEvent e) {
                        new CheckOpenFilesJob(getWindowConfigurer()
                                .getWorkbenchConfigurer().getWorkbench())
                                .schedule();
                    }
                });
            }
        }
    }

    @Override
    public void postWindowClose() {
        VerificationDelegate.getDefault().removeVerificationListener(this);
        super.postWindowClose();
    }

    public void verified(boolean valid) {
        this.showPro = valid;
        updateWindowTitle();
    }

    public void partActivated(IWorkbenchPartReference partRef) {
        if (partRef instanceof IEditorReference) {
            partRef.addPropertyListener(this);
        }
        updateWindowTitle();
    }

    public void partBroughtToTop(IWorkbenchPartReference partRef) {
    }

    public void partClosed(IWorkbenchPartReference partRef) {
        updateWindowTitle();
    }

    public void partDeactivated(IWorkbenchPartReference partRef) {
        partRef.removePropertyListener(this);
    }

    public void partHidden(IWorkbenchPartReference partRef) {
        updateWindowTitle();
    }

    public void partInputChanged(IWorkbenchPartReference partRef) {
        updateWindowTitle();
    }

    public void partOpened(IWorkbenchPartReference partRef) {
    }

    public void partVisible(IWorkbenchPartReference partRef) {
        updateWindowTitle();
    }

    private void updateWindowTitle() {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
            public void run() {
                doUpdateWindowTitle();
            }
        });
    }

    private void doUpdateWindowTitle() {
        IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
        IWorkbenchWindow window = configurer.getWindow();
        if (window == null)
            return;

        Shell shell = window.getShell();
        if (shell == null || shell.isDisposed())
            return;

        StringBuffer sb = new StringBuffer(20);
        if (showPro) {
            sb.append(WorkbenchMessages.ProWindowTitle);
        } else {
            sb.append(WorkbenchMessages.AppWindowTitle);
        }
        IWorkbenchPage page = window.getActivePage();
        if (page != null) {
            IEditorPart editor = page.getActiveEditor();
            if (editor != null) {
                sb.append(" - "); //$NON-NLS-1$
                IEditorInput input = editor.getEditorInput();
                if (input != null
                        && !editor.getClass().toString().contains(
                                "org.xmind.ui.internal.browser")) { //$NON-NLS-1$
                    String text = input.getToolTipText();
                    if (text != null) {
                        sb.append(text);
                    } else {
                        sb.append(editor.getTitle());
                    }
                } else {
                    sb.append(editor.getTitle());
                }
            }
        }
        configurer.setTitle(sb.toString());
    }

    public void propertyChanged(Object source, int propId) {
        updateWindowTitle();
    }

}