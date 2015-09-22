
package com.blackberry.widgets.smartintentchooser;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.provider.Telephony;
import android.util.Log;

import com.blackberry.widgets.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An {@link ActionFilter} which returns only those actions which are acceptable
 * to be displayed in the HUB Compose Disambiguation screen.
 */
public class HubComposeFilter implements ActionFilter {
    private static final List<String> mComponentWhitelist = new ArrayList<String>(
            Arrays.asList(new String[] {
                    // email intent
                    "com.blackberry.emailservices/com.blackberry.email.ui.compose.controllers.ComposeActivity"
            }));
    private Context mContext;

    /**
     * @param context The context.
     */
    public HubComposeFilter(Context context) {
        mContext = context;
    }

    @Override
    public List<ActionDetails> filterActions(List<ActionDetails> input, Intent originalIntent) {
        List<ActionDetails> result = new ArrayList<ActionDetails>(input.size());

        // create a mutable list so we can add components to it if needed
        List<String> componentWhitelist = new ArrayList<String>(mComponentWhitelist);

        // figure out which sms app we should be keeping.
        ResolveInfo resolveInfo = getSmsResolveInfo();
        if (resolveInfo != null) {
            ComponentName componentName = new ComponentName(resolveInfo.activityInfo.packageName,
                    resolveInfo.activityInfo.name);
            String component = componentName.flattenToString();
            // we need to do something special with the display name for SMS, so
            // we need a custom ResolveInfoActionDetails. Find it and replace
            // it.
            boolean isSet = false;
            for (int i = 0; i < input.size(); i += 1) {
                ActionDetails actionDetails = input.get(i);
                if (component.equals(actionDetails.getIntent(originalIntent).getComponent()
                        .flattenToString())) {
                    // we only add the component whitelist if we have it
                    // injected into the input array.
                    componentWhitelist.add(component);
                    input.set(i, new SmsActionDetails(resolveInfo));
                    isSet = true;
                    break;
                }
            }
            // Hangouts is stupid and wont match because it uses a different
            // activity from PM query results for SEND than it does for SENDTO
            // (which is how we get the default SMS app). For this case just put
            // Text Messages at the top. This isn't Hangout-specific so if a
            // third party app does something similar it will fall back to this
            // case too.
            if (!isSet) {
                result.add(new SmsActionDetails(resolveInfo));
            }
        }

        for (ActionDetails actionDetails : input) {
            if (componentWhitelist.contains(actionDetails.getIntent(originalIntent).getComponent()
                    .flattenToString())) {
                result.add(actionDetails);
            }
        }
        return result;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private ResolveInfo getSmsResolveInfo() {
        Intent smsIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("sms:"));
        if (Build.VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
            List<ResolveInfo> resolveInfos = mContext.getPackageManager().queryIntentActivities(
                    smsIntent, 0);
            String defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(mContext);
            for (ResolveInfo resolveInfo : resolveInfos) {
                if (resolveInfo.activityInfo.packageName.equals(defaultSmsPackage)) {
                    return resolveInfo;
                }
            }
        } else {
            // Pre-KitKat doesn't have a "default" SMS app. Do our best to find
            // one.
            ResolveInfo resolveInfo = mContext.getPackageManager().resolveActivity(smsIntent, 0);
            // if no default has been set, according to Google a resolveInfo
            // pointing to an internal android class is returned.
            // http://stackoverflow.com/questions/8626421/get-preferred-default-app-on-android
            if (resolveInfo.activityInfo.packageName.equals("android")) {
                // no default one set to handle SMS compose. Grab the first one
                // in the list? Sure!
                List<ResolveInfo> resolveInfos = mContext.getPackageManager()
                        .queryIntentActivities(
                                smsIntent, 0);
                if (resolveInfos.size() > 0) {
                    return resolveInfos.get(0);
                }
            } else {
                return resolveInfo;
            }
        }
        return null;
    }

    private static final class SmsActionDetails extends ResolveInfoActionDetails {

        public SmsActionDetails(ResolveInfo resolveInfo) {
            super(resolveInfo);
        }

        @Override
        public CharSequence getTitle(Context context) {
            return context.getResources().getText(R.string.text_messages);
        }

        @Override
        public CharSequence getSubtitle(Context context) {
            // get the title (which should be the app display name) to display
            // as the subtitle.
            return super.getTitle(context);
        }

        @Override
        public Intent getIntent(Intent originalIntent) {
            // Hangouts (again) is weird and needs it to be SENDTO and a Uri of
            // "sms:" or it doesn't go directly to the compose page. So overrode
            // these for this Intent. It works for other SMS apps too so no need
            // for custom logic just for Hangouts.
            Intent intent = super.getIntent(originalIntent);
            intent.setAction(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("sms:"));
            return intent;
        }
    }
}
