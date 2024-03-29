package de.ccc.events.postixdroid.ui;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.core.app.NavUtils;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import de.ccc.events.postixdroid.R;
import de.ccc.events.postixdroid.check.CheckException;
import de.ccc.events.postixdroid.check.OnlineCheckProvider;
import de.ccc.events.postixdroid.check.TicketCheckProvider;

public class SearchActivity extends AppCompatActivity {

    private int loading = 0;
    private EditText etQuery;
    private ListView lvResults;
    private TicketCheckProvider checkProvider;
    private ProgressDialog pdCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkProvider = new OnlineCheckProvider(this);

        setContentView(R.layout.activity_search);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toggleLoadingIndicator();

        lvResults = (ListView) findViewById(R.id.lvResults);
        etQuery = (EditText) findViewById(R.id.etQuery);
        etQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                startSearch(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        lvResults.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                TicketCheckProvider.SearchResult item = (TicketCheckProvider.SearchResult) adapterView.getAdapter().getItem(i);

                new CheckTask().execute(item.getSecret());
            }
        });
    }

    private void startSearch(String query) {
        if (query.length() < 4) {
            SearchResultAdapter adapter = new SearchResultAdapter(
                    this, R.layout.listitem_searchresult, R.id.tvAttendeeName,
                    new ArrayList<TicketCheckProvider.SearchResult>());
            lvResults.setAdapter(adapter);
            return;
        }
        new SearchTask().execute(query);
    }

    public class SearchTask extends AsyncTask<String, Integer, List<TicketCheckProvider.SearchResult>> {
        String error = "";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loading++;
            toggleLoadingIndicator();
        }

        @Override
        protected List<TicketCheckProvider.SearchResult> doInBackground(String... params) {
            try {
                return checkProvider.search(params[0]);
            } catch (CheckException e) {
                error = e.getMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<TicketCheckProvider.SearchResult> checkResult) {
            if (checkResult == null) {
                Toast.makeText(SearchActivity.this, error, Toast.LENGTH_SHORT).show();
            } else {
                showList(checkResult);
            }
            loading--;
            toggleLoadingIndicator();
        }
    }

    public class CheckTask extends AsyncTask<String, Integer, TicketCheckProvider.CheckResult> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            pdCheck = ProgressDialog.show(SearchActivity.this, "",
                    getString(R.string.redeeming), true);
        }

        @Override
        protected TicketCheckProvider.CheckResult doInBackground(String... params) {
            if (params[0].matches("[0-9A-Za-z-]+")) {
                return checkProvider.check(params[0], new JSONObject());
            } else {
                return new TicketCheckProvider.CheckResult(TicketCheckProvider.CheckResult.Type.ERROR, getString(R.string.scan_result_invalid));
            }
        }

        @Override
        protected void onPostExecute(TicketCheckProvider.CheckResult checkResult) {
            pdCheck.dismiss();

            int default_string = 0;
            switch (checkResult.getType()) {
                case ERROR:
                    default_string = R.string.err_unknown;
                    break;
                case INPUT:
                    // TODO
                    break;
                case CONFIRMATION:
                    // TODO
                    break;
                case VALID:
                    default_string = R.string.scan_result_redeemed;
                    break;
            }

            new AlertDialog.Builder(SearchActivity.this)
                    .setMessage(getString(default_string))
                    .setPositiveButton(R.string.dismiss, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .show();

            startSearch(etQuery.getText().toString());
        }
    }

    private void showList(List<TicketCheckProvider.SearchResult> checkResult) {
        SearchResultAdapter adapter = new SearchResultAdapter(
                this, R.layout.listitem_searchresult, R.id.tvAttendeeName, checkResult);
        lvResults.setAdapter(adapter);
    }

    private void toggleLoadingIndicator() {
        if (loading > 0) {
            findViewById(R.id.toolbar_progress_bar).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.toolbar_progress_bar).setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
