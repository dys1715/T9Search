package cn.tinkling.t9search.sample;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.dys.t9search.R;
import com.nabinbhandari.android.permissions.PermissionHandler;
import com.nabinbhandari.android.permissions.Permissions;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    static List<Contact> mContactsAll;
    static ContactsAdapter mContactsAdapter;
    T9Filter mT9Filter;
    static ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermission();

        mContactsAdapter = new ContactsAdapter();
        mT9Filter = new T9Filter();

        ListView listView = findViewById(R.id.listView);
        listView.setAdapter(mContactsAdapter);

        //textWatcher
        ((EditText) findViewById(R.id.editText)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mT9Filter.filter(s);
            }
        });

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("Loading...");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
    }

    private void checkPermission() {
        String[] permissions = new String[]{Manifest.permission.READ_CONTACTS};
        Permissions.Options options = new Permissions.Options()
                .setRationaleDialogTitle("Info")
                .setSettingsDialogTitle("Warning");
        Permissions.check(this, permissions, "Please provide permissions", options,
                new PermissionHandler() {
                    @Override
                    public void onGranted() {
                        Log.e("permission","onGranted");
                        //加载联系人
                        new MyTask(getContentResolver()).execute();
                    }

                    @Override
                    public void onDenied(Context context, ArrayList<String> deniedPermissions) {
                        super.onDenied(context, deniedPermissions);
                        Log.e("permission","onDenied");
                    }
                });
    }

    private static class MyTask extends AsyncTask<Void, Void, List<Contact>> {
        private final ContentResolver contentResolver;

        public MyTask(ContentResolver contentResolver) {
            this.contentResolver = contentResolver;
        }

        @Override
        protected void onPreExecute() {
            mProgressDialog.show();
        }

        @Override
        protected List<Contact> doInBackground(Void... params) {
            Cursor cursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER},
                    null, null,
                    ContactsContract.CommonDataKinds.Phone.SORT_KEY_PRIMARY);

            List<Contact> contacts = new ArrayList<>();
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(0);
                    String number = cursor.getString(1).replaceAll("[ \\(\\)-]+", "");
                    //生成T9Key
                    String t9Key = T9SearchSupport.buildT9Key(name);
                    contacts.add(new Contact(name, number, t9Key));
                }
                cursor.close();
            }

            return contacts;
        }

        @Override
        protected void onPostExecute(List<Contact> contacts) {
            mContactsAll = contacts;
            mContactsAdapter.setContacts(contacts);
            if (mProgressDialog.isShowing())
                mProgressDialog.dismiss();
        }
    }

    public void onDestroy() {
        if (mProgressDialog.isShowing())
            mProgressDialog.dismiss();
        super.onDestroy();
    }

    private static class ViewHolder {
        public TextView name;
        public TextView phoneNumber;

        ViewHolder(View itemView) {
            name = itemView.findViewById(R.id.name);
            phoneNumber = itemView.findViewById(R.id.phoneNumber);
        }
    }

    private class ContactsAdapter extends BaseAdapter {

        private final SpannableStringBuilder mHighLightBuffer = new SpannableStringBuilder();

        private final List<Contact> mContacts = new ArrayList<>();

        public void setContacts(List<Contact> contacts) {
            mContacts.clear();
            if (contacts != null)
                mContacts.addAll(contacts);

            notifyDataSetInvalidated();
        }

        @Override
        public int getCount() {
            return mContacts.size();
        }

        @Override
        public Contact getItem(int position) {
            return mContacts.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.contact_list_item,
                        parent, false);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            Contact contact = mContacts.get(position);

            holder.name.setText(T9SearchSupport.highLight(mHighLightBuffer, contact.nameMatchInfo,
                    contact.name, 0xFFFF4081));
            holder.phoneNumber.setText(T9SearchSupport.highLight(mHighLightBuffer,
                    contact.phoneNumberMatchInfo, contact.phoneNumber, 0xAFFF4081));

            return convertView;
        }
    }

    private static class T9Filter extends Filter {
        //执行筛选
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Contact> list = TextUtils.isEmpty(constraint)
                    ? mContactsAll : T9SearchSupport.filter(mContactsAll, constraint.toString());

            FilterResults results = new FilterResults();
            if (list != null) {
                results.count = list.size();
                results.values = list;
            } else {
                results.count = 0;
                results.values = null;
            }
            return results;
        }

        //筛选结果
        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mContactsAdapter.setContacts((List<Contact>) results.values);
        }

    }

}
