/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /Users/gofu/AndroidStudioProjects/CantoneseChallenge/libpimclient/src/main/aidl/com/blackberry/message/service/IMessagingService.aidl
 */
package com.blackberry.message.service;
public interface IMessagingService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.blackberry.message.service.IMessagingService
{
private static final java.lang.String DESCRIPTOR = "com.blackberry.message.service.IMessagingService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.blackberry.message.service.IMessagingService interface,
 * generating a proxy if needed.
 */
public static com.blackberry.message.service.IMessagingService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.blackberry.message.service.IMessagingService))) {
return ((com.blackberry.message.service.IMessagingService)iin);
}
return new com.blackberry.message.service.IMessagingService.Stub.Proxy(obj);
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
case TRANSACTION_getApiLevel:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getApiLevel();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_syncAccount:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
com.blackberry.message.service.ServiceResult _arg1;
_arg1 = new com.blackberry.message.service.ServiceResult();
this.syncAccount(_arg0, _arg1);
reply.writeNoException();
if ((_arg1!=null)) {
reply.writeInt(1);
_arg1.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_setOutOfOffice:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
java.lang.String _arg1;
_arg1 = data.readString();
com.blackberry.message.service.ServiceResult _arg2;
_arg2 = new com.blackberry.message.service.ServiceResult();
this.setOutOfOffice(_arg0, _arg1, _arg2);
reply.writeNoException();
if ((_arg2!=null)) {
reply.writeInt(1);
_arg2.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_createFolder:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
com.blackberry.message.service.FolderValue _arg1;
if ((0!=data.readInt())) {
_arg1 = com.blackberry.message.service.FolderValue.CREATOR.createFromParcel(data);
}
else {
_arg1 = null;
}
com.blackberry.message.service.ServiceResult _arg2;
_arg2 = new com.blackberry.message.service.ServiceResult();
java.lang.String _result = this.createFolder(_arg0, _arg1, _arg2);
reply.writeNoException();
reply.writeString(_result);
if ((_arg2!=null)) {
reply.writeInt(1);
_arg2.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_renameFolder:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
java.lang.String _arg1;
_arg1 = data.readString();
java.lang.String _arg2;
_arg2 = data.readString();
com.blackberry.message.service.ServiceResult _arg3;
_arg3 = new com.blackberry.message.service.ServiceResult();
this.renameFolder(_arg0, _arg1, _arg2, _arg3);
reply.writeNoException();
if ((_arg3!=null)) {
reply.writeInt(1);
_arg3.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_deleteFolder:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
java.lang.String _arg1;
_arg1 = data.readString();
com.blackberry.message.service.ServiceResult _arg2;
_arg2 = new com.blackberry.message.service.ServiceResult();
this.deleteFolder(_arg0, _arg1, _arg2);
reply.writeNoException();
if ((_arg2!=null)) {
reply.writeInt(1);
_arg2.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_setFolderSyncEnabled:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
java.lang.String _arg1;
_arg1 = data.readString();
boolean _arg2;
_arg2 = (0!=data.readInt());
com.blackberry.message.service.ServiceResult _arg3;
_arg3 = new com.blackberry.message.service.ServiceResult();
this.setFolderSyncEnabled(_arg0, _arg1, _arg2, _arg3);
reply.writeNoException();
if ((_arg3!=null)) {
reply.writeInt(1);
_arg3.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_fetchMore:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
java.lang.String _arg1;
_arg1 = data.readString();
com.blackberry.message.service.ServiceResult _arg2;
_arg2 = new com.blackberry.message.service.ServiceResult();
this.fetchMore(_arg0, _arg1, _arg2);
reply.writeNoException();
if ((_arg2!=null)) {
reply.writeInt(1);
_arg2.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_sendMessage:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
com.blackberry.message.service.MessageValue _arg1;
if ((0!=data.readInt())) {
_arg1 = com.blackberry.message.service.MessageValue.CREATOR.createFromParcel(data);
}
else {
_arg1 = null;
}
com.blackberry.message.service.ServiceResult _arg2;
_arg2 = new com.blackberry.message.service.ServiceResult();
java.lang.String _result = this.sendMessage(_arg0, _arg1, _arg2);
reply.writeNoException();
reply.writeString(_result);
if ((_arg2!=null)) {
reply.writeInt(1);
_arg2.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_saveMessage:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
com.blackberry.message.service.MessageValue _arg1;
if ((0!=data.readInt())) {
_arg1 = com.blackberry.message.service.MessageValue.CREATOR.createFromParcel(data);
}
else {
_arg1 = null;
}
com.blackberry.message.service.ServiceResult _arg2;
_arg2 = new com.blackberry.message.service.ServiceResult();
java.lang.String _result = this.saveMessage(_arg0, _arg1, _arg2);
reply.writeNoException();
reply.writeString(_result);
if ((_arg2!=null)) {
reply.writeInt(1);
_arg2.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_replyMessage:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
java.lang.String _arg1;
_arg1 = data.readString();
com.blackberry.message.service.MessageValue _arg2;
if ((0!=data.readInt())) {
_arg2 = com.blackberry.message.service.MessageValue.CREATOR.createFromParcel(data);
}
else {
_arg2 = null;
}
com.blackberry.message.service.ServiceResult _arg3;
_arg3 = new com.blackberry.message.service.ServiceResult();
java.lang.String _result = this.replyMessage(_arg0, _arg1, _arg2, _arg3);
reply.writeNoException();
reply.writeString(_result);
if ((_arg3!=null)) {
reply.writeInt(1);
_arg3.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_forwardMessage:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
java.lang.String _arg1;
_arg1 = data.readString();
com.blackberry.message.service.MessageValue _arg2;
if ((0!=data.readInt())) {
_arg2 = com.blackberry.message.service.MessageValue.CREATOR.createFromParcel(data);
}
else {
_arg2 = null;
}
com.blackberry.message.service.ServiceResult _arg3;
_arg3 = new com.blackberry.message.service.ServiceResult();
java.lang.String _result = this.forwardMessage(_arg0, _arg1, _arg2, _arg3);
reply.writeNoException();
reply.writeString(_result);
if ((_arg3!=null)) {
reply.writeInt(1);
_arg3.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_fileMessage:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
java.lang.String _arg1;
_arg1 = data.readString();
java.lang.String _arg2;
_arg2 = data.readString();
com.blackberry.message.service.ServiceResult _arg3;
_arg3 = new com.blackberry.message.service.ServiceResult();
this.fileMessage(_arg0, _arg1, _arg2, _arg3);
reply.writeNoException();
if ((_arg3!=null)) {
reply.writeInt(1);
_arg3.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_fileMessages:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
java.lang.String[] _arg1;
_arg1 = data.createStringArray();
java.lang.String _arg2;
_arg2 = data.readString();
com.blackberry.message.service.ServiceResult _arg3;
_arg3 = new com.blackberry.message.service.ServiceResult();
this.fileMessages(_arg0, _arg1, _arg2, _arg3);
reply.writeNoException();
if ((_arg3!=null)) {
reply.writeInt(1);
_arg3.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_downloadMessage:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
java.lang.String _arg1;
_arg1 = data.readString();
com.blackberry.message.service.ServiceResult _arg2;
_arg2 = new com.blackberry.message.service.ServiceResult();
this.downloadMessage(_arg0, _arg1, _arg2);
reply.writeNoException();
if ((_arg2!=null)) {
reply.writeInt(1);
_arg2.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_deleteMessage:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
java.lang.String _arg1;
_arg1 = data.readString();
com.blackberry.message.service.ServiceResult _arg2;
_arg2 = new com.blackberry.message.service.ServiceResult();
this.deleteMessage(_arg0, _arg1, _arg2);
reply.writeNoException();
if ((_arg2!=null)) {
reply.writeInt(1);
_arg2.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_prefetchMessage:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
java.lang.String _arg1;
_arg1 = data.readString();
com.blackberry.message.service.ServiceResult _arg2;
_arg2 = new com.blackberry.message.service.ServiceResult();
this.prefetchMessage(_arg0, _arg1, _arg2);
reply.writeNoException();
if ((_arg2!=null)) {
reply.writeInt(1);
_arg2.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_setMessageFlags:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
java.lang.String _arg1;
_arg1 = data.readString();
long _arg2;
_arg2 = data.readLong();
boolean _arg3;
_arg3 = (0!=data.readInt());
com.blackberry.message.service.ServiceResult _arg4;
_arg4 = new com.blackberry.message.service.ServiceResult();
this.setMessageFlags(_arg0, _arg1, _arg2, _arg3, _arg4);
reply.writeNoException();
if ((_arg4!=null)) {
reply.writeInt(1);
_arg4.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_clearMessageFlags:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
java.lang.String _arg1;
_arg1 = data.readString();
long _arg2;
_arg2 = data.readLong();
com.blackberry.message.service.ServiceResult _arg3;
_arg3 = new com.blackberry.message.service.ServiceResult();
this.clearMessageFlags(_arg0, _arg1, _arg2, _arg3);
reply.writeNoException();
if ((_arg3!=null)) {
reply.writeInt(1);
_arg3.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_setMessagePriority:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
java.lang.String _arg1;
_arg1 = data.readString();
int _arg2;
_arg2 = data.readInt();
com.blackberry.message.service.ServiceResult _arg3;
_arg3 = new com.blackberry.message.service.ServiceResult();
this.setMessagePriority(_arg0, _arg1, _arg2, _arg3);
reply.writeNoException();
if ((_arg3!=null)) {
reply.writeInt(1);
_arg3.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_downloadAttachment:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
java.lang.String _arg1;
_arg1 = data.readString();
com.blackberry.message.service.ServiceResult _arg2;
_arg2 = new com.blackberry.message.service.ServiceResult();
this.downloadAttachment(_arg0, _arg1, _arg2);
reply.writeNoException();
if ((_arg2!=null)) {
reply.writeInt(1);
_arg2.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_bulkActionMessage:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
com.blackberry.message.service.MessageFilter _arg1;
if ((0!=data.readInt())) {
_arg1 = com.blackberry.message.service.MessageFilter.CREATOR.createFromParcel(data);
}
else {
_arg1 = null;
}
int _arg2;
_arg2 = data.readInt();
com.blackberry.message.service.ServiceResult _arg3;
_arg3 = new com.blackberry.message.service.ServiceResult();
this.bulkActionMessage(_arg0, _arg1, _arg2, _arg3);
reply.writeNoException();
if ((_arg3!=null)) {
reply.writeInt(1);
_arg3.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_startRemoteSearch:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
com.blackberry.message.service.MessageFilter _arg1;
if ((0!=data.readInt())) {
_arg1 = com.blackberry.message.service.MessageFilter.CREATOR.createFromParcel(data);
}
else {
_arg1 = null;
}
com.blackberry.message.service.ServiceResult _arg2;
_arg2 = new com.blackberry.message.service.ServiceResult();
java.lang.String _result = this.startRemoteSearch(_arg0, _arg1, _arg2);
reply.writeNoException();
reply.writeString(_result);
if ((_arg2!=null)) {
reply.writeInt(1);
_arg2.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.blackberry.message.service.IMessagingService
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
@Override public int getApiLevel() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getApiLevel, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
// methods for API level 1
// account actions

@Override public void syncAccount(long accountId, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(accountId);
mRemote.transact(Stub.TRANSACTION_syncAccount, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
result.readFromParcel(_reply);
}
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void setOutOfOffice(long accountId, java.lang.String label, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(accountId);
_data.writeString(label);
mRemote.transact(Stub.TRANSACTION_setOutOfOffice, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
result.readFromParcel(_reply);
}
}
finally {
_reply.recycle();
_data.recycle();
}
}
// folder actions

@Override public java.lang.String createFolder(long accountId, com.blackberry.message.service.FolderValue folder, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(accountId);
if ((folder!=null)) {
_data.writeInt(1);
folder.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_createFolder, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
if ((0!=_reply.readInt())) {
result.readFromParcel(_reply);
}
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void renameFolder(long accountId, java.lang.String folderId, java.lang.String newName, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(accountId);
_data.writeString(folderId);
_data.writeString(newName);
mRemote.transact(Stub.TRANSACTION_renameFolder, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
result.readFromParcel(_reply);
}
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void deleteFolder(long accountId, java.lang.String folderId, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(accountId);
_data.writeString(folderId);
mRemote.transact(Stub.TRANSACTION_deleteFolder, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
result.readFromParcel(_reply);
}
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void setFolderSyncEnabled(long accountId, java.lang.String folderId, boolean syncEnabled, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(accountId);
_data.writeString(folderId);
_data.writeInt(((syncEnabled)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_setFolderSyncEnabled, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
result.readFromParcel(_reply);
}
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void fetchMore(long accountId, java.lang.String folderId, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(accountId);
_data.writeString(folderId);
mRemote.transact(Stub.TRANSACTION_fetchMore, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
result.readFromParcel(_reply);
}
}
finally {
_reply.recycle();
_data.recycle();
}
}
// message actions

@Override public java.lang.String sendMessage(long accountId, com.blackberry.message.service.MessageValue message, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(accountId);
if ((message!=null)) {
_data.writeInt(1);
message.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_sendMessage, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
if ((0!=_reply.readInt())) {
result.readFromParcel(_reply);
}
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public java.lang.String saveMessage(long accountId, com.blackberry.message.service.MessageValue message, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(accountId);
if ((message!=null)) {
_data.writeInt(1);
message.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_saveMessage, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
if ((0!=_reply.readInt())) {
result.readFromParcel(_reply);
}
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public java.lang.String replyMessage(long accountId, java.lang.String originalMessageId, com.blackberry.message.service.MessageValue message, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(accountId);
_data.writeString(originalMessageId);
if ((message!=null)) {
_data.writeInt(1);
message.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_replyMessage, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
if ((0!=_reply.readInt())) {
result.readFromParcel(_reply);
}
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public java.lang.String forwardMessage(long accountId, java.lang.String originalMessageId, com.blackberry.message.service.MessageValue message, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(accountId);
_data.writeString(originalMessageId);
if ((message!=null)) {
_data.writeInt(1);
message.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_forwardMessage, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
if ((0!=_reply.readInt())) {
result.readFromParcel(_reply);
}
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void fileMessage(long accountId, java.lang.String messageId, java.lang.String destFolderId, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(accountId);
_data.writeString(messageId);
_data.writeString(destFolderId);
mRemote.transact(Stub.TRANSACTION_fileMessage, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
result.readFromParcel(_reply);
}
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void fileMessages(long accountId, java.lang.String[] messageId, java.lang.String destFolderId, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(accountId);
_data.writeStringArray(messageId);
_data.writeString(destFolderId);
mRemote.transact(Stub.TRANSACTION_fileMessages, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
result.readFromParcel(_reply);
}
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void downloadMessage(long accountId, java.lang.String messageId, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(accountId);
_data.writeString(messageId);
mRemote.transact(Stub.TRANSACTION_downloadMessage, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
result.readFromParcel(_reply);
}
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void deleteMessage(long accountId, java.lang.String messageId, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(accountId);
_data.writeString(messageId);
mRemote.transact(Stub.TRANSACTION_deleteMessage, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
result.readFromParcel(_reply);
}
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void prefetchMessage(long accountId, java.lang.String messageId, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(accountId);
_data.writeString(messageId);
mRemote.transact(Stub.TRANSACTION_prefetchMessage, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
result.readFromParcel(_reply);
}
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void setMessageFlags(long accountId, java.lang.String messageId, long flagsMask, boolean replace, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(accountId);
_data.writeString(messageId);
_data.writeLong(flagsMask);
_data.writeInt(((replace)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_setMessageFlags, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
result.readFromParcel(_reply);
}
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void clearMessageFlags(long accountId, java.lang.String messageId, long flagsMask, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(accountId);
_data.writeString(messageId);
_data.writeLong(flagsMask);
mRemote.transact(Stub.TRANSACTION_clearMessageFlags, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
result.readFromParcel(_reply);
}
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void setMessagePriority(long accountId, java.lang.String messageId, int priority, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(accountId);
_data.writeString(messageId);
_data.writeInt(priority);
mRemote.transact(Stub.TRANSACTION_setMessagePriority, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
result.readFromParcel(_reply);
}
}
finally {
_reply.recycle();
_data.recycle();
}
}
// attachment actions

@Override public void downloadAttachment(long accountId, java.lang.String attachmentId, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(accountId);
_data.writeString(attachmentId);
mRemote.transact(Stub.TRANSACTION_downloadAttachment, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
result.readFromParcel(_reply);
}
}
finally {
_reply.recycle();
_data.recycle();
}
}
// auxiliary actions

@Override public void bulkActionMessage(long accountId, com.blackberry.message.service.MessageFilter filter, int action, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(accountId);
if ((filter!=null)) {
_data.writeInt(1);
filter.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
_data.writeInt(action);
mRemote.transact(Stub.TRANSACTION_bulkActionMessage, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
result.readFromParcel(_reply);
}
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public java.lang.String startRemoteSearch(long accountId, com.blackberry.message.service.MessageFilter filter, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(accountId);
if ((filter!=null)) {
_data.writeInt(1);
filter.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_startRemoteSearch, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
if ((0!=_reply.readInt())) {
result.readFromParcel(_reply);
}
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_getApiLevel = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_syncAccount = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_setOutOfOffice = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_createFolder = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_renameFolder = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_deleteFolder = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_setFolderSyncEnabled = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
static final int TRANSACTION_fetchMore = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
static final int TRANSACTION_sendMessage = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
static final int TRANSACTION_saveMessage = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
static final int TRANSACTION_replyMessage = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
static final int TRANSACTION_forwardMessage = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
static final int TRANSACTION_fileMessage = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
static final int TRANSACTION_fileMessages = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
static final int TRANSACTION_downloadMessage = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
static final int TRANSACTION_deleteMessage = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
static final int TRANSACTION_prefetchMessage = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
static final int TRANSACTION_setMessageFlags = (android.os.IBinder.FIRST_CALL_TRANSACTION + 17);
static final int TRANSACTION_clearMessageFlags = (android.os.IBinder.FIRST_CALL_TRANSACTION + 18);
static final int TRANSACTION_setMessagePriority = (android.os.IBinder.FIRST_CALL_TRANSACTION + 19);
static final int TRANSACTION_downloadAttachment = (android.os.IBinder.FIRST_CALL_TRANSACTION + 20);
static final int TRANSACTION_bulkActionMessage = (android.os.IBinder.FIRST_CALL_TRANSACTION + 21);
static final int TRANSACTION_startRemoteSearch = (android.os.IBinder.FIRST_CALL_TRANSACTION + 22);
}
public int getApiLevel() throws android.os.RemoteException;
// methods for API level 1
// account actions

public void syncAccount(long accountId, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException;
public void setOutOfOffice(long accountId, java.lang.String label, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException;
// folder actions

public java.lang.String createFolder(long accountId, com.blackberry.message.service.FolderValue folder, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException;
public void renameFolder(long accountId, java.lang.String folderId, java.lang.String newName, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException;
public void deleteFolder(long accountId, java.lang.String folderId, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException;
public void setFolderSyncEnabled(long accountId, java.lang.String folderId, boolean syncEnabled, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException;
public void fetchMore(long accountId, java.lang.String folderId, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException;
// message actions

public java.lang.String sendMessage(long accountId, com.blackberry.message.service.MessageValue message, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException;
public java.lang.String saveMessage(long accountId, com.blackberry.message.service.MessageValue message, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException;
public java.lang.String replyMessage(long accountId, java.lang.String originalMessageId, com.blackberry.message.service.MessageValue message, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException;
public java.lang.String forwardMessage(long accountId, java.lang.String originalMessageId, com.blackberry.message.service.MessageValue message, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException;
public void fileMessage(long accountId, java.lang.String messageId, java.lang.String destFolderId, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException;
public void fileMessages(long accountId, java.lang.String[] messageId, java.lang.String destFolderId, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException;
public void downloadMessage(long accountId, java.lang.String messageId, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException;
public void deleteMessage(long accountId, java.lang.String messageId, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException;
public void prefetchMessage(long accountId, java.lang.String messageId, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException;
public void setMessageFlags(long accountId, java.lang.String messageId, long flagsMask, boolean replace, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException;
public void clearMessageFlags(long accountId, java.lang.String messageId, long flagsMask, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException;
public void setMessagePriority(long accountId, java.lang.String messageId, int priority, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException;
// attachment actions

public void downloadAttachment(long accountId, java.lang.String attachmentId, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException;
// auxiliary actions

public void bulkActionMessage(long accountId, com.blackberry.message.service.MessageFilter filter, int action, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException;
public java.lang.String startRemoteSearch(long accountId, com.blackberry.message.service.MessageFilter filter, com.blackberry.message.service.ServiceResult result) throws android.os.RemoteException;
}
