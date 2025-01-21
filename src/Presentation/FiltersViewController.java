package Presentation;

import Domain.Model;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

public class FiltersViewController implements ActionListener {
    private Model model;
    private FiltersView filtersView;
    private Map<Object, Runnable> actionMap;
    public FiltersViewController(Model model, FiltersView filtersView) {
        this.model = model;
        this.filtersView = filtersView;
        initializeActionMap();
    }
    private void initializeActionMap() {
        actionMap = new HashMap<>();
        actionMap.put(filtersView.getAddKeywordButton(), this::handleAddKeywordButton);
        actionMap.put(filtersView.getDeleteKeywordButton(), this::handleDeleteKeywordButton);
        actionMap.put(filtersView.getResetKeywordsButton(), this::handleResetKeywordsButton);
        actionMap.put(filtersView.getResetFiltersButton(), this::handleResetFiltersButton);
        actionMap.put(filtersView.getApplyFiltersButton(), this::handleApplyFiltersButton);
        actionMap.put(filtersView.getCancelButton(), this::handleCancelButton);

    }

    private void handleCancelButton() {
        filtersView.close();
    }

    private void handleApplyFiltersButton() {
        model.setFilters(filtersView.getFilters());
    }

    private void handleResetFiltersButton() {
        filtersView.resetFilters();
    }

    private void handleResetKeywordsButton() {
        filtersView.resetKeywords();
    }

    private void handleDeleteKeywordButton() {
        filtersView.deleteKeyword();
    }

    private void handleAddKeywordButton() {
        filtersView.addKeyword();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Runnable action = actionMap.get(e.getSource());
        if (action != null) {
            action.run();
        }
    }
}
