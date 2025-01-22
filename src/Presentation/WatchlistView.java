package Presentation;
import Domain.Classes.Movie;
import Domain.Classes.Watchlist;
import Domain.Model;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class WatchlistView extends JDialog {
    private Model model;
    private WatchlistViewController watchlistViewController;
    private JPanel panel1;
    private JButton okButton;
    private JButton openIMDBButton;
    private JPanel watchlistPanel;
    private JScrollPane watchlistScrollPane;
    private JButton addToSeenButton;
    private JButton deleteFromSeenButton;
    private JButton selectAllButton;
    private JButton deselectAllButton;
    private List<WatchlistGUIItem> selectedMovies = new ArrayList<>();

    public WatchlistView(Model model, JFrame parent) {
        super(parent, "Watchlist", true);
        this.model = model;
        setContentPane(panel1);
        setSize(500, 800);
        setLocationRelativeTo(parent);
        initialize();
        initializeController();
    }

    private void initializeController() {
        watchlistViewController = new WatchlistViewController(this, model);
        okButton.addActionListener(watchlistViewController);
        openIMDBButton.addActionListener(watchlistViewController);
        addToSeenButton.addActionListener(watchlistViewController);
        deleteFromSeenButton.addActionListener(watchlistViewController);
        selectAllButton.addActionListener(watchlistViewController);
        deselectAllButton.addActionListener(watchlistViewController);
    }

    private void initialize() {
        JScrollBar verticalScrollBar = watchlistScrollPane.getVerticalScrollBar();
        verticalScrollBar.setUnitIncrement(16);
        watchlistPanel.setLayout(new BoxLayout(watchlistPanel, BoxLayout.Y_AXIS));
        watchlistPanel.setBorder(BorderFactory.createMatteBorder(1, 1, 0, 1,Color.decode("#f57900")));
        Watchlist watchlist = model.getFilteredWatchlist();
        int watchlistPanelWidth = watchlistPanel.getWidth();
        boolean hasUser = model.isThereUser();
        for (Movie movie : watchlist.getMovies()) {
            WatchlistGUIItem watchlistGUIItem = new WatchlistGUIItem(movie, this, watchlistPanelWidth);
            watchlistPanel.add(watchlistGUIItem);
            if (hasUser){
                 if(model.userHasSeenMovie(movie)) {
                     watchlistGUIItem.paintItSeen(0);
                 }
                    else {
                        watchlistGUIItem.paintItSeen(1);
                    }
            }
            else {
                watchlistGUIItem.paintItSeen(2);
            }
        }
        if (!hasUser){
            addToSeenButton.setEnabled(false);
            deleteFromSeenButton.setEnabled(false);
        }
    }

    public void addSelectedMovie(WatchlistGUIItem watchlistGUIItem) {
        selectedMovies.add(watchlistGUIItem);
    }
    public void removeSelectedMovie(WatchlistGUIItem watchlistGUIItem) {
        selectedMovies.remove(watchlistGUIItem);
    }

    public Object getOkButton() {
        return okButton;
    }

    public Object getOpenIMDBButton() {
        return openIMDBButton;
    }
    public Object getAddToSeenButton() {
        return addToSeenButton;
    }
    public Object getDeleteFromSeenButton() {
        return deleteFromSeenButton;
    }
    public Object getSelectAllButton() {
        return selectAllButton;
    }
    public Object getDeselectAllButton() {
        return deselectAllButton;
    }

    public void close() {
        this.dispose();
    }

    public void openIMDB() {
        for (WatchlistGUIItem watchlistGUIItem : selectedMovies) {
            watchlistGUIItem.openIMDBURL();
        }
    }

    public List<Movie> getSelectedMovies() {
        List<Movie> movies = new ArrayList<>();
        for (WatchlistGUIItem watchlistGUIItem : selectedMovies) {
            movies.add(watchlistGUIItem.getMovie());
        }
        return movies;
    }

    public void selectAll() {
        for (Component component : watchlistPanel.getComponents()) {
            if (component instanceof WatchlistGUIItem) {
                WatchlistGUIItem watchlistGUIItem = (WatchlistGUIItem) component;
                watchlistGUIItem.setSelected(true);
                selectedMovies.add(watchlistGUIItem);
            }
        }
    }

    public void deselectAll() {
        for (Component component : watchlistPanel.getComponents()) {
            if (component instanceof WatchlistGUIItem) {
                WatchlistGUIItem watchlistGUIItem = (WatchlistGUIItem) component;
                watchlistGUIItem.setSelected(false);
            }
        }
        selectedMovies.clear();
    }

    public void addSeenMovie(Movie movie) {
        for (WatchlistGUIItem watchlistGUIItem : selectedMovies) {
            if (watchlistGUIItem.getMovie().equals(movie)) {
                watchlistGUIItem.paintItSeen(0);
            }
        }
    }

    public void removeSeenMovie(Movie movie) {
        for (WatchlistGUIItem watchlistGUIItem : selectedMovies) {
            if (watchlistGUIItem.getMovie().equals(movie)) {
                watchlistGUIItem.paintItSeen(1);
            }
        }
    }

    public boolean hasUser() {
        return model.isThereUser();
    }
}
class WatchlistGUIItem extends JPanel {
    private final WatchlistView parent;
    private final Movie movie;
    private boolean selected = false;
    private JPanel contentPanel;
    private JPanel seenPanel;

    public WatchlistGUIItem(Movie movie, WatchlistView watchlistView, int watchlistPanelWidth) {
        this.parent = watchlistView;
        this.movie = movie;
        this.setLayout(new BorderLayout());
        this.setPreferredSize(new Dimension(watchlistPanelWidth, 100));
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10)); // Add margins to the left and right

        JLabel title = new JLabel("<html>Title: " +"<b>"+ movie.getTitle() + "</b></html>");        JLabel director = new JLabel("Director: " + movie.getDirector());
        JLabel year = new JLabel("Year: " + movie.getYear());
        JLabel rating = new JLabel("IMDB Rating: " + movie.getRating());

        StringBuilder genres = new StringBuilder();
        genres.append("Genres: ");
        for (String genre : movie.getGenres()) {
            if (genre.equals(movie.getGenres().get(movie.getGenres().size() - 1))) {
                genres.append(genre);
            } else {
                genres.append(genre).append(", ");
            }
        }
        JLabel genresLabel = new JLabel(String.valueOf(genres));
        JLabel type = new JLabel("Type: " + movie.getType());

        contentPanel.add(title);
        contentPanel.add(director);
        contentPanel.add(year);
        contentPanel.add(rating);
        contentPanel.add(genresLabel);
        contentPanel.add(type);

        this.add(contentPanel, BorderLayout.CENTER);
        seenPanel = new JPanel();
        this.add(seenPanel, BorderLayout.EAST);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.decode("#f57900")));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selected = !selected;
                updateAppearance();
                if (selected) {
                    parent.addSelectedMovie(WatchlistGUIItem.this);
                } else {
                    parent.removeSelectedMovie(WatchlistGUIItem.this);
                }
            }
        });
        this.setVisible(true);
    }

    private void updateAppearance() {
        boolean hasUser = parent.hasUser();
        if (selected) {
            contentPanel.setBackground(Color.decode("#ffc080"));
            if (!hasUser) {
                this.paintItSeen(3);
            }
        } else {
            contentPanel.setBackground(null);
            if (!hasUser) {
                this.paintItSeen(2);
            }
        }
        repaint();
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        updateAppearance();
    }

    public Movie getMovie() {
        return movie;
    }

    public void openIMDBURL() {
        movie.openIMDB();
    }

    public void paintItSeen(int cases) {
        switch (cases) {
            // 0: seen, 1: not seen 2: no user && no selected 3: no user && selected
            case 0:
                seenPanel.setBackground(Color.decode("#42f58a"));
                break;
            case 1:
                seenPanel.setBackground(Color.decode("#f54242"));
                break;
            case 2:
                seenPanel.setBackground(null);
                break;
            case 3:
                seenPanel.setBackground(Color.decode("#ffc080"));
                break;
        }
    }
}