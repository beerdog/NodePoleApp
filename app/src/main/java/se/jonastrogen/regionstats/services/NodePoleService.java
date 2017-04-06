package se.jonastrogen.regionstats.services;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import se.jonastrogen.regionstats.models.StatisticsModel;

public interface NodePoleService {
    @GET("api/statistics/{revision}")
    Call<StatisticsModel> getStatistics(@Path("revision") int revision);
}
