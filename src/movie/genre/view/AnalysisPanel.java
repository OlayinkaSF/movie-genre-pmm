/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package movie.genre.view;

import org.json.JSONArray;

/**
 *
 * @author Olayinka
 */
public class AnalysisPanel extends javax.swing.JPanel implements AnalysisListener {

    /**
     * Creates new form AnalysisPhase
     */
    public AnalysisPanel() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        stateLabel = new javax.swing.JLabel();

        setMaximumSize(new java.awt.Dimension(800, 450));
        setPreferredSize(new java.awt.Dimension(800, 450));
        setLayout(new java.awt.GridBagLayout());

        stateLabel.setFont(new java.awt.Font("Verdana", 0, 14)); // NOI18N
        stateLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        stateLabel.setText("Folorunso");
        stateLabel.setAlignmentX(0.5F);
        stateLabel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        stateLabel.setPreferredSize(new java.awt.Dimension(300, 100));
        add(stateLabel, new java.awt.GridBagConstraints());
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel stateLabel;
    // End of variables declaration//GEN-END:variables

    @Override
    public void onChangeAnalysis(String log) {
        stateLabel.setText("<html><p>" + log + "</p></html>");
    }

}
