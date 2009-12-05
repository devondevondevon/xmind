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
package org.xmind.ui.internal.spellsupport;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.xmind.ui.texteditor.ISpellingSupport;

/**
 * 
 * @author Karelun huang
 */
public class SpellingSupport {

    private static ISpellingSupport impl = null;

    private static boolean failed = false;

    public static ISpellingSupport getInstance() {
        if (impl == null && !failed) {
            impl = createImplementation();
            if (impl == null)
                failed = true;
        }
        if (impl != null)
            return impl;
        return ISpellingSupport.NULL;
    }

    private static ISpellingSupport createImplementation() {
        Bundle bundle = Platform.getBundle("org.xmind.ui.spelling"); //$NON-NLS-1$
        if (bundle != null) {
            try {
                Class clazz = bundle
                        .loadClass("org.xmind.ui.internal.spelling.JazzySpellingSupport"); //$NON-NLS-1$
                return (ISpellingSupport) clazz.newInstance();
            } catch (Throwable e) {
            }
        }
        try {
            return (ISpellingSupport) Class.forName(
                    "org.xmind.ui.internal.spelling.JazzySpellingSupport") //$NON-NLS-1$
                    .newInstance();
        } catch (Throwable ignore) {
            // we don't rely on that implementation
        }
        return null;
    }
}
