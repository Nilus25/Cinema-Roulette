package Presentation;

import Domain.Model;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

public class FiltersViewController implements ActionListener {
    private final Model model;
    private final FiltersView filtersView;
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
        actionMap.put(filtersView.getDeselectGenresButton(), this::handleDeselectGenresButton);
        actionMap.put(filtersView.getSelectAllGenresButton(), this::handleSelectAllGenresButton);
        actionMap.put(filtersView.getDeselectAllTypesButton(), this::handleDeselectAllTypesButton);
        actionMap.put(filtersView.getSelectAllTypesButton(), this::handleSelectAllTypesButton);

    }

    private void handleSelectAllTypesButton() {
        filtersView.selectAllTypes();
    }

    private void handleDeselectAllTypesButton() {
        filtersView.deselectAllTypes();
    }

    private void handleSelectAllGenresButton() {
        filtersView.selectAllGenres();
    }

    private void handleDeselectGenresButton() {
        filtersView.deselectGenres();
    }

    private void handleCancelButton() {
        filtersView.close();
    }

    private void handleApplyFiltersButton() {
        model.setFilters(filtersView.getFilters());
        filtersView.close();
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
