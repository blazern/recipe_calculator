package korablique.recipecalculator.ui.mainactivity.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.joda.time.DateTime;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseBottomDialog;
import korablique.recipecalculator.base.TimeProvider;
import korablique.recipecalculator.dagger.InjectorHolder;
import korablique.recipecalculator.model.UserParameters;
import korablique.recipecalculator.ui.DecimalUtils;

public class NewMeasurementsDialog extends BaseBottomDialog {
    @Inject
    TimeProvider timeProvider;

    public interface OnSaveNewMeasurementsListener {
        void onSave(UserParameters newUserParams);
    }
    private static final String NEW_MEASUREMENTS_DIALOG_TAG = "NEW_MEASUREMENTS_DIALOG_TAG";
    private static final String LAST_PARAMS = "LAST_PARAMS";
    private OnSaveNewMeasurementsListener onSaveNewMeasurementsListener;

    @Override
    protected boolean shouldOpenKeyboardWhenShown() {
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        InjectorHolder.getInjector().inject(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View dialogLayout = LayoutInflater.from(getContext())
                .inflate(R.layout.new_measurements_card_layout, container);

        // сегодняшняя дата
        TextView newMeasurementHeader = dialogLayout.findViewById(R.id.new_measurement_header);
        newMeasurementHeader.setText(getString(
                R.string.new_measurements, timeProvider.now().toString(getString(R.string.date_format))));

        // предыдущее значение веса
        Bundle args = getArguments();
        if (args == null || !args.containsKey(LAST_PARAMS)) {
            throw new IllegalStateException("Unacceptable condition " +
                    "because we put last user parameters to dialog after it creating");
        }
        UserParameters lastParams = args.getParcelable(LAST_PARAMS);
        float lastWeight = lastParams.getWeight();
        TextView lastValue = dialogLayout.findViewById(R.id.last_measurement_value);
        lastValue.setText(DecimalUtils.toDecimalString(lastWeight));

        // предыдущая дата взвешивания
        TextView lastMeasurementHeader = dialogLayout.findViewById(R.id.last_measurement_header);
        DateTime lastDate = new DateTime(lastParams.getMeasurementsTimestamp());
        lastMeasurementHeader.setText(getString(
                R.string.last_measurements, lastDate.toString(getString(R.string.date_format))));

        // кнопка сохранить
        Button saveButton = dialogLayout.findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // создать новый UserParameters, содержащий старые данные + новый вес + новая дата
                EditText newWeightView = dialogLayout.findViewById(R.id.new_measurement_value);
                float newWeight = Float.parseFloat(newWeightView.getText().toString());
                UserParameters paramsWithNewWeight = new UserParameters(
                        lastParams.getTargetWeight(),
                        lastParams.getGender(),
                        lastParams.getDateOfBirth(),
                        lastParams.getHeight(),
                        newWeight,
                        lastParams.getLifestyle(),
                        lastParams.getFormula(),
                        timeProvider.nowUtc().getMillis());
                onSaveNewMeasurementsListener.onSave(paramsWithNewWeight);
                dismiss();
            }
        });

        // кнопка закрыть
        View closeButton = dialogLayout.findViewById(R.id.button_close);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        return dialogLayout;
    }

    public void setOnSaveNewMeasurementsListener(OnSaveNewMeasurementsListener listener) {
        onSaveNewMeasurementsListener = listener;
    }

    public static NewMeasurementsDialog showDialog(FragmentManager fragmentManager, UserParameters lastParams) {
        NewMeasurementsDialog dialog = new NewMeasurementsDialog();
        Bundle args = new Bundle();
        args.putParcelable(LAST_PARAMS, lastParams);
        dialog.setArguments(args);
        dialog.show(fragmentManager, NEW_MEASUREMENTS_DIALOG_TAG);
        return dialog;
    }

    @Nullable
    public static NewMeasurementsDialog findDialog(FragmentManager fragmentManager) {
        return (NewMeasurementsDialog) fragmentManager.findFragmentByTag(NEW_MEASUREMENTS_DIALOG_TAG);
    }
}
