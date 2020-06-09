package korablique.recipecalculator.model;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import org.joda.time.LocalDate;

import java.util.Calendar;
import java.util.Objects;

import korablique.recipecalculator.util.FloatUtils;

public class UserParameters implements Parcelable {
    private final String name;
    private final float targetWeight;
    private final Gender gender;
    private final LocalDate dateOfBirth;
    private final int height;
    private final float weight;
    private final Lifestyle lifestyle;
    private final Formula formula;
    private final Nutrition rates;
    private final long measurementsTimestamp;

    public UserParameters(
            String name,
            float targetWeight,
            Gender gender,
            LocalDate dateOfBirth,
            int height,
            float weight,
            Lifestyle lifestyle,
            Formula formula,
            Nutrition rates,
            long measurementsTimestamp) {
        this.name = name;
        this.targetWeight = targetWeight;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.height = height;
        this.weight = weight;
        this.lifestyle = lifestyle;
        this.formula = formula;
        this.rates = rates;
        this.measurementsTimestamp = measurementsTimestamp;
    }

    protected UserParameters(Parcel in) {
        name = in.readString();
        targetWeight = in.readFloat();
        gender = (Gender) in.readSerializable();
        int day = in.readInt();
        int month = in.readInt();
        int year = in.readInt();
        dateOfBirth = new LocalDate(year, month, day);
        height = in.readInt();
        weight = in.readFloat();
        lifestyle = (Lifestyle) in.readSerializable();
        formula = (Formula) in.readSerializable();
        rates = (Nutrition) in.readSerializable();
        measurementsTimestamp = in.readLong();
    }

    public Goal getGoal() {
        if (targetWeight < weight) {
            return Goal.LOSING_WEIGHT;
        } else if (targetWeight == weight) {
            return Goal.MAINTAINING_CURRENT_WEIGHT;
        } else {
            return Goal.MASS_GATHERING;
        }
    }

    public String getName() {
        return name;
    }

    public float getTargetWeight() {
        return targetWeight;
    }

    public Gender getGender() {
        return gender;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public int getAge() {
        Calendar dateOfBirthCalendar = Calendar.getInstance();
        dateOfBirthCalendar.setTime(dateOfBirth.toDate());
        Calendar today = Calendar.getInstance();

        int age = today.get(Calendar.YEAR) - dateOfBirthCalendar.get(Calendar.YEAR);

        if (today.get(Calendar.DAY_OF_YEAR) < dateOfBirthCalendar.get(Calendar.DAY_OF_YEAR)){
            --age;
        }
        return age;
    }

    public int getHeight() {
        return height;
    }

    public float getWeight() {
        return weight;
    }

    public Lifestyle getLifestyle() {
        return lifestyle;
    }

    public Formula getFormula() {
        return formula;
    }

    public Nutrition getRates() {
        return rates;
    }

    public long getMeasurementsTimestamp() {
        return measurementsTimestamp;
    }

    public UserParameters recreateWithWeight(float newWeight) {
        return new UserParameters(
                name, targetWeight, gender, dateOfBirth, height, newWeight,
                lifestyle, formula, rates, measurementsTimestamp);
    }

    public UserParameters recreateWithMeasurementTime(long newTime) {
        return new UserParameters(
                name, targetWeight, gender, dateOfBirth, height, weight,
                lifestyle, formula, rates, newTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserParameters that = (UserParameters) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(dateOfBirth, that.dateOfBirth) &&
                height == that.height &&
                FloatUtils.areFloatsEquals(weight, that.weight) &&
                Objects.equals(lifestyle, that.lifestyle) &&
                FloatUtils.areFloatsEquals(targetWeight, that.targetWeight) &&
                Objects.equals(gender, that.gender) &&
                Objects.equals(formula, that.formula) &&
                Objects.equals(rates, that.rates) &&
                measurementsTimestamp == that.measurementsTimestamp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetWeight, gender, dateOfBirth, height, weight, lifestyle, formula, measurementsTimestamp);
    }

    public static final Creator<UserParameters> CREATOR = new Creator<UserParameters>() {
        @Override
        public UserParameters createFromParcel(Parcel in) {
            return new UserParameters(in);
        }

        @Override
        public UserParameters[] newArray(int size) {
            return new UserParameters[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(name);
        parcel.writeFloat(targetWeight);
        parcel.writeSerializable(gender);
        parcel.writeInt(dateOfBirth.getDayOfMonth());
        parcel.writeInt(dateOfBirth.getMonthOfYear());
        parcel.writeInt(dateOfBirth.getYear());
        parcel.writeInt(height);
        parcel.writeFloat(weight);
        parcel.writeSerializable(lifestyle);
        parcel.writeSerializable(formula);
        parcel.writeSerializable(rates);
        parcel.writeLong(measurementsTimestamp);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        return String.format("{name: %s, targetWeight: %f, gender: %s, birthday: %s, "
                + "height: %d, weight: %f, lifestyle: %s, formula: %s, rates: %s, measurementsTimestamp: %d}",
                name, targetWeight, gender.toString(), dateOfBirth.toString(), height, weight,
                lifestyle.toString(), formula.toString(), rates.toString(), measurementsTimestamp);
    }
}

