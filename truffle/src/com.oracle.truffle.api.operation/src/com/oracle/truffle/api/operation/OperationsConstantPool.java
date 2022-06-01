package com.oracle.truffle.api.operation;

import java.util.ArrayList;

public class OperationsConstantPool {

    private ArrayList<Object> values = new ArrayList<>();
    private boolean frozen = false;
    private Object[] frozenValues = null;

    public synchronized int add(Object o) {
        if (frozen)
            throw new IllegalStateException("constant pool already frozen");
        int idx = values.indexOf(o);
        if (idx == -1) {
            idx = values.size();
            values.add(o);
        }
        return idx;
    }

    public synchronized int reserve() {
        return reserve(1);
    }

    public synchronized int reserve(int count) {
        if (frozen)
            throw new IllegalStateException("constant pool already frozen");
        int idx = values.size();
        for (int i = 0; i < count; i++) {
            values.add(null);
        }
        return idx;
    }

    public synchronized void reset() {
        this.frozen = false;
        this.values = new ArrayList<>();
        this.frozenValues = null;
    }

    public void setValue(int offset, Object value) {
        values.set(offset, value);
    }

    public synchronized void freeze() {
        frozen = true;
    }

    public synchronized Object[] getValues() {
        freeze();
        if (frozenValues == null) {
            frozenValues = values.toArray();
            values = null;
        }
        return frozenValues;
    }

}
