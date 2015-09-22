
package com.blackberry.datagraph.provider;

import com.blackberry.provider.ListItemContract;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * The contract between Data Graph provider and applications.
 */
public class DataGraphContract {

    /**
     * Authority for the Data Graph provider.
     */
    public static final String AUTHORITY = "com.blackberry.datagraph.provider";

    /**
     * Notification authority for the Data Graph provider.
     */
    public static final String NOTIFY_AUTHORITY = "com.blackberry.datagraph.notifier";
    
    public static final String ENTITY_URI_SUFFIX = "entity";
    
    public static final String LINK_URI_SUFFIX = "link";

    /**
     * URI at which Entities (List Items) for a profile may be accessed.
     */
    public static final Uri ENTITY_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/"
            + ENTITY_URI_SUFFIX);
    
    /**
     * URI at which Links for a profile may be accessed.
     */
    public static final Uri LINK_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/"
            + LINK_URI_SUFFIX);
    
    
    public interface EntityColumns extends ListItemContract.ListItemColumns {
        
        /**
         * A list of Entity URIs.  This column can be used in two ways:
         * 
         * Querying:
         * Each item in the ensuing result set should be linked to each Entity specified in this 
         * column.  
         * To specify that the entities in the result set should be linked to multiple
         * other entities, delimit the URIs of those entities with the '^' character.
         * To specify that the entities in the result set need only be linked to one of any of the
         * specified entities, delimit the URIs of those entities with the '|' character.
         * 
         * Inserting:
         * The specified Entity should be linked with the Entities specified in this column.
         * Delimit multiple entities with the '^' character.
         * Note that if this column is specified on insert, the LINKED_ENTITY_TYPES column must be
         * specified as well.
         * 
         * Updating:
         * When specifying this column during an update, additional links to the specified entities
         * will be created in exactly the same manner as an insert.  Already established links for
         * this entity will not be affected.
         * 
         * Deleting:
         * This field should not be present in the selection clause of a delete
         */
        public static final String LINKED_ENTITY_URIS = "linked_entity_uris";

        /**
         * A comma-separated list of Link Types.  This column can be used in two ways:
         * 
         * Querying: 
         * If this field is left blank, the ensuing result set should be linked to each Entity specified 
         * in the LINKED_ENTITY_URIs is any way.
         * If this field contains only one type, the ensuing result set should be linked to each Entity
         * as specified by this type.
         * If this field contains more than one type, the ensuring result set should be linked to each
         * Entity based on the link type specified in the same ordinal position 
         * 
         * Inserting: 
         * Supply the comma-delimited list of Link Types that should be used to create the Links 
         * with the specified Entity URIs.  If only one type of Link Type is specified, it will be 
         * used for all Links
         * 
         * Updating:
         * When specifying this column during an update, additional links to the specified entities
         * will be created in exactly the same manner as an insert.  Already established links for
         * this entity will not be affected.
         * 
         * Deleting:
         * This field should not be present in the selection clause of a delete
         */
        public static final String LINKED_ENTITY_TYPES = "linked_entity_types";
    }
    
    public interface LinkColumns extends BaseColumns {
        /**
         * The URI of the Entity to be linked from
         */
        public static final String FROM_ENTITY_URI = "from_entity_uri";

        /**
         * The URI of the Entity to be linked to
         */
        public static final String TO_ENTITY_URI = "to_entity_uri";
        
        /**
         * The type of link
         * 
         * Acceptable link types are listed in the LinkTypes interface
         */
        public static final String LINK_TYPE = "link_type";
        
        /**
         * Full projection of available fields for a Link
         */
        public static final String[] PROJ_FULL = {
                BaseColumns._ID,
                FROM_ENTITY_URI,
                TO_ENTITY_URI,
                LINK_TYPE
        };
    }
    
    public interface EntityMimeTypes {
        /**
         * The mime type of an email
         */
        public static final String EMAIL = "application/vnd.blackberry.email";
        
        /**
         * The mime type of an email conversation
         */
        public static final String EMAIL_CONVERSATION = "application/vnd.blackberry.conversation.email";
        
        /**
         * The mime type of an sms conversation
         */
        public static final String SMS_CONVERSATION = "application/vnd.blackberry.conversation.sms";
        
        /**
         * The mime type of an email folder
         */
        public static final String EMAIL_FOLDER = "application/vnd.blackberry.folder.email";
    }
    
    public interface LinkTypes {
        /**
         * The link type for specifying that the 'from' entity is contained in the 'to' entity
         */
        public static final int CONTAINED_IN = 0;
        
        /**
         * The link type for specifying that the 'from' entity contains the 'to' entity
         */
        public static final int CONTAINS = 1;
        
        /**
         * The link type for specifying that the 'from' entity is included in the 'to' entity
         */
        public static final int INCLUDED_IN = 2;
        
        /**
         * The link type for specifying that the 'from' entity includes the 'to' entity
         */
        public static final int INCLUDES = 3;
    }
}
