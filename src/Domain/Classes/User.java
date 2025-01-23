package Domain.Classes;

import DataLayer.UserGateway;

import java.util.ArrayList;
import java.util.List;

public class User {
    private final String name;
    private final List<Movie> seenMovies;
    private Movie lastMovieGenerated;
    public User(String name) {
        this.name = name;
        this.seenMovies = new ArrayList<>();
        this.lastMovieGenerated = null;
    }
    public User(UserGateway user) {
        this.name = user.getName();
        this.seenMovies = user.getSeenMovies();
        this.lastMovieGenerated = user.getLastMovieGenerated();
    }
    public void setLastMovie(Movie currentMovie) {
        this.lastMovieGenerated = currentMovie;
    }
    public void addMovie(Movie movie) {
        if (!seenMovies.contains(movie)) {
            seenMovies.add(movie);
        }
    }
    public String getName() {
        return name;
    }
    public List<Movie> getSeenMovies() {
        return seenMovies;
    }
    public Movie getLastMovieGenerated() {
        return lastMovieGenerated;
    }

    public boolean LastMovieGeneratedAlreadySeen() {
        return seenMovies.contains(lastMovieGenerated);
    }

    public void deleteMovie(Movie movie) {
        seenMovies.remove(movie);
    }

    public boolean hasSeen(Movie movie) {
        return seenMovies.contains(movie);
    }
}
