package com.samagra.parent.ui.competencyselection.readonlycompetency;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.samagra.commons.models.metadata.CompetencyModel;
import com.samagra.parent.ui.competencyselection.ReadOnlyCompetencyItemView;
import com.samagra.parent.ui.detailselection.ItemSelectionListener;

import java.util.List;

public class ReadOnlyCompetencyAdapter extends RecyclerView.Adapter<ReadOnlyCompetencyAdapter.RecyclerViewHolder> {

    private List<CompetencyModel> mData;
    private ItemSelectionListener<CompetencyModel> mListener;
    private CompetencyModel mSelectedCompetency;

    public ReadOnlyCompetencyAdapter(List<CompetencyModel> recyclerDataArrayList) {
        mData = recyclerDataArrayList;
    }

    @NonNull
    @Override
    public RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ReadOnlyCompetencyItemView itemView = new ReadOnlyCompetencyItemView(parent.getContext());
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        itemView.setLayoutParams(lp);
        return new RecyclerViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewHolder holder, int position) {
        CompetencyModel recyclerData = mData.get(holder.getAdapterPosition());
        recyclerData.setId(position + 1);
        holder.itemView.setData(recyclerData, position + 1);
        holder.itemView.setSelected(true);
    }

    private void updateSelection(int position) {
        if (mData != null) {
            for (int i = 0; i < mData.size(); i++) {
                mData.get(i).setSelected(i == position);
            }
            notifyDataSetChanged();
            if (mListener != null) {
                if (position < 0) {
                    mSelectedCompetency = null;
                } else {
                    mSelectedCompetency = mData.get(position);
                }
                mListener.onSelectionChange(position, mSelectedCompetency);
            }
        }
    }

    public void resetSelection() {
        updateSelection(-1);
    }

    public CompetencyModel getSelectedItem() {
        return mSelectedCompetency;
    }

    @Override
    public int getItemCount() {
        // this method returns the size of recyclerview
        return mData.size();
    }

    // View Holder Class to handle Recycler View.
    public class RecyclerViewHolder extends RecyclerView.ViewHolder {
        private ReadOnlyCompetencyItemView itemView;

        public RecyclerViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = (ReadOnlyCompetencyItemView) itemView;

        }
    }

    public boolean isLastRowAndShouldBeCentered(int pos) {
        if (pos == mData.size() - 1 && mData.size() % 2 != 0) {
            return true;
        } else {
            return false;
        }
    }

    public void setItemSelectionListener(ItemSelectionListener<CompetencyModel> listener) {
        this.mListener = listener;
    }
}