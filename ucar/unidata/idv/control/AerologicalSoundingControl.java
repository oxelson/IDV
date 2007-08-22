/*
 * $Id: AerologicalSoundingControl.java,v 1.30 2007/05/23 20:45:24 dmurray Exp $
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

package ucar.unidata.idv.control;


import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.idv.ViewDescriptor;
import ucar.unidata.idv.ViewManager;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.view.sounding.AerologicalCellNetwork;
import ucar.unidata.view.sounding.AerologicalCoordinateSystem;
import ucar.unidata.view.sounding.AerologicalDisplay;
import ucar.unidata.view.sounding.AerologicalDisplayConstants;
import ucar.unidata.view.sounding.AerologicalReadoutTable;
import ucar.unidata.view.sounding.CellNetwork;
import ucar.unidata.view.sounding.ComputeCell;
import ucar.unidata.view.sounding.Hodograph3DDisplay;
import ucar.unidata.view.sounding.ParcelMode;
import ucar.unidata.view.sounding.PseudoAdiabaticDisplayable;
import ucar.unidata.view.sounding.RealEvaluatorCell;
import ucar.unidata.view.sounding.Sounding;

import ucar.visad.display.Displayable;

import ucar.visad.display.DisplayableData;
import ucar.visad.display.DisplayableDataRef;
import ucar.visad.display.LineDrawing;
import ucar.visad.functiontypes.AirTemperatureProfile;
import ucar.visad.functiontypes.CartesianHorizontalWindOfPressure;
import ucar.visad.functiontypes.DewPointProfile;
import ucar.visad.quantities.AirPressure;
import ucar.visad.quantities.AirTemperature;
import ucar.visad.quantities.CartesianHorizontalWind;
import ucar.visad.quantities.PolarHorizontalWind;

import visad.ActionImpl;
import visad.CoordinateSystem;
import visad.DataReference;
import visad.DataReferenceImpl;
import visad.ErrorEstimate;
import visad.Field;
import visad.FlowControl;
import visad.GraphicsModeControl;
import visad.Gridded1DSet;
import visad.Linear1DSet;
import visad.Real;
import visad.RealTuple;
import visad.RealTupleType;
import visad.RealType;
import visad.SampledSet;
import visad.Unit;
import visad.VisADException;

import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;


import java.awt.*;
import java.awt.event.*;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;

import java.rmi.RemoteException;

import java.util.List;

import javax.swing.*;
import javax.swing.border.BevelBorder;


/**
 * Abstract class for displaying an aerological Skew-T log p diagram of an
 * atmospheric sounding.
 *
 * @author IDV Development Team
 * @version $Revision: 1.30 $Date: 2007/05/23 20:45:24 $
 */
public abstract class AerologicalSoundingControl extends DisplayControlImpl implements AerologicalDisplayConstants {

    /** The view manager for this control */
    protected ViewManager viewManager;

    /**
     * The Skew-T log p display.
     */
    protected AerologicalDisplay aeroDisplay;  // accessed in subclasses

    /**
     * The 3D hodograph display.
     */
    protected Hodograph3DDisplay hodoDisplay;  // accessed in subclasses

    /** label for the location */
    private JLabel locLabel;

    /** readout panel */
    private JPanel readoutPanel;

    /** the parcel path */
    private PseudoAdiabaticDisplayable parcelPath;

    /** the parcel virtual temperature path */
    private DisplayableDataRef parVirtTempPath;

    /** the environmental virtual temperature path */
    private DisplayableDataRef envVirtTempPath;

    /** flag for whehter the trajectory is active */
    private boolean trajectoryActive = false;

    /** flag for whehter the virtual temperature is active */
    private boolean virtTempActive = false;

    /** parcel mode index */
    private int parcelModeIndex = 0;

    /** the evaluation network */
    private CellNetwork proEvalNet;

    /** the cell network */
    private AerologicalCellNetwork aeroCellNet;

    /** a reference for the temperature profile */
    private DataReferenceImpl tempProRef;

    /** a reference for the dewpoint profile */
    private DataReferenceImpl dewProRef;

    /** a reference for the wind profile */
    private DataReferenceImpl windProRef;

    /** the temperature profiles */
    private Field[] tempProfiles;

    /** the dewpoint profiles */
    private Field[] dewProfiles;

    /** the wind profiles */
    private Field[] windProfiles;

    /** the current index */
    private int currIndex = 0;

    /** flag for visibility of the spatial locations */
    private boolean spatialLociVisible;

    /** the displayable for the spatial locations */
    private Displayable spatialLoci;

    /** display type */
    private String displayType = SKEWT_DISPLAY;

    /** wind sampling modes */
    private static float[] MANDATORY_LEVEL_VALUES = {
        1000.f, 925.f, 850.f, 700.f, 500.f, 400.f, 300.f, 250.f, 200.f, 150.f,
        100.f
    };

    /** Identifier for all wind levels */
    private static final String ALL_LEVELS = "All";

    /** Identifier for mandatory levels */
    private static final String MANDATORY_LEVELS = "Mandatory Levels";

    /** default wind sampling mode */
    private String windBarbSpacing = ALL_LEVELS;

    /** cursor pressure data reference */
    private DataReferenceImpl cursorPresRef;

    /** point pressure data reference */
    private DataReferenceImpl pointerPresRef;

    /** cursor temperature data reference */
    private DataReferenceImpl cursorTempRef;

    /** cursor wind data reference */
    private DataReferenceImpl cursorWindRef;

    /** pointer temperature data reference */
    private DataReferenceImpl pointerTempRef;

    /** cursor pressure data reference */
    private AerologicalReadoutTable readoutTable;

    /** location */
    private LatLonPoint location;

    /**
     * Class ParcelModeInfo
     */
    private static class ParcelModeInfo {

        /** the label for the info */
        private final String label;

        /** the parcel mode */
        private final ParcelMode mode;

        /**
         * Create a new ParcelModeInfo
         *
         * @param label   the label
         * @param mode    the mode
         */
        private ParcelModeInfo(String label, ParcelMode mode) {
            this.label = label;
            this.mode  = mode;
        }

        /**
         * String representation of this object
         *
         * @return String representation of this object
         */
        public String toString() {
            return label;
        }
    }

    /** array of parcel mode infos */
    private static ParcelModeInfo[] parcelModeInfos = { new ParcelModeInfo(
                                                          "Bottom of Sounding",
                                                          ParcelMode.BOTTOM),
            new ParcelModeInfo("Below Cursor", ParcelMode.LAYER),
            new ParcelModeInfo("At Cursor Pressure", ParcelMode.PRESSURE),
            new ParcelModeInfo("At Cursor (Press,Temp)", ParcelMode.POINT), };

    /**
     * Constructs from nothing.
     *
     * @param lociVisible      Whether or not the spatial loci are initially
     *                         visible.
     * @throws VisADException  if a VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    AerologicalSoundingControl(boolean lociVisible)
            throws VisADException, RemoteException {
        setAttributeFlags(FLAG_COLOR);
        spatialLociVisible = lociVisible;
    }

    /**
     * Initializes this instance according to profile data references.
     *
     * @return                 True if and only if this instance was initialized
     *                         OK.
     * @throws VisADException  couldn't create a VisAD object needed
     * @throws RemoteException couldn't create a remote object needed
     */
    boolean init() throws VisADException, RemoteException {

        aeroDisplay = AerologicalDisplay.getInstance(displayType,
                getGraphicsConfiguration(true, false));
        hodoDisplay = new Hodograph3DDisplay();
        viewManager = new ViewManager(
            getViewContext(), aeroDisplay, new ViewDescriptor("SkewTView"),
            "showControlLegend=false;wireframe=false;aniReadout=false") {
            public boolean animationOk() {
                return false;
            }

            public boolean getShowDisplayList() {
                return true;
            }

            public void setShowDisplayList(boolean v) {
                super.setShowDisplayList(true);
            }

        };

        //TODO: For now don't do this because it screws up the image dumping.
        //If and when we put this back we need to not destroy the
        //VM in our doRemove method
        addViewManager(viewManager);
        // aeroDisplay.setPointMode(true);  // for debugging
        locLabel   = new JLabel(" ", JLabel.LEFT);
        tempProRef = new DataReferenceImpl("TemperatureProfile");
        dewProRef  = new DataReferenceImpl("DewPointProfile");
        windProRef = new DataReferenceImpl("WindProfile");

        tempProRef.setData(AirTemperatureProfile.instance().missingData());
        dewProRef.setData(DewPointProfile.instance().missingData());
        windProRef.setData(
            CartesianHorizontalWindOfPressure.instance().missingData());




        cursorPresRef  = new DataReferenceImpl("CursorPressure");
        pointerPresRef = new DataReferenceImpl("PointerPressure");
        cursorTempRef  = new DataReferenceImpl("CursorTemperature");
        cursorWindRef  = new DataReferenceImpl("CursorWind");
        pointerTempRef = new DataReferenceImpl("pointerTemperature");


        DataReferenceImpl minPresRef =
            new DataReferenceImpl("MinimumPressure");

        minPresRef.setData(aeroDisplay.getMinimumPressure());
        cursorPresRef.setData(AirPressure.getRealType().missingData());
        pointerPresRef.setData(AirPressure.getRealType().missingData());
        cursorTempRef.setData(AirTemperature.getRealType().missingData());
        cursorWindRef.setData(
            CartesianHorizontalWind.getRealTupleType().missingData());
        pointerTempRef.setData(AirTemperature.getRealType().missingData());

        aeroCellNet = new AerologicalCellNetwork(tempProRef, dewProRef,
                cursorPresRef, cursorTempRef, minPresRef, windProRef);

        setParcelMode(parcelModeIndex);

        parcelPath =
            new PseudoAdiabaticDisplayable(aeroCellNet.getDryTrajectoryRef(),
                                           aeroCellNet.getWetTrajectoryRef());

        parcelPath.setLineWidth(2);
        parcelPath.setColor(java.awt.Color.pink);
        addDisplayable(parcelPath, viewManager);

        parVirtTempPath = new DisplayableDataRef(
            aeroCellNet.getParcelVirtualTemperatureProfileRef());

        parVirtTempPath.setLineWidth(2);
        parVirtTempPath.setLineStyle(GraphicsModeControl.DASH_STYLE);
        parVirtTempPath.setColor(java.awt.Color.pink);
        addDisplayable(parVirtTempPath, viewManager);

        envVirtTempPath = new DisplayableDataRef(
            aeroCellNet.getEnvironmentVirtualTemperatureProfileRef());

        envVirtTempPath.setLineWidth(2);
        envVirtTempPath.setLineStyle(GraphicsModeControl.DASH_STYLE);
        envVirtTempPath.setColor(java.awt.Color.red);
        addDisplayable(envVirtTempPath, viewManager);

        readoutTable = new AerologicalReadoutTable();


        aeroDisplay.addPropertyChangeListener(this);

        final DataReference profileTempRef =
            aeroCellNet.getProfileTemperatureRef();

        new ActionImpl("ProfileTemperature") {
            public void doAction() {
                try {
                    readoutTable.setProfileTemperature(
                        (Real) profileTempRef.getData());
                } catch (Exception ex) {
                    logException(ex);
                }
            }
        }.addReference(profileTempRef);

        final DataReference profileDewRef =
            aeroCellNet.getProfileDewPointRef();

        new ActionImpl("ProfileDewPoint") {

            public void doAction() {

                try {
                    readoutTable.setProfileDewPoint(
                        (Real) profileDewRef.getData());
                } catch (Exception ex) {
                    logException(ex);
                }
            }
        }.addReference(profileDewRef);

        final DataReference profileMixingRatioRef =
            aeroCellNet.getProfileMixingRatioRef();

        new ActionImpl("ProfileMixingRatio") {

            public void doAction() {

                try {
                    readoutTable.setProfileMixingRatio(
                        (Real) profileMixingRatioRef.getData());
                } catch (Exception ex) {
                    logException(ex);
                }
            }
        }.addReference(profileMixingRatioRef);

        /*
        final DataReference profileWindRef =
            aeroCellNet.getProfileWindRef();

        new ActionImpl("ProfileWind") {

            public void doAction() {

                try {
                    RealTuple spdDir =
                        PolarHorizontalWind.newRealTuple(
                            (RealTuple) proWindAtPointPresRef.getData());
                    readoutTable.setProfileWindSpeed((Real)spdDir.getComponent(0));
                    readoutTable.setProfileWindDirection((Real)spdDir.getComponent(1));
                } catch (Exception ex) {
                    logException(ex);
                }
            }
        }.addReference(profileWindRef);
        */

        final DataReference lclPressureRef = aeroCellNet.getLclPressureRef();

        new ActionImpl("LclPressure") {

            public void doAction() {

                try {
                    readoutTable.setLclPressure(
                        (Real) lclPressureRef.getData());
                } catch (Exception ex) {
                    logException(ex);
                }
            }
        }.addReference(lclPressureRef);

        final DataReference lclTemperatureRef =
            aeroCellNet.getLclTemperatureRef();

        new ActionImpl("LclTemperature") {

            public void doAction() {

                try {
                    readoutTable.setLclTemperature(
                        (Real) lclTemperatureRef.getData());
                } catch (Exception ex) {
                    logException(ex);
                }
            }
        }.addReference(lclTemperatureRef);

        final DataReference capeRef = aeroCellNet.getCapeRef();

        new ActionImpl("CAPE") {

            public void doAction() {

                try {
                    readoutTable.setCape((Real) capeRef.getData());
                } catch (Exception ex) {
                    logException(ex);
                }
            }
        }.addReference(capeRef);

        final DataReference lfcRef = aeroCellNet.getLfcRef();

        new ActionImpl("LFC") {

            public void doAction() {

                try {
                    readoutTable.setLfc((Real) lfcRef.getData());
                } catch (Exception ex) {
                    logException(ex);
                }
            }
        }.addReference(lfcRef);

        final DataReference lfcTempRef = aeroCellNet.getLfcTemperatureRef();

        new ActionImpl("LfcTemperature") {

            public void doAction() {

                try {
                    readoutTable.setLfcTemperature(
                        (Real) lfcTempRef.getData());
                } catch (Exception ex) {
                    logException(ex);
                }
            }
        }.addReference(lfcTempRef);

        final DataReference lnbRef = aeroCellNet.getLnbRef();

        new ActionImpl("LNB") {

            public void doAction() {

                try {
                    readoutTable.setLnb((Real) lnbRef.getData());
                } catch (Exception ex) {
                    logException(ex);
                }
            }
        }.addReference(lnbRef);

        final DataReference lnbTempRef = aeroCellNet.getLnbTemperatureRef();

        new ActionImpl("LnbTemperature") {

            public void doAction() {

                try {
                    readoutTable.setLnbTemperature(
                        (Real) lnbTempRef.getData());
                } catch (Exception ex) {
                    logException(ex);
                }
            }
        }.addReference(lnbTempRef);

        final DataReference cinRef = aeroCellNet.getCinRef();

        new ActionImpl("CIN") {

            public void doAction() {

                try {
                    readoutTable.setCin((Real) cinRef.getData());
                } catch (Exception ex) {
                    logException(ex);
                }
            }
        }.addReference(cinRef);

        /*
         * Configure a computational network for evaluating the temperature,
         * dew-point, and wind profiles at the mouse-pointer pressure.
         */
        {
            proEvalNet = new CellNetwork();

            ComputeCell proTempAtPointPres =
                new RealEvaluatorCell(tempProRef, pointerPresRef,
                                      cursorTempRef.getData());

            proEvalNet.add(proTempAtPointPres);

            ComputeCell proDewAtPointPres = new RealEvaluatorCell(dewProRef,
                                                pointerPresRef,
                                                cursorTempRef.getData());

            proEvalNet.add(proDewAtPointPres);

            ComputeCell proWindAtPointPres =
                new RealEvaluatorCell(windProRef, pointerPresRef,
                                      cursorWindRef.getData());

            proEvalNet.add(proWindAtPointPres);
            proEvalNet.configure();

            final DataReference proTempAtPointPresRef =
                proTempAtPointPres.getOutputRef();

            new ActionImpl("PointerProfileTemperature") {

                public void doAction() {

                    try {
                        readoutTable.setProfileTemperature(
                            (Real) proTempAtPointPresRef.getData());
                    } catch (Exception ex) {
                        logException(ex);
                    }
                }
            }.addReference(proTempAtPointPresRef);

            final DataReference proDewAtPointPresRef =
                proDewAtPointPres.getOutputRef();

            new ActionImpl("PointerProfileDewPoint") {

                public void doAction() {

                    try {
                        readoutTable.setProfileDewPoint(
                            (Real) proDewAtPointPresRef.getData());
                    } catch (Exception ex) {
                        logException(ex);
                    }
                }
            }.addReference(proDewAtPointPresRef);

            final DataReference proWindAtPointPresRef =
                proWindAtPointPres.getOutputRef();

            new ActionImpl("PointerProfileWind") {

                public void doAction() {

                    try {
                        RealTuple spdDir =
                            (RealTuple) proWindAtPointPresRef.getData();
                        if ( !(PolarHorizontalWind.getRealTupleType().equals(
                                (RealTupleType) spdDir.getType()))) {
                            spdDir = PolarHorizontalWind.newRealTuple(spdDir);
                        }
                        readoutTable.setProfileWindSpeed(
                            (Real) spdDir.getComponent(0));
                        readoutTable.setProfileWindDirection(
                            (Real) spdDir.getComponent(1));
                    } catch (Exception ex) {
                        logException(ex);
                    }
                }
            }.addReference(proWindAtPointPresRef);
        }

        readoutPanel = new JPanel();

        readoutPanel.setLayout(new BorderLayout());
        readoutPanel.add(readoutTable);
        aeroDisplay.draw();
        hodoDisplay.draw();
        setTrajectoryActive(trajectoryActive);
        setVirtTempActive(virtTempActive);

        return true;
    }


    /**
     * Handle property change
     *
     * @param event The event
     */
    public void propertyChange(PropertyChangeEvent event) {
        try {
            if (event.getPropertyName().equals(
                    AerologicalDisplay.CURSOR_PRESSURE)) {
                Real pressure = (Real) event.getNewValue();
                cursorPresRef.setData(pressure);
                readoutTable.setPressure(pressure);
            } else if (event.getPropertyName().equals(
                    AerologicalDisplay.POINTER_PRESSURE)) {
                Real pressure = (Real) event.getNewValue();
                pointerPresRef.setData(pressure);
                readoutTable.setPressure(pressure);
            } else if (event.getPropertyName().equals(
                    AerologicalDisplay.CURSOR_TEMPERATURE)) {
                Real temp = (Real) event.getNewValue();
                cursorTempRef.setData(temp);
                readoutTable.setBackgroundTemperature(temp);
            } else if (event.getPropertyName().equals(
                    AerologicalDisplay.POINTER_TEMPERATURE)) {
                Real temp = (Real) event.getNewValue();
                readoutTable.setBackgroundTemperature(temp);
            } else {
                super.propertyChange(event);
            }
        } catch (Exception e) {
            logException(e);
        }
    }




    /**
     * Get specific menu items for the View menu
     *
     * @param items items
     * @param forMenuBar true for menu bar, false for popup
     */
    protected void getFileMenuItems(List items, boolean forMenuBar) {
        super.getFileMenuItems(items, forMenuBar);
        JMenuItem saveButton = new JMenuItem("Save Image...");
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                viewManager.doSaveImageInThread();
            }
        });
        items.add(saveButton);
    }


    /**
     * Remove this control. Call the parent  class doRemove and clears
     * references to gridLocs, etc.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    public void doRemove() throws VisADException, RemoteException {
        super.doRemove();
        /* Don't need to do this since we have the base class handle the view manager
        if (viewManager != null) {
            viewManager.destroy();
            viewManager = null;
        }
        */
        aeroDisplay     = null;
        hodoDisplay     = null;
        locLabel        = null;
        readoutPanel    = null;
        parcelPath      = null;
        parVirtTempPath = null;
        envVirtTempPath = null;
        proEvalNet      = null;
        aeroCellNet     = null;
        tempProRef      = null;
        dewProRef       = null;
        windProRef      = null;
        tempProfiles    = null;
        dewProfiles     = null;
        windProfiles    = null;
    }


    /**
     * Set the spatial location displayable
     *
     * @param loci  Displayable for spatial locations
     */
    protected void setSpatialLoci(Displayable loci) {
        spatialLoci = loci;
        try {
            setSpatialLociVisible(spatialLociVisible);
        } catch (Exception exc) {}
    }




    /**
     * Sets the visibility of the spatial loci in the main, 3D window.
     *
     * @param visible           If true, then the loci will be rendered visible;
     *                          otherwise, they will be rendered invisible.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    public void setSpatialLociVisible(boolean visible)
            throws VisADException, RemoteException {
        spatialLociVisible = visible;
        if (spatialLoci != null) {
            spatialLoci.setVisible(getDisplayVisibility()
                                   && spatialLociVisible);
        }
    }

    /**
     * Override base class method so we can control the visiblity of
     * the grid points.
     *
     * @param on true to make it visible
     */
    public void setDisplayVisibility(boolean on) {
        if (settingVisibility) {
            return;
        }
        if ( !getHaveInitialized()) {
            return;
        }
        super.setDisplayVisibility(on);
        try {
            setSpatialLociVisible(spatialLociVisible);
            setTrajectoryActive(trajectoryActive);
            setVirtTempActive(virtTempActive);
        } catch (Exception exc) {}
    }




    /**
     * Returns the visibility of the spatial loci in the main, 3D window.
     *
     *                          otherwise, they will be rendered invisible.
     *
     * @return true if loci are visible
     */
    public boolean getSpatialLociVisible() {
        return spatialLociVisible;
    }



    /**
     * Sets the currently-displayed sounding.  The index is that of the
     * soundings set by {@link #setSoundings(Field[], Field[], Field[]}.
     * If there are no soundings, then nothing is done.
     *
     * @param index                The index of the sounding to display.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    void setSounding(int index) throws VisADException, RemoteException {

        if (tempProfiles != null) {

            /*
             * The following nested try-blocks ensure that the display
             * returns to its original state if an exception occurs.
             */
            try {
                aeroDisplay.setProfileVisible(currIndex, false);
                aeroDisplay.setProfileVisible(index, true);
                hodoDisplay.setProfileVisible(currIndex, false);
                hodoDisplay.setProfileVisible(index, true);

                try {
                    aeroDisplay.setActiveSounding(index);
                    hodoDisplay.setActiveWindProfile(index);
                    // need to get the actual data for manipulation
                    Sounding s = aeroDisplay.getActiveSounding();
                    setSounding(s.getTemperatureField(),
                                s.getDewPointField(), windProfiles[index]);  // not manipuable

                    currIndex = index;
                } catch (VisADException ex) {
                    aeroDisplay.setProfileVisible(index, false);
                    hodoDisplay.setProfileVisible(index, false);

                    throw ex;
                } catch (RemoteException ex) {
                    aeroDisplay.setProfileVisible(index, false);
                    hodoDisplay.setProfileVisible(index, false);

                    throw ex;
                }
            } catch (VisADException ex) {
                // need to get the actual data for manipulation
                Sounding s = aeroDisplay.getActiveSounding();
                setSounding(s.getTemperatureField(), s.getDewPointField(),
                            windProfiles[currIndex]);  // not manipuable
                throw ex;

            } catch (RemoteException ex) {
                // need to get the actual data for manipulation
                Sounding s = aeroDisplay.getActiveSounding();
                setSounding(tempProfiles[currIndex], dewProfiles[currIndex],
                            windProfiles[currIndex]);

                throw ex;
            }
        }
    }

    /**
     * Set the sounding profiles from the data
     *
     * @param tempPro   temperature profile
     * @param dewPro    dewpoint profile
     * @param windPro   wind profile
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    private void setSounding(Field tempPro, Field dewPro, Field windPro)
            throws VisADException, RemoteException {
        tempProRef.setData(tempPro);
        dewProRef.setData(dewPro);
        windProRef.setData(windPro);
        updateLegendAndList();
    }

    /**
     * Sets the set of soundings -- completely replacing the previous set.
     *
     * @param tempPros         The temperature profiles.
     * @param dewPros          The dew-point profiles.
     * @param windPros         The wind profiles
     *
     * @throws VisADException  if a VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    void setSoundings(Field[] tempPros, Field[] dewPros, Field[] windPros)
            throws VisADException, RemoteException {

        int n = tempPros.length;

        if (tempProfiles == null) {
            tempProfiles = (Field[]) tempPros.clone();
            dewProfiles  = (Field[]) dewPros.clone();
            windProfiles = (Field[]) windPros.clone();
        } else {
            System.arraycopy(tempPros, 0, tempProfiles, 0, n);
            System.arraycopy(dewPros, 0, dewProfiles, 0, n);
            System.arraycopy(windPros, 0, windProfiles, 0, n);
        }

        for (int i = 0; i < n; i++) {
            aeroDisplay.addProfile(i, tempProfiles[i], dewProfiles[i],
                                   windProfiles[i]);
            hodoDisplay.addProfile(i, windProfiles[i]);
        }

        aeroDisplay.setProfileVisible(currIndex, true);
        hodoDisplay.setProfileVisible(currIndex, true);
        setSounding(currIndex);
    }

    /**
     * <p>Returns the title of this display.</p>
     *
     *
     * @return                       The title of this display.
     */
    protected final String xxxgetTitle() {
        return "SkewT";
    }

    /**
     * Indicates if this instance displays the path of a lifted parcel.
     *
     * @return                       True if and only if the path is or will be
     *                               displayed.
     */
    public synchronized final boolean getTrajectoryActive() {
        return trajectoryActive;
    }

    /**
     * Sets whether or not this instance will display the path of a lifted
     * parcel.
     *
     * @param active                 Whether or not to display the path.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    public synchronized final void setTrajectoryActive(boolean active)
            throws VisADException, RemoteException {

        trajectoryActive = active;

        if (parcelPath != null) {
            parcelPath.setVisible(trajectoryActive);
        }

        if (parVirtTempPath != null) {
            parVirtTempPath.setVisible(trajectoryActive && virtTempActive);
        }
    }

    /**
     * Indicates whether or not this instance is or will display virtual
     * temperature paths.
     *
     * @return                       True if and only if virtual temperature
     *                               paths are or will be displayed.
     */
    public synchronized final boolean getVirtTempActive() {
        return virtTempActive;
    }

    /**
     * Sets whether or not this instance will display virtual temperature
     * paths.
     *
     * @param active                 Whether or not to display virtual
     *                               temperature paths.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    public synchronized final void setVirtTempActive(boolean active)
            throws VisADException, RemoteException {

        virtTempActive = active;

        if (envVirtTempPath != null) {
            envVirtTempPath.setVisible(virtTempActive);
        }

        if (parVirtTempPath != null) {
            parVirtTempPath.setVisible(trajectoryActive && virtTempActive);
        }
    }

    /**
     * Returns the mode that is used to determine the initial conditions of the
     * lifted parcel.
     *
     * @return                       The mode used to determine the initial
     *                               conditions of the lifted parcel.
     */
    public synchronized final ParcelMode getParcelMode() {
        return parcelModeInfos[parcelModeIndex].mode;
    }

    /**
     * Sets the mode used to determine the initial conditions of the lifted
     * parcel.
     *
     * @param mode                   The mode used to determine the initial
     *                               conditions of the lifted parcel.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    public synchronized final void setParcelMode(ParcelMode mode)
            throws VisADException, RemoteException {

        for (int i = 0; i < parcelModeInfos.length; i++) {
            if (parcelModeInfos[i].mode == mode) {
                setParcelMode(i);

                break;
            }
        }
    }

    /**
     * Set the parcel mode
     *
     * @param i   the parcel mode (index into array)
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    private void setParcelMode(int i) throws VisADException, RemoteException {

        if ((i >= 0) && (i < parcelModeInfos.length)) {
            if (aeroCellNet != null) {
                aeroCellNet.setParcelMode(parcelModeInfos[i].mode);
            }

            parcelModeIndex = i;
        }
    }

    /**
     * Creates the Skew-T display component.
     *
     * @return                       The Skew-T display component.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {

        JCheckBox trajBox = new JCheckBox("Parcel Path", trajectoryActive);

        trajBox.setToolTipText("Display Parcel's Pseudoadiabatic Path");
        trajBox.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent event) {

                try {
                    setTrajectoryActive(event.getStateChange()
                                        == ItemEvent.SELECTED);
                } catch (Exception e) {
                    logException(e);
                }
            }
        });

        JCheckBox virtTempBox = new JCheckBox("Virtual Temperature",
                                    virtTempActive);

        virtTempBox.setToolTipText("Display Virtual Temperatures");
        virtTempBox.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent event) {

                try {
                    setVirtTempActive(event.getStateChange()
                                      == ItemEvent.SELECTED);
                } catch (Exception e) {
                    logException(e);
                }
            }
        });

        final JComboBox parcelModeBox = new JComboBox(parcelModeInfos);

        parcelModeBox.setToolTipText("Parcel Determination Mode");
        parcelModeBox.setSelectedIndex(parcelModeIndex);
        parcelModeBox.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {

                try {
                    setParcelMode(parcelModeBox.getSelectedIndex());
                } catch (Exception ex) {
                    logException(ex);
                }
            }
        });


        String[]        spacings   = new String[] {
            ALL_LEVELS, MANDATORY_LEVELS, "10", "25", "50", "100"
        };
        final JComboBox spacingBox = new JComboBox(spacings);

        spacingBox.setToolTipText("Wind Barb Spacing");
        spacingBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                try {

                    String tmp = (String) spacingBox.getSelectedItem();
                    setWindLevels(tmp);
                    windBarbSpacing = tmp;

                } catch (Exception ex) {
                    logException(ex);
                }
            }
        });
        spacingBox.setSelectedItem(windBarbSpacing);


        JCheckBox showBox = new JCheckBox(getSpatialLociLabel(),
                                          spatialLociVisible);

        showBox.setToolTipText(
            "Show Spatial Loci of Soundings in Main, 3-D Window");
        showBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                try {
                    setSpatialLociVisible(event.getStateChange()
                                          == ItemEvent.SELECTED);
                } catch (Exception e) {
                    logException(e);
                }
            }
        });

        GuiUtils.tmpInsets = new Insets(4, 4, 4, 4);
        Component comboBoxes = GuiUtils.doLayout(new Component[] {
            GuiUtils.rLabel("Parcel mode:"), parcelModeBox, GuiUtils.filler(),
            GuiUtils.rLabel("Wind spacing:"), spacingBox,
            GuiUtils.lLabel("(hPa)")
        }, 3,              //3 columns
           GuiUtils.WT_N,  //Don't expand the grid horizontally
           GuiUtils.WT_N   //Don't expand the grid  vertically
               );

        Container checkBoxes = GuiUtils.vbox(trajBox, virtTempBox, showBox);

        /*
         * Do not fill the widgets in the gridbag.
         * Create a 2 column by 2 row GridBagLayout,
         * Don't expand the widgets:
         */
        Component viewOptions = GuiUtils.doLayout(new Component[] {
                                    checkBoxes,
                                    GuiUtils.filler(),
                                    GuiUtils.top(comboBoxes) }, 3,  //3 columns
            GuiUtils.WT_N,  //Don't expand the grid horizontally
            GuiUtils.WT_N   //Don't expand the grid  vertically
                );
        Container controlArea    = Box.createHorizontalBox();
        Component specificWidget = getSpecificWidget();

        if (specificWidget != null) {
            controlArea.add(GuiUtils.center(specificWidget));
        }

        controlArea.add(GuiUtils.inset(viewOptions, 4));

        //GuiUtils.leftRight(viewManager.getContents (),
        //hodoDisplay.getComponent()), controlArea);

        JScrollPane sp =
            new JScrollPane(
                readoutPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setViewportBorder(
            BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        JSplitPane spl = GuiUtils.hsplit(sp, viewManager.getContents(), .35);
        /*
        JSplitPane spl =
            GuiUtils
                .hsplit(sp, GuiUtils
                    .hsplit(viewManager.getContents(), hodoDisplay
                        .getComponent(), .50), .35);
        */
        spl.setOneTouchExpandable(true);
        Container contents = GuiUtils.topCenterBottom(locLabel,
        // GuiUtils.hbox(sp, viewManager.getContents(), hodoDisplay.getComponent()),
        spl, controlArea);

        return contents;
    }

    /**
     * <p>Returns the data-specific widget for controlling the data-specific
     * aspects of the display or <code>null</code> if no such widget exists.
     * The widget is added to the window in the apppropriate place.</p>
     *
     * <p>This implementation returns <code>null</code>.
     *
     * @return                      The data-specific control-widget or
     *                              <code>null</code>.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    Component getSpecificWidget() throws VisADException, RemoteException {
        return null;
    }

    /**
     *  Return the label used for the spatial loci checkbox.
     *  This can get overwritten by derived classes to return the
     *  correct label.
     *
     * @return true if they are visible
     */
    protected String getSpatialLociLabel() {
        return "Spatial Loci";
    }


    /**
     * Gets the location of the profile.
     *
     * @return The location of the profile (may be null).
     */
    protected final LatLonPoint getLocation() {
        return location;
    }

    /**
     * Sets the location of the profile.
     *
     * @param loc                   The location of the profile.
     * @throws VisADException       VisAD failure.
     * @throws RemoteException      Java RMI failure.
     */
    final void setLocation(LatLonPoint loc)
            throws VisADException, RemoteException {

        location = loc;
        double lat = loc.getLatitude().getValue();
        double lon = loc.getLongitude().getValue();

        locLabel.setText(" Lat: " + getDisplayConventions().format(lat)
                         + "  Long: " + getDisplayConventions().format(lon));
        aeroDisplay.setBarbOrientation((lat >= 0)
                                       ? FlowControl.NH_ORIENTATION
                                       : FlowControl.SH_ORIENTATION);
    }

    /**
     * Returns the latitude/longitude point corresponding to a display X/Y
     * point or <code>null</code> if no such point exists.
     *
     * @param xy                 The display X/Y point.
     * @return                   The corresponding lat/lon position or <code>
     *                           null</code>.
     * @throws VisADException    if a VisAD failure occurs.
     * @throws RemoteException   if a Java RMI failure occurs.
     */
    final LatLonPoint latLon(RealTuple xy)
            throws VisADException, RemoteException {

        LatLonPoint latLon = null;
        Real[]      reals  = xy.getRealComponents();

        if (getControlContext() != null) {
            EarthLocationTuple elt =
                (EarthLocationTuple) boxToEarth(new double[] {
                    reals[0].getValue(),
                    reals[1].getValue(), (xy.getDimension() == 3)
                                         ? reals[0].getValue()
                                         : 1.0 });

            latLon = elt.getLatLonPoint();
        }

        return latLon;
    }

    /**
     * Returns the display X/Y point corresponding to a latitude/longitude
     * point or <code>null</code> if no such point exists.
     *
     * @param latLon             The display lat/lon point.
     *
     * @return                   The corresponding X/Y position or <code>
     *                           null</code>.
     * @throws VisADException    if a VisAD failure occurs.
     * @throws RemoteException   if a Java RMI failure occurs.
     */
    final RealTuple xY(LatLonPoint latLon)
            throws VisADException, RemoteException {

        RealTuple xy = null;

        if (getControlContext() != null) {
            RealTuple xyz =
                earthToBoxTuple(new EarthLocationTuple(latLon.getLatitude(),
                    latLon.getLongitude(), new Real(RealType.Altitude, 0.0)));

            xy = new RealTuple(RealTupleType.SpatialCartesian2DTuple,
                               new Real[] { (Real) xyz.getComponent(0),
                                            (Real) xyz.getComponent(
                                            1) }, (CoordinateSystem) null);
        }

        return xy;
    }

    /**
     * Add the  relevant edit menu items into the list
     *
     * @param items List of menu items
     * @param forMenuBar Is this for the menu in the window's menu bar or
     *                   for a popup menu in the legend
     */
    protected void getEditMenuItems(List items, boolean forMenuBar) {
        JMenuItem mi = new JMenuItem("Reset sounding");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                resetProfile(currIndex);
            }
        });
        items.add(mi);
        super.getEditMenuItems(items, forMenuBar);
    }

    /**
     * Add an extra menu for configuration
     *
     * @param menus      List of extra menus
     * @param forMenuBar Is this for the menu in the window's menu bar or
     *                   for a popup menu in the legend
     */
    protected void getExtraMenus(List menus, boolean forMenuBar) {

        super.getExtraMenus(menus, forMenuBar);
        JMenu items = new JMenu("Customize");
        if (forMenuBar) {
            items.setMnemonic(GuiUtils.charToKeyCode("C"));
        }

        JCheckBoxMenuItem cbmi = new JCheckBoxMenuItem("Dry Adiabats",
                                     aeroDisplay.getDryAdiabatVisibility());
        cbmi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    aeroDisplay.setDryAdiabatVisibility(
                        ((JCheckBoxMenuItem) e.getSource()).isSelected());
                } catch (Exception excp) {
                    logException("Dry Adiabat Visibility", excp);
                }
            }
        });
        items.add(cbmi);
        cbmi = new JCheckBoxMenuItem(
            "Saturation Adiabats",
            aeroDisplay.getSaturationAdiabatVisibility());
        cbmi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    aeroDisplay.setSaturationAdiabatVisibility(
                        ((JCheckBoxMenuItem) e.getSource()).isSelected());
                } catch (Exception excp) {
                    logException("Saturation Adiabat Visibility", excp);
                }
            }
        });
        items.add(cbmi);
        cbmi = new JCheckBoxMenuItem(
            "Saturation Mixing Ratios",
            aeroDisplay.getSaturationMixingRatioVisibility());
        cbmi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    aeroDisplay.setSaturationMixingRatioVisibility(
                        ((JCheckBoxMenuItem) e.getSource()).isSelected());
                } catch (Exception excp) {
                    logException("Saturation Mixing Ratio Visibility", excp);
                }
            }
        });
        items.add(cbmi);

        items.addSeparator();

        JMenu                displayTypes = new JMenu("Display Type");
        ButtonGroup          bg           = new ButtonGroup();

        JRadioButtonMenuItem rmi          =
            makeDisplayTypeMenu(SKEWT_DISPLAY);
        bg.add(rmi);
        displayTypes.add(rmi);

        rmi = makeDisplayTypeMenu(STUVE_DISPLAY);
        bg.add(rmi);
        displayTypes.add(rmi);

        rmi = makeDisplayTypeMenu(EMAGRAM_DISPLAY);
        bg.add(rmi);
        displayTypes.add(rmi);

        items.add(displayTypes);

        menus.add(items);
    }

    /**
     * Make the display type menu
     *
     * @param type display type
     *
     * @return the JRadioButtonMenuItem menu
     */
    private JRadioButtonMenuItem makeDisplayTypeMenu(String type) {

        JRadioButtonMenuItem rmi =
            new JRadioButtonMenuItem(getTypeLabel(type), isDisplayType(type));
        rmi.setActionCommand(type);
        rmi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JRadioButtonMenuItem myRmi =
                    (JRadioButtonMenuItem) e.getSource();
                setDisplayType(myRmi.getActionCommand());
                setDisplayName(myRmi.getText());
                updateLegendAndList();
            }
        });
        return rmi;
    }

    /**
     * See if the display type in question is the same as this type.
     * @param type   display type
     * @return true if display types are the same
     */
    public boolean isDisplayType(String type) {
        return getDisplayType().equals(type);
    }

    /**
     * Reset the profile to the original field
     *
     * @param index index of the profile to reset
     */
    private void resetProfile(int index) {
        try {
            aeroDisplay.setOriginalProfiles(index);
            setSounding(index);
        } catch (Exception excp) {
            logException("Unable to reset sounding", excp);
        }
    }

    /**
     * Set the type of display.  Used by persistence.
     *
     * @param type the display type
     */
    public void setDisplayType(String type) {
        try {
            if (getHaveInitialized()) {
                aeroDisplay.setCoordinateSystem(type);
            }
            displayType = type;
        } catch (Exception e) {
            LogUtil.printMessage("setDisplayType got " + e.toString());
        }
    }

    /**
     * Get the type of display.  Used by persistence.
     *
     * @return the display type
     */
    public String getDisplayType() {
        return displayType;
    }

    /**
     * Get the wind barb spacing as a String
     * @return String representation of the wind barb spacing
     */
    public String getWindBarbSpacing() {
        return windBarbSpacing;
    }

    /**
     * Set the wind barb spacing.
     * @param newSpacing  either ALL_LEVELS or MANDATORY_LEVELS, or the value
     *                    as a String.
     *
     */
    public void setWindBarbSpacing(String newSpacing) {
        try {
            if (getHaveInitialized()) {
                setWindLevels(newSpacing);
            }
            windBarbSpacing = newSpacing;
        } catch (Exception e) {
            LogUtil.printMessage("setWindBarbSpacing got " + e.toString());
        }
    }

    /**
     * Set the wind intervals based on the String
     *
     * @param windInterval  either ALL_LEVELS or MANDATORY_LEVELS, or the value
     *                      as a String.
     *
     */
    private void setWindLevels(String windInterval) {
        AerologicalCoordinateSystem acs = aeroDisplay.getCoordinateSystem();
        Real                        minP       = acs.getMinimumPressure();
        Real                        maxP       = acs.getMaximumPressure();
        RealType                    pRT        = (RealType) minP.getType();
        Unit                        pUnit      = minP.getUnit();
        Gridded1DSet                spacingSet = null;
        float[]                     vals       = null;
        if (windInterval == MANDATORY_LEVELS) {
            vals = MANDATORY_LEVEL_VALUES;
        } else if (windInterval != ALL_LEVELS) {
            try {
                float spacing = Misc.parseFloat(windInterval);
                if ( !Float.isNaN(spacing)) {
                    float maxPVal = (float) maxP.getValue();
                    float minPVal = (float) minP.getValue();
                    vals = Misc.computeTicks(maxPVal, minPVal, minPVal,
                                             spacing);
                }
            } catch (Exception excp) {}
        }
        if (vals != null) {
            try {
                spacingSet = new Gridded1DSet(AirPressure.getRealTupleType(),
                        new float[][] {
                    vals
                }, vals.length, (CoordinateSystem) null,
                   new Unit[] { pUnit }, (ErrorEstimate[]) null);
            } catch (VisADException ve) {}
        }
        try {
            aeroDisplay.setWindLevels(spacingSet);
        } catch (Exception e) {
            LogUtil.printMessage("AerologicalDisplay.setWindLevels got "
                                 + e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Get the label for the type of display
     *
     * @param displayType  type name
     *
     * @return the label
     */
    public static String getTypeLabel(String displayType) {
        if (displayType.equals(SKEWT_DISPLAY)) {
            return "Skew T";
        } else if (displayType.equals(STUVE_DISPLAY)) {
            return "Stuve";
        } else if (displayType.equals(EMAGRAM_DISPLAY)) {
            return "Emagram";
        }
        return displayType;
    }

}

