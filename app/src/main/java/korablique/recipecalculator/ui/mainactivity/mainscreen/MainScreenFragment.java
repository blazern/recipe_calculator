package korablique.recipecalculator.ui.mainactivity.mainscreen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseFragment;
import korablique.recipecalculator.model.Foodstuff;


public class MainScreenFragment extends BaseFragment {
    @Inject
    MainScreenController controller;
    @Inject
    UpFABController upFABController;

    public static Bundle createArguments(
            ArrayList<Foodstuff> top, ArrayList<Foodstuff> allFoodstuffsFirstBatch) {
        return MainScreenController.createArguments(top, allFoodstuffsFirstBatch);
    }

    @Override
    public View createView(@NonNull LayoutInflater inflater,
                           ViewGroup container,
                           Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main_screen, container, false);
    }
}
