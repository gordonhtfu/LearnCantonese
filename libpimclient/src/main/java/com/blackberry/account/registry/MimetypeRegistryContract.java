package com.blackberry.account.registry;

import android.net.Uri;
import android.provider.BaseColumns;

/*
 * This will either be merged into lib.accounts or called directly from lib.accounts
 * so that providers plugging in only have to make one content provider call to make
 *
 * The tables "TemplateMapping" and "IconMapping" could be merged into one, where the
 * only thing to change would be the Projection that the client/provider specify, for
 * queries/inserts respectively. This needs to be thought about however since each
 * mimetype-accountId-templateId TemplateMapping should be unique, while there could be multiple
 * of these entries for the IconMapping table
 */

public class MimetypeRegistryContract {

    /**
     * This authority is used for writing to or querying from the mimetyperegistry
     * provider. Note: This is set at first run and cannot be changed without
     * breaking apps that access the provider.
     */
    public static String AUTHORITY = "com.blackberry.account.registry";

    /**
     * The content:// style URL for the top-level account authority
     */
    public static Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    protected interface TemplateMappingColumns extends BaseColumns {
        // The accountId for the associated mime-type to templateId mapping
        public static final String ACCOUNT_KEY = "account_id";
        // The mime-type that will be mapped
        public static final String MIME_TYPE = "mime_type";
        // The id of the template to display the data for a certain mime-type
        public static final String TEMPLATE_ID = "template_id";
    }

    public static final class TemplateMapping implements TemplateMappingColumns {
        public static final String TABLE_NAME = "TemplateMapping";
        public static final String URI_SUFFIX = "templatemapping";

        public static final int StandardItem = 0;
        public static final int ExpandableItem = 1;
        public static final int TwoIconItem = 2;

        public static final String[] DEFAULT_PROJECTION = {
            TemplateMappingColumns.ACCOUNT_KEY,
            TemplateMappingColumns.MIME_TYPE,
            TemplateMappingColumns.TEMPLATE_ID
        };

        public static final int DEFAULT_PROJECTION_ACCOUNT_KEY_COLUMN = 0;
        public static final int DEFAULT_PROJECTION_MIME_TYPE_COLUMN = 1;
        public static final int DEFAULT_PROJECTION_TEMPLATE_ID_COLUMN = 2;

        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + URI_SUFFIX);
    }

    protected interface DecorColumns extends BaseColumns {
        // The accountId for the associated mime-type to templateId mapping
        public static final String ACCOUNT_KEY = "account_id";
        // The mime-type that will be mapped
        public static final String MIME_TYPE = "mime_type";
        // The id of the template to display the data for a certain mime-type
        public static final String TEMPLATE_ID = "template_id";
        // Package Name to fetch the icon from
        public static final String PACKAGE_NAME = "package_name";
        // The position of the icon for the specified template defined
        public static final String ELEMENT_TYPE = "element_type";
        // The name of the icon resource to use/fetch using package manager
        public static final String ELEMENT_POSITION = "element_position";
        // the element style of this decor registry
        public static final String ELEMENT_STYLE = "element_style";
        public static final String ELEMENT_RESOURCE_ID = "element_resource_id";
        // The state for the icon to be used
        public static final String ITEM_STATE = "item_state";
    }

    public static final class DecorMapping implements DecorColumns {
        public static final String TABLE_NAME = "DecorMapping";
        public static final String URI_SUFFIX = "decormapping";

        public static final String[] DEFAULT_PROJECTION = {
            DecorMapping.ACCOUNT_KEY,
            DecorMapping.MIME_TYPE,
            DecorMapping.TEMPLATE_ID,
            DecorMapping.PACKAGE_NAME,
            DecorMapping.ELEMENT_TYPE,
            DecorMapping.ELEMENT_POSITION,
            DecorMapping.ELEMENT_STYLE,
            DecorMapping.ELEMENT_RESOURCE_ID,
            DecorMapping.ITEM_STATE
        };

        public static final int DEFAULT_PROJECTION_ACCOUNT_KEY_COLUMN = 0;
        public static final int DEFAULT_PROJECTION_MIME_TYPE_COLUMN = 1;
        public static final int DEFAULT_PROJECTION_TEMPLATE_ID_COLUMN = 2;
        public static final int DEFAULT_PROJECTION_PACKAGE_NAME = 3;
        public static final int DEFAULT_PROJECTION_ELEMENT_TYPE_COLUMN = 4;
        public static final int DEFAULT_PROJECTION_ELEMENT_POSITION_COLUMN = 5;
        public static final int DEFAULT_PROJECTION_ELEMENT_STYLE_COLUMN = 6;
        public static final int DEFAULT_PROJECTION_ELEMENT_RESOURCE_ID_COLUMN = 7;
        public static final int DEFAULT_PROJECTION_ITEM_STATE_COLUMN = 8;

        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + URI_SUFFIX);
    }
}
