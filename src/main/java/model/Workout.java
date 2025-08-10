package model;

import java.util.List;

public class Workout {
    // ISO date: "YYYY-MM-DD"
    public String date;
    public String exercise;     // e.g., "Bench Press" or "Running"
    public List<SetEntry> sets; // strength sets (can be null/empty)
    public Integer duration;    // cardio minutes (optional)

    public Workout() {}
}
