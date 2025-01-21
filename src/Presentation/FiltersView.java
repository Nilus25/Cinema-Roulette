package Presentation;

import Domain.Classes.Filter;
import Domain.Classes.FiltersDTO;
import Domain.Model;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class FiltersView extends JDialog {
    private Model model;
    private FiltersViewController filtersViewController;
    private JPanel mainPanel;
    private JTextField keywordTextField;
    private JButton addKeywordButton;
    private JList keywordsList;
    private JButton deleteKeywordButton;
    private JSpinner minYearSpinner;
    private JSpinner maxYearSpinner;
    private JSpinner maxRatingSpinner;
    private JSpinner minRatingSpinner;
    private JButton resetKeywordsButton;
    private JButton resetFiltersButton;
    private JButton applyFiltersButton;
    private JButton cancelButton;
    private JLabel YearLabel;
    private JPanel MinYearLabel;
    private JLabel maxYearLabel;
    private JLabel maxRatingLabel;
    private JLabel minRatingLabel;
    private JLabel genresLabel;
    private JLabel typesLabel;
    private JPanel genresScrollBar;
    private JPanel typesScrollBar;
    private JLabel keywordsLabel;

    public FiltersView(Model model, JFrame parent) {
        this.model = model;
        setContentPane(mainPanel);
        setSize(900, 600);
        setLocationRelativeTo(parent);
        initialize();
        setFilters(model.getFilters());
        initializeController();
    }

    public void initializeController() {
        filtersViewController = new FiltersViewController(model, this);
        addKeywordButton.addActionListener(filtersViewController);
        deleteKeywordButton.addActionListener(filtersViewController);
        resetKeywordsButton.addActionListener(filtersViewController);
        resetFiltersButton.addActionListener(filtersViewController);
        applyFiltersButton.addActionListener(filtersViewController);
        cancelButton.addActionListener(filtersViewController);

    }

    public Container getMainPanel() {
        return mainPanel;
    }
    public JButton getAddKeywordButton() {
        return addKeywordButton;
    }

    public JButton getDeleteKeywordButton() {
        return deleteKeywordButton;
    }

    public JButton getResetFiltersButton() {
        return resetFiltersButton;
    }

    public JButton getApplyFiltersButton() {
        return applyFiltersButton;
    }

    public JButton getResetKeywordsButton() {
        return resetKeywordsButton;
    }

    public JButton getCancelButton() {
        return cancelButton;
    }

    private void initialize() {
        genresScrollBar.setLayout(new GridLayout(4, 7));
        typesScrollBar.setLayout(new GridLayout(3, 5));
        for (String genre : Filter.GENREARRAY) {
            JCheckBox checkBox = new JCheckBox(genre);
            checkBox.setPreferredSize(new Dimension(35, 35));
            genresScrollBar.add(checkBox);
        }
        for (String type : Filter.TYPEARRAY) {
            JCheckBox checkBox = new JCheckBox(type);
            checkBox.setPreferredSize(new Dimension(30, 30));
            typesScrollBar.add(checkBox);
        }
        minYearSpinner.setModel(new SpinnerNumberModel(Filter.MINIMUMYEAR, Filter.MINIMUMYEAR, Filter.MAXIMUMYEAR, 1));
        maxYearSpinner.setModel(new SpinnerNumberModel(Filter.MAXIMUMYEAR, Filter.MINIMUMYEAR, Filter.MAXIMUMYEAR, 1));
        JSpinner.NumberEditor minYearEditor = new JSpinner.NumberEditor(minYearSpinner,  "####");
        minYearSpinner.setEditor(minYearEditor);
        JSpinner.NumberEditor maxYearEditor = new JSpinner.NumberEditor(maxYearSpinner,  "####");
        maxYearSpinner.setEditor(maxYearEditor);
        minRatingSpinner.setModel(new SpinnerNumberModel((double)Filter.MINIMUMRATING, (double)Filter.MINIMUMRATING, (double)Filter.MAXIMUMRATING, 0.1));
        maxRatingSpinner.setModel(new SpinnerNumberModel((double)Filter.MAXIMUMRATING,(double) Filter.MINIMUMRATING,(double) Filter.MAXIMUMRATING, 0.1));
        JSpinner.NumberEditor maxRatingEditor = new JSpinner.NumberEditor(maxRatingSpinner, "#0.0");
        maxRatingSpinner.setEditor(maxRatingEditor);
        JSpinner.NumberEditor minRatingEditor = new JSpinner.NumberEditor(minRatingSpinner, "#0.0");
        minRatingSpinner.setEditor(minRatingEditor);
    }

    public void resetFilters() {
        minYearSpinner.setValue(Filter.MINIMUMYEAR);
        maxYearSpinner.setValue(Filter.MAXIMUMYEAR);
        minRatingSpinner.setValue(Filter.MINIMUMRATING);
        maxRatingSpinner.setValue(Filter.MAXIMUMRATING);
        for (int i = 0; i < Filter.GENREARRAY.length; i++) {
            ((JCheckBox) genresScrollBar.getComponent(i)).setSelected(false);
        }
        for (int i = 0; i < Filter.TYPEARRAY.length; i++) {
            ((JCheckBox) typesScrollBar.getComponent(i)).setSelected(false);
        }
        keywordsList.setListData(new String[0]);
    }

    public void setFilters(FiltersDTO filters) {
        minYearSpinner.setValue(filters.getMinimumYearValue());
        maxYearSpinner.setValue(filters.getMaximumYearValue());
        minRatingSpinner.setValue(filters.getMinimumRatingValue());
        maxRatingSpinner.setValue(filters.getMaximumRatingValue());
        for (int i = 0; i < Filter.GENREARRAY.length; i++) {
            ((JCheckBox) genresScrollBar.getComponent(i)).setSelected(filters.getCheckedGenres()[i]);
        }
        for (int i = 0; i < Filter.TYPEARRAY.length; i++) {
            ((JCheckBox) typesScrollBar.getComponent(i)).setSelected(filters.getCheckedTypes()[i]);
        }
        keywordsList.setListData(filters.getKeywords().toArray(new String[0]));
    }

    public FiltersDTO getFilters() {
        int minimumYearValue = (int) minYearSpinner.getValue();
        int maximumYearValue = (int) maxYearSpinner.getValue();
        double minimumRatingValue = (double) minRatingSpinner.getValue();
        double maximumRatingValue = (double) maxRatingSpinner.getValue();
        boolean[] checkedGenres = new boolean[Filter.GENREARRAY.length];
        for (int i = 0; i < Filter.GENREARRAY.length; i++) {
            checkedGenres[i] = ((JCheckBox) genresScrollBar.getComponent(i)).isSelected();
        }
        boolean[] checkedTypes = new boolean[Filter.TYPEARRAY.length];
        for (int i = 0; i < Filter.TYPEARRAY.length; i++) {
            checkedTypes[i] = ((JCheckBox) typesScrollBar.getComponent(i)).isSelected();
        }
        ArrayList<String> keywords = getAllValues(keywordsList);
        FiltersDTO filters = new FiltersDTO(checkedTypes, checkedGenres, minimumYearValue, maximumYearValue, minimumRatingValue, maximumRatingValue, keywords);
        return filters;

    }

    private ArrayList<String> getAllValues(JList keywordsList) {
        ArrayList<String> keywords = new ArrayList<>();
        for (int i = 0; i < keywordsList.getModel().getSize(); i++) {
            keywords.add((String) keywordsList.getModel().getElementAt(i));
        }
        return keywords;
    }

    public void resetKeywords() {
        keywordsList.setListData(new String[0]);
    }

    public void deleteKeyword() {
        DefaultListModel<String> model = (DefaultListModel<String>) keywordsList.getModel();
        keywordsList.getSelectedValuesList().forEach(model::removeElement);
    }

    public void addKeyword() {
        DefaultListModel<String> model = (DefaultListModel<String>) keywordsList.getModel();
        model.addElement(keywordTextField.getText());
    }

    public void close() {
        this.dispose();
    }
}
