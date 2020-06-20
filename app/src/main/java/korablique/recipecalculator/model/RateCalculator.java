package korablique.recipecalculator.model;

public class RateCalculator {

    private RateCalculator() {}

    /**
     * @param formula must not be Formula.MANUAL
     */
    public static Nutrition calculate(
            float targetWeight,
            Gender gender,
            int age,
            int height,
            float currentWeight,
            Lifestyle lifestyle,
            Formula formula) {
        if (formula == Formula.MANUAL) {
            throw new IllegalArgumentException("Cannot calculate manual rates!");
        }
        // 1) рассчитываем базальный метаболизм
        float basalMetabolism;
        // формула Харриса-Бенедикта
        if (formula == Formula.HARRIS_BENEDICT) {
            if (gender == Gender.MALE) {
                // Men: 88,362 + (13,397 × вес [кг]) + (4,799 × рост [см]) − (5,677 × возраст [лет]).
                basalMetabolism = 88.362f + 13.397f * currentWeight + 4.799f * height - 5.677f * age;
            } else {
                // Women: 447,593 + (9,247 × вес [кг]) + (3,098 × рост [см]) − (4,33 × возраст [лет]).
                basalMetabolism = 447.593f + 9.247f * currentWeight + 3.098f * height - 4.33f * age;
            }
        } else {
            // формула Миффлина-Джеора
            if (gender == Gender.MALE) {
                // Men: 5 + (10 × вес [кг]) + (6,25 × рост [см]) − (5 × возраст [лет]).
                basalMetabolism = 5 + 10 * currentWeight + 6.25f * height - 5 * age;
            } else {
                // Women: (10 × вес [кг]) + (6,25 × рост [см]) − (5 × возраст [лет]) − 161.
                basalMetabolism = 10 * currentWeight + 6.25f * height - 5 * age - 161;
            }
        }

        // 2) корректируем калорийность в зависимости от активности (умножаем на коэф. активности)
        // (это поддержка)
        float coefficient = lifestyle.getPhysActivityCoefficient();
        float calories = basalMetabolism * coefficient;

        float caloriesDependingOnGoal;
        // 3) корректируем калорийность в зависимости от целей
        Goal goal = Goal.fromTargetWeight(targetWeight, currentWeight);
        if (goal == Goal.LOSING_WEIGHT) {
            // если цель похудеть - берём 10% дефицит
            caloriesDependingOnGoal = calories * 0.9f;
        } else if (goal == Goal.MAINTAINING_CURRENT_WEIGHT) {
            caloriesDependingOnGoal = calories;
        } else {
            // если массонабор - 10% профицит
            caloriesDependingOnGoal = calories * 1.1f;
        }

        // 4) рассчитываем БЖУ
        // пока что будут белки 2 г/кг, жиры 1 г/кг
        float protein = currentWeight * 2;
        float fats = currentWeight;
        float carbs = (caloriesDependingOnGoal - (protein * 4 + fats * 9)) / 4;
        return Nutrition.withValues(protein, fats, carbs, caloriesDependingOnGoal);
    }

    public static Nutrition recalculate(
            Nutrition oldRates, Nutrition newRates, Nutrient changedNutrient) {
        // https://trello.com/c/5s81c6ir
        if (changedNutrient == Nutrient.CALORIES) {
            double proteinFactor = oldRates.getProtein() / oldRates.getCalories();
            double fatsFactor = oldRates.getFats() / oldRates.getCalories();
            double carbsFactor = oldRates.getCarbs() / oldRates.getCalories();
            double newProtein = newRates.getCalories() * proteinFactor;
            double newFats = newRates.getCalories() * fatsFactor;
            double newCarbs = newRates.getCalories() * carbsFactor;
            return Nutrition.withValues(
                    newProtein, newFats, newCarbs, newRates.getCalories());
        } else {
            return Nutrition.withValues(
                    newRates.getProtein(),
                    newRates.getFats(),
                    newRates.getCarbs(),
                    newRates.getProtein() * 4 + newRates.getFats() * 9 + newRates.getCarbs() * 4);
        }
    }
}
