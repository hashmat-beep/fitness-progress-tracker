package service;

import model.SetEntry;
import model.Workout;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class StatsService {

    public static class Daily {
        public String date;
        public double volume;
        public Daily(String date, double volume) {
            this.date = date;
            this.volume = volume;
        }
    }

    public static class StatsResult {
        public double weeklyVolume;
        public double monthlyVolume;
        public Map<String, Double> bestOneRm = new HashMap<>();
        public List<Daily> dailyVolumes = new ArrayList<>();
    }

    public StatsResult compute(List<Workout> workouts) {
        StatsResult res = new StatsResult();
        Map<String, Double> dateToVolume = new HashMap<>();
        Map<String, Double> best1RM = new HashMap<>();

        for (Workout w : workouts) {
            if (w.sets != null && !w.sets.isEmpty()) {
                double wVol = 0.0;
                for (SetEntry s : w.sets) {
                    double vol = s.reps * s.weight;
                    wVol += vol;
                    double oneRm = epleyOneRm(s.weight, s.reps);
                    best1RM.merge(w.exercise, oneRm, Math::max);
                }
                dateToVolume.merge(w.date, wVol, Double::sum);
            }
            // cardio (duration) is ignored for volume/1RM
        }

        // Build daily series sorted by date
        List<String> dates = new ArrayList<>(dateToVolume.keySet());
        Collections.sort(dates);
        for (String d : dates) {
            res.dailyVolumes.add(new Daily(d, round1(dateToVolume.get(d))));
        }

        // Rolling sums
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(6);
        LocalDate monthAgo = today.minusDays(29);

        for (Map.Entry<String, Double> e : dateToVolume.entrySet()) {
            LocalDate d = LocalDate.parse(e.getKey());
            if (!d.isBefore(weekAgo) && !d.isAfter(today)) {
                res.weeklyVolume += e.getValue();
            }
            if (!d.isBefore(monthAgo) && !d.isAfter(today)) {
                res.monthlyVolume += e.getValue();
            }
        }

        // Best 1RM per exercise
        for (Map.Entry<String, Double> e : best1RM.entrySet()) {
            res.bestOneRm.put(e.getKey(), round1(e.getValue()));
        }

        res.weeklyVolume = round1(res.weeklyVolume);
        res.monthlyVolume = round1(res.monthlyVolume);
        return res;
    }

    private double epleyOneRm(double weight, int reps) {
        return weight * (1.0 + reps / 30.0);
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
