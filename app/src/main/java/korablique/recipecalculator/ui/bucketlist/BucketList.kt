package korablique.recipecalculator.ui.bucketlist

import android.os.Looper
import korablique.recipecalculator.DishNutritionCalculator
import korablique.recipecalculator.DishNutritionCalculator.calculateIngredients
import korablique.recipecalculator.TestEnvironmentDetector
import korablique.recipecalculator.WrongThreadException
import korablique.recipecalculator.base.logging.Log
import korablique.recipecalculator.base.prefs.PrefsOwner
import korablique.recipecalculator.base.prefs.SharedPrefsManager
import korablique.recipecalculator.database.FoodstuffsList
import korablique.recipecalculator.model.Foodstuff
import korablique.recipecalculator.model.Ingredient
import korablique.recipecalculator.model.Ingredient.Companion.create
import korablique.recipecalculator.model.Nutrition
import korablique.recipecalculator.model.Recipe
import korablique.recipecalculator.model.proto.RecipeProtos
import korablique.recipecalculator.util.FloatUtils
import java.io.IOException
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_RECIPE = "PREFS_RECIPE"

@Singleton
class BucketList @Inject constructor(
        private val prefsManager: SharedPrefsManager) {
    interface Observer {
        fun onIngredientAdded(ingredient: Ingredient) {}
        fun onIngredientRemoved(ingredient: Ingredient) {}
    }

    private var editedRecipe: Recipe
    private val observers: MutableList<Observer> = CopyOnWriteArrayList()

    init {
        editedRecipe = Recipe.create(
                Foodstuff.withName("").withNutrition(Nutrition.zero()),
                emptyList(),
                0f,
                "")
        val recipeBytes: ByteArray? = prefsManager.getBytes(PrefsOwner.BUCKET_LIST, PREFS_RECIPE)
        if (recipeBytes != null) {
            try {
                editedRecipe = Recipe.fromProto(RecipeProtos.Recipe.parseFrom(recipeBytes))
            } catch (e: IOException) {
                Log.e(e)
            }
        }
    }

    fun add(ingredients: List<Ingredient>) {
        checkCurrentThread()

        editedRecipe = editedRecipe.copy(ingredients = editedRecipe.ingredients + ingredients)
        editedRecipe = editedRecipe.recalculateNutrition()
        updatePersistentState()
        for (ingredient in ingredients) {
            for (observer in observers) {
                observer.onIngredientAdded(ingredient)
            }
        }
    }

    fun add(ingredient: Ingredient) {
        checkCurrentThread()

        editedRecipe = editedRecipe.copy(ingredients = editedRecipe.ingredients + ingredient)
        editedRecipe = editedRecipe.recalculateNutrition()
        updatePersistentState()
        for (observer in observers) {
            observer.onIngredientAdded(ingredient)
        }
    }

    fun remove(ingredient: Ingredient) {
        checkCurrentThread()

        editedRecipe = editedRecipe.copy(ingredients = editedRecipe.ingredients - ingredient)
        editedRecipe = editedRecipe.recalculateNutrition()
        updatePersistentState()
        for (observer in observers) {
            observer.onIngredientRemoved(ingredient)
        }
    }

    fun setRecipe(recipe: Recipe) {
        checkCurrentThread()

        val addedIngredients = recipe.ingredients - editedRecipe.ingredients
        val removedIngredients = editedRecipe.ingredients - recipe.ingredients

        editedRecipe = recipe
        updatePersistentState()

        observers.forEach { obs -> addedIngredients.forEach { obs.onIngredientAdded(it) } }
        observers.forEach { obs -> removedIngredients.forEach { obs.onIngredientRemoved(it) } }
    }

    fun getName(): String = editedRecipe.foodstuff.name

    fun getComment(): String = editedRecipe.comment

    fun getTotalWeight(): Float = editedRecipe.weight

    fun getList(): List<Ingredient> = editedRecipe.ingredients

    fun getRecipe(): Recipe = editedRecipe

    fun setName(name: String) {
        editedRecipe = editedRecipe.copy(foodstuff = editedRecipe.foodstuff.recreateWithName(name))
        updatePersistentState()
    }

    fun setComment(comment: String) {
        editedRecipe = editedRecipe.copy(comment = comment)
        updatePersistentState()
    }

    fun setTotalWeight(weight: Float) {
        editedRecipe = editedRecipe.copy(weight = weight)
        editedRecipe = editedRecipe.recalculateNutrition()
        updatePersistentState()
    }

    fun clear() {
        checkCurrentThread()
        val oldIngredients = editedRecipe.ingredients
        editedRecipe = Recipe.create(
                Foodstuff.withName("").withNutrition(Nutrition.zero()),
                emptyList(),
                0f,
                "")
        updatePersistentState()
        for (ingredient in oldIngredients) {
            for (observer in observers) {
                observer.onIngredientRemoved(ingredient)
            }
        }
    }

    fun isEmpty(): Boolean {
        checkCurrentThread()
        return editedRecipe.id < 0
                && editedRecipe.ingredients.isEmpty()
                && FloatUtils.areFloatsEquals(0f, editedRecipe.weight)
                && editedRecipe.name.isEmpty()
                && editedRecipe.comment.isEmpty()
                && Nutrition.zero() == Nutrition.of100gramsOf(editedRecipe.foodstuff)
    }

    private fun updatePersistentState() {
        prefsManager.putBytes(PrefsOwner.BUCKET_LIST, PREFS_RECIPE, editedRecipe.toProto().toByteArray())
    }

    fun addObserver(o: Observer) {
        checkCurrentThread()
        observers.add(o)
    }

    fun removeObserver(o: Observer) {
        checkCurrentThread()
        observers.remove(o)
    }

    private fun checkCurrentThread() {
        if (TestEnvironmentDetector.isInTests()) {
            return
        }
        if (Thread.currentThread().id != Looper.getMainLooper().thread.id) {
            throw WrongThreadException("Can't invoke BucketList's methods from not UI thread")
        }
    }
}