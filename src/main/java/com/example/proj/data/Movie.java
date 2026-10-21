package com.example.proj.data;

public class Movie {
    private int id;
    private String title;
    private int duration;
    private String description;
    private String posterPath;
    private double rating;

    public Movie(String title, int duration, String description, String posterPath, double rating) {
        this.title = title;
        this.duration = duration;
        this.description = description;
        this.posterPath = posterPath;
        this.rating = rating;
    }

    public String getTitle(){
        return title;
    }
    public int getDuration(){
        return duration;
    }
    public String getDescription(){
        return description;
    }
    public String getPosterPath(){
        return posterPath;
    }
    public double getRating(){
        return rating;
    }
}
