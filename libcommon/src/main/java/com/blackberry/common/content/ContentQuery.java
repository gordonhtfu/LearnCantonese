
package com.blackberry.common.content;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Example:
 * ContentQuery q = new ContentQueryBuilder.create()
        .where("is_awesome", EQ, true)
        .and()
        .where("money", GT, 50.0f)
        .and()
        .whereState("speed") .matches(21)
        .or()
        .where("door_number", EQ, 123)
        .or()
        .where( ContentQueryBuilder.create()
                .where("a", GT, 0)
                .and()
                .where("a", LT, 100) )
                .orderBy("money")
                .descending()
                .build();
 * Produces:
 * selection: is_awesome = ? AND money > ? AND (( speed & ? ) == 0) OR * door_number = ? OR (a > ? AND a < ?)
 * args: [1, 50.0, 21, 123, 0, 100]
 * sortOrder: money DESC
 **/

public class ContentQuery implements Parcelable {

    public interface WhereOp {
        public final String EQ = " = ";
        public final String NEQ = " != ";
        public final String GT = " > ";
        public final String LT = " < ";
        public final String GTEQ = " >= ";
        public final String LTEQ = " <= ";
        public final String LIKE = " LIKE ";
        public final String IS = " IS ";
        public final String ISNOT = " IS NOT ";
    }

    public interface Builder {
        public interface WhereClause {
            public Builder and();

            public Builder or();

            public SortOrder orderBy(String column);

            public FinalClause groupBy(String column);

            public ContentQuery build();
        }

        public interface SortOrder {
            FinalClause ascending();

            FinalClause descending();
        }

        public interface FinalClause {
            public ContentQuery build();
        }

        public interface StateClause {
            WhereClause matches(long arg);

            WhereClause contains(long arg);
        }

        public interface ListClause {
            WhereClause in(boolean[] argList);

            WhereClause in(int[] argList);

            WhereClause in(long[] argList);

            WhereClause in(float[] argList);

            WhereClause in(double[] argList);

            WhereClause in(String[] argList);
        }

        public WhereClause where(String column, String op, boolean arg);

        public WhereClause where(String column, String op, int arg);

        public WhereClause where(String column, String op, long arg);

        public WhereClause where(String column, String op, float arg);

        public WhereClause where(String column, String op, double arg);

        public WhereClause where(String column, String op, String arg);

        public WhereClause whereIsNull(String column);

        public WhereClause where(WhereClause q);

        public StateClause whereState(String column);

        public ListClause where(String column);

        public SortOrder orderBy(String column);

        public ContentQuery build();
    }

    private ContentQueryBuilderImpl mBuilder;

    private ContentQuery(ContentQueryBuilderImpl builder) {
        mBuilder = builder;
    }

    private ContentQuery(Parcel pc) {
        mBuilder = (ContentQueryBuilderImpl) pc.readParcelable(ContentQueryBuilderImpl.class
                .getClassLoader());
    }

    public String[] selectionArgs() {
        if (mBuilder.mArgs.isEmpty()) {
            return null;
        }
        return mBuilder.mArgs.toArray(new String[0]);
    }

    public String selection() {
        if (mBuilder.mSelection.length() == 0) {
            return null;
        }
        return mBuilder.mSelection.toString();
    }

    public String sortOrder() {
        if (mBuilder.mSortOrder.length() == 0) {
            return null;
        }
        return mBuilder.mSortOrder.toString();
    }

    public static final Parcelable.Creator<ContentQuery> CREATOR = new Parcelable.Creator<ContentQuery>() {
        @Override
        public ContentQuery createFromParcel(Parcel pc) {
            return new ContentQuery(pc);
        }

        @Override
        public ContentQuery[] newArray(int size) {
            return new ContentQuery[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mBuilder, flags);
    }

    public static Builder newBuilder() {
        return new ContentQueryBuilderImpl();
    }

    private static class ContentQueryBuilderImpl implements Builder, Builder.WhereClause,
            Builder.SortOrder, Builder.StateClause, Builder.ListClause, Builder.FinalClause,
            Parcelable {

        private StringBuilder mSortOrder;
        private StringBuilder mSelection;
        private ArrayList<String> mArgs;

        private ContentQueryBuilderImpl() {
            mSortOrder = new StringBuilder();
            mSelection = new StringBuilder();
            mArgs = new ArrayList<String>();
        }

        private ContentQueryBuilderImpl(Parcel pc) {
            mSelection = new StringBuilder(pc.readString());
            String[] argsArray = null;
            pc.readStringArray(argsArray);
            mArgs = new ArrayList<String>(Arrays.asList(argsArray));
            mSortOrder = new StringBuilder(pc.readString());
        }

        @Override
        public Builder and() {
            mSelection.append(" AND ");
            return this;
        }

        @Override
        public Builder or() {
            mSelection.append(" OR ");
            return this;
        }

        @Override
        public WhereClause where(WhereClause q) {
            ContentQueryBuilderImpl w = (ContentQueryBuilderImpl) q;
            mSelection.append("(");
            mSelection.append(w.mSelection.toString());
            mSelection.append(")");
            mArgs.addAll(w.mArgs);
            return this;
        }

        @Override
        public ContentQuery build() {
            return new ContentQuery(this);
        }

        @Override
        public WhereClause where(String column, String op, boolean arg) {
            return where(column, op, arg ? "1" : "0");
        }

        @Override
        public WhereClause where(String column, String op, int arg) {
            return where(column, op, String.valueOf(arg));
        }

        @Override
        public WhereClause where(String column, String op, long arg) {
            return where(column, op, String.valueOf(arg));
        }

        @Override
        public WhereClause where(String column, String op, float arg) {
            return where(column, op, String.valueOf(arg));
        }

        @Override
        public WhereClause where(String column, String op, double arg) {
            return where(column, op, String.valueOf(arg));
        }

        @Override
        public WhereClause where(String column, String op, String arg) {
            if (arg == null) {
                throw new IllegalArgumentException("'arg' must not be null.");
            }
            checkOperation(op);
            mSelection.append(column).append(op).append("?");
            mArgs.add(arg);

            return this;
        }

        @Override
        public WhereClause whereIsNull(String column) {
            mSelection.append(column).append(WhereOp.IS).append("null");
            return this;
        }

        @Override
        public SortOrder orderBy(String column) {
            mSortOrder.append(column);
            return this;
        }

        @Override
        public StateClause whereState(String column) {
            mSelection.append("(( ");
            mSelection.append(column);
            return this;
        }

        @Override
        public WhereClause matches(long arg) {
            String strArg = String.valueOf(arg);
            mSelection.append(" & ? ) == ");
            mSelection.append(strArg);
            mSelection.append(")");
            mArgs.add(strArg);
            return this;
        }

        @Override
        public WhereClause contains(long arg) {
            String strArg = String.valueOf(arg);
            mSelection.append(" & ? ) > 0)");
            mArgs.add(strArg);
            return this;
        }

        private void checkOperation(String op) {
            if (!op.equals(WhereOp.EQ) &&
                    !op.equals(WhereOp.NEQ) &&
                    !op.equals(WhereOp.GT) &&
                    !op.equals(WhereOp.LT) &&
                    !op.equals(WhereOp.GTEQ) &&
                    !op.equals(WhereOp.LTEQ) &&
                    !op.equals(WhereOp.LIKE) &&
                    !op.equals(WhereOp.IS) &&
                    !op.equals(WhereOp.ISNOT)) {
                throw new IllegalArgumentException("Unknown Operation: " + op);
            }
        }

        @Override
        public FinalClause ascending() {
            mSortOrder.append(" ASC ");
            return this;
        }

        @Override
        public FinalClause descending() {
            mSortOrder.append(" DESC ");
            return this;
        }

        @Override
        public WhereClause in(boolean[] argList) {
            mSelection.append(" IN (");
            String[] builder = new String[argList.length];
            for (int i = 0; i < argList.length; i++) {
                if (argList[i])
                    builder[i] = "1";
                else
                    builder[i] = "0";
            }
            String a = Arrays.toString(builder); // toString the List or Vector
            String ar = a.substring(1, a.length() - 1);
            mSelection.append(ar);
            mSelection.append(") ");
            return this;
        }

        @Override
        public WhereClause in(int[] argList) {
            mSelection.append(" IN (");
            String a = Arrays.toString(argList);
            // remove the brackets
            String ar = a.substring(1, a.length() - 1);
            mSelection.append(ar);
            mSelection.append(") ");
            return this;
        }

        @Override
        public WhereClause in(long[] argList) {
            mSelection.append(" IN (");
            String a = Arrays.toString(argList);
            // remove the brackets
            String ar = a.substring(1, a.length() - 1);
            mSelection.append(ar);
            mSelection.append(") ");
            return this;
        }

        @Override
        public WhereClause in(float[] argList) {
            mSelection.append(" IN (");
            String a = Arrays.toString(argList);
            // remove the brackets
            String ar = a.substring(1, a.length() - 1);
            mSelection.append(ar);
            mSelection.append(") ");
            return this;
        }

        @Override
        public WhereClause in(double[] argList) {
            mSelection.append(" IN (");
            String a = Arrays.toString(argList);
            // remove the brackets
            String ar = a.substring(1, a.length() - 1);
            mSelection.append(ar);
            mSelection.append(") ");
            return this;
        }

        @Override
        public WhereClause in(String[] argList) {
            mSelection.append(" IN (");

            StringBuilder result = new StringBuilder();
            for (String string : argList) {
                result.append("'");
                result.append(string);
                result.append("'");
                result.append(", ");
            }
            String ar = result.length() > 0 ? result.substring(0, result.length() - 2) : "";
            mSelection.append(ar);
            mSelection.append(") ");
            return this;
        }

        @Override
        public ListClause where(String column) {
            mSelection.append(column);
            return this;
        }

        @Override
        public FinalClause groupBy(String column) {
            mSelection.append(" GROUP BY ");
            mSelection.append(column);
            return this;
        }

        @Override
        public int describeContents() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mSelection.toString());
            dest.writeStringArray(mArgs.toArray(new String[0]));
            dest.writeString(mSortOrder.toString());
        }

        public static final Parcelable.Creator<ContentQueryBuilderImpl> CREATOR = new Parcelable.Creator<ContentQueryBuilderImpl>() {
            @Override
            public ContentQueryBuilderImpl createFromParcel(Parcel pc) {
                return new ContentQueryBuilderImpl(pc);
            }

            @Override
            public ContentQueryBuilderImpl[] newArray(int size) {
                return new ContentQueryBuilderImpl[size];
            }
        };
    }
}
