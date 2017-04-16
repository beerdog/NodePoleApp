package se.jonastrogen.regionstats;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

import retrofit2.Response;
import se.jonastrogen.regionstats.models.CostModel;
import se.jonastrogen.regionstats.models.StatisticsModel;

public class MainActivity extends AppCompatActivity {

    private int mSeekbarValue;
    private Spinner mCountrySpinner;
    private TextView mEmissionText;
    private TextView mCostText;
    private CostModel mCostSweden;
    private StatisticsModel mStatisticsModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStatisticsModel = NodePoleApp.getInstance().getCurrentStatisticsModel();

        for(CostModel cost : mStatisticsModel.cost) {
            if (cost.country.equalsIgnoreCase("sweden")) {
                mCostSweden = cost;
                break;
            }
        }

        if (NodePoleApp.getInstance().isNetworkAvailable()) {
            new FetchStatistics().execute(mStatisticsModel.revision);
        }

        // Setup country spinner
        mCountrySpinner = (Spinner) findViewById(R.id.countrySpinner);
        mCountrySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateCalculations();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        ArrayAdapter<CostModel> countryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        countryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //countryAdapter.add(new CostModel());
        for(CostModel cost : mStatisticsModel.cost) {
            countryAdapter.add(cost);
        }
        mCountrySpinner.setAdapter(countryAdapter);

        // Result views
        mEmissionText = (TextView) findViewById(R.id.emissionResult);
        mCostText = (TextView) findViewById(R.id.costResult);

        final SeekBar seekBar = (SeekBar) findViewById(R.id.consumptionBar);
        mSeekbarValue = seekBar.getProgress();
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mSeekbarValue = progress;
                updateCalculations();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        ImageButton aboutButton = (ImageButton) findViewById(R.id.aboutButton);
        aboutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Not implemented", Toast.LENGTH_SHORT).show();
            }
        });

        updateCalculations();
    }

    private void updateCalculations() {
        CostModel selectedCountry = (CostModel) mCountrySpinner.getSelectedItem();

        long emission = 0;
        long cost = 0;

        // Multiply cost with kWh
        switch (mSeekbarValue){
            case 0:
                emission = (long) ((selectedCountry.emissions / 1000.0) * 16000);
                cost = (long) (selectedCountry.small * 2 * 1000 * mStatisticsModel.hours);
                // Calculate difference to sweden.
                emission -= (long) ((mCostSweden.emissions / 1000.0) * 16000);
                cost -= (long) (mCostSweden.small * 2 * 1000 * mStatisticsModel.hours);
                break;
            case 1:
                emission = (long) ((selectedCountry.emissions / 1000.0) * 40000);
                cost = (long) (selectedCountry.medium * 5 * 1000 * mStatisticsModel.hours);
                emission -= (long) ((mCostSweden.emissions / 1000.0) * 40000);
                cost -= (long) (mCostSweden.medium * 5 * 1000 * mStatisticsModel.hours);
                break;
            case 2:
                emission = (long) ((selectedCountry.emissions / 1000.0) * 120000);
                cost = (long) (selectedCountry.large * 15 * 1000 * mStatisticsModel.hours);
                emission -= (long) ((mCostSweden.emissions / 1000.0) * 120000);
                cost -= (long) (mCostSweden.large * 15 * 1000 * mStatisticsModel.hours);
                break;
        }

        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.GERMANY);
        String costSavings = formatter.format(cost);

        mEmissionText.setText(String.valueOf(emission) + " metric tons");
        mCostText.setText(costSavings);
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

        @Override
        protected void onPostExecute(StatisticsModel model) {
            if (model != null) {
                mStatisticsModel = model;
            }
        }
    }
}