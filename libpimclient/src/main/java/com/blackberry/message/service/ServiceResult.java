package com.blackberry.message.service;

import android.os.Parcel;
import android.os.Parcelable;

public class ServiceResult implements Parcelable {

    private int mResponseCode;
    private String mResponseMessage;

    public static final int CODE_SUCCESS = 0;
    public static final int CODE_INVALID_ARGUMENT = 1;
    public static final int CODE_SERVICE_ERROR = 2;
    public static final int CODE_OPERATION_NOT_SUPPORTED = 3;

    public int getResponseCode() {
        return mResponseCode;
    }

    public void setResponseCode(int responseCode) {
        this.mResponseCode = responseCode;
    }

    public String getResponseMessage() {
        return mResponseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.mResponseMessage = responseMessage;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mResponseCode);
        dest.writeString(mResponseMessage);
    }

    public ServiceResult() {
    }

    public ServiceResult(Parcel in) {
        readFromParcel(in);
    }

    /**
     * Supports Parcelable
     */
    public static final Parcelable.Creator<ServiceResult> CREATOR = new Parcelable.Creator<ServiceResult>() {
        @Override
        public ServiceResult createFromParcel(Parcel in) {
            return new ServiceResult(in);
        }

        @Override
        public ServiceResult[] newArray(int size) {
            return new ServiceResult[size];
        }
    };

    public void readFromParcel(Parcel in) {
        mResponseCode = in.readInt();
        mResponseMessage = in.readString();
    }
}
