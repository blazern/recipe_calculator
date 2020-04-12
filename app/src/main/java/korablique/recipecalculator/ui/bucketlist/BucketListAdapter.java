package korablique.recipecalculator.ui.bucketlist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import korablique.recipecalculator.R;
import korablique.recipecalculator.model.Ingredient;
import korablique.recipecalculator.ui.MyViewHolder;


public class BucketListAdapter extends RecyclerView.Adapter<MyViewHolder> {
    public interface OnItemsCountChangeListener {
        void onItemsCountChange(int count);
    }
    public interface OnItemClickedObserver {
        void onItemClicked(Ingredient ingredient, int position);
    }
    private List<Ingredient> allFoodstuffs = new ArrayList<>();
    private Context context;
    @LayoutRes
    private int itemLayoutRes;
    private OnItemsCountChangeListener listener;
    private OnItemClickedObserver onItemClickedObserver;

    public BucketListAdapter(
            Context context,
            @LayoutRes int itemLayoutId,
            OnItemsCountChangeListener listener,
            OnItemClickedObserver onItemClickedObserver) {
        this.context = context;
        this.itemLayoutRes = itemLayoutId;
        this.listener = listener;
        this.onItemClickedObserver = onItemClickedObserver;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final LinearLayout item = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(itemLayoutRes, parent, false);
        return new MyViewHolder(item);
    }

    @Override
    public void onBindViewHolder(@NonNull final MyViewHolder holder, int displayedPosition) {
        View item = holder.getItem();
        final Ingredient ingredient = getItem(displayedPosition);

        setTextViewText(item, R.id.name, ingredient.getFoodstuff().getName());
        setTextViewText(item, R.id.extra_info_block, context.getString(R.string.n_gramms, Math.round(ingredient.getWeight())));

        item.setOnClickListener(v -> {
            onItemClickedObserver.onItemClicked(ingredient, holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return allFoodstuffs.size();
    }

    public void addItems(List<Ingredient> ingredients) {
        int allFoodstuffsSizeBefore = allFoodstuffs.size();
        allFoodstuffs.addAll(ingredients);
        for (int index = 0; index < ingredients.size(); index++) {
            notifyItemInserted(allFoodstuffsSizeBefore + index);
        }
        listener.onItemsCountChange(getItemCount());
    }

    public void addItem(Ingredient ingredient) {
        addItems(Collections.singletonList(ingredient));
    }

    public void addItem(Ingredient foodstuff, int position) {
        allFoodstuffs.add(position, foodstuff);
        notifyItemInserted(position);
        listener.onItemsCountChange(getItemCount());
    }

    public void deleteItem(int displayedPosition) {
        allFoodstuffs.remove(displayedPosition);
        notifyItemRemoved(displayedPosition);
        listener.onItemsCountChange(getItemCount());
    }

    public void replaceItem(Ingredient newIngredient, int displayedPosition) {
        allFoodstuffs.set(displayedPosition, newIngredient);
        notifyItemChanged(displayedPosition);
    }

    public Ingredient getItem(int displayedPosition) {
        return allFoodstuffs.get(displayedPosition);
    }

    public List<Ingredient> getItems() {
        return Collections.unmodifiableList(allFoodstuffs);
    }

    private <T> void setTextViewText(View parent, int viewId, T text) {
        ((TextView) parent.findViewById(viewId)).setText(text.toString());
    }
}
