package com.example.multichate.ui;

import android.Manifest;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;

import android.content.Intent;

import android.content.pm.PackageManager;
import android.database.Cursor;

import android.graphics.Typeface;

import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.ContactsContract;

import android.provider.Settings;
import android.support.v4.app.Fragment;


import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;


import com.example.multichate.MainActivity;
import com.example.multichate.R;
import com.example.multichate.data.FriendDB;
import com.example.multichate.data.StaticConfig;
import com.example.multichate.model.Friend;
import com.example.multichate.model.ListFriend;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class FriendsFragment extends Fragment {

    private RecyclerView recyclerListFriends;
    private ListFriendsAdapter listadapter;
    private ListFriend dataListFriend = null;
    private ArrayList<String> listFriendID = null;

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private CountDownTimer detectFriendOnline;
    public static int ACTION_START_CHAT = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dataListFriend = FriendDB.getInstance(getContext()).getListFriend();
        listadapter = new ListFriendsAdapter(getContext(), dataListFriend, this);
    }

    // Empty public constructor, required by the system
    public FriendsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the fragment layout


        View root = inflater.inflate(R.layout.fragment_people, container, false);
        recyclerListFriends = (RecyclerView) root.findViewById(R.id.recycleListFriend);
        recyclerListFriends.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerListFriends.setItemAnimator(new DefaultItemAnimator());

        recyclerListFriends.setAdapter(listadapter);
        listadapter.notifyDataSetChanged();

        return root;
    }

    // Request code for READ_CONTACTS. It can be any number > 0.



}
    class ListFriendsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private ListFriend listFriend;
        private Context context;
        public static Map<String, Query> mapQuery;
        public static Map<String, DatabaseReference> mapQueryOnline;
        public static Map<String, ChildEventListener> mapChildListener;
        public static Map<String, ChildEventListener> mapChildListenerOnline;
        public static Map<String, Boolean> mapMark;
        private FriendsFragment fragment;


        public ListFriendsAdapter(Context context, ListFriend listFriend, FriendsFragment fragment) {
            this.listFriend = listFriend;
            this.context = context;
            mapQuery = new HashMap<>();
            mapChildListener = new HashMap<>();
            mapMark = new HashMap<>();
            mapChildListenerOnline = new HashMap<>();
            mapQueryOnline = new HashMap<>();
            this.fragment = fragment;

        }


        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.rc_item_friend, parent, false);
            return new ItemFriendViewHolder(context, view);
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
            final String name = listFriend.getListFriend().get(position).name;
            final String id = listFriend.getListFriend().get(position).id;
            final String idRoom = listFriend.getListFriend().get(position).idRoom;
            final String phone = listFriend.getListFriend().get(position).phone;

            ((ItemFriendViewHolder) holder).txtName.setText(name);
            ((ItemFriendViewHolder) holder).txtName.setText(phone);

            ((View) ((ItemFriendViewHolder) holder).txtName.getParent().getParent())
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ((ItemFriendViewHolder) holder).txtMessage.setTypeface(Typeface.DEFAULT);
                            ((ItemFriendViewHolder) holder).txtName.setTypeface(Typeface.DEFAULT);
                            Intent intent = new Intent(context, ChatActivity.class);
                            intent.putExtra(StaticConfig.INTENT_KEY_CHAT_FRIEND, name);
                            ArrayList<CharSequence> idFriend = new ArrayList<CharSequence>();
                            idFriend.add(id);
                            intent.putCharSequenceArrayListExtra(StaticConfig.INTENT_KEY_CHAT_ID, idFriend);
                            intent.putExtra(StaticConfig.INTENT_KEY_CHAT_ROOM_ID, idRoom);


                            mapMark.put(id, null);
                            fragment.startActivityForResult(intent, FriendsFragment.ACTION_START_CHAT);
                        }
                    });


            if (listFriend.getListFriend().get(position).message.text.length() > 0) {
                ((ItemFriendViewHolder) holder).txtMessage.setVisibility(View.VISIBLE);

                if (!listFriend.getListFriend().get(position).message.text.startsWith(id)) {
                    ((ItemFriendViewHolder) holder).txtMessage.setText(listFriend.getListFriend().get(position).message.text);
                    ((ItemFriendViewHolder) holder).txtMessage.setTypeface(Typeface.DEFAULT);
                    ((ItemFriendViewHolder) holder).txtName.setTypeface(Typeface.DEFAULT);
                } else {
                    ((ItemFriendViewHolder) holder).txtMessage.setText(listFriend.getListFriend().get(position).message.text.substring((id + "").length()));
                    ((ItemFriendViewHolder) holder).txtMessage.setTypeface(Typeface.DEFAULT_BOLD);
                    ((ItemFriendViewHolder) holder).txtName.setTypeface(Typeface.DEFAULT_BOLD);
                }

                ((ItemFriendViewHolder) holder).txtphone.setText(listFriend.getListFriend().get(position).getPhone());

            } else {
                ((ItemFriendViewHolder) holder).txtMessage.setVisibility(View.GONE);

                if (mapQuery.get(id) == null && mapChildListener.get(id) == null) {
                    mapQuery.put(id, FirebaseDatabase.getInstance().getReference().child("message/" + idRoom).limitToLast(1));
                    mapChildListener.put(id, new ChildEventListener() {
                        @Override
                        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                            HashMap mapMessage = (HashMap) dataSnapshot.getValue();
                            if (mapMark.get(id) != null) {
                                if (!mapMark.get(id)) {
                                    listFriend.getListFriend().get(position).message.text = id + mapMessage.get("text");
                                } else {
                                    listFriend.getListFriend().get(position).message.text = (String) mapMessage.get("text");
                                }
                                notifyDataSetChanged();
                                mapMark.put(id, false);
                            } else {
                                listFriend.getListFriend().get(position).message.text = (String) mapMessage.get("text");
                                notifyDataSetChanged();
                            }
                            listFriend.getListFriend().get(position).message.timestamp = (long) mapMessage.get("timestamp");
                        }

                        @Override
                        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                        }

                        @Override
                        public void onChildRemoved(DataSnapshot dataSnapshot) {

                        }

                        @Override
                        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                    mapQuery.get(id).addChildEventListener(mapChildListener.get(id));
                    mapMark.put(id, true);
                } else {
                    mapQuery.get(id).removeEventListener(mapChildListener.get(id));
                    mapQuery.get(id).addChildEventListener(mapChildListener.get(id));
                    mapMark.put(id, true);
                }
            }


            if (mapQueryOnline.get(id) == null && mapChildListenerOnline.get(id) == null) {
                mapQueryOnline.put(id, FirebaseDatabase.getInstance().getReference().child("user/" + id + "/status"));
                mapChildListenerOnline.put(id, new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        if (dataSnapshot.getValue() != null && dataSnapshot.getKey().equals("isOnline")) {
                            Log.d("FriendsFragment add " + id, (boolean) dataSnapshot.getValue() + "");
                            listFriend.getListFriend().get(position).status.isOnline = (boolean) dataSnapshot.getValue();
                            notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                        if (dataSnapshot.getValue() != null && dataSnapshot.getKey().equals("isOnline")) {
                            Log.d("FriendsFragment change " + id, (boolean) dataSnapshot.getValue() + "");
                            listFriend.getListFriend().get(position).status.isOnline = (boolean) dataSnapshot.getValue();
                            notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
                mapQueryOnline.get(id).addChildEventListener(mapChildListenerOnline.get(id));
            }


        }

        @Override
        public int getItemCount() {
            return  listFriend.getListFriend().size();
        }
    }

    class ItemFriendViewHolder extends RecyclerView.ViewHolder {

        public TextView txtName, txtphone, txtMessage;
        private Context context;

        ItemFriendViewHolder(Context context, View itemView) {
            super(itemView);
            txtName = (TextView) itemView.findViewById(R.id.txtName);
            txtphone = (TextView) itemView.findViewById(R.id.txtphone);
            txtMessage = (TextView) itemView.findViewById(R.id.txtMessage);
            this.context = context;
        }
    }
