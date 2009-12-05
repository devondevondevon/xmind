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
package org.xmind.gef.service;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.xmind.gef.IGraphicalViewer;
import org.xmind.gef.draw2d.geometry.Geometry;
import org.xmind.gef.part.IGraphicalPart;

public class SimpleRevealService extends GraphicalViewerService implements
        IRevealService {

    public SimpleRevealService(IGraphicalViewer viewer) {
        super(viewer);
    }

    protected void activate() {
    }

    protected void deactivate() {
    }

    public void reveal(ISelection selection) {
        if (isActive()) {
            getViewer().ensureVisible(calcSelectionBounds(selection));
        }
    }

    protected Rectangle calcSelectionBounds(ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            Object[] elements = ((IStructuredSelection) selection).toArray();
            Rectangle r = null;
            for (Object o : elements) {
                IGraphicalPart part = getViewer().findGraphicalPart(o);
                if (part != null) {
                    Rectangle bounds = ((IGraphicalPart) part).getFigure()
                            .getBounds();
                    r = Geometry.union(r, bounds);
                }
            }
            return r;
        }
        return null;
    }

}