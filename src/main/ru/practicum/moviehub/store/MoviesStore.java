package ru.practicum.moviehub.store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.practicum.moviehub.model.Movie;

public class MoviesStore {
    private final Map<Integer, Movie> movies = new HashMap<>();
    private int id;

    public MoviesStore() {
        id = 0;
    }

    public void addMovie(Movie movie) {
        id++;
        movie.setId(id);
        movies.put(id, movie);
    }

    public void cleanStore() {
        id = 0;
        movies.clear();
    }

    public List<Movie> getMovies() {
        return new ArrayList<>(movies.values());
    }

    public Movie getMovieById(int id) {
        return movies.get(id);
    }

    public void deleteMovie(int id) {
        movies.remove(id);
    }
}