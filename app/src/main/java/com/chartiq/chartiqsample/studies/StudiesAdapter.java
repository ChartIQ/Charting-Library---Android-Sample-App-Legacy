/*
 * Copyright 2014 Eduardo Barrenechea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.chartiq.chartiqsample.studies;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.chartiq.sdk.model.Study;
import com.chartiq.chartiqsample.R;
import com.chartiq.chartiqsample.ui.StickyHeaderAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class StudiesAdapter extends RecyclerView.Adapter<StudiesAdapter.ViewHolder> implements
        StickyHeaderAdapter<StudiesAdapter.HeaderHolder> {

    public static final int HIDDEN_ROW = 0;
    public static final int VISIBLE_ROW = 1;
    private final Study EMPTY_ROW = new Study();

    private LayoutInflater mInflater;
    private final String[] studiesHeaders;
    private List<Study> activeStudiesList;
    private List<Study> availableStudies;
    private Set<Study> selectedActiveItems = new HashSet<>();
    private Set<Study> selectedAvailableItems = new HashSet<>();

    private OnSelectionChangeListener selectionChangeListener = null;
    private View.OnClickListener imageButtonOnClickListener;

    public StudiesAdapter(Context context, List<Study> activeStudiesList, List<Study> availableStudies, View.OnClickListener imageButtonOnClickListener) {
        mInflater = LayoutInflater.from(context);
        this.studiesHeaders = context.getResources().getStringArray(R.array.studies_list_headers);
        this.imageButtonOnClickListener = imageButtonOnClickListener;
        setAvailableStudies(availableStudies);
        setActiveStudiesList(activeStudiesList);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        final View view;
        if (i != 0) {
            view = mInflater.inflate(R.layout.studies_item, viewGroup, false);
            return new ViewHolder(view);
        } else {
            return new StubViewHolder(new View(mInflater.getContext()), new TextView(mInflater.getContext()));
        }
    }

    public Study getItemByPosition(int i) {
        if (i < activeStudiesList.size()) {
            return activeStudiesList.get(i);
        } else {
            return availableStudies.get(i - activeStudiesList.size());
        }
    }

    @Override
    public int getItemViewType(int i) {
        Study item = getItemByPosition(i);
        if (!EMPTY_ROW.equals(item)) {
            return VISIBLE_ROW;
        }
        return HIDDEN_ROW;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int i) {
        final Study item = getItemByPosition(i);
        final boolean isActiveStudies = i < activeStudiesList.size();
        String itemName;
        if (isActiveStudies) {
            itemName = item.name;
        } else {
            itemName = item.name;
            if (itemName == null || itemName.isEmpty()) {
                itemName = item.shortName;
            }
        }
        viewHolder.textView.setText(itemName);
        if (selectedAvailableItems.contains(item) || selectedActiveItems.contains(item)) {
            viewHolder.textView.setBackgroundColor(mInflater.getContext().getResources().getColor(R.color.selected_study_color));
        } else {
            viewHolder.textView.setBackgroundColor(mInflater.getContext().getResources().getColor(R.color.grey));
        }
        viewHolder.textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isActiveStudies && !selectedAvailableItems.isEmpty()) {
                    selectedAvailableItems.clear();
                    notifyDataSetChanged();
                } else if (!isActiveStudies && !selectedActiveItems.isEmpty()) {
                    selectedActiveItems.clear();
                    notifyDataSetChanged();
                }
                Set<Study> selectedItems = isActiveStudies ? selectedActiveItems : selectedAvailableItems;
                if (selectedItems.contains(item)) {
                    selectedItems.remove(item);
                    v.setBackgroundColor(mInflater.getContext().getResources().getColor(R.color.grey));
                } else {
                    selectedItems.add(item);
                    v.setBackgroundColor(mInflater.getContext().getResources().getColor(R.color.selected_study_color));
                }
                if (selectionChangeListener != null) {
                    selectionChangeListener.onSelectionchange(selectedItems, isActiveStudies);
                }
            }
        });
        if (viewHolder.imageButton != null) {
            viewHolder.imageButton.setOnClickListener(imageButtonOnClickListener);
            viewHolder.imageButton.setTag(i);
            if (isActiveStudies) {
                viewHolder.imageButton.setVisibility(View.VISIBLE);
            } else {
                viewHolder.imageButton.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return activeStudiesList.size() + availableStudies.size();
    }

    @Override
    public long getHeaderId(int position) {
        return activeStudiesList.size() > position ? 0 : 1;
    }

    @Override
    public HeaderHolder onCreateHeaderViewHolder(ViewGroup parent) {
        final View view = mInflater.inflate(R.layout.studies_header, parent, false);
        return new HeaderHolder(view);
    }

    @Override
    public void onBindHeaderViewHolder(HeaderHolder viewholder, int position) {
        viewholder.header.setText(studiesHeaders[(int) getHeaderId(position)]);
    }

    public List<Study> getAvailableStudies() {
        if (availableStudies.size() > 0 && EMPTY_ROW.equals(availableStudies.get(0))) {
            return new LinkedList<>(availableStudies.subList(1, availableStudies.size()));
        }
        return availableStudies;
    }

    public List<Study> getActiveStudiesList() {
        if (activeStudiesList.size() > 0 && EMPTY_ROW.equals(activeStudiesList.get(0))) {
            return new LinkedList<>(activeStudiesList.subList(1, activeStudiesList.size()));
        }
        return activeStudiesList;
    }

    public StudiesAdapter setActiveStudiesList(List<Study> activeStudiesList) {
        if (activeStudiesList == null || activeStudiesList.size() == 0) {
            this.activeStudiesList = new ArrayList<>();
            this.activeStudiesList.add(EMPTY_ROW);
        } else {
            this.activeStudiesList = activeStudiesList;
        }
        return this;
    }

    public StudiesAdapter setAvailableStudies(List<Study> availableStudies) {
        if (availableStudies == null || availableStudies.size() == 0) {
            this.availableStudies = new ArrayList<>();
            this.availableStudies.add(EMPTY_ROW);
        } else {
            this.availableStudies = availableStudies;
        }
        return this;
    }

    public StudiesAdapter setSelectionChangeListener(OnSelectionChangeListener selectionChangeListener) {
        this.selectionChangeListener = selectionChangeListener;
        return this;
    }

    public void clearSelection() {
        selectedAvailableItems.clear();
        selectedActiveItems.clear();
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;
        public ImageView imageButton;

        public ViewHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView.findViewById(R.id.text_item);
            imageButton = (ImageView) itemView.findViewById(R.id.image_button);
        }
    }

    static class StubViewHolder extends ViewHolder {
        public StubViewHolder(View itemView, TextView textView) {
            super(itemView);
            this.textView = textView;
        }
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        public TextView header;

        public HeaderHolder(View itemView) {
            super(itemView);
            header = (TextView) itemView.findViewById(R.id.text_header);
        }
    }

    interface OnSelectionChangeListener {
        void onSelectionchange(Set<Study> selectedStudies, boolean isActive);
    }
}
