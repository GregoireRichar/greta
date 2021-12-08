/*
 * This file is part of the auxiliaries of Greta.
 *
 * Greta is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Greta is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Greta.  If not, see <https://www.gnu.org/licenses/>.
 *
 */
package greta.auxiliary.openface2.gui;

import greta.auxiliary.openface2.util.StringArrayListener;
import greta.auxiliary.openface2.util.ConnectionListener;
import greta.core.animation.mpeg4.bap.BAPFrame;
import greta.core.animation.mpeg4.bap.BAPFrameEmitter;
import greta.core.animation.mpeg4.bap.BAPFramePerformer;
import greta.core.keyframes.face.AUEmitter;
import greta.core.keyframes.face.AUPerformer;
import greta.core.repositories.AUAPFrame;
import greta.core.util.IniManager;
import greta.core.util.id.ID;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

import com.illposed.osc.transport.udp.OSCPort;
import com.illposed.osc.transport.udp.OSCPortOut;
import com.illposed.osc.transport.udp.OSCPortIn;
import greta.auxiliary.openface2.OpenFaceOutputStreamOSCReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.Level;

/**
 *
 * @author Philippe Gauthier <philippe.gauthier@sorbonne-universite.fr>
 * @author Brice Donval
 */
public class OpenFaceOutputStreamReaderOSC extends javax.swing.JFrame implements AUEmitter, BAPFrameEmitter, ConnectionListener, StringArrayListener {

   

    private static final Logger LOGGER = Logger.getLogger(OpenFaceOutputStreamReaderOSC.class.getName());

    private static final Color green = new Color(0, 150, 0);
    private static final Color red = Color.RED;

    private static final String statusProperty = "word.status";
    private static final String connectedProperty = "network.connected";
    private static final String notConnectedProperty = "network.notconnected";
    private static final String hostProperty = "network.host";
    private static final String portProperty = "network.port";

    private String oscConnectedProperty;
    // OSC is use to monitor AUs signal, mainly for debug
    protected boolean useOSCout = false;
    protected OSCPortOut oscOut = null;
    protected int oscOutPort = OSCPort.defaultSCOSCPort();    
    
    // OSC used as AU input listener
    protected boolean useOSCin = false;
    protected int oscInPort = 10000;   
    protected OSCPortIn oscIn = null;
   
    private OpenFaceOutputStreamOSCReader reader = new OpenFaceOutputStreamOSCReader();

    /**
     * Creates new form OpenFaceOutputStreamReader
     */
    public OpenFaceOutputStreamReaderOSC() {
        initComponents();        
        jSpinnerfilterPow.setModel(new SpinnerNumberModel(0.0,0.0,10.0,0.1));
        jSpinnerfilterMaxQueueSize.setValue(getFilterMaxQueueSize());
        oscInPortTextField.setText(Integer.toString(reader.DEFAULT_OSC_PORT)) ;
        setOSCConnected(false);
        reader.addConnectionListener(this);
        reader.addHeaderListener(this);
    }
    /*
    FILTERS
    */
    // We assume both reader use the same filter size
    public int getFilterMaxQueueSize(){
        return reader.getFilterMaxQueueSize();
    }
    
    // We assume both reader use the same filter size
    public void setFilterMaxQueueSize(int value){
        reader.setFilterMaxQueueSize(value);
    }
    
    // We assume both reader use the same filter pow
    public int getFilterPow(){
        return reader.getFilterMaxQueueSize();
    }
    
    // We assume both reader use the same parameters
    public void setFilterPow(double i){
        reader.setFilterPow(i);
    }
    
    /*
    OSC
    */
    public void setOutput(boolean b){        
        if(b){              
            startOSCOut(oscOutPort);            
        }
        else{
            stopOSCOut();
        }
    }
    
    protected void start(int port){
        try { 
            oscIn = new OSCPortIn(port);
            useOSCin = true;
        } catch (IOException ex)
        {
            useOSCin = false;
            LOGGER.log(Level.WARNING, null, ex);
        }
        LOGGER.log(Level.INFO, String.format("startOSCin port %d : %b", port, useOSCin));
    }
    
    protected void stop(){        
        useOSCin = false;
        if(oscIn!=null){
            try {
                oscIn.disconnect();
            } catch (IOException ex) {           
                LOGGER.log(Level.WARNING, null, ex);
            }
        }
        LOGGER.log(Level.INFO, String.format("stopOSCIn : %b",  !useOSCin));
    }
    
    protected void startOSCOut(int port){        
        try {            
            oscOut = new OSCPortOut(InetAddress.getLocalHost(), port);            
            useOSCout = true;
            reader.setOSCout(oscOut);
        } catch (IOException ex) {
            useOSCout = false;
            LOGGER.log(Level.WARNING, null, ex);
        }
        LOGGER.log(Level.INFO, String.format("startOSCOut port %d : %b", port, useOSCout));
    }
    
    protected void stopOSCOut(){        
        useOSCout = false;
        reader.setOSCout(null);
        if(oscOut!=null){
            try {
                oscOut.disconnect();
            } catch (IOException ex) {           
                LOGGER.log(Level.WARNING, null, ex);
            }
        }
        LOGGER.log(Level.INFO, String.format("stopOSCOut : %b",  !useOSCout));
    }
    
    protected int getOscOutPort(){
        return oscOutPort;
    }
    
    protected void setOscOutPort(int port){
        LOGGER.log(Level.INFO, String.format("setOscOutPort : %d",  port));
        oscOutPort = port;      
    }
    
    private void setOSCConnected(boolean connected) {
        if (connected) {
            oscConnectedProperty = connectedProperty;
            oscConnectedLabel.setForeground(green);
            oscInConnectButton.setText("Disconnect");            
        } else {
            oscConnectedProperty = notConnectedProperty;
            oscConnectedLabel.setForeground(red);        
            oscInConnectButton.setText("Connect");
        }
        updateConnectedLabels();
        updateIOPanelsEnabled(connected);
    }

    private void updateReader() {
        String portStr = oscInPortTextField.getText();
        int port = Integer.parseInt(portStr);
        if (reader.getPort() != port) {
            reader.setPort(port);
        }
    } 

    @Override
    public void setLocale(Locale l) {
        super.setLocale(l);
        updateConnectedLabels();
        
        updateLabelWithColon(oscStatusLabel, statusProperty);
        updateLabelWithColon(oscConnectedLabel, notConnectedProperty);        
    }

    private void updateConnectedLabels() {        
        if (oscConnectedLabel != null) {
            oscConnectedLabel.setText(IniManager.getLocaleProperty(oscConnectedProperty));
        }
    }

    private void updateLabelWithColon(javax.swing.JLabel label, String property) {
        if (label != null) {
            label.setText(IniManager.getLocaleProperty(property) + ":");
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        northPanel = new javax.swing.JPanel();
        inputTabbedPane = new javax.swing.JTabbedPane();
        oscTab = new javax.swing.JPanel();
        oscStatusPanel = new javax.swing.JPanel();
        oscStatusLabel = new javax.swing.JLabel();
        oscStatusPanelFiller = new javax.swing.Box.Filler(new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 32767));
        oscConnectedLabel = new javax.swing.JLabel();
        oscConnectorPanel = new javax.swing.JPanel();
        oscInPortLabel = new javax.swing.JLabel();
        oscInConnectorPanelFiller = new javax.swing.Box.Filler(new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 32767));
        oscInPortTextField = new javax.swing.JTextField();
        oscInConnectorPanelFiller2 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 32767));
        oscInConnectButton = new javax.swing.JButton();
        northPanelFiller1 = new javax.swing.Box.Filler(new java.awt.Dimension(20, 0), new java.awt.Dimension(20, 0), new java.awt.Dimension(20, 32767));
        jPanel1 = new javax.swing.JPanel();
        performCheckBox = new javax.swing.JCheckBox();
        jPanel2 = new javax.swing.JPanel();
        filterCheckBox = new javax.swing.JCheckBox();
        jSpinnerfilterMaxQueueSize = new javax.swing.JSpinner();
        jSpinnerfilterPow = new javax.swing.JSpinner();
        jCheckBoxSendOSC = new javax.swing.JCheckBox();
        jSpinnerSendOSCPort = new javax.swing.JSpinner();
        northPanelFiller2 = new javax.swing.Box.Filler(new java.awt.Dimension(20, 0), new java.awt.Dimension(20, 0), new java.awt.Dimension(20, 32767));
        centerPanel = new javax.swing.JPanel();
        separator = new javax.swing.JSeparator();
        outputPanel = new javax.swing.JPanel();
        outputScrollPane = new javax.swing.JScrollPane();
        featuresTable = new javax.swing.JTable();
        outputButtonPanel = new javax.swing.JPanel();
        setButton = new javax.swing.JButton();
        buttonPanelFiller1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 32767));
        selectAllButton = new javax.swing.JButton();
        selectNoneButton = new javax.swing.JButton();
        buttonPanelFiller2 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 32767));
        upButton = new javax.swing.JButton();
        downButton = new javax.swing.JButton();

        mainPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setLayout(new java.awt.BorderLayout(0, 10));

        northPanel.setLayout(new javax.swing.BoxLayout(northPanel, javax.swing.BoxLayout.LINE_AXIS));

        inputTabbedPane.setBorder(javax.swing.BorderFactory.createTitledBorder("Stream to read:"));
        inputTabbedPane.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                inputTabbedPaneStateChanged(evt);
            }
        });

        oscTab.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        oscTab.setLayout(new javax.swing.BoxLayout(oscTab, javax.swing.BoxLayout.PAGE_AXIS));

        oscStatusPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 5));

        oscStatusLabel.setText(IniManager.getLocaleProperty("word.status")+":");
        oscStatusPanel.add(oscStatusLabel);
        oscStatusPanel.add(oscStatusPanelFiller);

        oscConnectedLabel.setText(IniManager.getLocaleProperty("network.notconnected"));
        oscStatusPanel.add(oscConnectedLabel);

        oscTab.add(oscStatusPanel);

        oscConnectorPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 5));

        oscInPortLabel.setText(IniManager.getLocaleProperty(portProperty)+":");
        oscInPortLabel.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        oscConnectorPanel.add(oscInPortLabel);
        oscConnectorPanel.add(oscInConnectorPanelFiller);

        oscInPortTextField.setMaximumSize(new java.awt.Dimension(50, 2147483647));
        oscInPortTextField.setPreferredSize(new java.awt.Dimension(50, 20));
        oscInPortTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                oscInPortTextFieldFocusLost(evt);
            }
        });
        oscInPortTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                oscInPortTextFieldActionPerformed(evt);
            }
        });
        oscInPortTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                oscInPortTextFieldKeyTyped(evt);
            }
        });
        oscConnectorPanel.add(oscInPortTextField);
        oscConnectorPanel.add(oscInConnectorPanelFiller2);

        oscInConnectButton.setText("Connect");
        oscInConnectButton.setPreferredSize(new java.awt.Dimension(93, 23));
        oscInConnectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                oscInConnectButtonActionPerformed(evt);
            }
        });
        oscConnectorPanel.add(oscInConnectButton);

        oscTab.add(oscConnectorPanel);

        inputTabbedPane.addTab("OSC", oscTab);

        northPanel.add(inputTabbedPane);
        northPanel.add(northPanelFiller1);

        jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.Y_AXIS));

        performCheckBox.setText("Perform");
        performCheckBox.setMargin(new java.awt.Insets(10, 2, 2, 2));
        performCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                performCheckBoxActionPerformed(evt);
            }
        });
        jPanel1.add(performCheckBox);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Filter"));
        jPanel2.setToolTipText("Fitler");
        jPanel2.setName("Filter"); // NOI18N
        jPanel2.setLayout(new java.awt.GridLayout(2, 2));

        filterCheckBox.setText("On");
        filterCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterCheckBoxActionPerformed(evt);
            }
        });
        jPanel2.add(filterCheckBox);

        jSpinnerfilterMaxQueueSize.setModel(new javax.swing.SpinnerNumberModel(5, 1, 100, 1));
        jSpinnerfilterMaxQueueSize.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinnerfilterMaxQueueSizeStateChanged(evt);
            }
        });
        jPanel2.add(jSpinnerfilterMaxQueueSize);

        jSpinnerfilterPow.setModel(new javax.swing.SpinnerNumberModel(0.0d, 0.0d, null, 1.0d));
        jSpinnerfilterPow.setEditor(new javax.swing.JSpinner.NumberEditor(jSpinnerfilterPow, "0.00"));
        jSpinnerfilterPow.setValue(reader.getFilterPow());
        jSpinnerfilterPow.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinnerfilterPowStateChanged(evt);
            }
        });
        jPanel2.add(jSpinnerfilterPow);

        jCheckBoxSendOSC.setSelected(useOSCout);
        jCheckBoxSendOSC.setText("OSCOut");
        jCheckBoxSendOSC.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jCheckBoxSendOSCStateChanged(evt);
            }
        });
        jPanel2.add(jCheckBoxSendOSC);

        jSpinnerSendOSCPort.setModel(new javax.swing.SpinnerNumberModel(6000, 6000, 99999, 1));
        jSpinnerSendOSCPort.setValue(getOscOutPort());
        jSpinnerSendOSCPort.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinnerSendOSCPortStateChanged(evt);
            }
        });
        jPanel2.add(jSpinnerSendOSCPort);

        jPanel1.add(jPanel2);

        northPanel.add(jPanel1);
        northPanel.add(northPanelFiller2);

        mainPanel.add(northPanel, java.awt.BorderLayout.NORTH);

        centerPanel.setLayout(new java.awt.BorderLayout(0, 10));
        centerPanel.add(separator, java.awt.BorderLayout.NORTH);

        outputPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Available features:"));
        outputPanel.setLayout(new java.awt.BorderLayout(10, 0));

        featuresTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "Selected"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        featuresTable.setDragEnabled(true);
        featuresTable.setDropMode(javax.swing.DropMode.ON_OR_INSERT);
        featuresTable.setFillsViewportHeight(true);
        featuresTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        featuresTable.getTableHeader().setReorderingAllowed(false);
        outputScrollPane.setViewportView(featuresTable);

        outputPanel.add(outputScrollPane, java.awt.BorderLayout.CENTER);

        outputButtonPanel.setEnabled(false);
        outputButtonPanel.setLayout(new javax.swing.BoxLayout(outputButtonPanel, javax.swing.BoxLayout.PAGE_AXIS));

        setButton.setText("Set");
        setButton.setMaximumSize(new java.awt.Dimension(89, 23));
        setButton.setMinimumSize(new java.awt.Dimension(89, 23));
        setButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setButtonActionPerformed(evt);
            }
        });
        outputButtonPanel.add(setButton);
        outputButtonPanel.add(buttonPanelFiller1);

        selectAllButton.setText("Select All");
        selectAllButton.setMaximumSize(new java.awt.Dimension(89, 23));
        selectAllButton.setMinimumSize(new java.awt.Dimension(89, 23));
        selectAllButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectAllButtonActionPerformed(evt);
            }
        });
        outputButtonPanel.add(selectAllButton);

        selectNoneButton.setText("Select None");
        selectNoneButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectNoneButtonActionPerformed(evt);
            }
        });
        outputButtonPanel.add(selectNoneButton);
        outputButtonPanel.add(buttonPanelFiller2);

        upButton.setText("Up");
        upButton.setMaximumSize(new java.awt.Dimension(89, 23));
        upButton.setMinimumSize(new java.awt.Dimension(89, 23));
        upButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                upButtonActionPerformed(evt);
            }
        });
        outputButtonPanel.add(upButton);

        downButton.setText("Down");
        downButton.setMaximumSize(new java.awt.Dimension(89, 23));
        downButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                downButtonActionPerformed(evt);
            }
        });
        outputButtonPanel.add(downButton);

        outputPanel.add(outputButtonPanel, java.awt.BorderLayout.EAST);

        centerPanel.add(outputPanel, java.awt.BorderLayout.CENTER);

        mainPanel.add(centerPanel, java.awt.BorderLayout.CENTER);

        getContentPane().add(mainPanel, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void updateIOPanelsEnabled(boolean enabled) {
        updateInputPanelEnabled(enabled);
        updateOutputPanelEnabled(enabled);
    }

    private void setPanelComponentsEnabled(javax.swing.JPanel panel, boolean enabled) {
        for (java.awt.Component component : panel.getComponents()) {
            component.setEnabled(enabled);
        }
    }

    private void updateInputPanelEnabled(boolean enabled) {
        performCheckBox.setEnabled(enabled);
    }

    private void updateOutputPanelEnabled(boolean enabled) {
        outputPanel.setEnabled(enabled);
        setPanelComponentsEnabled(outputButtonPanel, enabled);
    }

    private void stopConnections() {
        reader.stopConnection();
    }

    /* ---------------------------------------------------------------------- *
     *                          Output Buttons Panel                          *
     * ---------------------------------------------------------------------- */

    private void setButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setButtonActionPerformed
        reader.setSelected(getSelectedFeatures());
    }//GEN-LAST:event_setButtonActionPerformed

    private void selectAllButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectAllButtonActionPerformed
        DefaultTableModel model = (DefaultTableModel) featuresTable.getModel();
        for (int i = 0; i < model.getRowCount(); ++i) {
            model.setValueAt(true, i, 1);
        }
    }//GEN-LAST:event_selectAllButtonActionPerformed

    private void selectNoneButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectNoneButtonActionPerformed
        DefaultTableModel model = (DefaultTableModel) featuresTable.getModel();
        for (int i = 0; i < model.getRowCount(); ++i) {
            model.setValueAt(false, i, 1);
        }
    }//GEN-LAST:event_selectNoneButtonActionPerformed

    private void upButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_upButtonActionPerformed
        int selectedIndex = featuresTable.getSelectedRow();
        if (selectedIndex > 0) {
            moveSelectedFeature(selectedIndex, selectedIndex - 1);
        }
    }//GEN-LAST:event_upButtonActionPerformed

    private void downButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_downButtonActionPerformed
        int selectedIndex = featuresTable.getSelectedRow();
        if (selectedIndex > 0) {
            moveSelectedFeature(selectedIndex, selectedIndex + 1);
        }
    }//GEN-LAST:event_downButtonActionPerformed

    private void filterCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterCheckBoxActionPerformed
        reader.setUseFilter(filterCheckBox.isSelected());
    }//GEN-LAST:event_filterCheckBoxActionPerformed

    private void performCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_performCheckBoxActionPerformed
        boolean perform = performCheckBox.isSelected();
        LOGGER.info(String.format("Perform: %b",perform));
        reader.setPerforming(perform);        
    }//GEN-LAST:event_performCheckBoxActionPerformed

    private void jSpinnerfilterPowStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSpinnerfilterPowStateChanged
        try {
            jSpinnerfilterPow.commitEdit();
        } catch ( java.text.ParseException e ) {  }
        double value = (Double)jSpinnerfilterPow.getValue();
        setFilterPow(value);
    }//GEN-LAST:event_jSpinnerfilterPowStateChanged

    private void jSpinnerfilterMaxQueueSizeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSpinnerfilterMaxQueueSizeStateChanged
        try {
            jSpinnerfilterMaxQueueSize.commitEdit();
        } catch ( java.text.ParseException e ) {  }
        int value = (Integer)jSpinnerfilterMaxQueueSize.getValue();
        setFilterMaxQueueSize(value);
    }//GEN-LAST:event_jSpinnerfilterMaxQueueSizeStateChanged

    private void jCheckBoxSendOSCStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jCheckBoxSendOSCStateChanged
        // TODO add your handling code here:
        setOutput(jCheckBoxSendOSC.isSelected());
    }//GEN-LAST:event_jCheckBoxSendOSCStateChanged

    private void jSpinnerSendOSCPortStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSpinnerSendOSCPortStateChanged
        try {
            jSpinnerSendOSCPort.commitEdit();
        } catch ( java.text.ParseException e ) {  }
        int value = (Integer)jSpinnerSendOSCPort.getValue();
        setOscOutPort(value);
    }//GEN-LAST:event_jSpinnerSendOSCPortStateChanged

    /* ---------------------------------------------------------------------- *
     *                              Tabbed Pane                               *
     * ---------------------------------------------------------------------- */

    private void inputTabbedPaneStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_inputTabbedPaneStateChanged
        stopConnections();
    }//GEN-LAST:event_inputTabbedPaneStateChanged

    private void oscInConnectButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_oscInConnectButtonActionPerformed
        // TODO add your handling code here:
        if (evt.getActionCommand().equals("Connect")) {
            int port = Integer.parseInt(oscInPortTextField.getText());
            reader.setPort(port);
            reader.startConnection();
        } else if (evt.getActionCommand().equals("Disconnect")) {
            reader.stopConnection();
        }
    }//GEN-LAST:event_oscInConnectButtonActionPerformed

    private void oscInPortTextFieldKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_oscInPortTextFieldKeyTyped
        // TODO add your handling code here:
    }//GEN-LAST:event_oscInPortTextFieldKeyTyped

    private void oscInPortTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_oscInPortTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_oscInPortTextFieldActionPerformed

    private void oscInPortTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_oscInPortTextFieldFocusLost
        // TODO add your handling code here:
    }//GEN-LAST:event_oscInPortTextFieldFocusLost

    /* ---------------------------------------------------------------------- */

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.Box.Filler buttonPanelFiller1;
    private javax.swing.Box.Filler buttonPanelFiller2;
    private javax.swing.JPanel centerPanel;
    private javax.swing.JButton downButton;
    private javax.swing.JTable featuresTable;
    private javax.swing.JCheckBox filterCheckBox;
    private javax.swing.JTabbedPane inputTabbedPane;
    private javax.swing.JCheckBox jCheckBoxSendOSC;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JSpinner jSpinnerSendOSCPort;
    private javax.swing.JSpinner jSpinnerfilterMaxQueueSize;
    private javax.swing.JSpinner jSpinnerfilterPow;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JPanel northPanel;
    private javax.swing.Box.Filler northPanelFiller1;
    private javax.swing.Box.Filler northPanelFiller2;
    private javax.swing.JLabel oscConnectedLabel;
    private javax.swing.JPanel oscConnectorPanel;
    private javax.swing.JButton oscInConnectButton;
    private javax.swing.Box.Filler oscInConnectorPanelFiller;
    private javax.swing.Box.Filler oscInConnectorPanelFiller2;
    private javax.swing.JLabel oscInPortLabel;
    private javax.swing.JTextField oscInPortTextField;
    private javax.swing.JLabel oscStatusLabel;
    private javax.swing.JPanel oscStatusPanel;
    private javax.swing.Box.Filler oscStatusPanelFiller;
    private javax.swing.JPanel oscTab;
    private javax.swing.JPanel outputButtonPanel;
    private javax.swing.JPanel outputPanel;
    private javax.swing.JScrollPane outputScrollPane;
    private javax.swing.JCheckBox performCheckBox;
    private javax.swing.JButton selectAllButton;
    private javax.swing.JButton selectNoneButton;
    private javax.swing.JSeparator separator;
    private javax.swing.JButton setButton;
    private javax.swing.JButton upButton;
    // End of variables declaration//GEN-END:variables

    /* ---------------------------------------------------------------------- */

    /**
     * @param index the tab index to select
     */
    public void setTabIndex(int index) {
        inputTabbedPane.setSelectedIndex(index);
    }

    /**
     * @return the selected tab index
     */
    public int getTabIndex() {
        return inputTabbedPane.getSelectedIndex();
    }

    /* ---------------------------------------------------------------------- */

    private void updateFeatures(List<String> newFeatures) {
        DefaultTableModel model = (DefaultTableModel) featuresTable.getModel();
        for (int i = model.getRowCount() - 1; i >= 0; --i) {
            model.removeRow(i);
        }
        for (String feature : newFeatures) {
            model.addRow(new Object[]{feature, false});
        }
    }

    private String[] getSelectedFeatures() {
        DefaultTableModel model = (DefaultTableModel) featuresTable.getModel();
        List<String> selected = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); ++i) {
            if ((Boolean) model.getValueAt(i, 1)) {
                selected.add((String) model.getValueAt(i, 0));
            }
        }
        return selected.toArray(new String[selected.size()]);
    }

    private void moveSelectedFeature(int old, int newIndex) {
        DefaultTableModel model = (DefaultTableModel) featuresTable.getModel();
        model.moveRow(old, old, newIndex);
        featuresTable.setRowSelectionInterval(old, newIndex);
    }

    /* ---------------------------------------------------------------------- *
     *                               AUEmitter                                *
     * ---------------------------------------------------------------------- */

    @Override
    public void addAUPerformer(AUPerformer auPerformer) {
        reader.addAUPerformer(auPerformer);
        
    }

    @Override
    public void removeAUPerformer(AUPerformer auPerformer) {        
        reader.removeAUPerformer(auPerformer);
    }

    public void sendAUFrame(AUAPFrame auFrame, ID id) {
        //LOGGER.info("sendAUFrame");
        reader.sendAUFrame(auFrame, id);
    }

    /* ---------------------------------------------------------------------- *
     *                            BAPFrameEmitter                             *
     * ---------------------------------------------------------------------- */

    @Override
    public void addBAPFramePerformer(BAPFramePerformer bapFramePerformer) {
        reader.addBAPFramePerformer(bapFramePerformer);        
    }

    @Override
    public void removeBAPFramePerformer(BAPFramePerformer bapFramePerformer) {
        reader.removeBAPFramePerformer(bapFramePerformer);
    }

    public void sendBAPFrame(BAPFrame bapFrame, ID id) {       
        reader.sendBAPFrame(bapFrame, id);
    }

    /* ---------------------------------------------------------------------- *
     *                           ConnectionListener                           *
     * ---------------------------------------------------------------------- */

    @Override
    public void onConnection() {
        setOSCConnected(true);
        
    }

    @Override
    public void onDisconnection() {
        setOSCConnected(false);
    }

    /* ---------------------------------------------------------------------- *
     *                          StringArrayListener                           *
     * ---------------------------------------------------------------------- */

    @Override
    public void stringArrayChanged(List<String> newFeatures) {
        updateFeatures(newFeatures);
    }
    
     /**
     * @return the oscInPort
     */
    public int getOscInPort() {
        return oscInPort;
    }

    /**
     * @param oscInPort the oscInPort to set
     */
    public void setOscInPort(int oscInPort) {
        this.oscInPort = oscInPort;
    }
}
