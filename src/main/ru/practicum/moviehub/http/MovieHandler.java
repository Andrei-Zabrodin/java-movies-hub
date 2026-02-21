package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.util.Optional;

public class MovieHandler extends BaseHttpHandler {

    private final MoviesStore moviesStore;
    private final Gson gson = new Gson();

    public MovieHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }

    @Override
    public void handle(HttpExchange ex) {
        try {
            Optional<Integer> optId = getRequestedId(ex);

            if (hasErrors(ex, optId)) {
                return;
            }

            int requestedId = optId.get() - 1;
            switch (ex.getRequestMethod().toUpperCase()) {
                case "GET":
                    handleGetMovieById(ex, requestedId);
                    break;
                case "DELETE":
                    handleDeleteMovie(ex, requestedId);
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

    private Optional<Integer> getRequestedId(HttpExchange ex) {
        String[] pathParts = ex.getRequestURI().getPath().split("/");

        try {
            if (pathParts.length == 3) {
                return Optional.of(Integer.parseInt(pathParts[2]));
            }

            return Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private boolean hasErrors(HttpExchange ex, Optional<Integer> optId) throws IOException {
        ErrorResponse er;

        if (optId.isEmpty()) {
            er = new ErrorResponse("Ошибка запроса", "ID фильма должен быть числом");
            sendJson(ex, 400, gson.toJson(er));
            return true;
        }

        int requestedId = optId.get() - 1;

        if (requestedId >= moviesStore.getMovies().size() || moviesStore.getMovies().get(requestedId) == null) {
            er = new ErrorResponse("Ошибка запроса", "Фильма с таким ID нет в списке");
            sendJson(ex, 404, gson.toJson(er));
            return true;
        }

        return false;
    }

    private void handleGetMovieById(HttpExchange ex, int requestedId) throws IOException {
        String json = gson.toJson(moviesStore.getMovies().get(requestedId));
        sendJson(ex, 200, json);
    }

    private void handleDeleteMovie(HttpExchange ex, int requestedId) throws IOException {
        moviesStore.deleteMovie(requestedId);
        sendNoContent(ex, 204);
    }
}
