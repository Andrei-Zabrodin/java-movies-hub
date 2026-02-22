package ru.practicum.moviehub.model;

public class Movie {
    private String title;
    private int year;
    private int id;

    public Movie(String title, int year, int id) {
        this.title = title;
        this.year = year;
        this.id = id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public int getYear() {
        return year;
    }
}