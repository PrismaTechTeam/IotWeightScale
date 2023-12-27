package com.tbossgroup.tbscale;

import android.os.Parcel;
import android.os.Parcelable;

public class EventBatteryPower implements Parcelable {

    private boolean isCharge;
    private boolean isLowPower;
    private int power;

    public EventBatteryPower() {
    }

    public EventBatteryPower(Parcel parcel) {
        isCharge = parcel.readByte() != 0;
        isLowPower = parcel.readByte() != 0;
        power = parcel.readInt();
    }

    public boolean isCharge() {
        return isCharge;
    }

    public void setCharge(boolean charge) {
        isCharge = charge;
    }

    public boolean isLowPower() {
        return isLowPower;
    }

    public void setLowPower(boolean lowPower) {
        isLowPower = lowPower;
    }

    public int getPower() {
        return power;
    }

    public void setPower(int power) {
        this.power = power;
    }

    public static final Creator<EventBatteryPower> CREATOR = new Creator<EventBatteryPower>() {

        @Override
        public EventBatteryPower createFromParcel(Parcel source) {
            return new EventBatteryPower(source);
        }

        @Override
        public EventBatteryPower[] newArray(int size) {
            return new EventBatteryPower[size];
        }
    };

    @Override
    public String toString() {
        return "isCharge: " + isCharge
                + "\npower: " + power;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeByte((byte) (isCharge?1:0));
        parcel.writeByte((byte) (isLowPower?1:0));
        parcel.writeInt(power);
    }
}
