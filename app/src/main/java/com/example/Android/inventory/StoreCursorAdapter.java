package com.example.Android.inventory;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.example.Android.inventory.data.StoreContract.ItemEntry;


/**
 * {@link StoreCursorAdapter} is an adapter for a list or grid view
 * that uses a {@link Cursor} of store inventory data as its data source. This adapter knows
 * how to create list items for each row of inventory data in the {@link Cursor}.
 */
public class StoreCursorAdapter extends CursorAdapter {

    private Context mContext;

    /**
     * Constructs a new {@link StoreCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    public StoreCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
        this.mContext = context;
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method binds the inventory data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current item can be set on the name TextView
     * in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {
        // Find fields to populate in inflated template
        TextView nameView = (TextView) view.findViewById(R.id.name);
        TextView subView = (TextView) view.findViewById(R.id.quan_price);
        // Extract properties from cursor
        String name = cursor.getString(cursor.getColumnIndexOrThrow(ItemEntry.COLUMN_ITEM_NAME));
        int price = cursor.getInt(cursor.getColumnIndexOrThrow(ItemEntry.COLUMN_ITEM_PRICE));
        int quantity = cursor.getInt(cursor.getColumnIndexOrThrow(ItemEntry.COLUMN_ITEM_QUANTITY));

        // Populate fields with extracted properties
        nameView.setText(name);
        subView.setText(context.getString(R.string.category_quantity)
                + ": " + quantity
                + "       "
                + context.getString(R.string.unit_item_price)
                + price
                + " "
                + context.getString(R.string.category_price));

        // Sold button - on click runs method back in activity to decrement quantity
        Button soldBtn = (Button) view.findViewById(R.id.sold);
        soldBtn.setTag(cursor.getPosition());
        soldBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mContext instanceof InventoryActivity) {
                    ((InventoryActivity) mContext).onClick(v, cursor);
                }
            }
        });
    }
}