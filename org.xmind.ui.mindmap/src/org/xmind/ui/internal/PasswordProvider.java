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
package org.xmind.ui.internal;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.xmind.core.Core;
import org.xmind.core.CoreException;
import org.xmind.core.IEncryptionHandler;

/**
 * @author MANGOSOFT
 * 
 */
public class PasswordProvider implements IEncryptionHandler {

    private class PasswordDialog extends Dialog {

        private String value = null;

        /**
         * @param parentShell
         */
        protected PasswordDialog(Shell parentShell) {
            super(parentShell);
            setShellStyle(getShellStyle() | SWT.RESIZE);
        }

        protected void configureShell(Shell shell) {
            super.configureShell(shell);
            shell.setText(MindMapMessages.EncryptDialog_title);
        }

        @Override
        protected Point getInitialSize() {
            return new Point(300, 200);
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            Composite composite = (Composite) super.createDialogArea(parent);

            Label messageLabel = new Label(composite, SWT.WRAP);
            messageLabel.setText(MindMapMessages.EncryteDialog_label_message);
            messageLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
                    false));

            Text passwordInput = new Text(composite, SWT.BORDER | SWT.SINGLE
                    | SWT.PASSWORD);
            passwordInput.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
                    false));

            return composite;
        }

        public String getValue() {
            return value;
        }
    }

    public String retrievePassword() throws CoreException {
        PasswordDialog dialog = new PasswordDialog(null);
        int ret = dialog.open();
        if (ret == PasswordDialog.OK) {
            return dialog.getValue();
        }
        throw new CoreException(Core.ERROR_CANCELLATION);
    }
}
