/*
 * $Id: VerticalXSDisplay.java,v 1.17 2006/12/27 20:14:09 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.unidata.idv;



import ucar.visad.display.XSDisplay;

import visad.*;

import java.awt.*;

import java.rmi.RemoteException;


/**
 * A VisAD display for 2D vertical cross sections of 3D data fields.
 *
 * @author Don Murray
 */
public class VerticalXSDisplay extends XSDisplay {


    /**
     * The name of the altitude property.
     */
    public static final String CURSOR_ALTITUDE = "cursorAltitude";

    /**
     * The cursor altitude.
     * @serial
     */
    private volatile Real cursorAltitude;

    /**
     * Default cstr with yAxisType of RealType.Altitude, xAxisType of
     * RealType.XAxis.
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public VerticalXSDisplay() throws VisADException, RemoteException {
        super("Vertical Cross Section", RealType.XAxis, RealType.Altitude);
        setAxisParams();
    }

    /**
     * Called on construction to initialize the axis parameters
     */
    private void setAxisParams() {

        // Make AxisScales-s - label the axes.
        //Font ftr = new Font ("Helvetica", Font.BOLD, 4);
        showAxisScales(true);
        AxisScale xscale = getXAxisScale();
        if (xscale != null) {
            xscale.setTitle("Distance along transect (km)");
            xscale.setSnapToBox(true);
            xscale.setColor(Color.blue);
            xscale.setAutoComputeTicks(true);
        }
        AxisScale zscale = getYAxisScale();
        if (zscale != null) {
            zscale.setSnapToBox(true);
            zscale.setColor(Color.blue);
            zscale.setAutoComputeTicks(true);
        }
    }

    /**
     * Sets the cursor altitude property.  Called by subclasses.
     *
     * @param altitude          The cursor altitude.
     *
     * @throws RemoteException
     * @throws VisADException
     */
    protected void setCursorAltitude(Real altitude)
            throws VisADException, RemoteException {
        Real oldAltitude = cursorAltitude;
        cursorAltitude = altitude;
        firePropertyChange(CURSOR_ALTITUDE, oldAltitude, cursorAltitude);
    }

    /**
     * Gets the cursor altitude property.
     *
     * @return The currently-selected altitude.  May be <code>null</code>.
     */
    public Real getCursorAltitude() {
        return cursorAltitude;
    }

}

