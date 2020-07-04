package korablique.recipecalculator.model

import android.os.Parcel
import android.os.Parcelable
import com.google.android.gms.common.internal.Objects
import korablique.recipecalculator.DishNutritionCalculator
import korablique.recipecalculator.database.room.RecipeEntity
import korablique.recipecalculator.model.proto.RecipeProtos
import korablique.recipecalculator.util.FloatUtils

// Please edit BucketList.isEmpty if any field is added
data class Recipe(
        val id: Long,
        val foodstuff: Foodstuff,
        val ingredients: List<Ingredient>,
        val weight: Float,
        val comment: String) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (other !is Recipe) {
            return false
        }
        return id == other.id
                && foodstuff == other.foodstuff
                && ingredients == other.ingredients
                && FloatUtils.areFloatsEquals(weight, other.weight)
                && comment == other.comment
    }

    override fun hashCode(): Int {
        return Objects.hashCode(id)
    }

    companion object {
        fun from(
                entity: RecipeEntity,
                foodstuff: Foodstuff,
                ingredients: List<Ingredient>): Recipe {
            return Recipe(entity.id, foodstuff, ingredients, entity.ingredientsTotalWeight, entity.comment)
        }

        @JvmStatic
        fun create(foodstuff: Foodstuff,
                   ingredients: List<Ingredient>,
                   weight: Float,
                   comment: String): Recipe {
            return Recipe(-1, foodstuff, ingredients, weight, comment)
        }

        @JvmField
        val CREATOR: Parcelable.Creator<Recipe> = object : Parcelable.Creator<Recipe> {
            override fun createFromParcel(input: Parcel): Recipe {
                return Recipe(
                        input.readLong(),
                        input.readParcelable(Foodstuff::class.java.classLoader)!!,
                        readIngredients(input),
                        input.readFloat(),
                        input.readString()!!)
            }
            override fun newArray(size: Int): Array<Recipe> {
                return arrayOf()
            }
        }

        @JvmStatic
        fun fromProto(protoRecipe: RecipeProtos.Recipe): Recipe {
            var id: Long = -1
            if (protoRecipe.hasLocalId()) {
                id = protoRecipe.localId
            }
            return Recipe(
                    id,
                    Foodstuff.fromProto(protoRecipe.foodstuff),
                    protoRecipe.ingredientsList.map { Ingredient.fromProto(it) },
                    protoRecipe.weight,
                    protoRecipe.comment)
        }
    }

    val name: String get() = foodstuff.name
    val isFromDB: Boolean get() = id > 0

    fun recalculateNutrition(): Recipe {
        var nutrition = DishNutritionCalculator.calculateIngredients(ingredients, weight.toDouble())
        nutrition = normalizeFoodstuffNutrition(nutrition)
        return copy(foodstuff = foodstuff.recreateWithNutrition(nutrition))
    }

    /**
     * When sum of protein, fats and carbs is greater than 100, then we should not create
     * a foodstuff with such nutrition, and must normalize the nutrition before foodstuff creation.
     */
    private fun normalizeFoodstuffNutrition(nutrition: Nutrition): Nutrition {
        val gramsSum = nutrition.protein + nutrition.fats + nutrition.carbs
        if (gramsSum <= 100f) {
            return nutrition
        }
        val factor = 100f / gramsSum
        return Nutrition.withValues(
                nutrition.protein * factor,
                nutrition.fats * factor,
                nutrition.carbs * factor,
                nutrition.calories * factor)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeParcelable(foodstuff, 0)
        dest.writeParcelableArray(ingredients.toTypedArray(), 0)
        dest.writeFloat(weight)
        dest.writeString(comment)
    }

    fun toProto(): RecipeProtos.Recipe {
        return RecipeProtos.Recipe.newBuilder()
                .setLocalId(id)
                .setFoodstuff(foodstuff.toProto())
                .addAllIngredients(ingredients.map { it.toProto() })
                .setWeight(weight)
                .setComment(comment)
                .build()
    }
}

private fun readIngredients(input: Parcel): List<Ingredient> {
    val ingredientsParcelable =
            input.readParcelableArray(Ingredient::class.java.classLoader)!!
    return ingredientsParcelable.map { it as Ingredient }
}
