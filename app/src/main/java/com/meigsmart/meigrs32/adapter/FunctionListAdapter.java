package com.meigsmart.meigrs32.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.model.TypeModel;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by chenMeng on 2018/4/24.
 */
public class FunctionListAdapter extends RecyclerView.Adapter<FunctionListAdapter.Holder>{
    private List<TypeModel> mList = new ArrayList<>();
    private OnFunctionItemClick mCallBack;

    public FunctionListAdapter( OnFunctionItemClick callBack){
        this.mCallBack = callBack;
    }

    public interface OnFunctionItemClick{
        void onClickItem(int position);
    }

    public void setData(List<TypeModel> list){
        this.mList = list;
        this.notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.function_list_item,null);
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

    class Holder extends RecyclerView.ViewHolder{
        @BindView(R.id.layout)
        public RelativeLayout mLayout;
        @BindView(R.id.name)
        public TextView mName;

        public Holder(View itemView) {
            super(itemView);
            ButterKnife.bind(this,itemView);
        }

        public void initData(final int position){
            TypeModel model = mList.get(position);
            mName.setText(model.getName());
            mLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mCallBack!=null)mCallBack.onClickItem(position);
                }
            });
        }
    }
}
