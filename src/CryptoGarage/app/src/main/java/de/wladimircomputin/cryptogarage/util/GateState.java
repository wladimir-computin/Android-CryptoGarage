package de.wladimircomputin.cryptogarage.util;

import android.graphics.drawable.Drawable;

import de.wladimircomputin.cryptogarage.R;

public enum GateState {
    GATE_NONE("GATE_NONE", R.drawable.unknown),
    GATE_CLOSED("GATE_CLOSED", R.drawable.closed),
    GATE_OPENING("GATE_OPENING", R.drawable.opening),
    GATE_OPEN("GATE_OPEN", R.drawable.open),
    GATE_CLOSING("GATE_CLOSING", R.drawable.closing),
    GATE_STOPPED_OPENING("GATE_STOPPED_OPENING",R.drawable.opening_stopped),
    GATE_STOPPED_CLOSING("GATE_STOPPED_CLOSING", R.drawable.closing_stopped);

    private String value;
    private int icon;

    GateState(String value, int icon) {
        this.value = value;
        this.icon = icon;
    }

    @Override
    public String toString() {
        return value;
    }

    public int getIcon(){
        return icon;
    }
}
