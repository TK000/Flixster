package com.tomoka.flixster;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.tomoka.flixster.models.Movie;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.parceler.Parcels;

import butterknife.BindView;
import butterknife.ButterKnife;
import cz.msebera.android.httpclient.Header;
import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

import static com.tomoka.flixster.MovieAdapter.placeholder;

public class MovieDetailsActivity extends AppCompatActivity {

    // the movie to display
    Movie movie;

    String youtubeId;

    public final static String place = "hi";

    // constants
    // the base URL for the API
    public final static String API_BASE_URL = "https://api.themoviedb.org/3";
    // the parameter name for the API key
    public final static String API_KEY_PARAM = "api_key";
    // tag for logging from this activity
    public final static String TAG = "MovieListActivity";

    // the view objects
    @BindView(R.id.tvTitle) TextView tvTitle;
    @BindView(R.id.tvOverview) TextView tvOverview;
    @BindView(R.id.rbVoteAverage) RatingBar rbVoteAverage;
    @BindView(R.id.tvDate) TextView tvDate;
    @BindView(R.id.imageView) ImageView imageView;
    // instance fields
    AsyncHttpClient client;

    // we need to track the item's position in the list
    //int position;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_details);
        // resolve the view objects
        ButterKnife.bind(this);

        client = new AsyncHttpClient();

        // unwrap the movie passed in via intent, using its simple name as a key
        movie = (Movie) Parcels.unwrap(getIntent().getParcelableExtra(Movie.class.getSimpleName()));
        Log.d("MovieDetailsActivity", String.format("Showing details for '%s'", movie.getTitle()));

        // set the title and overview
        tvTitle.setText(movie.getTitle());
        tvOverview.setText(movie.getOverview());
        tvDate.setText(String.format("Release date: %s", movie.getReleaseDate()));

        // vote average is 0..10, convert to 0..5 by dividing by 2
        float voteAverage = movie.getVoteAverage().floatValue();
        rbVoteAverage.setRating(voteAverage = voteAverage > 0 ? voteAverage / 2.0f : voteAverage);

        // load image using glide
        Glide.with(this)
                .load(this.getIntent().getStringExtra(placeholder))
                .apply(new RequestOptions()
                        .placeholder(R.drawable.flicks_backdrop_placeholder)
                        .error(R.drawable.flicks_backdrop_placeholder)
                        .transform(new RoundedCornersTransformation(25, 0)))
                .into(imageView);

        getVideo();
    }

    // get the youtube video
    public void getVideo() {
        // create the url
        String url = API_BASE_URL + String.format("/movie/%s/videos",  movie.getMovieId());
        Log.i(TAG, String.format("got id: %s", movie.getMovieId()));
        // set the request parameters
        RequestParams params = new RequestParams();
        params.put(API_KEY_PARAM, getString(R.string.api_key)); // API key, always required
        // execute a GET request expecting a JSON object response
        client.get(url, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                // get the youtube key
                try {
                    JSONArray a = response.getJSONArray("results");
                    JSONObject list = a.getJSONObject(0);
                    //youtubeId = list.getJSONObject().getString("key");
                    youtubeId = list.getString("key"); //Todo - if it exists??
                    Log.i(TAG, String.format("got key: %s", youtubeId));
                } catch (JSONException e) {
                    logError("Failed to get youtube id", e, true);
                }
            }
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                logError("Failed to get data from videos endpoint", throwable, true);
            }
        });
    }

    public void onPlay(View v) {
        Intent intent = new Intent(this, MovieTrailerActivity.class);
        intent.putExtra(place, youtubeId);
        this.startActivity(intent);
    }

    public void onBack(View v) {
        Intent intent = new Intent(getApplicationContext(), MovieDetailsActivity.class);
        setResult(RESULT_OK, intent); // set result code and bundle data for response
        finish(); // closes the edit activity, passes intent back to main
    }

    // handle errors, log and alert user
    private void logError(String message, Throwable error, boolean alertUser) {
        // always log the error
        Log.e(TAG, message, error);
        // alert the user to avoid silent errors
        if (alertUser) {
            // show a long toast with the error message
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        }
    }
}
