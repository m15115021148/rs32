package com.meigsmart.meigrs32.model;

/**
 * Created by chenMeng on 2018/4/24.
 */
public class TypeModel {
    private int id;
    private String name;
    private int type;//0 unselected ; 1 pass ; 2 failure

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

}
