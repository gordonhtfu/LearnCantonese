
package com.blackberry.email.utils;

import com.blackberry.email.mail.Address;
import com.blackberry.message.service.MessageContactValue;
import com.blackberry.provider.MessageContract;

import java.util.ArrayList;
import java.util.List;

/**
 * Class provides several utility methods allowing for easier integration
 * between Email specific contact list items and the MessageProvider API's
 */
public class EmailMessageContactUtilities {

    /**
     * Converts a packed address list into a list of MessageContacts
     * 
     * @param packgedAddressList
     * @param fieldType
     * @return
     */
    public static ArrayList<MessageContactValue> convertPackgedAddressList(String packgedAddressList,
            int fieldType) {

        ArrayList<MessageContactValue> msgContacts = null;
        if (packgedAddressList != null && packgedAddressList.length() > 0) {
            Address[] addresses = Address.unpack(packgedAddressList);
            msgContacts = convertAddresses(addresses, fieldType);
        } else {
            msgContacts = new ArrayList<MessageContactValue>();
        }

        return msgContacts;
    }

    /**
     * Converts a List of the Email Address objects to the MessageProvider
     * MessageContact objects
     * 
     * @param addresses
     * @param fieldType
     * @return a converted list of MessageContacts
     */
    public static ArrayList<MessageContactValue> convertAddresses(Address[] addresses, int fieldType) {
        ArrayList<MessageContactValue> msgContacts = new ArrayList<MessageContactValue>();
        if (addresses != null && addresses.length > 0) {
            for (int x = 0; x < addresses.length; x++) {

                String name = addresses[x].getPersonal();
                MessageContactValue msgContact = new MessageContactValue();
                msgContact.mName = name != null ? name : "";
                msgContact.mAddress = addresses[x].getAddress();
                msgContact.mAddressType = MessageContract.MessageContact.AddrType.EMAIL;
                msgContact.mFieldType = fieldType;

                msgContacts.add(msgContact);
            }
        }

        return msgContacts;
    }

    /**
     * Converts MessageContact into and Array of Address objects
     * 
     * @param contacts
     * @return
     */
    public static Address[] convertMessageContact(List<MessageContactValue> contacts) {
        Address[] addresses = new Address[contacts.size()];

        MessageContactValue currentContact = null;
        for (int x = 0; x < contacts.size(); x++) {
            currentContact = contacts.get(x);
            addresses[x] = new Address(currentContact.mAddress, currentContact.mName);
        }
        return addresses;
    }

    /**
     * @param contacts
     * @return
     */
    public static String convertMessageContract(List<MessageContactValue> contacts) {
        return Address.pack(convertMessageContact(contacts));
    }
}
