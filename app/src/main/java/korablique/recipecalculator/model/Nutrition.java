package korablique.recipecalculator.model;

import android.annotation.SuppressLint;

import java.io.Serializable;
import java.util.Objects;

import korablique.recipecalculator.util.FloatUtils;

public class Nutrition implements Serializable {
    private final double protein;
    private final double fats;
    private final double carbs;
    private final double calories;

    public static Nutrition of(WeightedFoodstuff foodstuff) {
        return new Nutrition(
                foodstuff.getProtein() * foodstuff.getWeight() / 100,
                foodstuff.getFats() * foodstuff.getWeight() / 100,
                foodstuff.getCarbs() * foodstuff.getWeight() / 100,
                foodstuff.getCalories() * foodstuff.getWeight() / 100);
    }

    public static Nutrition of100gramsOf(Foodstuff foodstuff) {
        return new Nutrition(
                foodstuff.getProtein(),
                foodstuff.getFats(),
                foodstuff.getCarbs(),
                foodstuff.getCalories());
    }

    public static Nutrition from(Nutrition rates) {
        return new Nutrition(rates.getProtein(), rates.getFats(), rates.getCarbs(), rates.getCalories());
    }

    public static Nutrition zero() {
        return new Nutrition(0, 0, 0, 0);
    }

    public Nutrition plus(Nutrition nutrition) {
        return new Nutrition(
                this.protein + nutrition.protein,
                this.fats + nutrition.fats,
                this.carbs + nutrition.carbs,
                this.calories + nutrition.calories);
    }

    public Nutrition minus(Nutrition nutrition) {
        return new Nutrition(
                this.protein - nutrition.protein,
                this.fats - nutrition.fats,
                this.carbs - nutrition.carbs,
                this.calories - nutrition.calories);
    }

    public Nutrition multiply(double factor) {
        return new Nutrition(
                this.protein * factor,
                this.fats * factor,
                this.carbs * factor,
                this.calories * factor);
    }

    public static Nutrition withValues(double protein, double fats, double carbs, double calories) {
        return new Nutrition(protein, fats, carbs, calories);
    }

    private Nutrition(double protein, double fats, double carbs, double calories) {
        this.protein = protein;
        this.fats = fats;
        this.carbs = carbs;
        this.calories = calories;
    }

    public double getProtein() {
        return protein;
    }

    public double getFats() {
        return fats;
    }

    public double getCarbs() {
        return carbs;
    }

    public double getCalories() {
        return calories;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        return String.format("{protein: %f, fats: %f, carbs: %f, calories: %f}",
                protein, fats, carbs, calories);
    }

    @Override
    public boolean equals(Object otherObject) {
        if (!(otherObject instanceof Nutrition)) {
            return false;
        }
        Nutrition other = (Nutrition) otherObject;
        return FloatUtils.areFloatsEquals(protein, other.protein, 0.00001f)
                && FloatUtils.areFloatsEquals(fats, other.fats, 0.00001f)
                && FloatUtils.areFloatsEquals(carbs, other.carbs, 0.00001f)
                && FloatUtils.areFloatsEquals(calories, other.calories, 0.00001f);
    }

    @Override
    public int hashCode() {
        return Objects.hash(protein, fats, carbs, calories);
    }
}
