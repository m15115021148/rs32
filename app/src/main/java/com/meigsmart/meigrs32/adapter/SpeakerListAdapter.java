package com.meigsmart.meigrs32.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.model.TypeModel;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by chenMeng on 2018/5/10.
 */
public class SpeakerListAdapter extends RecyclerView.Adapter<SpeakerListAdapter.Holder>{
    private List<TypeModel> mList = new ArrayList<>();
    private OnSpeakerSound mCallBack;

    public SpeakerListAdapter(OnSpeakerSound callBack){
        this.mCallBack = callBack;
    }

    public interface OnSpeakerSound{
        void onSpeakerItemListener(int pos);
    }

    public void setData(List<TypeModel> list){
        this.mList = list;
        this.notifyDataSetChanged();
    }

    public List<TypeModel> getData(){
        return this.mList;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.speaker_list_item,null);
        Holder holder = new Holder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.initData(position);
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public class Holder extends RecyclerView.ViewHolder{
        @BindView(R.id.name)
        public TextView mName;
        @BindView(R.id.img)
        public ImageView img;
        @BindView(R.id.layout)
        public RelativeLayout layout;

        public Holder(View itemView) {
            super(itemView);
            ButterKnife.bind(this,itemView);
        }

        public void initData(final int position){
            TypeModel model = mList.get(position);
            mName.setText(model.getName());

            if (model.getType() == 0){
                img.setSelected(false);
            }else if (model.getType() == 1){
                img.setSelected(true);
            }

            layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mCallBack!=null)mCallBack.onSpeakerItemListener(position);
                }
            });

        }
    }
}
