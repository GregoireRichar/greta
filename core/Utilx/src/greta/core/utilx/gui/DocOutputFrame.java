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

/**
 *
 * @author Andre-Marie Pez
 */
public class DocOutputFrame extends javax.swing.JFrame {

    private DocOutput docOutput;

    /** Creates new form DocOutputFrame */
    public DocOutputFrame() {
        initComponents();
    }

    public DocOutputFrame(DocOutput doc) {
        this();
        setDocOutput(doc);
    }

    public final void setDocOutput(DocOutput doc){
        docOutput = doc;
        logView.setDocument(docOutput);
        setBlack(docOutput.hasBlackBackground());
    }

    public boolean isBlack(){
        return docOutput.hasBlackBackground();
    }

    @Override
    public void validate() {
        super.validate();
        if(docOutput!=null) {
            setBlack(docOutput.hasBlackBackground());
        }
    }

    public void setBlack(boolean isBlack){
        if("Nimbus".equals(javax.swing.UIManager.getLookAndFeel().getName())){
            docOutput.setBlackBackground(false); // black background dont works with nimbus
            checkBoxIsBlack.setEnabled(false);
        }
        else{
            docOutput.setBlackBackground(isBlack);
            checkBoxIsBlack.setEnabled(true);
        }
        logView.setBackground(docOutput.hasBlackBackground() ? java.awt.Color.black : java.awt.Color.white);
        checkBoxIsBlack.setSelected(docOutput.hasBlackBackground());
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        logContainer = new javax.swing.JScrollPane();
        logView = new javax.swing.JTextPane();
        checkBoxIsBlack = new greta.core.utilx.gui.ToolBox.LocalizedJCheckBox("utilx.logframe.black");
        jButton1 = new javax.swing.JButton();

        logContainer.setViewportView(logView);

        checkBoxIsBlack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxIsBlackActionPerformed(evt);
            }
        });

        jButton1.setText("Clear");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(logContainer, javax.swing.GroupLayout.DEFAULT_SIZE, 582, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(checkBoxIsBlack)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton1)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(logContainer, javax.swing.GroupLayout.DEFAULT_SIZE, 346, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(checkBoxIsBlack)
                    .addComponent(jButton1))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void checkBoxIsBlackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxIsBlackActionPerformed
        setBlack(checkBoxIsBlack.isSelected());
    }//GEN-LAST:event_checkBoxIsBlackActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
        try{
        this.docOutput.remove(0, docOutput.getLength());
        }
        catch(Exception ex){
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox checkBoxIsBlack;
    private javax.swing.JButton jButton1;
    private javax.swing.JScrollPane logContainer;
    private javax.swing.JTextPane logView;
    // End of variables declaration//GEN-END:variables

}
