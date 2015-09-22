/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blackberry.email.account.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.blackberry.email.account.SecurityPolicy;
import com.blackberry.email.provider.contract.Policy;
import com.blackberry.email.service.IPolicyService;
import com.blackberry.common.utils.LogTag;
import com.blackberry.common.utils.LogUtils;

public class PolicyService extends Service {
    private static final String LOG_TAG = LogTag.getLogTag();

    private SecurityPolicy mSecurityPolicy;
    private Context mContext;

    private final IPolicyService.Stub mBinder = new IPolicyService.Stub() {
        @Override
        public boolean isActive(Policy policy) {
            try {
                return mSecurityPolicy.isActive(policy);
            } catch (RuntimeException e) {
                // Catch, log and rethrow the exception, as otherwise when the exception is
                // ultimately handled, the complete stack trace is losk
                LogUtils.e(LOG_TAG, e, "Exception thrown during call to SecurityPolicy#isActive");
                throw e;
            }
        }

        @Override
        public void setAccountHoldFlag(long accountId, boolean newState) {
            SecurityPolicy.setAccountHoldFlag(mContext, accountId, newState);
        }

        @Override
        public void remoteWipe() {
            try {
                mSecurityPolicy.remoteWipe();
            } catch (RuntimeException e) {
                // Catch, log and rethrow the exception, as otherwise when the exception is
                // ultimately handled, the complete stack trace is losk
                LogUtils.e(LOG_TAG, e, "Exception thrown during call to SecurityPolicy#remoteWipe");
                throw e;
            }
        }

        @Override
        public void setAccountPolicy(long accountId, Policy policy, String securityKey) {
            try {
                mSecurityPolicy.setAccountPolicy(accountId, policy, securityKey);
            } catch (RuntimeException e) {
                // Catch, log and rethrow the exception, as otherwise when the exception is
                // ultimately handled, the complete stack trace is losk
                LogUtils.e(LOG_TAG, e,
                        "Exception thrown from call to SecurityPolicy#setAccountPolicy");
                throw e;
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        // When we bind this service, save the context and SecurityPolicy singleton
        mContext = this;
        mSecurityPolicy = SecurityPolicy.getInstance(this);
        return mBinder;
    }
}