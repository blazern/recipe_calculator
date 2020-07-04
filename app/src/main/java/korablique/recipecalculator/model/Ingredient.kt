package korablique.recipecalculator.model

import android.os.Parcel
import android.os.Parcelable
import korablique.recipecalculator.database.room.IngredientEntity
import korablique.recipecalculator.model.proto.FoodstuffProtos
import korablique.recipecalculator.model.proto.IngredientProtos
import korablique.recipecalculator.util.FloatUtils
import java.util.*

data class Ingredient(
        val id: Long,
        val foodstuff: Foodstuff,
        val weight: Float,
        val comment: String) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (other !is Ingredient) {
            return false
        }
        return id == other.id
                && foodstuff == other.foodstuff
                && FloatUtils.areFloatsEquals(weight, other.weight)
                && comment == other.comment
    }

    override fun hashCode(): Int {
        return Objects.hashCode(id)
    }

    companion object {
        fun from(entity: IngredientEntity, foodstuff: Foodstuff): Ingredient {
            return Ingredient(entity.id, foodstuff, entity.ingredientWeight, entity.comment)
        }

        @JvmStatic
        fun create(foodstuff: Foodstuff, weight: Float, comment: String): Ingredient {
            return Ingredient(-1, foodstuff, weight, comment)
        }

        @JvmStatic
        fun create(foodstuff: WeightedFoodstuff, comment: String): Ingredient {
            return Ingredient(-1, foodstuff.withoutWeight(), foodstuff.weight.toFloat(), comment)
        }

        @JvmField
        val CREATOR: Parcelable.Creator<Ingredient> = object : Parcelable.Creator<Ingredient> {
            override fun createFromParcel(input: Parcel): Ingredient {
                return Ingredient(
                        input.readLong(),
                        input.readParcelable(Foodstuff::class.java.classLoader)!!,
                        input.readFloat(),
                        input.readString()!!)
            }
            override fun newArray(size: Int): Array<Ingredient> {
                return arrayOf()
            }
        }

        @JvmStatic
        fun fromProto(protoIngredient: IngredientProtos.Ingredient): Ingredient {
            var id: Long = -1
            if (protoIngredient.hasLocalId()) {
                id = protoIngredient.localId
            }
            return Ingredient(
                    id,
                    Foodstuff.fromProto(protoIngredient.foodstuff),
                    protoIngredient.weight,
                    protoIngredient.comment)
        }
    }

    fun toWeightedFoodstuff(): WeightedFoodstuff {
        return WeightedFoodstuff(foodstuff, weight.toDouble())
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeParcelable(foodstuff, 0)
        dest.writeFloat(weight)
        dest.writeString(comment)
    }

    fun toProto(): IngredientProtos.Ingredient? {
        return IngredientProtos.Ingredient.newBuilder()
                .setLocalId(id)
                .setFoodstuff(foodstuff.toProto())
                .setWeight(weight)
                .setComment(comment)
                .build()
    }
}
