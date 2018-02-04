package korablique.recipecalculator.ui.mainscreen;


import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public abstract class AdapterChild {
    private List<Observer> observers = new ArrayList<>();
    private int itemViewTypesCount;

    public interface Observer {
        void notifyItemInsertedToChild(int index, AdapterChild child);
    }

    public AdapterChild(int itemViewTypesCount) {
        this.itemViewTypesCount = itemViewTypesCount;
    }

    public abstract RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType);
    public abstract void onBindViewHolder(RecyclerView.ViewHolder holder, int childPosition);
    public abstract int getItemCount();
    public abstract int getItemViewType(int childPosition);

    public int getItemViewTypesCount() {
        return itemViewTypesCount;
    }

    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    protected List<Observer> getObservers() {
        return observers;
    }
}
