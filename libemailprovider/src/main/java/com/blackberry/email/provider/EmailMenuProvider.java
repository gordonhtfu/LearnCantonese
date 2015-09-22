
package com.blackberry.email.provider;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.blackberry.lib.emailprovider.R;
import com.blackberry.menu.MenuItemDetails;
import com.blackberry.menu.MenuProvider;
import com.blackberry.menu.MenuRegistrar;
import com.blackberry.menu.RequestedItem;
import com.blackberry.menu.MenuContract.Actions;
import com.blackberry.provider.MessageContract.Message.State;
import android.view.MenuItem;
import com.blackberry.intent.PimIntent;

import java.util.ArrayList;
import java.util.EnumSet;

/**
 * EmailMenuProvider class provides integration with the MenuService component
 */
public class EmailMenuProvider extends MenuProvider {

    private static final String TAG = EmailMenuProvider.class.getSimpleName();

    protected static String AUTHORITY = "com.blackberry.email.menu.provider";

    protected static Uri REGISTRATION_URI = Uri.parse("content://" + AUTHORITY);

    private enum MenuActionCategory {
        // Not all bits are expected to be set for all types of email
        // messages, so instead of assuming the default values for those
        // bits, we choose to not process certain bits at all.
        // These categories group related bit states into one.
        CATEGORY_FILE, CATEGORY_FLAG, CATEGORY_READ, CATEGORY_PRIORITY,
        CATEGORY_RESPONSE
    }

    /**
     * The following method will ensure that the EmaiMenulProvider is registered
     * with the MenuService
     * 
     * @param context
     */
    public static void initialize(Context context) {
        // Note that this EmailProvider has many mimetypes, for now will focus
        // on the Messages
        // but in future we may need more register calls if we need menus for
        // the other types (folders for example)
        // MenuRegistrar.registerGuestProvider(context,
        // context.getString(R.string.message_mimetype),
        // EmailMenuProvider.REGISTRATION_URI);
        MenuRegistrar.registerOwnerProvider(context, context.getString(R.string.message_mimetype),
                EmailMenuProvider.REGISTRATION_URI);
    }

    /**
     * The following method will provide all cleanup/unregistering logic for the
     * EmailMenuProivder
     * 
     * @param context
     */
    public static void terminate(Context context) {
        MenuRegistrar.deregisterProvider(context, context.getString(R.string.message_mimetype),
                EmailMenuProvider.REGISTRATION_URI);
    }

    @Override
    protected ArrayList<MenuItemDetails> getMenuItems(ArrayList<RequestedItem> requestedItems,
            int arg1, GuestMenuProviders guestMenuProviders) {
        // @TODO build the MenuItemDetails based on the RequestItem - Note calls
        // into the EmailProvider may
        // be required if there is not enough data contained in RequestedItem -
        // WE NEED TO WATCH HOW MUCH TIME
        // WE SPEND IN HERE AS IT WILL CAUSE A DELAY IN THE UI
        ArrayList<MenuItemDetails> returnMenuItems = new ArrayList<MenuItemDetails>();

        for (RequestedItem item : requestedItems) {
            // For now, just supporting a single first item
            String mimeType = item.getMime();
            if (mimeType.equals(getContext().getString(R.string.message_mimetype))) {
                returnMenuItems = getMenuItemsMessages(item);
            }
            break;
        }
        return returnMenuItems;
    }

    /**
     * The following method will provide all menus for Email mimetype based on
     * the state
     */
    private ArrayList<MenuItemDetails> getMenuItemsMessages(RequestedItem item) {
        long state = item.getState();
        String packageName = getContext().getPackageName();
        String mimeType = item.getMime();
        Uri itemUri = item.getItem();

        ArrayList<MenuItemDetails> messageMenuItems = new ArrayList<MenuItemDetails>();
        Intent intent = new Intent();
        intent.setType(mimeType);
        if (itemUri != null) {
            intent.setData(itemUri);
        }

        // Add menus handled by email message service
        intent.setComponent(new ComponentName(getContext().getPackageName(),
                "com.blackberry.email.service.EmailMessagingService"));

        MenuItemDetails menuItemDetails;
        EnumSet<MenuActionCategory> applicableCategories = getActionCategoriesForState(state);

        if (applicableCategories.contains(MenuActionCategory.CATEGORY_FILE)) {
            intent.setAction(PimIntent.PIM_MESSAGE_ACTION_FILE);
            menuItemDetails = new MenuItemDetails(intent, Actions.ACTION_FILE,
                    packageName, R.string.file, R.drawable.ic_menu_move_to_holo_light);
            menuItemDetails.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            messageMenuItems.add(menuItemDetails);
        }

        if (applicableCategories.contains(MenuActionCategory.CATEGORY_FLAG)) {
            if ((state & State.FLAGGED) == 0) {
                intent.setAction(PimIntent.PIM_MESSAGE_ACTION_FLAG);
                messageMenuItems.add(new MenuItemDetails(intent, Actions.ACTION_FLAG, packageName,
                        R.string.flag, R.drawable.ic_menu_add_star_holo_dark));
            } else {
                intent.setAction(PimIntent.PIM_MESSAGE_ACTION_CLEAR_FLAG);
                messageMenuItems.add(new MenuItemDetails(intent, Actions.ACTION_CLEAR_FLAG,
                        packageName, R.string.clear_flag, R.drawable.ic_menu_add_star_holo_dark));
            }
        }

        if (applicableCategories.contains(MenuActionCategory.CATEGORY_READ)) {
            if ((state & State.UNREAD) != 0) {
                intent.setAction(PimIntent.PIM_MESSAGE_ACTION_MARK_READ);
                messageMenuItems.add(new MenuItemDetails(intent, Actions.ACTION_MARK_AS_READ,
                        packageName, R.string.mark_read, R.drawable.ic_menu_mark_read_holo_dark));
            } else {
                intent.setAction(PimIntent.PIM_MESSAGE_ACTION_MARK_UNREAD);
                messageMenuItems.add(new MenuItemDetails(intent, Actions.ACTION_MARK_AS_UNREAD,
                        packageName, R.string.mark_unread, R.drawable.ic_menu_mark_read_holo_dark));
            }
        }

        if (applicableCategories.contains(MenuActionCategory.CATEGORY_PRIORITY)) {
            if ((state & State.PRIORITY) == 0) {
                intent.setAction(PimIntent.PIM_MESSAGE_ACTION_ADD_PRIORITY);
                messageMenuItems.add(new MenuItemDetails(intent, Actions.ACTION_ADD_PRIORITY,
                        packageName, R.string.add_priority, R.drawable.ic_menu_add_star_holo_dark));
            } else {
                intent.setAction(PimIntent.PIM_MESSAGE_ACTION_REMOVE_PRIORITY);
                messageMenuItems.add(new MenuItemDetails(intent, Actions.ACTION_REMOVE_PRIORITY,
                        packageName, R.string.remove_priority, R.drawable.ic_menu_add_star_holo_dark));
            }
        }

        if (applicableCategories.contains(MenuActionCategory.CATEGORY_RESPONSE)) {
            // Add reply, reply all and forward for compose activity
            Intent composeActivityIntent = intent.cloneFilter();
            composeActivityIntent.setComponent(new ComponentName(getContext().getPackageName(),
                    "com.blackberry.email.ui.compose.controllers.ComposeActivity"));
            // TODO: to be removed after menu service change is ready to handle
            // activity vs service
            composeActivityIntent.addCategory("activity");

            // Reply menu only if the message is not a sent message
            if ((state & State.SENT) == 0) {
                composeActivityIntent.setAction(PimIntent.PIM_MESSAGE_ACTION_REPLY);
                messageMenuItems.add(new MenuItemDetails(composeActivityIntent, Actions.ACTION_REPLY,
                       packageName, R.string.reply, R.drawable.ic_menu_add_star_holo_dark));
            }

            composeActivityIntent.setAction(PimIntent.PIM_MESSAGE_ACTION_REPLY_ALL);
            messageMenuItems.add(new MenuItemDetails(composeActivityIntent, Actions.ACTION_REPLY_ALL,
                    packageName, R.string.reply_all, R.drawable.ic_menu_add_star_holo_dark));

            composeActivityIntent.setAction(PimIntent.PIM_MESSAGE_ACTION_FORWARD);
            messageMenuItems.add(new MenuItemDetails(composeActivityIntent, Actions.ACTION_FORWARD,
                    packageName, R.string.forward, R.drawable.ic_menu_add_star_holo_dark));
          }

          // All email items should be deletable.
          intent.setAction(PimIntent.PIM_ITEM_ACTION_DELETE);
          menuItemDetails = new MenuItemDetails(intent, Actions.ACTION_DELETE, packageName,
                  R.string.delete, R.drawable.ic_menu_trash_holo_light);
          menuItemDetails.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
          messageMenuItems.add(menuItemDetails);

        return messageMenuItems;
    }

    private EnumSet<MenuActionCategory> getActionCategoriesForState(long state) {
        EnumSet<MenuActionCategory> applicableCategories = EnumSet.noneOf(MenuActionCategory.class);

        if ((state & State.DRAFT) > 0) {
            applicableCategories.add(MenuActionCategory.CATEGORY_FLAG);
        } else {
            // Process all menu categories if this is not a draft.
            applicableCategories.add(MenuActionCategory.CATEGORY_FLAG);
            applicableCategories.add(MenuActionCategory.CATEGORY_READ);
            applicableCategories.add(MenuActionCategory.CATEGORY_FILE);
            applicableCategories.add(MenuActionCategory.CATEGORY_PRIORITY);
            applicableCategories.add(MenuActionCategory.CATEGORY_RESPONSE);
         }
        return applicableCategories;
    }
}
