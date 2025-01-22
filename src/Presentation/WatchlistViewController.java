package Presentation;

import Domain.Classes.Movie;
import Domain.Model;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class WatchlistViewController implements ActionListener {
    private WatchlistView watchlistView;
    private Model model;
    public WatchlistViewController(WatchlistView watchlistView, Model model) {
        this.watchlistView = watchlistView;
        this.model = model;
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == watchlistView.getOkButton()) {
            watchlistView.close();
        }
        else if (e.getSource() == watchlistView.getOpenIMDBButton()) {
            watchlistView.openIMDB();
        }
        else if (e.getSource() == watchlistView.getAddToSeenButton()) {
            if (watchlistView.getSelectedMovies().size() >= 10) {
                if (!showConfirmationDialog()) {
                    return;
                }
            }
            for (Movie movie : watchlistView.getSelectedMovies()) {
                watchlistView.addSeenMovie(movie);
                model.addMovieToSeen(movie);
            }
        }
        else if (e.getSource() == watchlistView.getDeleteFromSeenButton()) {
            if (watchlistView.getSelectedMovies().size() >= 10) {
                if (!showConfirmationDialog()) {
                    return;
                }
            }
            for (Movie movie : watchlistView.getSelectedMovies()) {
                watchlistView.removeSeenMovie(movie);
                model.deleteMovieFromSeen(movie);
            }
        }
        else if (e.getSource() == watchlistView.getSelectAllButton()) {
            watchlistView.selectAll();
        }
        else if (e.getSource() == watchlistView.getDeselectAllButton()) {
            watchlistView.deselectAll();
        }
    }
    private boolean showConfirmationDialog() {
        int response = JOptionPane.showConfirmDialog(
                watchlistView,
                "You have selected 10 or more movies. Are you sure?",
                "Confirm operation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        return response == JOptionPane.YES_OPTION;
    }
}
