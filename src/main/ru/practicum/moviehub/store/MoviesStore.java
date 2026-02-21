package ru.practicum.moviehub.store;

import java.util.ArrayList;
import java.util.List;

import ru.practicum.moviehub.model.Movie;

public class MoviesStore {
    private final List<Movie> movies = new ArrayList<>();

    public void addMovie(Movie movie) {
        movies.add(movie);
    }

    public int getNextId() {
        return movies.size() + 1;
    }

    public void cleanStore() {
        movies.clear();
    }

    public List<Movie> getMovies() {
        return movies;
    }

    public void deleteMovie(int id) {
        movies.set(id, null);
    }
}