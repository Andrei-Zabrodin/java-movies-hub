package ru.practicum.moviehub.http;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {

    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;
    private static MoviesStore store;

    @BeforeAll
    static void beforeAll() {
        store = new MoviesStore();
        server = new MoviesServer(store, 8080);
        server.start();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @BeforeEach
    void beforeEach() {
        store.cleanStore();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        //создаём запрос
        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // проверяем корректность кода ответа
        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        // проверяем наличие требуемых заголовков
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
        "Content-Type должен содержать формат данных и кодировку");

        // проверяем, что вернулся массив
        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");

    }

    @Test
    void getMovies_whenNotEmpty_returnsCorrectArray() throws Exception {
        // добавляем фильмы
        store.addMovie(new Movie("Титаник", 1997, 1));
        store.addMovie(new Movie("Бешеные псы", 1992, 2));

        String expectedJson = "[" +
                "{\"title\":\"Титаник\",\"year\":1997,\"id\":1}," +
                "{\"title\":\"Бешеные псы\",\"year\":1992,\"id\":2}" +
                "]";

        // создаём запрос
        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // проверяем корректность кода ответа
        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        // проверяем наличие требуемых заголовков
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        // проверяем, что вернулся нужный массив
        String body = resp.body().trim();
        assertEquals(expectedJson, body,"Возвращаемый массив не соответствует ожидаемому");
    }

    @Test
    void postMovies_whenInputIsCorrect_addsMovie() throws Exception {
        // создаём запрос
        String movieJson = "{\"title\":\"Бешеные псы\",\"year\":1992}";
        HttpRequest req = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(movieJson, StandardCharsets.UTF_8))
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // проверяем корректность кода ответа
        assertEquals(201, resp.statusCode(), "POST /movies должен вернуть 201");

        // проверяем наличие требуемых заголовков
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        // проверяем, что вернулся нужный json
        String expectedJson = "{\"title\":\"Бешеные псы\",\"year\":1992,\"id\":1}";
        String body = resp.body().trim();
        assertEquals(expectedJson, body,"Возвращаемый массив не соответствует ожидаемому");

    }

    @Test
    void postMovies_whenEmptyTitle_getsError() throws Exception {
        // создаём запрос
        String movieJson = "{\"title\":\"\",\"year\":1992}";
        HttpRequest req = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(movieJson, StandardCharsets.UTF_8))
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // проверяем корректность кода ответа
        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");


        // проверяем, что вернулся нужный json
        String expectedJson = "{\"error\":\"Ошибка валидации\",\"details\":\"Название фильма не должно быть пустым\"}";
        String body = resp.body().trim();
        assertEquals(expectedJson, body,"Возвращаемая строка не соответствует ожидаемой");
    }

    @Test
    void postMovies_whenLongTitle_getsError() throws Exception {
        // создаём запрос
        String longTitle = "Очень длинное название фильма, настолько длинное," +
                " что очевидно не уместится в лимит символов, который заложен в программу";
        String movieJson = "{\"title\":\"" + longTitle + "\",\"year\":1992}";
        HttpRequest req = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(movieJson, StandardCharsets.UTF_8))
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // проверяем корректность кода ответа
        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");


        // проверяем, что вернулся нужный json
        String expectedJson = "{\"error\":\"Ошибка валидации\"," +
                "\"details\":\"Название фильма не должно превышать 100 символов\"}";
        String body = resp.body().trim();
        assertEquals(expectedJson, body,"Возвращаемая строка не соответствует ожидаемой");
    }

    @Test
    void postMovies_whenWrongYearLessThanMin_getsError() throws Exception {
        // создаём запрос
        String movieJson = "{\"title\":\"Бешеные псы\",\"year\":1172}";
        HttpRequest req = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(movieJson, StandardCharsets.UTF_8))
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // проверяем корректность кода ответа
        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");


        // проверяем, что вернулся нужный json
        String expectedJson = "{\"error\":\"Ошибка валидации\"," +
                "\"details\":\"Год фильма должен быть в диапазоне от 1888 до 2027\"}";
        String body = resp.body().trim();
        assertEquals(expectedJson, body,"Возвращаемая строка не соответствует ожидаемой");
    }

    @Test
    void postMovies_whenWrongYearMoreThanMax_getsError() throws Exception {
        // создаём запрос
        String movieJson = "{\"title\":\"Бешеные псы\",\"year\":2029}";
        HttpRequest req = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(movieJson, StandardCharsets.UTF_8))
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // проверяем корректность кода ответа
        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");


        // проверяем, что вернулся нужный json
        String expectedJson = "{\"error\":\"Ошибка валидации\"," +
                "\"details\":\"Год фильма должен быть в диапазоне от 1888 до 2027\"}";
        String body = resp.body().trim();
        assertEquals(expectedJson, body,"Возвращаемая строка не соответствует ожидаемой");
    }

    @Test
    void postMovies_whenWrongContentType_getsError() throws Exception {
        // создаём запрос
        String movieJson = "{\"title\":\"Бешеные псы\",\"year\":1992}";
        HttpRequest req = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(movieJson, StandardCharsets.UTF_8))
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "text/html")
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // проверяем корректность кода ответа
        assertEquals(415, resp.statusCode(), "POST /movies должен вернуть 415");

        // проверяем, что вернулся нужный json
        String expectedJson = "{\"error\":\"Ошибка валидации\"," +
                "\"details\":\"Некорректное содержимое заголовка Content-Type\"}";
        String body = resp.body().trim();
        assertEquals(expectedJson, body,"Возвращаемая строка не соответствует ожидаемой");
    }

    @Test
    void postMovies_whenWrongJson_getsError() throws Exception {
        // создаём запрос
        String movieJson = "{\"name\":\"Бешеные псы\",\"year\":1992}";
        HttpRequest req = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(movieJson, StandardCharsets.UTF_8))
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // проверяем корректность кода ответа
        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        // проверяем, что вернулся нужный json
        String expectedJson = "{\"error\":\"Ошибка валидации\"," +
                "\"details\":\"Передано некорректное тело запроса\"}";
        String body = resp.body().trim();
        assertEquals(expectedJson, body,"Возвращаемая строка не соответствует ожидаемой");
    }

    @Test
    void getMovieById_whenCorrect_returnsMovie() throws Exception {
        // добавляем фильмы
        store.addMovie(new Movie("Титаник", 1997, 1));
        store.addMovie(new Movie("Бешеные псы", 1992, 2));

        String expectedJson = "{\"title\":\"Бешеные псы\",\"year\":1992,\"id\":2}";

        // создаём запрос
        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE + "/movies/2"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // проверяем корректность кода ответа
        assertEquals(200, resp.statusCode(), "GET /movies/{id}  должен вернуть 200");

        // проверяем наличие требуемых заголовков
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        // проверяем, что вернулся нужный массив
        String body = resp.body().trim();
        assertEquals(expectedJson, body,"Возвращаемый строка не соответствует ожидаемой");
    }

    @Test
    void getMovieById_whenNotFound_returnsError() throws Exception {
        // добавляем фильмы
        store.addMovie(new Movie("Титаник", 1997, 1));
        store.addMovie(new Movie("Бешеные псы", 1992, 2));

        String expectedJson = "{\"error\":\"Ошибка запроса\",\"details\":\"Фильма с таким ID нет в списке\"}";

        // создаём запрос
        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE + "/movies/143"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // проверяем корректность кода ответа
        assertEquals(404, resp.statusCode(), "GET /movies/{id} должен вернуть 404");

        // проверяем, что вернулся нужный массив
        String body = resp.body().trim();
        assertEquals(expectedJson, body,"Возвращаемая строка не соответствует ожидаемой");
    }

    @Test
    void getMovieById_whenIDIsNotNumber_returnsError() throws Exception {
        // добавляем фильмы
        store.addMovie(new Movie("Титаник", 1997, 1));
        store.addMovie(new Movie("Бешеные псы", 1992, 2));

        String expectedJson = "{\"error\":\"Ошибка запроса\",\"details\":\"ID фильма должен быть числом\"}";

        // создаём запрос
        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE + "/movies/8saj82"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // проверяем корректность кода ответа
        assertEquals(400, resp.statusCode(), "GET /movies/{id} должен вернуть 400");

        // проверяем, что вернулся нужный массив
        String body = resp.body().trim();
        assertEquals(expectedJson, body,"Возвращаемая строка не соответствует ожидаемой");
    }

    @Test
    void deleteMovie_whenCorrect_returnsCode() throws Exception {
        // добавляем фильмы
        store.addMovie(new Movie("Титаник", 1997, 1));
        store.addMovie(new Movie("Бешеные псы", 1992, 2));
        store.addMovie(new Movie("Шестое чувство", 1999, 3));

        int targetIndex = 2; //индекс, по которому удаляем объект

        // создаём запрос
        HttpRequest req = HttpRequest.newBuilder()
                .DELETE()
                .uri(URI.create(BASE + "/movies/" + targetIndex))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // проверяем корректность кода ответа
        assertEquals(204, resp.statusCode(), "DELETE /movies/{id}  должен вернуть 204");

        // проверяем наличие требуемых заголовков
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        // проверяем, что фильм действительно удалён
        assertNull(store.getMovies().get(targetIndex - 1), "Ссылка должна быть null");
    }

    @Test
    void deleteMovie_whenNotFound_returnsError() throws Exception {
        // добавляем фильмы
        store.addMovie(new Movie("Титаник", 1997, 1));
        store.addMovie(new Movie("Бешеные псы", 1992, 2));

        String expectedJson = "{\"error\":\"Ошибка запроса\",\"details\":\"Фильма с таким ID нет в списке\"}";

        // создаём запрос
        HttpRequest req = HttpRequest.newBuilder()
                .DELETE()
                .uri(URI.create(BASE + "/movies/143"))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // проверяем корректность кода ответа
        assertEquals(404, resp.statusCode(), "DELETE /movies/{id} должен вернуть 404");

        // проверяем, что вернулся нужный массив
        String body = resp.body().trim();
        assertEquals(expectedJson, body,"Возвращаемая строка не соответствует ожидаемой");
    }

    @Test
    void deleteMovie_whenIDIsNotNumber_returnsError() throws Exception {
        // добавляем фильмы
        store.addMovie(new Movie("Титаник", 1997, 1));
        store.addMovie(new Movie("Бешеные псы", 1992, 2));

        String expectedJson = "{\"error\":\"Ошибка запроса\",\"details\":\"ID фильма должен быть числом\"}";

        // создаём запрос
        HttpRequest req = HttpRequest.newBuilder()
                .DELETE()
                .uri(URI.create(BASE + "/movies/8saj82"))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // проверяем корректность кода ответа
        assertEquals(400, resp.statusCode(), "DELETE /movies/{id} должен вернуть 400");

        // проверяем, что вернулся нужный массив
        String body = resp.body().trim();
        assertEquals(expectedJson, body,"Возвращаемая строка не соответствует ожидаемой");
    }

    @Test
    void getMovieByYear_whenYearCorrect_returnsList() throws Exception {
        // добавляем фильмы
        store.addMovie(new Movie("Титаник", 1997, 1));
        store.addMovie(new Movie("Бешеные псы", 1992, 2));
        store.addMovie(new Movie("Гаттака", 1997, 3));
        store.addMovie(new Movie("Достучаться до небес", 1997, 4));
        store.addMovie(new Movie("Майор Пэйн", 1995, 5));

        String expectedJson = "[" +
                "{\"title\":\"Титаник\",\"year\":1997,\"id\":1}," +
                "{\"title\":\"Гаттака\",\"year\":1997,\"id\":3}," +
                "{\"title\":\"Достучаться до небес\",\"year\":1997,\"id\":4}" +
                "]";

        // создаём запрос
        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE + "/movies?year=1997"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // проверяем корректность кода ответа
        assertEquals(200, resp.statusCode(), "GET /movies?year={year} должен вернуть 200");

        // проверяем наличие требуемых заголовков
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        // проверяем, что вернулся нужный массив
        String body = resp.body().trim();
        assertEquals(expectedJson, body,"Возвращаемая строка не соответствует ожидаемой");
    }

    @Test
    void getMovieByYear_whenNoMovies_returnsEmptyList() throws Exception {
        // добавляем фильмы
        store.addMovie(new Movie("Титаник", 1997, 1));
        store.addMovie(new Movie("Бешеные псы", 1992, 2));
        store.addMovie(new Movie("Гаттака", 1997, 3));
        store.addMovie(new Movie("Достучаться до небес", 1997, 4));
        store.addMovie(new Movie("Майор Пэйн", 1995, 5));

        String expectedJson = "[]";

        // создаём запрос
        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE + "/movies?year=2020"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // проверяем корректность кода ответа
        assertEquals(200, resp.statusCode(), "GET /movies?year={year} должен вернуть 200");

        // проверяем наличие требуемых заголовков
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        // проверяем, что вернулся нужный массив
        String body = resp.body().trim();
        assertEquals(expectedJson, body,"Возвращаемая строка не соответствует ожидаемой");
    }

    @Test
    void getMovieByYear_whenIncorrectYear_returnsError() throws Exception {
        // добавляем фильмы
        store.addMovie(new Movie("Титаник", 1997, 1));
        store.addMovie(new Movie("Бешеные псы", 1992, 2));
        store.addMovie(new Movie("Гаттака", 1997, 3));
        store.addMovie(new Movie("Достучаться до небес", 1997, 4));
        store.addMovie(new Movie("Майор Пэйн", 1995, 5));

        String expectedJson = "{\"error\":\"Ошибка запроса\",\"details\":\"Некорректный параметр запроса — year\"}";

        // создаём запрос
        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE + "/movies?year=f32fa"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // проверяем корректность кода ответа
        assertEquals(400, resp.statusCode(), "GET /movies?year={year} должен вернуть 400");

        // проверяем, что вернулся нужный массив
        String body = resp.body().trim();
        assertEquals(expectedJson, body,"Возвращаемая строка не соответствует ожидаемой");
    }
}