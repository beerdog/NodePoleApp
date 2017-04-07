package se.jonastrogen.regionstats;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.IOException;

import retrofit2.Response;
import se.jonastrogen.regionstats.models.StatisticsModel;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StatisticsModel model = NodePoleApp.getInstance().getCurrentStatisticsModel();

        if (NodePoleApp.getInstance().isNetworkAvailable()) {
            new FetchStatistics().execute(model.revision);
        }
    }

    private class FetchStatistics extends AsyncTask<Integer, Void, StatisticsModel> {

        @Override
        protected StatisticsModel doInBackground(Integer... params) {
            try {
                // Check if we have som new statistics
                Response<StatisticsModel> model = NodePoleApp.getInstance().getNodePoleService().getStatistics(params[0]).execute();
                // We send a 204 No Content if the revision we have is the same as the servers.
                if (model.isSuccessful() && model.code() != 204) {
                    NodePoleApp.getInstance().saveStatisticsModel(model.body());
                    return model.body();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}