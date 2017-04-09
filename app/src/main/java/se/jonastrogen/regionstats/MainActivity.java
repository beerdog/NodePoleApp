package se.jonastrogen.regionstats;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

import retrofit2.Response;
import se.jonastrogen.regionstats.models.CostModel;
import se.jonastrogen.regionstats.models.StatisticsModel;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final StatisticsModel model = NodePoleApp.getInstance().getCurrentStatisticsModel();
        CostModel costTemp = null;
        for(CostModel cost : model.cost) {
            if (cost.country.equalsIgnoreCase("sweden")) {
                costTemp = cost;
                break;
            }
        }
        final CostModel costSweden = costTemp;

        if (NodePoleApp.getInstance().isNetworkAvailable()) {
            new FetchStatistics().execute(model.revision);
        }

        final RelativeLayout resultLayout = (RelativeLayout) findViewById(R.id.resultLayout);
        resultLayout.setVisibility(View.GONE);

        // Setup country spinner
        final Spinner countrySpinner = (Spinner) findViewById(R.id.countrySpinner);
        countrySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                resultLayout.setVisibility(View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        final ArrayAdapter<CostModel> countryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        countryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //countryAdapter.add(new CostModel());
        for(CostModel cost : model.cost) {
            countryAdapter.add(cost);
        }
        countrySpinner.setAdapter(countryAdapter);

        // Setup size spinner
        final Spinner consumptionSpinner = (Spinner) findViewById(R.id.consumptionSpinner);
        final ArrayAdapter<CharSequence> consumptionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        consumptionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //consumptionAdapter.add("");
        consumptionAdapter.add("Small");
        consumptionAdapter.add("Medium");
        consumptionAdapter.add("Large");
        consumptionSpinner.setAdapter(consumptionAdapter);

        // Result views
        final TextView emissionText = (TextView) findViewById(R.id.emissionResult);
        final TextView costText = (TextView) findViewById(R.id.costResult);

        // Connect button
        Button button = (Button) findViewById(R.id.calculateButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Calculate and show result
                CostModel selectedCountry = (CostModel) countrySpinner.getSelectedItem();
                String selectedSize = (String) consumptionSpinner.getSelectedItem();

                int emission = 0;
                int cost = 0;

                // Multiply cost with kWh
                switch (selectedSize){
                    case "Small":
                        emission = (short) ((selectedCountry.emissions / 1000.0) * 16000);
                        cost = (int) (selectedCountry.small * 2 * 1000 * model.hours);
                        // Calculate difference to sweden.
                        emission -= (short) ((costSweden.emissions / 1000.0) * 16000);
                        cost -= (int) (costSweden.small * 2 * 1000 * model.hours);
                        break;
                    case "Medium":
                        emission = (short) ((selectedCountry.emissions / 1000.0) * 40000);
                        cost = (int) (selectedCountry.medium * 5 * 1000 * model.hours);
                        emission -= (short) ((costSweden.emissions / 1000.0) * 40000);
                        cost -= (int) (costSweden.medium * 5 * 1000 * model.hours);
                        break;
                    case "Large":
                        emission = (short) ((selectedCountry.emissions / 1000.0) * 120000);
                        cost = (int) (selectedCountry.large * 15 * 1000 * model.hours);
                        emission -= (short) ((costSweden.emissions / 1000.0) * 120000);
                        cost -= (int) (costSweden.large * 15 * 1000 * model.hours);
                        break;
                }

                NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.GERMANY);
                String costSavings = formatter.format(cost);

                emissionText.setText(String.valueOf(emission));
                costText.setText(costSavings);
                resultLayout.setVisibility(View.VISIBLE);
            }
        });
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