/*
 * $Id: ImageMovieControl.java,v 1.71 2007/08/09 17:22:25 dmurray Exp $
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


import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.geoloc.ProjectionRect;


import ucar.unidata.geoloc.projection.*;
import ucar.unidata.gis.mcidasmap.McidasMap;


import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.metdata.NamedStationImpl;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.ui.XmlTree;
import ucar.unidata.ui.XmlUi;

import ucar.unidata.util.CacheManager;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.FileManager;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.Resource;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.view.station.StationLocationMap;


import ucar.unidata.xml.*;

import ucar.visad.GeoUtils;
import ucar.visad.Util;

import ucar.visad.display.*;


import visad.*;

import visad.georef.*;

import visad.util.BaseRGBMap;
import visad.util.ColorPreview;
import visad.util.DataUtility;

import java.awt.*;

import java.awt.Color;
import java.awt.event.*;
import java.awt.image.*;

import java.beans.*;

import java.io.File;

import java.net.URL;

import java.rmi.RemoteException;



import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import java.util.regex.*;


import javax.swing.*;




/**
 * Class for controlling the display of color images.
 * @author Jeff McWhirter
 * @version $Revision: 1.71 $
 */
public class ImageMovieControl extends DisplayControlImpl implements ImageObserver {

    /** stop icon */
    private static Icon stopIcon;

    /** start icon */
    private static Icon startIcon;

    /** property for setting the widget to the first frame */
    public static final String CMD_BEGINNING = "CMD_BEGINNING";

    /** property for setting the widget to the loop in reverse */
    public static final String CMD_BACKWARD = "CMD_BACKWARD";

    /** property for setting the widget to the start or stop */
    public static final String CMD_STARTSTOP = "CMD_STARTSTOP";

    /** property for setting the widget to the loop forward */
    public static final String CMD_FORWARD = "CMD_FORWARD";

    /** property for setting the widget to the last frame */
    public static final String CMD_END = "CMD_END";

    /** Xml tag name */
    public static final String TAG_IMAGESET = "imageset";

    /** Xml tag name */
    public static final String TAG_IMAGES = "images";

    /** Xml tag name */
    public static final String TAG_IMAGESETS = "imagesets";

    /** Xml tag name */
    public static final String TAG_IMAGE = "image";

    /** Xml attribute name */
    public static final String ATTR_BASE = "base";

    /** Xml attribute name */
    public static final String ATTR_DESC = "desc";

    /** Xml attribute name */
    public static final String ATTR_LAT = "lat";

    /** Xml attribute name */
    public static final String ATTR_LON = "lon";

    /** Xml attribute name */
    public static final String ATTR_NAME = "name";

    /** Xml attribute name */
    public static final String ATTR_FORMAT = "format";

    /** Xml attribute name */
    public static final String ATTR_GROUP = "group";

    /** Xml attribute name */
    public static final String ATTR_ROOT = "root";

    /** Xml attribute name */
    public static final String ATTR_FILE = "file";

    /** Xml attribute name */
    public static final String ATTR_TIME = "time";

    /** Xml attribute name */
    public static final String ATTR_INDEX = "index";

    /** use index */
    private static final int DATETYPE_INDEX = 0;

    /** use filename to extract date */
    private static final int DATETYPE_FILENAME = 1;


    /** the display for the image in the main window */
    private ImageRGBDisplayable imageDisplay;

    /** the location of the image */
    private LatLonPoint imageLocation = null;

    /** flag for showing the image in the display */
    private boolean showImageInDisplay = false;

    /** How do we match an image with a time */
    private int dateType = DATETYPE_FILENAME;


    /** Keep track of if the current run thread is the latest run thread */
    private int runTimestamp = 0;

    /** The file index we are currently looking at */
    private int currentIndex = 0;

    /** Points to the imageset xml file */
    private String imageSetUrl;

    /** Points to the imageset root */
    private Element imageSetRoot;


    /** Do we do the imageset xml or do we look at the file system */
    private boolean doImageSet = true;

    /**
     * Should we load in the image set collection xml files from the system resources.
     *   We don't do this if this display was created with a data choice that points to
     *   a collections xml file
     */
    private boolean doImageSetResources = true;

    /** Main gui */
    private JTabbedPane tabbedPane;

    /** The jtree showing all available image sets */
    private XmlTree imageSetsTree;

    /** holds files */
    private JList fileList;

    /** List of lat/lon points */
    private LatLonPoint[] latLons = null;

    /** Is this control enabled */
    private boolean enabled = true;

    /** Ignore gui events. Keep form looping. */
    private boolean ignoreEvents = false;

    /** shows enabled */
    private JCheckBox enabledCbx;


    /** How we convert a date string to a DateTime */
    private String dateFormat = "yyyy_MM_dd_HH_mm_ss_Z";

    /** How we match files and extract the date string */
    private String datePattern = "image_(.*)_\\d*.jpg";

    /** Do we use the datetype index */
    private JRadioButton indexBtn;

    /** Holds the date format string */
    private JTextField formatFld;

    /** Holds the file name string */
    private JTextField patternFld;

    /** Holds the directory */
    private JTextField dirFld;

    /** Holds all of the files */
    private List files;

    /** Holds all of the times */
    private List times;

    /** Maps imageset id to  station for the station map */
    private Hashtable idToStation = new Hashtable();

    /** Maps station map to xml element from the imagesets xml */
    private Hashtable stationToElement = new Hashtable();

    /** Maps xml element to station */
    private Hashtable elementToStation = new Hashtable();

    /** Displays imageset locations */
    private StationLocationMap stationMap;

    /** The directory */
    private String directory;


    /** Shows file */
    private JLabel descLabel = new JLabel(" ");

    /** Displays the image */
    private JPanel imagePanel;

    /** Where we show the preview image */
    private JPanel previewPanel;

    /** Should we show the preview */
    private JCheckBox previewCbx;

    /** Last xml node we previewed */
    private Element lastPreviewNode;

    /** The one we are drawing */
    private Image currentImage;

    /** The one we are drawing */
    private Image previewImage;

    /** The one we are loading */
    private Image loadingImage;


    /** This is the index of the last selected point */
    private int pointIndex = -1;

    /** selection indicator */
    private IndicatorPoint selectedPoint;

    /** Holds the line drawings */
    private PickableLineDrawing locations;

    /** Xml nodes corresponding to the points */
    private List pointNodes = new ArrayList();

    /** Running with just on imageset file. Don't show the tabs, etc. */
    private boolean justOneFile = false;


    /** This is the root of the xml that is shown in the tree */
    private Element imageSetsRoot;

    /** If the user has imported any collection files this list holds them */
    private List extraCollections = new ArrayList();

    /** Have we seen a particular collections file before */
    private Hashtable haveSeen = new Hashtable();

    /** Image transparency */
    private float alpha = 1.0f;

    /** scale flag */
    private float lastScale = Float.NaN;

    /** the last data image */
    Image lastDataImage;



    /**
     * NOOP ctor
     */
    public ImageMovieControl() {}

    /**
     * Get the color table to use for the image glyphs
     *
     * @return The rgb color table
     */
    public ColorTable getRGBColorTable() {
        return getDisplayConventions().getParamColorTable("image");
    }

    /**
     *
     * Called to make this kind of Display Control;
     * This method is called from inside DisplayControlImpl init(several args).
     *
     * @param dataChoice the DataChoice of the moment.
     *
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {
        super.init(dataChoice);


        imageDisplay = new ImageRGBDisplayable("ImageDisplayable");
        ColorTable colorTable = getRGBColorTable();
        imageDisplay.setRangeForColor(0.0, 255.0);
        imageDisplay.setColorPalette(colorTable.getTable());
        if (getAlpha() != 1.0) {
            imageDisplay.setAlpha(getAlpha());
        }
        addDisplayable(imageDisplay);

        locations = new PickableLineDrawing("ImageMoviePoint");
        selectedPoint =
            new IndicatorPoint("Selected Point",
                               RealTupleType.LatitudeLongitudeTuple);
        setPointSize();
        stationMap = new StationLocationMap(false);
        stationMap.addPropertyChangeListener(stationMap.SELECTED_PROPERTY,
                                             this);
        McidasMap mmr = new McidasMap("/auxdata/maps/OUTLUSAM");
        mmr.setColor(Color.blue);
        stationMap.setMapRenderer(mmr);
        LambertConformal proj = new LambertConformal(40, -100, 60, 20);
        ProjectionRect   rect = new ProjectionRect(-2000, -1800, 2500, 1800);
        proj.setDefaultMapArea(rect);
        stationMap.setProjectionImpl(proj);


        imageSetsTree = new XmlTree(getImageSetsRoot(), true) {
            public void doDoubleClick(XmlTree theTree, Element node) {
                if (node.getTagName().equals(TAG_IMAGESET)) {
                    selectImageSet(node);
                    GuiUtils.showComponentInTabs(tabbedPane);
                }
            }

            public void doClick(XmlTree theTree, Element node) {
                NamedStationImpl station =
                    (NamedStationImpl) elementToStation.get(node);
                if ((station != null) && (stationMap != null)) {
                    stationMap.setSelectedStations(Misc.newList(station));
                }
                doPreview(node);
            }
        };


        files = new ArrayList();
        times = new ArrayList();

        if (dataChoice != null) {
            String path = dataChoice.getStringId();
            if ( !extraCollections.contains(path)) {
                importImageSet(path);
            }
            if (imageSetRoot != null) {
                justOneFile = true;
                return true;
            } else {
                doImageSetResources = false;
            }
        }


        if ( !doImageSet) {
            if (directory == null) {
                File dir = FileManager.getDirectory(directory,
                               "Please select a directory");
                if (dir != null) {
                    directory = dir.toString();
                }
            }
            if (directory == null) {
                return false;
            }
            if (files == null) {
                files = new ArrayList();
                times = new ArrayList();
                loadFilesFromDirectory();
            }
        } else {
            loadFilesFromXml();
        }

        return true;
    }

    /**
     * Get control widgets specific to this control.
     *
     * @param controlWidgets   list of control widgets from other places
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public void getControlWidgets(List controlWidgets)
            throws VisADException, RemoteException {
        super.getControlWidgets(controlWidgets);
        controlWidgets.add(
            new WrapperWidget(
                this, GuiUtils.rLabel("Transparency:"), doMakeAlphaSlider()));


    }


    /**
     * Add the  relevant file menu items into the list
     *
     * @param items List of menu items
     * @param forMenuBar Is this for the menu in the window's menu bar or
     * for a popup menu in the legend
     */
    protected void getFileMenuItems(List items, boolean forMenuBar) {
        super.getFileMenuItems(items, forMenuBar);
        items.add(GuiUtils.MENU_SEPARATOR);

        items.add(GuiUtils.makeMenuItem("Import Image Set From File", this,
                                        "importImageSetFromFile"));
        items.add(GuiUtils.makeMenuItem("Import Image Set From URL", this,
                                        "importImageSetFromURL"));
    }


    /**
     * Load in the url or filename. This can be the xml of the image set or the
     * xml of the image set collection
     * @param path File or url
     */
    public void importImageSet(String path) {
        if (path == null) {
            return;
        }
        path = path.trim();
        if (haveSeen.get(path) != null) {
            return;
        }
        try {
            Element root = XmlUtil.getRoot(path, getClass());
            if (root == null) {
                LogUtil.userErrorMessage("Could not load the image set xml: "
                                         + path);
                return;
            }

            if (root.getTagName().equals(TAG_IMAGES)) {
                pointIndex   = -1;
                imageSetRoot = root;
                imageSetUrl  = path;
                reloadFiles();
                if (tabbedPane != null) {
                    tabbedPane.setSelectedIndex(0);
                }
            } else if (root.getTagName().equals(TAG_IMAGESETS)) {
                haveSeen.put(path, path);
                if (extraCollections.contains(path)) {
                    LogUtil.userMessage("This image set is already loaded");
                } else {
                    extraCollections.add(path);
                    appendImageSetXml(root);
                    loadImageSetsMap();
                    imageSetsTree.loadTree();
                }
            } else {
                LogUtil.userErrorMessage("Unknown XML root:"
                                         + root.getTagName());
            }
        } catch (Exception exc) {
            logException("Error loading image set xml", exc);
        }

    }

    /**
     * Import the image set xml
     */
    public void importImageSetFromFile() {
        importImageSet(FileManager.getReadFile(FileManager.FILTER_XML));
    }


    /**
     * Import the image set xml
     */
    public void importImageSetFromURL() {
        importImageSet(
            GuiUtils.getInput(
                "Enter a URL to an image set xml file", "URL: ", ""));
    }


    /**
     * Set the selected file. Will change index if it is invalid
     *
     * @param theIndex Index of the file.
     */
    private void setSelectedFile(int theIndex) {
        if (theIndex < 0) {
            theIndex = 0;
        } else if (theIndex >= files.size()) {
            theIndex = files.size() - 1;
        }
        currentIndex = theIndex;

        String theFile = null;
        if ((theIndex >= 0) && (theIndex < files.size())) {
            theFile = (String) files.get(theIndex);
        }


        if (theFile == null) {
            loadingImage = null;
            currentImage = null;
            imagePanel.repaint();
        } else {
            long t1 = System.currentTimeMillis();
            try {
                //fileList.setSelectedIndex(theIndex);
                loadingImage = getImageFile(theFile);
                loadingImage.getWidth(this);
            } catch (Exception exc) {
                logException("Error loading image", exc);
            }
            long t2 = System.currentTimeMillis();
            //            System.err.println ("time:" + (t2-t1));
        }




    }


    /**
     * Read in the image from the given filename or url
     *
     * @param file File or url
     *
     * @return The image
     *
     * @throws Exception On badness
     */
    private Image getImageFile(String file) throws Exception {
        if (file.startsWith("http:")) {
            byte[] imageBytes = IOUtil.readBytesAndCache(file,
                                    "ImageMovieControl");
            if (imageBytes == null) {
                return null;
            }
            return Toolkit.getDefaultToolkit().createImage(imageBytes);
        } else {
            return Toolkit.getDefaultToolkit().createImage(file);
        }
    }


    /**
     * Should we add a control listener
     *
     * @return true
     */
    protected boolean shouldAddControlListener() {
        return true;
    }

    /**
     * Handle the viewpoint changed.
     */
    public void viewpointChanged() {
        try {
            super.viewpointChanged();
            float scale = getDisplayScale();
            if (scale == lastScale) {
                return;
            }
            lastScale = scale;
            if (lastDataImage != null) {
                //Clear the last image to force the reload
                lastDataImage = null;
                loadImage(currentImage);
            }
        } catch (Exception exc) {
            logException("Handling viewpoint changed", exc);
        }

    }


    /**
     * Add to view menu
     *
     * @param items List of ites
     * @param forMenuBar for the menu bar
     */
    protected void getViewMenuItems(List items, boolean forMenuBar) {
        items.add(GuiUtils.makeCheckboxMenuItem("Show image in display",
                this, "showImageInDisplay", null));
        super.getViewMenuItems(items, forMenuBar);
    }




    /**
     * Make the alpha slider
     *
     * @return alpha slider component
     */
    protected JComponent doMakeAlphaSlider() {
        JSlider alphaSlider = GuiUtils.makeSlider(0, 100,
                                  100 - (int) (getAlpha() * 100), this,
                                  "setInverseAlphaFromSlider");
        JPanel transLabel = GuiUtils.leftRight(GuiUtils.lLabel("0%"),
                                GuiUtils.rLabel("100%"));
        return GuiUtils.vbox(alphaSlider, transLabel);
    }



    /**
     * Set the alpha
     *
     * @param f Alpha
     */
    public void setAlpha(float f) {
        alpha = f;
    }

    /**
     * Get the alpha
     *
     * @return Get the alpha
     */
    public float getAlpha() {
        return alpha;
    }


    /**
     * Set the alpha
     *
     *
     * @param newAlpha new value
     */
    protected void setAlphaFromSlider(float newAlpha) {
        try {
            alpha = newAlpha;
            if (imageDisplay != null) {
                imageDisplay.setAlpha(newAlpha);
            }
        } catch (Exception e) {
            logException("Setting alpha value", e);
        }
    }


    /**
     * Called on slider action
     *
     * @param sliderValue slider value
     */
    public void setInverseAlphaFromSlider(int sliderValue) {
        sliderValue = 100 - sliderValue;
        setAlphaFromSlider((float) (((double) sliderValue) / 100.0));
    }


    /**
     * Handle the image update
     *
     * @param img img
     * @param flags flags
     * @param x x
     * @param y y
     * @param width width
     * @param height height
     *
     * @return keep going
     */
    public boolean imageUpdate(Image img, int flags, int x, int y, int width,
                               int height) {
        if (img == currentImage) {
            imagePanel.repaint();
            return true;
        }
        if (img == previewImage) {
            previewPanel.repaint();
        }
        if ((img != previewImage) && (img != loadingImage)) {
            return false;
        }
        boolean all  = (flags & ImageObserver.ALLBITS) != 0;
        boolean some = (flags & ImageObserver.SOMEBITS) != 0;
        if (all && (img == loadingImage)) {
            currentImage = loadingImage;

            loadImage(currentImage);
            imagePanel.repaint();
            return true;
        }
        return true;
    }

    /**
     * Load in the image
     *
     * @param image  the image to load
     */
    private void loadImage(Image image) {
        try {
            if ((image == null) || (imageLocation == null)
                    || !showImageInDisplay) {
                lastDataImage = null;
                imageDisplay.setVisible(false);
                return;
            }
            if (lastDataImage == image) {
                return;
            }
            lastDataImage = image;
            imageDisplay.setVisible(getDisplayVisibility());
            image = ImageUtils.resize(image, 150, -1);
            FlatField imageData = Util.makeField(image, true);
            //FlatField   imageData   = DataUtility.makeField(image, true);
            Linear2DSet imageDomain = (Linear2DSet) imageData.getDomainSet();
            int[] screen =
                earthToScreen(new EarthLocationTuple(imageLocation,
                    new Real(RealType.Altitude, 0)));
            int width  = imageDomain.getX().getLength();
            int height = imageDomain.getY().getLength();
            int left, right, top, bottom;
            left   = screen[0];
            right  = screen[0] + width;
            top    = screen[1];
            bottom = screen[1] + height;
            double[] origin = screenToBox(left, top);
            double[] lr     = screenToBox(right, bottom);
            Linear2DSet domain =
                new Linear2DSet(RealTupleType.SpatialCartesian2DTuple,
                                (float) origin[0], (float) lr[0],
                                imageDomain.getX().getLength(),
                                (float) origin[1], (float) lr[1],
                                imageDomain.getY().getLength());
            FlatField newImageData =
                (FlatField) GridUtil.setSpatialDomain(imageData, domain);
            imageDisplay.loadData(newImageData);




        } catch (Exception exc) {
            logException("Making image data", exc);
        }
    }

    /**
     * Draw the image
     *
     * @param g graphics_
     * @param imagePanel Where do we paint into
     * @param currentImage The image to paint
     */
    private void paintImage(Graphics g, JPanel imagePanel,
                            Image currentImage) {


        if (currentImage == null) {
            return;
        }
        Rectangle bounds = imagePanel.getBounds();
        if ((bounds.width == 0) || (bounds.height == 0)) {
            return;
        }
        int imageWidth = currentImage.getWidth(this);
        if (imageWidth <= 0) {
            return;
        }
        int imageHeight = currentImage.getHeight(this);
        if (imageHeight <= 0) {
            return;
        }
        if ((imageWidth < bounds.width) && (imageHeight < bounds.height)) {
            g.drawImage(currentImage, 0, 0, null);
            return;
        }


        if (imageWidth / (double) bounds.width
                > imageHeight / (double) bounds.height) {
            imageHeight = (int) (imageHeight
                                 * (bounds.width / (double) imageWidth));
            imageWidth = bounds.width;
        } else {
            imageWidth = (int) (imageWidth
                                * (bounds.height / (double) imageHeight));

            imageHeight = bounds.height;
        }
        g.drawImage(currentImage, 0, 0, imageWidth, imageHeight, null);
    }



    /**
     * Find the image  for the current animation time and display it
     */
    private void setImageForTime() {
        setSelectedFile(currentIndex);
    }



    /**
     * Make the gui
     *
     * @return The gui
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {

        enabledCbx = GuiUtils.makeCheckbox("Enabled", this, "enabled");
        tabbedPane = new JTabbedPane();
        imagePanel = new JPanel() {
            public void paint(Graphics g) {
                super.paint(g);
                paintImage(g, imagePanel, currentImage);
            }
        };
        imagePanel.setPreferredSize(new Dimension(300, 300));
        previewCbx = new JCheckBox("Preview", false);

        previewCbx.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                doPreview(lastPreviewNode);
            }
        });
        previewPanel = new JPanel() {
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                if ((previewImage != null) && previewCbx.isSelected()) {
                    paintImage(g, previewPanel, previewImage);
                }
            }
        };
        //        previewPanel.setPreferredSize(new Dimension(300, 300));
        JPanel topPanel = GuiUtils.left(getAnimationWidget().getContents());
        JPanel moviePanel = GuiUtils.topCenterBottom(topPanel, imagePanel,
                                GuiUtils.left(descLabel));
        fileList = new JList();
        fileList.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    int[] indices = fileList.getSelectedIndices();
                    List  tmp     = new ArrayList(files);
                    for (int i = 0; i < indices.length; i++) {
                        tmp.set(indices[i], null);
                    }
                    files = new ArrayList();

                    for (int i = 0; i < tmp.size(); i++) {
                        if (tmp.get(i) != null) {
                            files.add(tmp.get(i));
                        }
                    }
                    addMoviesToList();
                    setImageForTime();
                }
            }

        });
        addMoviesToList();


        patternFld = new JTextField(datePattern, 40);
        formatFld  = new JTextField(dateFormat, 40);

        indexBtn = new JRadioButton("Use animation index in list",
                                    dateType == DATETYPE_INDEX);
        GuiUtils.addActionListener(indexBtn, this, "dateTypeButtonPressed",
                                   new Integer(DATETYPE_INDEX));

        JRadioButton nameBtn = new JRadioButton("Extract from filename",
                                   dateType == DATETYPE_FILENAME);
        GuiUtils.addActionListener(indexBtn, this, "dateTypeButtonPressed",
                                   new Integer(DATETYPE_FILENAME));

        GuiUtils.buttonGroup(indexBtn, nameBtn);
        patternFld.setToolTipText(
            "Use this to extract the date string from the file name");
        formatFld.setToolTipText(
            "Use this to extract the date from the date string");
        GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
        JPanel fields = GuiUtils.doLayout(new Component[] {
                            GuiUtils.rLabel("Date pattern:"),
                            patternFld, GuiUtils.rLabel("Date format:"),
                            formatFld, }, 2, GuiUtils.WT_NYN, GuiUtils.WT_N);


        JPanel namePanel = GuiUtils.vbox(nameBtn,
                                         GuiUtils.inset(fields,
                                             new Insets(0, 20, 0, 0)));

        JPanel settingsTop =
            GuiUtils.vbox(
                GuiUtils.left(enabledCbx),
                GuiUtils.left(new JLabel("How is time determined:")),
                namePanel, GuiUtils.left(indexBtn));

        dirFld = new JTextField(directory, 30);
        JButton dirBtn = GuiUtils.makeButton("Reload Files", this,
                                             "reloadFiles");

        JPanel dirPanel = GuiUtils.hbox(new JLabel("Directory: "), dirFld,
                                        dirBtn);
        JPanel filesTop;


        if ( !doImageSet) {
            filesTop = dirPanel;
        } else {
            filesTop = new JPanel();
        }

        JScrollPane filesSp = GuiUtils.makeScrollPane(fileList, 200, 200);
        filesSp.setPreferredSize(new Dimension(200, 200));



        JPanel filesListPanel =
            GuiUtils.topCenter(GuiUtils.left(new JLabel("Images:")), filesSp);


        JPanel filesPanel = GuiUtils.topCenter(filesTop, filesListPanel);

        if (justOneFile) {
            reloadFiles();
            return moviePanel;
        }

        tabbedPane.add("Movie", moviePanel);

        if ( !doImageSet) {
            tabbedPane.add("Settings",
                           GuiUtils.inset(GuiUtils.top(filesPanel), 5));
        }

        if (doImageSet) {
            doMakeImageSetTree(tabbedPane);
        }

        tabbedPane.add("Settings",
                       GuiUtils.inset(GuiUtils.top(doMakeWidgetComponent()),
                                      5));
        return tabbedPane;
    }


    /**
     * Add the xml to the xml root
     *
     * @param path filename or url
     *
     * @return success
     *
     * @throws Exception On badness
     */
    private boolean appendImageSetXml(String path) throws Exception {
        if (haveSeen.get(path) != null) {
            return true;
        }
        haveSeen.put(path, path);
        String contents = IOUtil.readContents(path, getClass(),
                              (String) null);
        if (contents == null) {
            return false;
        }
        return appendImageSetXml(XmlUtil.getRoot(contents));
    }

    /**
     * Add the xml to the xml root
     *
     * @param element Element to add
     *
     * @return success
     *
     * @throws Exception On badness
     */
    private boolean appendImageSetXml(Element element) throws Exception {
        if (element == null) {
            return false;
        }
        element = (Element) getImageSetsRoot().getOwnerDocument().importNode(
            element, true);
        getImageSetsRoot().appendChild(element);
        return true;
    }


    /**
     * Load all image sets into the station map
     *
     *
     * @throws Exception On badness
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private void loadImageSetsMap()
            throws VisADException, RemoteException, Exception {
        List stations = new ArrayList();
        List locs     = new ArrayList();
        List children = XmlUtil.findDescendants(getImageSetsRoot(),
                            TAG_IMAGESET);
        pointNodes = new ArrayList();
        for (int cidx = 0; cidx < children.size(); cidx++) {
            final Element child = (Element) children.get(cidx);
            String latString = XmlUtil.getAttribute(child, ATTR_LAT,
                                   (String) null);
            //If no lat/lon then skip
            if (latString == null) {
                continue;
            }
            String lonString = XmlUtil.getAttribute(child, ATTR_LON);
            String id        = XmlUtil.getAttribute(child, ATTR_NAME);
            NamedStationImpl station =
                new NamedStationImpl(id,
                                     XmlUtil.getAttribute(child, ATTR_NAME),
                                     Misc.decodeLatLon(latString),
                                     Misc.decodeLatLon(lonString), 0,
                                     CommonUnit.meter);
            stations.add(station);
            locs.add(station.getEarthLocation().getLatLonPoint());
            pointNodes.add(child);
            idToStation.put(id, station);
            stationToElement.put(station, child);
            elementToStation.put(child, station);
        }
        latLons = (LatLonTuple[]) locs.toArray(new LatLonTuple[locs.size()]);
        locations.setData(Util.indexedField(latLons, false));
        stationMap.setStations(stations);
    }

    /**
     * Add the image set tree to the tabbed pane
     *
     * @param tabbedPane pane to add to
     */
    private void doMakeImageSetTree(JTabbedPane tabbedPane) {
        try {
            if (doImageSetResources) {
                XmlResourceCollection xrc =
                    getControlContext().getResourceManager().getXmlResources(
                        IdvResourceManager.RSC_IMAGESETS);
                for (int i = 0; i < xrc.size(); i++) {
                    try {
                        appendImageSetXml(xrc.get(i).toString());
                    } catch (Exception exc) {
                        logException("Error reading xml: " + xrc.get(i), exc);
                    }
                }
            }

            for (int i = 0; i < extraCollections.size(); i++) {
                try {
                    appendImageSetXml(extraCollections.get(i).toString());
                } catch (Exception exc) {
                    logException("Error reading xml: "
                                 + extraCollections.get(i).toString(), exc);
                }
            }

            imageSetsTree.loadTree();
            loadImageSetsMap();

            locations.addAction(new ActionImpl("Image Movie Probe Action") {
                private boolean first = true;
                public void doAction() {
                    if (first) {
                        first = false;
                    } else {
                        try {
                            int i = locations.getCloseIndex();
                            if (i >= 0) {
                                selectImageSet((Element) pointNodes.get(i));
                            }
                        } catch (Exception ex) {
                            logException(ex);
                        }
                    }
                }
            });

        } catch (Exception exc) {
            logException("Error reading xml", exc);
        }



        JScrollPane treeScroller = imageSetsTree.getScroller();
        stationMap.setPreferredSize(new Dimension(100, 100));
        treeScroller.setPreferredSize(new Dimension(100, 100));
        previewPanel.setPreferredSize(new Dimension(100, 100));
        previewPanel.setSize(new Dimension(100, 100));
        //      treeScroller.setPreferredSize(new Dimension(100, 100));

        JButton loadButton = GuiUtils.makeButton("Load Image Set", this,
                                 "loadSelectedImageSet");
        JPanel previewContents =
            GuiUtils.topCenter(GuiUtils.left(previewCbx),
                               GuiUtils.inset(previewPanel, 5));
        //        JComponent leftPanel = GuiUtils.vsplit(treeScroller, previewContents, 0.5);
        JComponent leftPanel = GuiUtils.doLayout(new Component[] {
                                   treeScroller,
                                   previewContents }, 1, GuiUtils.WT_Y,
                                       GuiUtils.WT_Y);
        //        JComponent imageSetsPanel = GuiUtils.hsplit(leftPanel, stationMap, 0.5);
        JComponent imageSetsPanel = GuiUtils.doLayout(new Component[] {
                                        leftPanel,
                                        stationMap }, 2,
                                            new double[] { 0.3, 1.0 },
                                            GuiUtils.WT_Y);
        imageSetsPanel = GuiUtils.centerBottom(imageSetsPanel,
                GuiUtils.wrap(GuiUtils.inset(loadButton, 5)));

        tabbedPane.add("Image Sets", GuiUtils.inset(imageSetsPanel, 5));

        if ((pointIndex == -1) && (imageSetUrl != null)) {
            reloadFiles();
        } else if ((pointIndex >= 0) && (pointIndex < pointNodes.size())) {
            selectImageSet((Element) pointNodes.get(pointIndex));
        }

        if ( !justOneFile) {
            addDisplayable(locations, FLAG_COLOR | FLAG_ZPOSITION);
            addDisplayable(selectedPoint, FLAG_COLOR | FLAG_ZPOSITION);
        }

    }


    /**
     * Create, if needed, and return the root of the xml tree
     *
     * @return xml tree root
     */
    private Element getImageSetsRoot() {
        if (imageSetsRoot == null) {
            try {
                imageSetsRoot =
                    XmlUtil.getRoot("<dummy name=\"Image Sets\"/>");
            } catch (Exception exc) {
                throw new IllegalStateException("Could not create tree root");
            }
        }
        return imageSetsRoot;
    }


    /**
     * Respond to a timeChange event
     *
     * @param time new time
     */
    protected void timeChanged(Real time) {
        super.timeChanged(time);
        try {
            setSelectedFile(getAnimation(true).getCurrent());
        } catch (Exception exc) {
            logException("Error setting time", exc);
        }
    }


    /**
     * Handle property change
     *
     * @param event The event
     */
    public void propertyChange(PropertyChangeEvent event) {
        if (event.getPropertyName().equals(
                StationLocationMap.SELECTED_PROPERTY)) {
            List selected = stationMap.getSelectedStations();
            if (selected.size() == 0) {
                return;
            }
            Element element = (Element) stationToElement.get(selected.get(0));
            if (element == null) {
                return;
            }
            imageSetsTree.selectElement(element);
            if (stationMap.getWasDoubleClick()) {
                selectImageSet(element);
            } else {
                doPreview(element);
            }
        } else {
            super.propertyChange(event);
        }
    }




    /**
     * Load in the selected image set from the jtree
     */
    public void loadSelectedImageSet() {
        List l = imageSetsTree.getSelectedElements();
        if (l.size() > 0) {
            selectImageSet((Element) l.get(0));
            GuiUtils.showComponentInTabs(tabbedPane);
        }
    }


    /**
     * Set the selected point by index
     *
     */
    private void setSelectedPoint() {
        imageLocation = null;
        if (pointIndex >= 0) {
            try {
                imageLocation = latLons[pointIndex];
                selectedPoint.setPoint((RealTuple) latLons[pointIndex]);
            } catch (Exception exc) {
                logException("Error setting indicator point", exc);
            }
            NamedStationImpl station =
                (NamedStationImpl) elementToStation.get(
                    pointNodes.get(pointIndex));
            if ((station != null) && (stationMap != null)) {
                stationMap.setSelectedStations(Misc.newList(station));
            }
        }
    }

    /**
     * Show the prevew image defined by the given node
     *
     * @param node Node
     */
    private void doPreview(Element node) {
        try {
            previewImage = null;
            doPreviewInner(node);
            previewPanel.repaint();
        } catch (Exception exc) {
            logException("Error loading preview", exc);
        }
    }


    /**
     * Show the prevew image defined by the given node
     *
     * @param node Node
     * @throws Exception On badness
     */
    private void doPreviewInner(Element node) throws Exception {
        lastPreviewNode = node;
        if ((node == null) || !previewCbx.isSelected()) {
            return;
        }

        String url = getImageSetUrl(node);
        if (url == null) {
            return;
        }
        Element root = XmlUtil.getRoot(url, getClass());
        String  base = XmlUtil.getAttribute(root, ATTR_BASE, (String) null);
        if (base == null) {
            base = IOUtil.getFileRoot(url);
        }
        NodeList children = XmlUtil.getElements(root, TAG_IMAGE);
        if (children.getLength() == 0) {
            return;
        }
        Element latestChild = null;
        String format = XmlUtil.getAttribute(root, ATTR_FORMAT,
                                             "yyyyMMddHHmmz");


        double latestTime = 0;

        for (int i = 0; i < children.getLength(); i++) {
            Element  child    = (Element) children.item(i);
            String   time     = XmlUtil.getAttribute(child, ATTR_TIME);
            DateTime dateTime = DateTime.createDateTime(time, format);
            if ((i == 0) || (dateTime.getValue() > latestTime)) {
                latestTime  = dateTime.getValue();
                latestChild = child;
            }
        }

        if (latestChild == null) {
            return;
        }
        url = XmlUtil.getAttribute(latestChild, ATTR_FILE);
        if ((base != null) && !url.startsWith("http:")
                && !url.startsWith(File.separator + "")
                && (url.indexOf(":") < 0)) {
            if ( !base.endsWith("/") && !url.startsWith("/")) {
                url = base + "/" + url;
            } else {
                url = base + url;
            }
        }
        previewImage = getImageFile(url);



    }


    /**
     * Find the iamge set url from the given node
     *
     * @param node Node
     *
     * @return URL pointing to the image set
     */
    private String getImageSetUrl(Element node) {
        Element root = XmlUtil.findAncestor(node, TAG_IMAGESETS);
        String  base = ((imageSetsRoot == null)
                        ? null
                        : XmlUtil.getAttribute(root, ATTR_BASE,
                            (String) null));
        String url = XmlUtil.getAttribute(node, ATTR_INDEX);
        if (base != null) {
            if ( !base.endsWith("/") && !url.startsWith("/")) {
                url = base + "/" + url;
            } else {
                url = base + url;
            }
        } else {
            url = url;
        }
        return url;
    }


    /**
     * Load in the image set
     *
     * @param node Defines the image set
     */
    private void selectImageSet(Element node) {
        String name = XmlUtil.getAttribute(node, ATTR_NAME, (String) null);
        if (name != null) {
            LogUtil.message("Location: " + name);
        }


        pointIndex = pointNodes.indexOf(node);
        setSelectedPoint();
        imageSetRoot = null;
        imageSetUrl  = getImageSetUrl(node);
        reloadFiles();
        tabbedPane.setSelectedIndex(0);
    }

    /**
     * Reload the files
     */
    public void reloadFiles() {
        currentIndex = 0;
        if ( !doImageSet) {
            loadFilesFromDirectory();
        } else {
            loadFilesFromXml();
        }
        addMoviesToList();
        setImageForTime();
    }

    /**
     * Load files
     */
    public void loadFilesFromXml() {
        files = new ArrayList();
        times = new ArrayList();
        if (imageSetUrl == null) {
            return;
        }
        try {
            if (imageSetRoot == null) {
                imageSetRoot = XmlUtil.getRoot(imageSetUrl, getClass());
            }
            String base = XmlUtil.getAttribute(imageSetRoot, ATTR_BASE,
                              (String) null);
            if (base == null) {
                base = IOUtil.getFileRoot(imageSetUrl);
            }
            String format = XmlUtil.getAttribute(imageSetRoot, ATTR_FORMAT,
                                "yyyyMMddHHmmz");


            NodeList children  = XmlUtil.getElements(imageSetRoot, TAG_IMAGE);
            List     fileDates = new ArrayList();
            for (int i = 0; i < children.getLength(); i++) {
                Element child = (Element) children.item(i);
                String  url   = XmlUtil.getAttribute(child, ATTR_FILE);
                String  time  = XmlUtil.getAttribute(child, ATTR_TIME);
                if ((base != null) && !url.startsWith("http:")
                        && !url.startsWith(File.separator + "")
                        && (url.indexOf(":") < 0)) {
                    if ( !base.endsWith("/") && !url.startsWith("/")) {
                        url = base + "/" + url;
                    } else {
                        url = base + url;
                    }
                }
                DateTime dateTime = DateTime.createDateTime(time, format);
                fileDates.add(new FileDate(url, dateTime));
            }

            fileDates = Misc.sort(fileDates);
            for (int i = 0; i < fileDates.size(); i++) {
                FileDate fd = (FileDate) fileDates.get(i);
                files.add(fd.file);
                times.add(fd.dttm);
            }


            //Make sure we create the animation before we call setAnimationSet
            getAnimation(true);
            setAnimationSet(times);

            currentIndex = files.size() - 1;
            getAnimation(true).setCurrent(currentIndex);
            String group = XmlUtil.getAttribute(imageSetRoot, ATTR_GROUP, "");
            String desc  = XmlUtil.getAttribute(imageSetRoot, ATTR_DESC, "");
            if (desc.length() > 0) {
                desc = " - " + desc;
            }
            descLabel.setText(group + " - "
                              + XmlUtil.getAttribute(imageSetRoot, ATTR_NAME,
                                  "") + desc);

        } catch (Exception exc) {
            logException("Error reading xml", exc);
        }
    }

    /**
     * Class FileDate For sorting
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.71 $
     */
    private static class FileDate implements Comparable {

        /** file */
        String file;

        /** date */
        DateTime dttm;

        /**
         * ctor
         *
         * @param file file
         * @param dttm date
         */
        public FileDate(String file, DateTime dttm) {
            this.file = file;
            this.dttm = dttm;
        }

        /**
         * compare
         *
         * @param o to
         *
         * @return comparison
         */
        public int compareTo(Object o) {
            FileDate that = (FileDate) o;
            if (dttm.getValue() < that.dttm.getValue()) {
                return -1;
            }
            if (dttm.getValue() > that.dttm.getValue()) {
                return 1;
            }
            return 0;
        }
    }


    /**
     * Load in the files from the directory
     */
    public void loadFilesFromDirectory() {
        try {
            File   dir = new File(getDirectory());
            String filePattern;
            if (dateType == DATETYPE_INDEX) {
                filePattern = "\\.jpg$,\\.jpeg$,\\.gif$,\\.png$";
            } else {
                filePattern = datePattern;
            }
            File[] imageFiles =
                IOUtil.sortFilesOnAge(
                    dir,
                    (java.io.FileFilter) new PatternFileFilter(
                        filePattern, false), false);
            files = new ArrayList();
            String  errorMsg = null;
            Pattern pattern  = Pattern.compile(getDatePattern());
            for (int i = 0; i < imageFiles.length; i++) {
                String f    = imageFiles[i].toString();
                String name = (new File(f)).getName();
                try {
                    Matcher matcher = pattern.matcher(name);
                    if ( !matcher.find()) {
                        errorMsg = "Could not match pattern:" + name;
                        break;
                    }
                    if (matcher.groupCount() < 1) {
                        errorMsg = "No groups in pattern:" + getDatePattern();
                        break;
                    }
                    name = matcher.group(1);
                    DateTime dttm = DateTime.createDateTime(name,
                                        getDateFormat());
                    files.add(f);
                    times.add(dttm);
                } catch (Exception exc) {
                    errorMsg = "Error extracting the date from the filename:"
                               + name + "\n" + "Format:" + dateFormat;
                    break;
                }
                if (errorMsg != null) {
                    LogUtil.userErrorMessage(errorMsg);
                    setEnabled(false);
                    if (enabledCbx != null) {
                        enabledCbx.setSelected(false);
                    }
                }
            }

            if (files.size() == 0) {
                userMessage("No files were found using the pattern:"
                            + pattern);
            }
        } catch (Exception exc) {
            logException("Error finding files", exc);
        }
    }


    /**
     * Apply the z position to the displayables with FLAG_ZPOSITION set
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    protected void applyZPosition() throws VisADException, RemoteException {
        deactivateDisplays();
        if (imageDisplay != null) {
            imageDisplay.setConstantPosition(getZPosition() + .01,
                                             Display.ZAxis);
        }
        super.applyZPosition();
        activateDisplays();
    }

    /**
     * Hook
     *
     * @param v New type
     */
    public void dateTypeButtonPressed(Integer v) {}

    /**
     * Put files into the jlist
     */
    private void addMoviesToList() {
        Vector items = new Vector();
        for (int i = 0; i < files.size(); i++) {
            items.add(times.get(i).toString());
        }
        if (fileList != null) {
            fileList.setListData(items);
        }
    }



    /**
     * initdone
     */
    public void initDone() {
        if ((files.size() == 0) && (tabbedPane.getTabCount() > 1)) {
            tabbedPane.setSelectedIndex(1);
        }
        setImageForTime();
        super.initDone();
    }

    /**
     * Set the size of the points
     */
    private void setPointSize() {
        try {
            locations.setPointSize(DEFAULT_POINT_SIZE);
            selectedPoint.setPointSize(DEFAULT_POINT_SIZE * 2);
        } catch (Exception exc) {
            logException("Set point size ", exc);
        }
    }

    /**
     * Set the Directory property.
     *
     * @param value The new value for Directory
     */
    public void setDirectory(String value) {
        directory = value;
    }

    /**
     * Get the Directory property.
     *
     * @return The Directory
     */
    public String getDirectory() {
        if (dirFld != null) {
            directory = dirFld.getText().trim();
        }
        return directory;
    }


    /**
     * Set the Files property.
     *
     * @param value The new value for Files
     */
    public void setFiles(List value) {
        files = value;
    }

    /**
     * Get the Files property.
     *
     * @return The Files
     */
    public List getFiles() {
        if (imageSetUrl != null) {
            return null;
        }
        return files;
    }



    /**
     * Set the DateFormat property.
     *
     * @param value The new value for DateFormat
     */
    public void setDateFormat(String value) {
        dateFormat = value;
    }

    /**
     * Get the DateFormat property.
     *
     * @return The DateFormat
     */
    public String getDateFormat() {
        if (formatFld != null) {
            dateFormat = formatFld.getText().trim();
        }
        return dateFormat;
    }


    /**
     * Set the DatePattern property.
     *
     * @param value The new value for DatePattern
     */
    public void setDatePattern(String value) {
        datePattern = value;
    }

    /**
     * Get the DatePattern property.
     *
     * @return The DatePattern
     */
    public String getDatePattern() {
        if (patternFld != null) {
            datePattern = patternFld.getText().trim();
        }
        return datePattern;
    }



    /**
     * Set the Enabled property.
     *
     * @param value The new value for Enabled
     */
    public void setEnabled(boolean value) {
        if (enabled == value) {
            return;
        }
        enabled = value;
    }

    /**
     * Get the Enabled property.
     *
     * @return The Enabled
     */
    public boolean getEnabled() {
        return enabled;
    }


    /**
     * Set the DateType property.
     *
     * @param value The new value for DateType
     */
    public void setDateType(int value) {
        dateType = value;
    }

    /**
     * Get the DateType property.
     *
     * @return The DateType
     */
    public int getDateType() {
        if (indexBtn != null) {
            if (indexBtn.isSelected()) {
                dateType = DATETYPE_INDEX;
            } else {
                dateType = DATETYPE_FILENAME;
            }
        }
        return dateType;
    }


    /**
     * Set the ImageSetUrl property.
     *
     * @param value The new value for ImageSetUrl
     */
    public void setImageSetUrl(String value) {
        imageSetUrl = value;
    }

    /**
     * Get the ImageSetUrl property.
     *
     * @return The ImageSetUrl
     */
    public String getImageSetUrl() {
        return imageSetUrl;
    }


    /**
     * Set the DoImageSet property.
     *
     * @param value The new value for DoImageSet
     */
    public void setDoImageSet(boolean value) {
        doImageSet = value;
    }

    /**
     * Get the DoImageSet property.
     *
     * @return The DoImageSet
     */
    public boolean getDoImageSet() {
        return doImageSet;
    }


    /**
     * Set the PointIndex property.
     *
     * @param value The new value for PointIndex
     */
    public void setPointIndex(int value) {
        pointIndex = value;
    }

    /**
     * Get the PointIndex property.
     *
     * @return The PointIndex
     */
    public int getPointIndex() {
        return pointIndex;
    }

    /**
     *  Set the AnimationMode property.
     *
     * @param value The new value for AnimationMode
     * @deprecated Keep around for legacy bundles
     */
    public void setAnimationMode(int value) {}


    /**
     * Set the ExtraCollections property.
     *
     * @param value The new value for ExtraCollections
     */
    public void setExtraCollections(List value) {
        extraCollections = value;
    }

    /**
     * Get the ExtraCollections property.
     *
     * @return The ExtraCollections
     */
    public List getExtraCollections() {
        return extraCollections;
    }

    /**
     * Set the ShowImageInDisplay property.
     *
     * @param value The new value for ShowImageInDisplay
     */
    public void setShowImageInDisplay(boolean value) {
        showImageInDisplay = value;
        if (imageDisplay != null) {
            loadImage(currentImage);
        }
    }

    /**
     * Get the ShowImageInDisplay property.
     *
     * @return The ShowImageInDisplay
     */
    public boolean getShowImageInDisplay() {
        return showImageInDisplay;
    }

}
