/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package movie.genre.view;

import movie.genre.knn.KNNClassifier;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.IOException;
import javax.swing.JPanel;
import movie.genre.pmm.PMMClassifier;
import org.json.JSONException;

/**
 *
 * @author Olayinka
 */
public class RootView extends javax.swing.JFrame implements AnalysisListener {

    KNNClassifier knnClassifier;
    PMMClassifier pmmClassifier;
    CardLayout cardLayout;
    private final JPanel cards;
    private final ClassifyPanel classifyPanel;
    private final AnalysisPanel analysisPanel;

    public void setKnnClassifier(KNNClassifier knnClassifier) {
        this.knnClassifier = knnClassifier;
        classifyPanel.setKnnClassifier(knnClassifier);
    }

    public void setPmmClassifier(PMMClassifier pmmClassifier) {
        this.pmmClassifier = pmmClassifier;
        classifyPanel.setPmmClassifier(pmmClassifier);
    }

    /**
     * Creates new form RootView
     */
    public static String ANALYSIS_PANEL_KEY = "analysis";
    public static String CLASSIFY_PANEL_KEY = "classify";

    public RootView() {
        initComponents();
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(dim.width / 2 - this.getSize().width / 2, dim.height / 2 - this.getSize().height / 2);
        analysisPanel = new AnalysisPanel();
        classifyPanel = new ClassifyPanel();
        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);
        cards.add(classifyPanel, CLASSIFY_PANEL_KEY);
        cards.add(analysisPanel, ANALYSIS_PANEL_KEY);
        cardLayout.show(cards, ANALYSIS_PANEL_KEY);
        add(cards);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Movie Classifier (KNN, PMM)");
        setPreferredSize(new java.awt.Dimension(800, 450));
        setResizable(false);

        pack();
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
    public void onFinishAnalysis() throws IOException, JSONException {
        cardLayout.show(cards, CLASSIFY_PANEL_KEY);
        classifyPanel.loadTestData();
    }

    @Override
    public void onChangeAnalysis(String log) {
        analysisPanel.onChangeAnalysis(log);
    }
}
