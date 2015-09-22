
package com.blackberry.widgets.tagview.contact.email;

import com.blackberry.widgets.tagview.contact.ContactRelatedAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author tallen
 *         <p/>
 *         The standard Adapter used by EmailTags for generating related items.
 * @see EmailTags
 */
public class EmailRelatedAdapter extends ContactRelatedAdapter {
    @Override
    protected List<Object> getSearchResults() {
        // TODO: This method should do something useful...

        List<Object> newRelatedItems = new ArrayList<Object>(0);
        // EmailContact contact;
        //
        // TagAdapter tagAdapter = getTagAdapter();
        //
        // if (tagAdapter != null) {
        // int maxToScan = Math.min(6, tagAdapter.getCount());
        // for (int i = 0; i < maxToScan; i += 1) {
        // contact = new EmailContact();
        // Object o = tagAdapter.getItem(i);
        // if (o instanceof EmailContact) {
        // EmailContact selectedContact = (EmailContact) o;
        // contact.setName(selectedContact.getName() + " RLTD");
        // EmailAddress selectedEmailAddress =
        // selectedContact.getActiveEmailAddress();
        // if (selectedEmailAddress != null) {
        // EmailAddress emailAddress = new EmailAddress();
        // emailAddress.setValue("RLTD" + selectedEmailAddress.getValue());
        // contact.getEmailAddresses().add(emailAddress);
        // contact.setActiveEmailAddressIndex(0);
        // }
        // } else {
        // contact.setName(o.toString());
        // }
        // newRelatedItems.add(contact);
        // }
        // }
        //
        // int fakeItemCounter = 1;
        // while (newRelatedItems.size() < 2) {
        // contact = new EmailContact();
        // contact.setName("Fake Item " + fakeItemCounter);
        // EmailAddress emailAddress = new EmailAddress();
        // emailAddress.setValue("fakeitem" + fakeItemCounter
        // + "@blackberry.com");
        // contact.getEmailAddresses().add(emailAddress);
        // contact.setActiveEmailAddressIndex(0);
        // newRelatedItems.add(contact);
        // fakeItemCounter += 1;
        // }
        //
        return newRelatedItems;
    }
}
