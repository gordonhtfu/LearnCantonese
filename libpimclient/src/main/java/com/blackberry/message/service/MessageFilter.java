package com.blackberry.message.service;

import java.util.ArrayList;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class MessageFilter implements Parcelable {

    // a collection of rules
    public List<Criterion> mCriteria;
    // preferable number of records
    public Integer mMinCount;
    // maximum number of records
    public Integer mMaxCount;

    public StringCriterion addStringCriterion(String propName, String... strings) {
        StringCriterion c = new StringCriterion();
        c.mPropName = propName;
        c.mStrValues = strings;
        mCriteria.add(c);
        return c;
    }

    public NumberCriterion addNumberCriterion(String propName, long... longs) {
        NumberCriterion c = new NumberCriterion();
        c.mPropName = propName;
        c.mNumberValues = longs;
        mCriteria.add(c);
        return c;
    }

    public String getSingleStringCriterion(String propName) {
        Criterion c = findPropertyCriterion(propName);
        if (!(c instanceof StringCriterion))
            return null;
        StringCriterion sc = (StringCriterion) c;
        if (sc.mStrValues == null || sc.mStrValues.length != 1)
            return null;
        return sc.mStrValues[0];
    }

    public Long getSingleLongCriterion(String propName) {
        Criterion c = findPropertyCriterion(propName);
        if (!(c instanceof NumberCriterion))
            return null;
        NumberCriterion sc = (NumberCriterion) c;
        if (sc.mNumberValues == null || sc.mNumberValues.length != 1)
            return null;
        return sc.mNumberValues[0];
    }

    public Criterion findPropertyCriterion(String propName) {
        for (Criterion cr : mCriteria) {
            if (TextUtils.equals(cr.mPropName, propName)) {
                return cr;
            }
        }
        return null;
    }

    public static class StringCriterion extends Criterion {

        public static interface CompareFlags {
            public static final int SUBSTRING_SEARCH = 1 << 0;
            public static final int CASE_SENSITIVE = 1 << 1;
        }

        // Generic property to be searched in all text fields
        public static final String PARAM_TEXT = "_text";

        // collection of strings
        public String[] mStrValues;
        // flags on how to compare values, i.e. exact or substring search, case
        // sensitive for strings, etc.
        public int mCompareFlags;

        public StringCriterion() {
        }

        public StringCriterion(Parcel in) {
            super(in);
            mStrValues = in.createStringArray();
            mCompareFlags = in.readInt();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeStringArray(mStrValues);
            dest.writeInt(mCompareFlags);
        }

        public static final Parcelable.Creator<StringCriterion> CREATOR = new Parcelable.Creator<StringCriterion>() {
            @Override
            public StringCriterion createFromParcel(Parcel in) {
                return new StringCriterion(in);
            }

            @Override
            public StringCriterion[] newArray(int size) {
                return new StringCriterion[size];
            }
        };
    }

    public static class NumberCriterion extends Criterion {

        // collection of strings
        public long[] mNumberValues;

        public NumberCriterion() {
        }

        public NumberCriterion(Parcel in) {
            super(in);
            mNumberValues = in.createLongArray();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeLongArray(mNumberValues);
        }

        public static final Parcelable.Creator<NumberCriterion> CREATOR = new Parcelable.Creator<NumberCriterion>() {
            @Override
            public NumberCriterion createFromParcel(Parcel in) {
                return new NumberCriterion(in);
            }

            @Override
            public NumberCriterion[] newArray(int size) {
                return new NumberCriterion[size];
            }
        };
    }

    public static class RangeCriterion extends Criterion {

        public Long mMinValue;
        public Long mMaxValue;

        public RangeCriterion() {
        }

        public RangeCriterion(Parcel in) {
            super(in);
            mMinValue = in.readLong();
            mMaxValue = in.readLong();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeLong(mMinValue);
            dest.writeLong(mMaxValue);
        }

        /**
         * Supports Parcelable
         */
        public static final Parcelable.Creator<RangeCriterion> CREATOR = new Parcelable.Creator<RangeCriterion>() {
            @Override
            public RangeCriterion createFromParcel(Parcel in) {
                return new RangeCriterion(in);
            }

            @Override
            public RangeCriterion[] newArray(int size) {
                return new RangeCriterion[size];
            }
        };
    }

    public static class MaskCriterion extends Criterion {

        // flags that must be set
        public int mSetMask;
        // flags that must be cleared
        public int mClearedMask;

        public MaskCriterion() {
        }

        public MaskCriterion(Parcel in) {
            super(in);
            mSetMask = in.readInt();
            mClearedMask = in.readInt();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mSetMask);
            dest.writeInt(mClearedMask);
        }

        /**
         * Supports Parcelable
         */
        public static final Parcelable.Creator<MaskCriterion> CREATOR = new Parcelable.Creator<MaskCriterion>() {
            @Override
            public MaskCriterion createFromParcel(Parcel in) {
                return new MaskCriterion(in);
            }

            @Override
            public MaskCriterion[] newArray(int size) {
                return new MaskCriterion[size];
            }
        };
    }

    public static class Criterion implements Parcelable {

        // property name from corresponding contract, i.e.
        // DomainMessageContract.MessageColumns.TIMESTAMP
        public String mPropName;

        public Criterion() {
        }

        public Criterion(Parcel in) {
            mPropName = in.readString();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mPropName);
        }

        /**
         * Supports Parcelable
         */
        public static final Parcelable.Creator<Criterion> CREATOR = new Parcelable.Creator<Criterion>() {
            @Override
            public Criterion createFromParcel(Parcel in) {
                return new Criterion(in);
            }

            @Override
            public Criterion[] newArray(int size) {
                return new Criterion[size];
            }
        };
    }

    @Override
    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mMinCount != null ? mMinCount : -1);
        dest.writeInt(mMaxCount != null ? mMaxCount : -1);
        dest.writeList(mCriteria);
    }

    public MessageFilter() {
        mCriteria = new ArrayList<MessageFilter.Criterion>();
    }

    /**
     * Supports Parcelable
     */
    public MessageFilter(Parcel in) {
        mMinCount = in.readInt();
        if (mMinCount < 0)
            mMinCount = null;
        mMaxCount = in.readInt();
        if (mMaxCount < 0)
            mMaxCount = null;
        mCriteria = in.readArrayList(MessageFilter.Criterion.class.getClassLoader());
    }

    /**
     * Supports Parcelable
     */
    public static final Parcelable.Creator<MessageFilter> CREATOR = new Parcelable.Creator<MessageFilter>() {
        @Override
        public MessageFilter createFromParcel(Parcel in) {
            return new MessageFilter(in);
        }

        @Override
        public MessageFilter[] newArray(int size) {
            return new MessageFilter[size];
        }
    };

}
