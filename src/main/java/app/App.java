package app;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import model.SetEntry;
import model.Workout;
import service.StatsService;
import store.FileStore;

import static spark.Spark.*;

public class App {
    public static void main(String[] args) {
        port(getHerokuAssignedPort()); // Dynamically pick the port from environment

        staticFiles.location("/public"); // src/main/resources/public

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        FileStore store = new FileStore("data/workouts.json");
        StatsService statsService = new StatsService();

        // Disable caching
        before((request, response) -> response.header("Cache-Control", "no-cache"));

        get("/api/workouts", (req, res) -> {
            res.type("application/json");
            return gson.toJson(store.getAll());
        });

        post("/api/workouts", (req, res) -> {
            res.type("application/json");
            Workout w = gson.fromJson(req.body(), Workout.class);
            String error = validate(w);
            if (error != null) {
                res.status(400);
                return "{\"error\":\"" + error + "\"}";
            }
            store.add(w);
            res.status(201);
            return "{\"ok\":true}";
        });

        get("/api/stats", (req, res) -> {
            res.type("application/json");
            return gson.toJson(statsService.compute(store.getAll()));
        });

        get("/api/export", (req, res) -> {
            res.type("text/csv; charset=utf-8");
            res.header("Content-Disposition", "attachment; filename=\"workouts.csv\"");
            StringBuilder sb = new StringBuilder("date,exercise,reps,weight,volume\n");
            for (Workout w : store.getAll()) {
                if (w.sets != null && !w.sets.isEmpty()) {
                    for (SetEntry s : w.sets) {
                        double vol = s.reps * s.weight;
                        sb.append(w.date).append(',')
                          .append(escape(w.exercise)).append(',')
                          .append(s.reps).append(',')
                          .append(s.weight).append(',')
                          .append(vol).append('\n');
                    }
                } else {
                    sb.append(w.date).append(',')
                      .append(escape(w.exercise)).append(',')
                      .append("").append(',')
                      .append("").append(',')
                      .append("").append('\n');
                }
            }
            return sb.toString();
        });
    }

    private static int getHerokuAssignedPort() {
        String port = System.getenv("PORT");
        return port != null ? Integer.parseInt(port) : 4567;
    }

    private static String validate(Workout w) {
        if (w == null) return "Body missing";
        if (w.date == null || w.date.isBlank()) return "date is required";
        if (w.exercise == null || w.exercise.isBlank()) return "exercise is required";
        boolean hasSets = w.sets != null && !w.sets.isEmpty();
        boolean hasDuration = (w.duration != null && w.duration > 0);
        if (!hasSets && !hasDuration) return "provide sets or duration";
        if (hasSets) {
            for (SetEntry s : w.sets) {
                if (s.reps <= 0) return "reps must be > 0";
                if (s.weight < 0) return "weight must be >= 0";
            }
        }
        return null;
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\"\"");
    }
}
