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

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.Android.inventory.data.StoreContract.ItemEntry;

/**
 * Displays store inventory that has been entered and stored in the app.
 */
public class InventoryActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    // Identifies a particular Loader being used in this component
    private static final int URL_LOADER = 0;

    // Make CursorAdapter a global variable
    StoreCursorAdapter mStoreAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        setTitle(getString(R.string.app_label));

        // Setup FAB to open EditorActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(InventoryActivity.this, EditorActivity.class);
                startActivity(intent);
            }
        });

        // Find ListView to populate
        ListView storeView = (ListView) findViewById(R.id.list_view_item);
        // Find and set empty view on the ListView, so that it only shows when the list has 0 items.
        View emptyView = findViewById(R.id.empty_view);
        storeView.setEmptyView(emptyView);

        // Setup cursor adapter
        mStoreAdapter = new StoreCursorAdapter(this, null);
        // Attach cursor adapter to ListView
        storeView.setAdapter(mStoreAdapter);

        // Setup item click listener for the ListView
        storeView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Create new intent to go to (@link EditorActivity)
                Intent intent = new Intent(InventoryActivity.this, EditorActivity.class);

                //URI for item clicked
                Uri currentItemUri = ContentUris.withAppendedId(ItemEntry.CONTENT_URI, id);

                // Set URI on Intent data stream
                intent.setData(currentItemUri);

                startActivity(intent);
            }
        });

        // Start the loader
        getLoaderManager().initLoader(URL_LOADER, null, this);

    }

    /*
    * Callback that's invoked when the system has initialized the Loader and
    * is ready to start the query. This usually happens when initLoader() is
    * called. The loaderID argument contains the ID value passed to the
    * initLoader() call.
    */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        String[] projection = {
                ItemEntry._ID,
                ItemEntry.COLUMN_ITEM_NAME,
                ItemEntry.COLUMN_ITEM_DESC,
                ItemEntry.COLUMN_ITEM_QUANTITY,
                ItemEntry.COLUMN_ITEM_PRICE
        };
        /*
         * Takes action based on the ID of the Loader that's being created
         */
        switch (id) {
            case URL_LOADER:
                // Returns a new CursorLoader
                return new CursorLoader(
                        this,                   // Parent activity context
                        ItemEntry.CONTENT_URI,  // Table to query
                        projection,             // Projection to return
                        null,                   // No selection clause
                        null,                   // No selection arguments
                        null                    // Default sort order
                );
            default:
                // An invalid id was passed in
                return null;
        }
    }

    /*
     * Defines the callback that CursorLoader calls
     * when it's finished its query
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
    /*
     * Moves the query results into the adapter, causing the
     * ListView fronting this adapter to re-display
     */
        mStoreAdapter.swapCursor(cursor);
    }

    /*
     * Invoked when the CursorLoader is being reset. For example, this is
     * called if the data in the provider changes and the Cursor becomes stale.
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    /*
     * Clears out the adapter's reference to the Cursor.
     * This prevents memory leaks.
     */
        mStoreAdapter.swapCursor(null);
    }

    // method called from adapter for sold button onclick - decrements quantity by 1
    public void onClick(View v, Cursor cursor) {
        // reorient cursor after method call
        int position = (Integer) v.getTag();
        cursor.moveToPosition(position);

        // pull current quantity and name from row
        int quantity = cursor.getInt(cursor.getColumnIndexOrThrow(ItemEntry.COLUMN_ITEM_QUANTITY));
        String name = cursor.getString(cursor.getColumnIndexOrThrow(ItemEntry.COLUMN_ITEM_NAME));

        // if stock remains, decrement quantity by one
        if (quantity > 0) {
            quantity -= 1;
            position += 1;
            Uri currentItemUri = Uri.withAppendedPath(ItemEntry.CONTENT_URI, Integer.toString(position));
            ContentValues values = new ContentValues();
            values.put(ItemEntry.COLUMN_ITEM_QUANTITY, quantity);
            int rowsUpdated = getContentResolver().update(
                    currentItemUri,       // inventory table URI
                    values,               // columns to update
                    null,
                    null
            );

            // refresh cursor and view if row changed
            if (rowsUpdated == 1) {
                // Notify all listeners that the data has changed for the pet content URI
                getContentResolver().notifyChange(currentItemUri, null);
            }
        }

        // if no stock remains, throw a toast error to show item out of stock
        else {
            Toast.makeText(this, name + getString(R.string.out_of_stock), Toast.LENGTH_SHORT).show();
        }
    }
}
