package com.examples.contacts;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SimpleCursorAdapter;


public class ContactsEditActivity extends ListActivity implements AdapterView.OnItemClickListener, DialogInterface.OnClickListener {

    private static final String TEST_EMAIL = "tester@email.com";
    
    private Cursor mContacts, mEmail;
    private int selectedContactId;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Return all contacts, ordered by name
        String[] projection = new String[] { ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME };
        //List only contacts visible to the user
        mContacts = managedQuery(ContactsContract.Contacts.CONTENT_URI,
                projection, ContactsContract.Contacts.IN_VISIBLE_GROUP+" = 1",
                null, ContactsContract.Contacts.DISPLAY_NAME);

        // Display all contacts in a ListView
        SimpleCursorAdapter mAdapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_list_item_1, mContacts,
                new String[] { ContactsContract.Contacts.DISPLAY_NAME },
                new int[] { android.R.id.text1 });
        setListAdapter(mAdapter);
        // Listen for item selections
        getListView().setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        if (mContacts.moveToPosition(position)) {
            selectedContactId = mContacts.getInt(0); // _ID column
            // Gather email data from email table
            String[] projection = new String[] { ContactsContract.Data._ID,
                    ContactsContract.CommonDataKinds.Email.DATA };
            mEmail = getContentResolver().query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    projection,
                    ContactsContract.Data.CONTACT_ID + " = " + selectedContactId, null, null);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Email Addresses");
            builder.setCursor(mEmail, this, ContactsContract.CommonDataKinds.Email.DATA);
            builder.setPositiveButton("Add", this);
            builder.setNegativeButton("Cancel", null);
            builder.create().show();
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        //Data must be associated with a RAW contact, retrieve the first raw ID
        Cursor raw = getContentResolver().query(
                ContactsContract.RawContacts.CONTENT_URI,
                new String[] { ContactsContract.Contacts._ID },
                ContactsContract.Data.CONTACT_ID + " = " + selectedContactId, null, null);
        if(!raw.moveToFirst()) {
            return;
        }

        int rawContactId = raw.getInt(0);
        ContentValues values = new ContentValues();
        switch(which) {
        case DialogInterface.BUTTON_POSITIVE:
            //User wants to add a new email
            values.put(ContactsContract.CommonDataKinds.Email.RAW_CONTACT_ID, rawContactId);
            values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
            values.put(ContactsContract.CommonDataKinds.Email.DATA, TEST_EMAIL);
            values.put(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_OTHER);
            getContentResolver().insert(ContactsContract.Data.CONTENT_URI, values);
            break;
        default:
            //User wants to edit selection
            values.put(ContactsContract.CommonDataKinds.Email.DATA, TEST_EMAIL);
            values.put(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_OTHER);
            getContentResolver().update(ContactsContract.Data.CONTENT_URI, values,
                    ContactsContract.Data._ID+" = "+mEmail.getInt(0), null);
            break;
        }
        
        //Don't need the email cursor anymore
        mEmail.close();
    }
}