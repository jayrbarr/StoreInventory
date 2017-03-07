/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.example.Android.inventory;

import android.Manifest;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.Android.inventory.data.StoreContract.ItemEntry;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;

import static com.example.Android.inventory.R.id.imgView;
import static com.example.Android.inventory.data.DbBitmapUtility.getBytes;
import static com.example.Android.inventory.data.DbBitmapUtility.getImage;

/**
 * Allows user to create a new item or edit an existing one.
 */
public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    // Identifies a particular Loader being used in this component
    private static final int EXISTING_ITEM_LOADER = 1;
    private final static int RESULT_LOAD_IMG = 1;
    // constants for permissions
    private final static int MY_PERMISSIONS_READ_EXTERNAL_STORAGE = 20666;
    /**
     * EditText field to enter the item's name
     */
    private EditText mNameEditText;
    /**
     * EditText field to enter the item's description
     */
    private EditText mDescEditText;
    /**
     * EditText field to enter the item's price
     */
    private EditText mPriceEditText;
    /**
     * EditText field to enter the item's quantity
     */
    private EditText mQuantityEditText;
    /**
     * ImageView field to capture the item's image
     */
    private ImageView mImageView;
    /**
     * Content URI for the existing item (null if it's a new item)
     */
    private Uri mCurrentItemUri;
    private boolean mItemHasChanged = false;
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mItemHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // New item or editing existing?
        Intent intent = getIntent();
        mCurrentItemUri = intent.getData();


        if (mCurrentItemUri == null) {
            // New Item
            setTitle(getString(R.string.editor_activity_title_new_item));
            // Invalidate the options menu and order button, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete an item or order product that hasn't been created yet.)
            invalidateOptionsMenu();
            Button buttonView = (Button) findViewById(R.id.order_button);
            buttonView.setVisibility(View.INVISIBLE);
        } else {
            // Existing Item - editing
            setTitle(getString(R.string.editor_activity_title_edit_item));
            getLoaderManager().initLoader(EXISTING_ITEM_LOADER, null, this);
        }

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_item_name);
        mDescEditText = (EditText) findViewById(R.id.edit_item_desc);
        mPriceEditText = (EditText) findViewById(R.id.edit_item_price);
        mQuantityEditText = (EditText) findViewById(R.id.edit_item_quantity);
        mImageView = (ImageView) findViewById(imgView);

        mNameEditText.setOnTouchListener(mTouchListener);
        mDescEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);

        // Set OnClickListener for order button
        Button buttonView = (Button) findViewById(R.id.order_button);
        buttonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent send = new Intent(Intent.ACTION_SENDTO);
                String uriText = "mailto:" + Uri.encode(getString(R.string.order_email))
                        + "?subject=" + Uri.encode(getString(R.string.order_subject))
                        + "&body=" + Uri.encode(getString(R.string.order_intro) + mNameEditText.getText().toString().trim() + ".");
                Uri uri = Uri.parse(uriText);
                send.setData(uri);
                startActivity(Intent.createChooser(send, getString(R.string.send_order)));
            }
        });

        // Set OnClickListener for increment button
        Button incrementView = (Button) findViewById(R.id.price_increment);
        incrementView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String quantityString = mQuantityEditText.getText().toString().trim();
                int quantity = 0;
                if (!TextUtils.isEmpty(quantityString)) {
                    quantity = Integer.parseInt(quantityString);
                }
                quantity += 1;
                mQuantityEditText.setText(Integer.toString(quantity));
                mItemHasChanged = true;
            }
        });

        // Set OnClickListener for decrement button
        Button decrementView = (Button) findViewById(R.id.price_decrement);
        decrementView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String quantityString = mQuantityEditText.getText().toString().trim();
                int quantity = 0;
                if (!TextUtils.isEmpty(quantityString)) {
                    quantity = Integer.parseInt(quantityString);
                }
                if (quantity != 0) {
                    quantity -= 1;
                }
                mQuantityEditText.setText(Integer.toString(quantity));
                mItemHasChanged = true;
            }
        });

        // Set onClickListener for Load Image button - runs loadImageFromGallery method
        Button loadImageView = (Button) findViewById(R.id.buttonLoadImage);
        loadImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadImageFromGallery();
            }
        });
    }

    // method run when Load Image button clicked. Starts intent to open gallery activity for result
    private void loadImageFromGallery() {
        // check for permissions
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // request permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_READ_EXTERNAL_STORAGE);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_READ_EXTERNAL_STORAGE);
        }
    }

    // once permissions are granted or confirmed, start the intent to open image app such as Gallery, Google Photos
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Create intent to open image application such as Gallery, Google Photos
                    Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
                }
                return;
            }
        }
    }

    // method to receive result from gallery activity after load image button clicked
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // when an image is picked
        if (requestCode == RESULT_LOAD_IMG && resultCode == RESULT_OK && data != null) {
            decodeUri(data.getData());
        }
    }

    // method to decode URI path of image
    public void decodeUri(Uri uri) {
        ParcelFileDescriptor parcelFD = null;
        try {
            parcelFD = getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor imageSource = parcelFD.getFileDescriptor();

            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFileDescriptor(imageSource, null, o);

            // the new size we want to scale to
            final int REQUIRED_SIZE = 1024;

            // Find the correct scale value. It should be the power of 2.
            int width_tmp = o.outWidth, height_tmp = o.outHeight;
            int scale = 1;
            while (true) {
                if (width_tmp < REQUIRED_SIZE && height_tmp < REQUIRED_SIZE) {
                    break;
                }
                width_tmp /= 2;
                height_tmp /= 2;
                scale *= 2;
            }

            // decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            Bitmap bitmap = BitmapFactory.decodeFileDescriptor(imageSource, null, o2);

            mImageView.setImageBitmap(bitmap);

        } catch (FileNotFoundException e) {
            Toast.makeText(this, getString(R.string.throw_error),
                    Toast.LENGTH_LONG).show();
        } finally {
            if (parcelFD != null)
                try {
                    parcelFD.close();
                } catch (IOException e) {
                    // ignored
                }
        }
    }

    /**
     * Get user input from editor and save new item into database.
     */
    private void saveItem() {
        // Read from input fields
        // Use trim to eliminate leading or trailing white space
        String nameString = mNameEditText.getText().toString().trim();
        String descString = mDescEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();
        String quantityString = mQuantityEditText.getText().toString().trim();
        Drawable imageDrawable = mImageView.getDrawable();

        // check to see if a new item was created by add on Inventory Activity
        // but no edits were made to any fields
        if (mCurrentItemUri == null &&
                TextUtils.isEmpty(nameString) && TextUtils.isEmpty(descString) &&
                TextUtils.isEmpty(priceString) && TextUtils.isEmpty(quantityString) &&
                imageDrawable == null) {
            finish();
            return;
        }

        // check to see if fields are empty. All fields are required.)
        if (TextUtils.isEmpty(nameString) ||
                TextUtils.isEmpty(descString) || TextUtils.isEmpty(priceString) ||
                TextUtils.isEmpty(quantityString) || imageDrawable == null) {
            // if name field empty, send Toast message and return to editor screen
            Toast.makeText(this, getString(R.string.required_fields),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a ContentValues object where column names are the keys,
        // and item attributes from the editor are the values.
        ContentValues values = new ContentValues();
        values.put(ItemEntry.COLUMN_ITEM_NAME, nameString);
        values.put(ItemEntry.COLUMN_ITEM_DESC, descString);
        values.put(ItemEntry.COLUMN_ITEM_PRICE, Integer.parseInt(priceString));
        values.put(ItemEntry.COLUMN_ITEM_QUANTITY, Integer.parseInt(quantityString));

        // Convert drawable to bitmap then to byte array before saving in database
        Bitmap image = ((BitmapDrawable) imageDrawable).getBitmap();
        byte[] imageArray = getBytes(image);
        values.put(ItemEntry.COLUMN_IMAGE, imageArray);

        boolean itemSaved = false;

        if (mCurrentItemUri == null) {
            // Insert a new item into the provider, returning the content URI for the new item.
            Uri itemUri = getContentResolver().insert(ItemEntry.CONTENT_URI, values);
            if (itemUri != null) {
                itemSaved = true;
            }
        } else {
            // Existing Item - edit item in provider list, returning content URI for existing item.
            int rowsUpdated = getContentResolver().update(
                    mCurrentItemUri,       // inventory table URI
                    values,               // columns to update
                    null,
                    null
            );
            if (rowsUpdated == 1) {
                itemSaved = true;
            }
        }

        // Show a toast message depending on whether or not the insertion was successful
        if (!itemSaved) {
            // If the new content URI is null, then there was an error with insertion.
            Toast.makeText(this, getString(R.string.editor_insert_item_failed),
                    Toast.LENGTH_SHORT).show();
        } else {
            // Otherwise, the insertion was successful and we can display a toast.
            Toast.makeText(this, getString(R.string.editor_insert_item_successful),
                    Toast.LENGTH_SHORT).show();
        }
        // Exit activity
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save item to database
                saveItem();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Open Delete dialog
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the item hasn't changed, continue with navigating up to parent activity
                // which is the {@link InventoryActivity}.
                if (!mItemHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Since the editor shows all item attributes, define a projection that contains
        // all columns from the inventory table
        String[] projection = {
                ItemEntry._ID,
                ItemEntry.COLUMN_ITEM_NAME,
                ItemEntry.COLUMN_ITEM_DESC,
                ItemEntry.COLUMN_ITEM_PRICE,
                ItemEntry.COLUMN_ITEM_QUANTITY,
                ItemEntry.COLUMN_IMAGE
        };

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mCurrentItemUri,         // Query the content URI for the current item
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {
            // Find the columns of item attributes that we're interested in
            int nameColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_NAME);
            int breedColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_DESC);
            int priceColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_PRICE);
            int quantityColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_QUANTITY);
            int imageColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_IMAGE);

            // Extract out the value from the Cursor for the given column index
            String name = cursor.getString(nameColumnIndex);
            String breed = cursor.getString(breedColumnIndex);
            Integer price = cursor.getInt(priceColumnIndex);
            Integer quantity = cursor.getInt(quantityColumnIndex);
            byte[] imageArray = cursor.getBlob(imageColumnIndex);

            Bitmap image = null;
            if (imageArray != null) {
                // convert byte array to image
                image = getImage(imageArray);
            }

            // Update the views on the screen with the values from the database
            mNameEditText.setText(name);
            mDescEditText.setText(breed);
            mPriceEditText.setText(Integer.toString(price));
            mQuantityEditText.setText(Integer.toString(quantity));
            mImageView.setImageBitmap(image);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCurrentItemUri = null;
        loader.abandon();
    }

    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the item.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onBackPressed() {
        // If the item hasn't changed, continue with handling back button press
        if (!mItemHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new item, hide the "Delete" menu item.
        if (mCurrentItemUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the item.
                deleteItem();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the item.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the item in the database.
     */
    private void deleteItem() {
        if (mCurrentItemUri != null) {
            // Deletes the item that matches the current URI
            int mRowsDeleted = getContentResolver().delete(
                    mCurrentItemUri,      // current item URI
                    null,                // no selection clause/args needed since URI specifies row already
                    null
            );
            // Show a toast message depending on whether or not the deletion was successful
            if (mRowsDeleted == 0) {
                // If no rows deleted, then delete operation failed.
                Toast.makeText(this, getString(R.string.editor_delete_item_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the deletion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_delete_item_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }
        finish();
    }
}