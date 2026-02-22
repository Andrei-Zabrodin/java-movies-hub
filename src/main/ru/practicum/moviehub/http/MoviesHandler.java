package ru.practicum.moviehub.http;

import com.google.gson.Gson;

import com.sun.net.httpserver.HttpExchange;

import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class MoviesHandler extends BaseHttpHandler {
    private static final int MAX_MOVIE_YEAR = LocalDate.now().getYear() + 1;
    private static final int MIN_MOVIE_YEAR = 1888;
    private static final int MAX_MOVIE_TITLE = 100;

    private final MoviesStore moviesStore;
    private final Gson gson = new Gson();

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }

    @Override
    public void handle(HttpExchange ex) {
        try {
            switch (ex.getRequestMethod().toUpperCase()) {
                case "GET":
                    if (ex.getRequestURI().getQuery() != null && ex.getRequestURI().getQuery().contains("year=")) {
                        handleGetMoviesListByYear(ex);
                    } else {
                        handleGetMoviesList(ex);
                    }

                    break;
                case "POST":
                    handlePostMovie(ex);
                    break;
                default:
                    ErrorResponse er = new ErrorResponse("Ошибка запроса",
                            "Данного эндпойнта не существует");
                    sendJson(ex, 405, gson.toJson(er));
            }
        } catch (IOException e) {
            throw new RuntimeException("Не удалось отправить ответ", e);
        }
    }

    private void handleGetMoviesList(HttpExchange ex) throws IOException {
        String response = gson.toJson(moviesStore.getMovies());

        sendJson(ex, 200, response);
    }

    private void handleGetMoviesListByYear(HttpExchange ex) throws IOException {
        Optional<Integer> optYear = getYearFromQuery(ex);
        String response;
        int code;

        if (optYear.isEmpty() || !isCorrectYear(optYear.get())) {
            ErrorResponse er = new ErrorResponse("Ошибка запроса",
                    "Некорректный параметр запроса — year");
            response = gson.toJson(er);
            code = 400;
        } else {
            int year = optYear.get();

            List<Movie> filteredByYear = moviesStore.getMovies().stream()
                    .filter(movie -> movie.getYear() == year)
                    .toList();

            response = gson.toJson(filteredByYear);
            code = 200;
        }

        sendJson(ex, code, response);
    }

    private void handlePostMovie(HttpExchange ex) throws IOException {
        Optional<Movie> optMovie = getMovieFromRequest(ex);

        if (!validateMovie(ex, optMovie)) {
            return;
        }

        Movie movie = optMovie.get();

        moviesStore.addMovie(movie);

        sendJson(ex, 201, gson.toJson(movie));
    }

    private Optional<Movie> getMovieFromRequest(HttpExchange ex) {
        try (InputStream is = ex.getRequestBody()) {
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            Movie movie = gson.fromJson(body, Movie.class);

            if (movie.getTitle() == null || movie.getYear() == 0) {
                return Optional.empty();
            }

            return Optional.of(movie);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private boolean validateMovie(HttpExchange ex, Optional<Movie> optMovie) throws IOException {
        String error = "Ошибка валидации";
        String details = "";
        ErrorResponse errorResponse;

        if (!ex.getRequestHeaders().containsKey("Content-Type") ||
                !ex.getRequestHeaders().getFirst("Content-Type").equals("application/json; charset=UTF-8")) {
            details = "Некорректное содержимое заголовка Content-Type";
            errorResponse = new ErrorResponse(error, details);
            sendJson(ex, 415, gson.toJson(errorResponse));
            return false;
        }

        if (optMovie.isEmpty()) {
            details = "Передано некорректное тело запроса";
            errorResponse = new ErrorResponse(error, details);
            sendJson(ex, 422, gson.toJson(errorResponse));
            return false;
        }

        Movie movie = optMovie.get();

        if (movie.getTitle().isEmpty()) {
            details = "Название фильма не должно быть пустым";
        } else if (movie.getTitle().length() > MAX_MOVIE_TITLE) {
            details = "Название фильма не должно превышать 100 символов";
        } else if (!isCorrectYear(movie.getYear())) {
            details = String.format("Год фильма должен быть в диапазоне от %d до %d", MIN_MOVIE_YEAR, MAX_MOVIE_YEAR);
        }

        if (!details.isBlank()) {
            errorResponse = new ErrorResponse(error, details);
            sendJson(ex, 422, gson.toJson(errorResponse));

            return false;
        } else {
            return true;
        }
    }

    private boolean isCorrectYear(int year) {
        return year >= MIN_MOVIE_YEAR && year <= MAX_MOVIE_YEAR;
    }

    private Optional<Integer> getYearFromQuery(HttpExchange ex) throws IOException {
        try {
            String query = ex.getRequestURI().getQuery();

            if (query.contains("&")) {
                query = query.substring(query.indexOf("year"), query.indexOf("&"));
            }

            String year = query.substring(query.indexOf("=") + 1);

            return Optional.of(Integer.parseInt(year));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
