/*
 * 				Twidere - Twitter client for Android
 * 
 *  Copyright (C) 2012-2014 Mariotaku Lee <mariotaku.lee@gmail.com>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mariotaku.twidere.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import org.mariotaku.twidere.Constants;
import org.mariotaku.twidere.R;
import org.mariotaku.twidere.adapter.iface.IContentCardAdapter;
import org.mariotaku.twidere.app.TwidereApplication;
import org.mariotaku.twidere.provider.TwidereDataStore.DirectMessages.ConversationEntries;
import org.mariotaku.twidere.util.AsyncTwitterWrapper;
import org.mariotaku.twidere.util.ImageLoaderWrapper;
import org.mariotaku.twidere.util.ImageLoadingHandler;
import org.mariotaku.twidere.util.MultiSelectManager;
import org.mariotaku.twidere.util.SharedPreferencesWrapper;
import org.mariotaku.twidere.util.Utils;
import org.mariotaku.twidere.view.holder.LoadIndicatorViewHolder;
import org.mariotaku.twidere.view.holder.MessageEntryViewHolder;

public class MessageEntriesAdapter extends Adapter<ViewHolder> implements Constants, IContentCardAdapter, OnClickListener {

    private final Context mContext;
    private final LayoutInflater mInflater;
    private final ImageLoaderWrapper mImageLoader;
    private final MultiSelectManager mMultiSelectManager;
    private final boolean mNicknameOnly;
    private boolean mLoadMoreIndicatorEnabled;
    private final int mTextSize;
    private final int mProfileImageStyle;
    private final int mMediaPreviewStyle;
    private Cursor mCursor;
    private MessageEntriesAdapterListener mListener;

    public Context getContext() {
        return mContext;
    }

    @Override
    public ImageLoadingHandler getImageLoadingHandler() {
        return null;
    }

    @Override
    public int getProfileImageStyle() {
        return mProfileImageStyle;
    }

    @Override
    public int getMediaPreviewStyle() {
        return mMediaPreviewStyle;
    }

    @Override
    public AsyncTwitterWrapper getTwitterWrapper() {
        return null;
    }

    @Override
    public float getTextSize() {
        return mTextSize;
    }

    @Override
    public void setLoadMoreIndicatorEnabled(boolean enabled) {
        if (mLoadMoreIndicatorEnabled == enabled) return;
        mLoadMoreIndicatorEnabled = enabled;
        notifyDataSetChanged();
    }

    @Override
    public boolean hasLoadMoreIndicator() {
        return mLoadMoreIndicatorEnabled;
    }


    public ImageLoaderWrapper getImageLoader() {
        return mImageLoader;
    }

    @Override
    public boolean isGapItem(int position) {
        return false;
    }

    @Override
    public void onGapClick(ViewHolder holder, int position) {

    }

    public boolean isNicknameOnly() {
        return mNicknameOnly;
    }

    public static final int ITEM_VIEW_TYPE_MESSAGE = 0;
    public static final int ITEM_VIEW_TYPE_LOAD_INDICATOR = 1;

    @Override
    public int getItemViewType(int position) {
        if (position == getMessagesCount()) {
            return ITEM_VIEW_TYPE_LOAD_INDICATOR;
        }
        return ITEM_VIEW_TYPE_MESSAGE;
    }

    private int getMessagesCount() {
        final Cursor c = mCursor;
        if (c == null || c.isClosed()) return 0;
        return c.getCount();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case ITEM_VIEW_TYPE_MESSAGE: {
                final View view = mInflater.inflate(R.layout.list_item_message_entry, parent, false);
                return new MessageEntryViewHolder(this, view);
            }
            case ITEM_VIEW_TYPE_LOAD_INDICATOR: {
                final View view = mInflater.inflate(R.layout.card_item_load_indicator, parent, false);
                return new LoadIndicatorViewHolder(view);
            }
        }
        throw new IllegalStateException("Unknown view type " + viewType);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case ITEM_VIEW_TYPE_MESSAGE: {
                final Cursor c = mCursor;
                c.moveToPosition(position);
                ((MessageEntryViewHolder) holder).displayMessage(c);
                break;
            }
        }
    }

    @Override
    public void onItemActionClick(ViewHolder holder, int id, int position) {

    }

    @Override
    public void onItemMenuClick(ViewHolder holder, View menuView, int position) {

    }

    public void onMessageClick(int position) {
        if (mListener == null) return;
        mListener.onEntryClick(position, getEntry(position));
    }

    public void setCursor(Cursor cursor) {
        mCursor = cursor;
        notifyDataSetChanged();
    }

    @Override
    public final int getItemCount() {
        return getMessagesCount() + (mLoadMoreIndicatorEnabled ? 1 : 0);
    }

    public MessageEntriesAdapter(final Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        final TwidereApplication app = TwidereApplication.getInstance(context);
        mMultiSelectManager = app.getMultiSelectManager();
        mImageLoader = app.getImageLoaderWrapper();
        final SharedPreferencesWrapper preferences = SharedPreferencesWrapper.getInstance(context,
                SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        mProfileImageStyle = Utils.getProfileImageStyle(preferences.getString(KEY_PROFILE_IMAGE_STYLE, null));
        mMediaPreviewStyle = Utils.getMediaPreviewStyle(preferences.getString(KEY_MEDIA_PREVIEW_STYLE, null));
        mTextSize = preferences.getInt(KEY_TEXT_SIZE, context.getResources().getInteger(R.integer.default_text_size));
        mNicknameOnly = preferences.getBoolean(KEY_NICKNAME_ONLY, false);
    }

    public static class DirectMessageEntry {

        public final long account_id, conversation_id;
        public final String screen_name, name;

        DirectMessageEntry(Cursor cursor) {
            account_id = cursor.getLong(ConversationEntries.IDX_ACCOUNT_ID);
            conversation_id = cursor.getLong(ConversationEntries.IDX_CONVERSATION_ID);
            screen_name = cursor.getString(ConversationEntries.IDX_SCREEN_NAME);
            name = cursor.getString(ConversationEntries.IDX_NAME);
        }

    }

    public DirectMessageEntry getEntry(final int position) {
        final Cursor c = mCursor;
        if (c == null || c.isClosed() || !c.moveToPosition(position)) return null;
        return new DirectMessageEntry(c);
    }

    @Override
    public void onClick(final View view) {
//        if (mMultiSelectManager.isActive()) return;
//        final Object tag = view.getTag();
//        final int position = tag instanceof Integer ? (Integer) tag : -1;
//        if (position == -1) return;
//        switch (view.getId()) {
//            case R.id.profile_image: {
//                if (mContext instanceof Activity) {
//                    final long account_id = getAccountId(position);
//                    final long user_id = getConversationId(position);
//                    final String screen_name = getScreenName(position);
//                    openUserProfile(mContext, account_id, user_id, screen_name, null);
//                }
//                break;
//            }
//        }
    }

    public void setListener(MessageEntriesAdapterListener listener) {
        mListener = listener;
    }

    public interface MessageEntriesAdapterListener {
        public void onEntryClick(int position, DirectMessageEntry entry);
    }

}
