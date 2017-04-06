package se.jonastrogen.regionstats;

import android.app.Application;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import se.jonastrogen.regionstats.database.NodePoleDbHelper;
import se.jonastrogen.regionstats.models.CostModel;
import se.jonastrogen.regionstats.models.StatisticsModel;
import se.jonastrogen.regionstats.services.NodePoleService;


public class NodePoleApp extends Application {

    private NodePoleService _nodePoleService;
    private static NodePoleApp _instance;
    private NodePoleDbHelper _mDbHelper;
    private Gson _gson;

    @Override
    public void onCreate(){
        super.onCreate();

        _instance = this;
        _mDbHelper = new NodePoleDbHelper(this);
        _gson = new Gson();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://nodepole.azurewebsites.net/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        _nodePoleService = retrofit.create(NodePoleService.class);
    }

    public static NodePoleApp getInstance() {
        return _instance;
    }

    public NodePoleService getNodePoleService(){
        return _nodePoleService;
    }

    public StatisticsModel getCurrentStatisticsModel() {
        SQLiteDatabase db = _mDbHelper.getReadableDatabase();
        Cursor cursor = db.query("statistics", new String[]{"revision", "hours", "cost"}, null, null, null, null, null);
        if (cursor.moveToNext()) {
            StatisticsModel model = new StatisticsModel();
            model.revision = cursor.getInt(cursor.getColumnIndexOrThrow("revision"));
            model.hours = cursor.getInt(cursor.getColumnIndexOrThrow("hours"));
            String cost = cursor.getString(cursor.getColumnIndexOrThrow("cost"));
            Type type = new TypeToken<ArrayList<CostModel>>() {}.getType();
            model.cost = _gson.fromJson(cost, type);
            return model;
        }
        return null;
    }

    public void saveStatisticsModel(StatisticsModel model) {
        SQLiteDatabase db = _mDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("revision", model.revision);
        values.put("hours", model.hours);
        values.put("cost", _gson.toJson(model.cost));
        db.update("statistics", values, null, null);
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        // if no network is available networkInfo will be null, otherwise check if we are connected
        try {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
                return true;
            }
        } catch (Exception e) {
            Log.e("NodePole", "isNetworkAvailable():" + e.getMessage());
        }
        return false;
    }
}
