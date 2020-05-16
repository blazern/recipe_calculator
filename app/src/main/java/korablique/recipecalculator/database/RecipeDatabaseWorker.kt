package korablique.recipecalculator.database

import korablique.recipecalculator.model.Foodstuff
import korablique.recipecalculator.model.Ingredient
import korablique.recipecalculator.model.Recipe

interface RecipeDatabaseWorker {
    /**
     * @param foodstuff MUST already exist in db, otherwise an SQLite exception will be thrown.
     */
    suspend fun createRecipe(
            foodstuff: Foodstuff,
            ingredients: List<Ingredient>,
            comment: String,
            weight: Float): Recipe
    suspend fun getRecipeOfFoodstuff(foodstuff: Foodstuff): Recipe?
    suspend fun getAllRecipes(): List<Recipe>
    /**
     * @param updatedRecipe must already exist in DB (have a valid ID)
     * @return exactly same recipe as the given one on success, null on error
     */
    suspend fun updateRecipe(updatedRecipe: Recipe): Recipe?
}