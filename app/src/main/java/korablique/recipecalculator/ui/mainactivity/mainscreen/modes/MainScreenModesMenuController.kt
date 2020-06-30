package korablique.recipecalculator.ui.mainactivity.mainscreen.modes

import android.animation.Animator
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.transition.TransitionManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import korablique.recipecalculator.R
import korablique.recipecalculator.base.ActivityCallbacks
import korablique.recipecalculator.base.BaseFragment
import korablique.recipecalculator.base.FragmentCallbacks
import korablique.recipecalculator.dagger.FragmentScope
import korablique.recipecalculator.database.RecipesRepository
import korablique.recipecalculator.ui.bucketlist.BucketList
import javax.inject.Inject

@FragmentScope
class MainScreenModesMenuController @Inject constructor(
        private val fragment: BaseFragment,
        private val fragmentCallbacks: FragmentCallbacks,
        private val activityCallbacks: ActivityCallbacks,
        private val modesController: MainScreenModesController,
        private val bucketList: BucketList,
        private val recipesRepository: RecipesRepository)
    : FragmentCallbacks.Observer, ActivityCallbacks.Observer {
    private lateinit var menuButton: FloatingActionButton

    init {
        fragmentCallbacks.addObserver(this)
    }

    override fun onFragmentViewCreated(fragmentView: View, savedInstanceState: Bundle?) {
        menuButton = fragmentView.findViewById(R.id.mode_fab)
        menuButton.setOnClickListener {
            val parent = fragment.requireActivity().findViewById(R.id.main_fullscreen_container) as ViewGroup
            val menuLayout = showMenuLayout(fragmentView, parent)
            initMenuLayout(menuLayout)
        }

        val existingLayout = fragment.requireActivity().findViewById<View>(R.id.main_screen_modes_menu_layout)
        if (existingLayout != null) {
            initMenuLayout(existingLayout)
        }

        activityCallbacks.addObserver(this)
    }

    private fun showMenuLayout(fragmentView: View, parent: ViewGroup): ViewGroup {
        val menuLayout = LayoutInflater.from(fragmentView.context).inflate(
                R.layout.main_screen_modes_menu_layout,
                parent,
                false) as ConstraintLayout
        parent.addView(menuLayout)

        menuLayout.alpha = 0.0f
        ObjectAnimator.ofFloat(menuLayout, "alpha", 0.0f, 1.0f).start()

        // Visible
        TransitionManager.beginDelayedTransition(menuLayout)
        val constraints = ConstraintSet()
        constraints.clone(menuLayout)
        constraints.setVisibility(R.id.create_recipe_menu_button, View.VISIBLE)
        constraints.applyTo(menuLayout)

        return menuLayout
    }

    override fun onFragmentDestroy() {
        activityCallbacks.removeObserver(this)
    }

    private fun initMenuLayout(layout: View) {
        layout.findViewById<Button>(R.id.create_recipe_menu_button).setOnClickListener {
            removeMenu(layout)
            if (modesController.modeId() == MainScreenMode.ID.RECIPE) {
                return@setOnClickListener
            }
            modesController.switchModeTo(
                    MainScreenRecipeMode(
                            modesController, fragment, bucketList, recipesRepository))
        }
    }

    override fun onActivityBackPressed(): Boolean {
        val existingLayout = fragment.requireActivity()
                .findViewById<View>(R.id.main_screen_modes_menu_layout)
        if (existingLayout != null) {
            removeMenu(existingLayout)
            return true
        }
        return false
    }

    private fun removeMenu(menuLayout: View) {
        menuLayout.alpha = 1.0f
        val animator = ObjectAnimator.ofFloat(menuLayout, "alpha", 1.0f, 0.0f)
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) = Unit
            override fun onAnimationCancel(animation: Animator?) = Unit
            override fun onAnimationStart(animation: Animator?) = Unit
            override fun onAnimationEnd(animation: Animator?) {
                (menuLayout.parent as ViewGroup).removeView(menuLayout)
            }
        })
        animator.start()
    }
}