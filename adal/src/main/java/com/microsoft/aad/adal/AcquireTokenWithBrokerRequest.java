//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.

package com.microsoft.aad.adal;

import android.accounts.Account;
import android.accounts.AccountManagerCallback;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;


/**
 * Internal class handling the logic for acquire token with Broker app(Either Company Portal or Azure Authenticator).
 * Including the logic for silent flow and interactive flow.
 */
public class AcquireTokenWithBrokerRequest {
    private static final String TAG = AcquireTokenWithBrokerRequest.class.getSimpleName();

    private final AuthenticationRequest mAuthRequest;
    private final IBrokerProxy mBrokerProxy;

    /**
     * Constructor for {@link AcquireTokenWithBrokerRequest}.
     */
    AcquireTokenWithBrokerRequest(final AuthenticationRequest authRequest,
                                  final IBrokerProxy brokerProxy) {
        mAuthRequest = authRequest;
        mBrokerProxy = brokerProxy;
    }

    /**
     * Call {@link android.accounts.AccountManager#getAuthToken(Account, String, Bundle, Activity,
     * AccountManagerCallback, Handler)} for silent request to broker.
     */
    AuthenticationResult acquireTokenWithBrokerSilent()
            throws AuthenticationException {

        mAuthRequest.setVersion(AuthenticationContext.getVersionName());
        mAuthRequest.setBrokerAccountName(mAuthRequest.getLoginHint());

        final AuthenticationResult authenticationResult;
        if (!StringExtensions.IsNullOrBlank(mAuthRequest.getBrokerAccountName()) || !StringExtensions
                .IsNullOrBlank(mAuthRequest.getUserId())) {
            Logger.v(TAG, "User is specified for background(silent) token request, trying to acquire token silently.");
            authenticationResult = mBrokerProxy.getAuthTokenInBackground(mAuthRequest);
        } else {
            Logger.v(TAG, "User is not specified, skipping background(silent) token request");
            authenticationResult = null;
        }

        return authenticationResult;
    }

    /**
     * Call {@link android.accounts.AccountManager#addAccount(String, String, String[], Bundle, Activity,
     * AccountManagerCallback, Handler)} for interactive request to broker.
     */
    AuthenticationResult acquireTokenWithBrokerInteractively(final IWindowComponent activity)
            throws AuthenticationException {
        Logger.v(TAG, "Launch activity for interactive authentication via broker.");

        final Intent brokerIntent = mBrokerProxy.getIntentForBrokerActivity(mAuthRequest);
        if (brokerIntent != null) {
                Logger.v(TAG, "Calling activity pid:" + android.os.Process.myPid()
                        + " tid:" + android.os.Process.myTid() + "uid:"
                        + android.os.Process.myUid());
                activity.startActivityForResult(brokerIntent,
                        AuthenticationConstants.UIRequest.BROWSER_FLOW);
        } else {
            throw new AuthenticationException(ADALError.DEVELOPER_ACTIVITY_IS_NOT_RESOLVED);
        }

        //It will start activity if callback is provided. Return null here.
        //activity onActivityResult will receive the result, and result will be sent back via callback.
        return null;
    }
}
