/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /Users/gofu/AndroidStudioProjects/CantoneseChallenge/libemailprovider/src/main/aidl/com/blackberry/dav/service/IDavCheckSettings.aidl
 */
package com.blackberry.dav.service;
public interface IDavCheckSettings extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.blackberry.dav.service.IDavCheckSettings
{
private static final java.lang.String DESCRIPTOR = "com.blackberry.dav.service.IDavCheckSettings";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.blackberry.dav.service.IDavCheckSettings interface,
 * generating a proxy if needed.
 */
public static com.blackberry.dav.service.IDavCheckSettings asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.blackberry.dav.service.IDavCheckSettings))) {
return ((com.blackberry.dav.service.IDavCheckSettings)iin);
}
return new com.blackberry.dav.service.IDavCheckSettings.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_hasCalendar:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _arg1;
_arg1 = data.readString();
java.lang.String _arg2;
_arg2 = data.readString();
boolean _result = this.hasCalendar(_arg0, _arg1, _arg2);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_hasContacts:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _arg1;
_arg1 = data.readString();
java.lang.String _arg2;
_arg2 = data.readString();
boolean _result = this.hasContacts(_arg0, _arg1, _arg2);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.blackberry.dav.service.IDavCheckSettings
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
@Override public boolean hasCalendar(java.lang.String host, java.lang.String username, java.lang.String password) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(host);
_data.writeString(username);
_data.writeString(password);
mRemote.transact(Stub.TRANSACTION_hasCalendar, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public boolean hasContacts(java.lang.String host, java.lang.String username, java.lang.String password) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(host);
_data.writeString(username);
_data.writeString(password);
mRemote.transact(Stub.TRANSACTION_hasContacts, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_hasCalendar = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_hasContacts = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
}
public boolean hasCalendar(java.lang.String host, java.lang.String username, java.lang.String password) throws android.os.RemoteException;
public boolean hasContacts(java.lang.String host, java.lang.String username, java.lang.String password) throws android.os.RemoteException;
}
