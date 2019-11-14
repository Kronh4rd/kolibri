package de.leonlatsch.olivia.main.fragment;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import de.leonlatsch.olivia.R;
import de.leonlatsch.olivia.chat.ChatActivity;
import de.leonlatsch.olivia.constants.Values;
import de.leonlatsch.olivia.database.interfaces.ChatInterface;
import de.leonlatsch.olivia.database.interfaces.ContactInterface;
import de.leonlatsch.olivia.database.model.Chat;
import de.leonlatsch.olivia.main.MainActivity;
import de.leonlatsch.olivia.main.UserSearchActivity;
import de.leonlatsch.olivia.main.adapter.ChatListAdapter;

public class ChatFragment extends Fragment {

    private MainActivity parent;
    private View view;
    private ListView listView;
    private ChatListAdapter chatListAdapter;

    private ChatInterface chatInterface;
    private ContactInterface contactInterface;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view =  inflater.inflate(R.layout.fragment_chats, container, false);
        parent = (MainActivity) getActivity();
        chatInterface = ChatInterface.getInstance();
        contactInterface = ContactInterface.getInstance();

        listView = view.findViewById(R.id.fragment_chat_list_view);
        chatListAdapter = new ChatListAdapter(parent, chatInterface.getALl());
        listView.setAdapter(chatListAdapter);
        listView.setOnItemClickListener(itemClickListener);

        FloatingActionButton newChatFab = view.findViewById(R.id.newChatFab);
        newChatFab.setOnClickListener(v -> newChat());

        return view;
    }

    private AdapterView.OnItemClickListener itemClickListener = (parent, view, position, id) -> {
        Object raw = listView.getItemAtPosition(position);
        if (raw instanceof Chat) {
            Chat chat = (Chat) raw;
            Intent intent = new Intent(this.parent.getApplicationContext(), ChatActivity.class);
            intent.putExtra(Values.INTENT_KEY_CHAT_UID, chat.getUid());
            startActivity(intent);
        }
    };

    private void newChat() {
        Intent intent = new Intent(parent.getApplicationContext(), UserSearchActivity.class);
        startActivity(intent);
    }
}
