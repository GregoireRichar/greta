/*
 * This file is part of Greta.
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
package greta.core.utilx.gui;

import greta.core.util.CharacterManager;
import greta.core.util.speech.Speech;
import greta.core.util.speech.TTS;

/**
 *
 * @author Andre-Marie Pez
 */
public class NVBG_MM_Controller extends javax.swing.JFrame {

    private CharacterManager cm;

    /** Creates new form TTSController */
    public NVBG_MM_Controller(CharacterManager cm) {
        this.cm = cm;
        initComponents();
        doMM.setSelected(false);
        doNVBG.setSelected(false);
    }

    private void updateNVBG_MM_Options(){
            if(doNVBG.isSelected()){
                this.cm.set_use_NVBG(true); 
            }
            else{
                this.cm.set_use_NVBG(false); 
            }
            if(doMM.isSelected()){
                this.cm.set_use_MM(true);            
            }
            else{
                this.cm.set_use_MM(false); 
            }
            
            
    }


    public boolean getNVBG(){
        return doNVBG.isSelected();
    }


    public boolean getMM(){
        return doMM.isSelected();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        doNVBG = new greta.core.utilx.gui.ToolBox.LocalizedJCheckBox("NVBG");
        doMM = new greta.core.utilx.gui.ToolBox.LocalizedJCheckBox("Meaning Miner");

        doNVBG.setSelected(true);
        doNVBG.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                doNVBGActionPerformed(evt);
            }
        });

        doMM.setSelected(true);
        doMM.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                doMMActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(doNVBG)
                    .addComponent(doMM))
                .addContainerGap(180, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(doNVBG)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 29, Short.MAX_VALUE)
                .addComponent(doMM)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void doNVBGActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_doNVBGActionPerformed
        updateNVBG_MM_Options();
    }//GEN-LAST:event_doNVBGActionPerformed

    private void doMMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_doMMActionPerformed
        updateNVBG_MM_Options();
    }//GEN-LAST:event_doMMActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox doMM;
    private javax.swing.JCheckBox doNVBG;
    // End of variables declaration//GEN-END:variables

}
