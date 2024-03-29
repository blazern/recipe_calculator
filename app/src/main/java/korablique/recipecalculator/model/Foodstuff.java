package korablique.recipecalculator.model;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;

import korablique.recipecalculator.model.proto.FoodstuffProtos;
import korablique.recipecalculator.util.FloatUtils;

public class Foodstuff implements Parcelable, Comparable<Foodstuff> {
    public final static long INVALID_ID = -1;

    private final long id;
    private final String name;
    private final double protein;
    private final double fats;
    private final double carbs;
    private final double calories;

    Foodstuff(String name, double protein, double fats, double carbs, double calories) {
        this(INVALID_ID, name, protein, fats, carbs, calories);
    }

    Foodstuff(long id, String name, double protein, double fats, double carbs, double calories) {
        this.id = id;
        this.name = name;
        this.protein = protein;
        this.fats = fats;
        this.carbs = carbs;
        this.calories = calories;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(name);
        dest.writeDouble(protein);
        dest.writeDouble(fats);
        dest.writeDouble(carbs);
        dest.writeDouble(calories);
    }

    public static final Parcelable.Creator<Foodstuff> CREATOR
            = new Parcelable.Creator<Foodstuff>() {
        public Foodstuff createFromParcel(Parcel in) {
            return new Foodstuff(
                    in.readLong(),
                    in.readString(),
                    in.readDouble(),
                    in.readDouble(),
                    in.readDouble(),
                    in.readDouble());
        }

        public Foodstuff[] newArray(int size) {
            return new Foodstuff[size];
        }
    };

    @Override
    public int compareTo(@NonNull Foodstuff foodstuff) {
        return name.compareToIgnoreCase(foodstuff.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Foodstuff) {
            Foodstuff foodstuff = (Foodstuff) o;
            return id == foodstuff.id && name.equals(foodstuff.name) && haveSameNutrition(this, foodstuff);
        } else {
            // если передан аргумент неправильного типа, метод обязан возвращать false,
            // или если передан null (в этом случае данная проверка всё равно сработает)
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Long.valueOf(id).hashCode();
    }

    /**
     * @param lhs - left hand side
     * @param rhs - right hand side
     * @return true, if foodstuffs' nutritions are equal
     */
    public static boolean haveSameNutrition(Foodstuff lhs, Foodstuff rhs) {
        if (FloatUtils.areFloatsEquals(lhs.getProtein(), rhs.getProtein(), 0.00001f)
                && FloatUtils.areFloatsEquals(lhs.getFats(), rhs.getFats(), 0.00001f)
                && FloatUtils.areFloatsEquals(lhs.getCarbs(), rhs.getCarbs(), 0.00001f)
                && FloatUtils.areFloatsEquals(lhs.getCalories(), rhs.getCalories(), 0.00001f)) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean haveSameNutrition(WeightedFoodstuff lhs, WeightedFoodstuff rhs) {
        return haveSameNutrition(lhs.withoutWeight(), rhs.withoutWeight());
    }

    public static FoodstuffBuilderIDStep withId(long id) {
        return new FoodstuffBuilderIDStep(id);
    }

    public static FoodstuffBuilderNameStep withName(String name) {
        return new FoodstuffBuilderNameStep(name);
    }

    public WeightedFoodstuff withWeight(double weight) {
        return new WeightedFoodstuff(this, weight);
    }

    public Foodstuff recreateWithId(long id) {
        return Foodstuff.withId(id).withName(name).withNutrition(protein, fats, carbs, calories);
    }

    public Foodstuff recreateWithName(String name) {
        return Foodstuff.withId(id).withName(name).withNutrition(protein, fats, carbs, calories);
    }

    public Foodstuff recreateWithNutrition(Nutrition nutrition) {
        return Foodstuff.withId(id).withName(name).withNutrition(nutrition);
    }

    @Override
    public String toString() {
        return "Foodstuff{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", protein=" + protein +
                ", fats=" + fats +
                ", carbs=" + carbs +
                ", calories=" + calories +
                '}';
    }

    public boolean hasValidID() {
        return id != INVALID_ID;
    }

    public FoodstuffProtos.Foodstuff toProto() {
        return FoodstuffProtos.Foodstuff.newBuilder()
                .setLocalId(id)
                .setName(name)
                .setProtein((float) protein)
                .setFats((float) fats)
                .setCarbs((float) carbs)
                .setCalories((float) calories)
                .build();
    }

    public static Foodstuff fromProto(FoodstuffProtos.Foodstuff protoFoodstuff) {
        long id = INVALID_ID;
        if (protoFoodstuff.hasLocalId()) {
            id = protoFoodstuff.getLocalId();
        }
        return new Foodstuff(
                id,
                protoFoodstuff.getName(),
                protoFoodstuff.getProtein(),
                protoFoodstuff.getFats(),
                protoFoodstuff.getCarbs(),
                protoFoodstuff.getCalories());
    }
}
