/*
 * This file is part of Butter.
 *
 * Butter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Butter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Butter. If not, see <http://www.gnu.org/licenses/>.
 */

package butter.droid.base.providers.media.magnet;

import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import butter.droid.base.ButterApplication;
import butter.droid.base.R;
import butter.droid.base.providers.media.magnet.pojo.MovieMagnet;
import butter.droid.base.providers.media.MediaProvider;
import butter.droid.base.providers.media.models.Genre;
import butter.droid.base.providers.media.models.Media;
import butter.droid.base.providers.media.models.Movie;
import butter.droid.base.utils.LocaleUtils;

public class PelisMagnetProvider extends MediaProvider {

    private static final PelisMagnetProvider sMediaProvider = new PelisMagnetProvider();
    private static Integer CURRENT_API = 0;
    private static final String[] API_URLS = {
            "http://pelismag.net/api"
    };
    public static String CURRENT_URL = API_URLS[CURRENT_API];

    private static Filters sFilters = new Filters();

    @Override
    protected Call enqueue(Request request, com.squareup.okhttp.Callback requestCallback) {
        Context context = ButterApplication.getAppContext();
        PackageInfo pInfo;
        String versionName = "0.0.0";
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            versionName = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        request = request.newBuilder().removeHeader("User-Agent").addHeader("User-Agent", String.format("Mozilla/5.0 (Linux; U; Android %s; %s; %s Build/%s) AppleWebkit/534.30 (KHTML, like Gecko) PT/%s", Build.VERSION.RELEASE, LocaleUtils.getCurrentAsString(), Build.MODEL, Build.DISPLAY, versionName)).build();
        return super.enqueue(request, requestCallback);
    }

    @Override
    public Call getList(final ArrayList<Media> existingList, Filters filters, final Callback callback) {
        sFilters = filters;

        final ArrayList<Media> currentList;
        if (existingList == null) {
            currentList = new ArrayList<>();
        } else {
            currentList = (ArrayList<Media>) existingList.clone();
        }

        ArrayList<NameValuePair> params = new ArrayList<>();

        if (filters == null) {
            filters = new Filters();
        }

        if (filters.keywords != null) {
            params.add(new NameValuePair("keywords", filters.keywords));
        }

        switch (filters.sort)
        {
            default:
            case ALPHABET:
                params.add(new NameValuePair("sort_by", "''"));
                break;
            case DATE:
                params.add(new NameValuePair("sort_by", "date_added"));
                break;
            case HD:
                params.add(new NameValuePair("sort_by", "date_added"));
                filters.onlyHD = true;
                break;
            case RATING:
                params.add(new NameValuePair("sort_by", "rating"));
                break;
            case POPULARITY:
                break;
        }

        if (filters.page != null) {
            params.add(new NameValuePair("page", filters.page.toString()));
        }

        String query = "?" + buildQuery(params);

        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(CURRENT_URL + query);
        requestBuilder.tag(MEDIA_CALL);

        return fetchList(currentList, requestBuilder, filters, callback);

    }

    /**
     * Fetch the list of movies from YTS
     *
     * @param currentList    Current shown list to be extended
     * @param requestBuilder Request to be executed
     * @param callback       Network callback
     * @return Call
     */
    private Call fetchList(final ArrayList<Media> currentList, final Request.Builder requestBuilder, final Filters filters, final Callback callback) {
        return enqueue(requestBuilder.build(), new com.squareup.okhttp.Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                String url = requestBuilder.build().urlString();
                if (CURRENT_API >= API_URLS.length - 1) {
                    callback.onFailure(e);
                } else {
                    if(url.contains(API_URLS[CURRENT_API])) {
                        url = url.replace(API_URLS[CURRENT_API], API_URLS[CURRENT_API + 1]);
                        url = url.replace(API_URLS[CURRENT_API], API_URLS[CURRENT_API + 1]);
                        CURRENT_API++;
                    } else {
                        url = url.replace(API_URLS[CURRENT_API - 1], API_URLS[CURRENT_API]);
                        url = url.replace(API_URLS[CURRENT_API - 1], API_URLS[CURRENT_API]);
                    }
                    requestBuilder.url(url);
                    fetchList(currentList, requestBuilder, filters, callback);
                }
            }

            @Override
            public void onResponse(Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseStr;
                    try {
                        responseStr = response.body().string();
                    } catch (SocketException e) {
                        onFailure(response.request(), new IOException("Socket failed"));
                        return;
                    }

                    List<MovieMagnet> result;
                    try
                    {
                        Type listMagnet = new TypeToken<List<MovieMagnet>>() {}.getType();
                        result = mGson.fromJson(responseStr, listMagnet);
                    } catch (IllegalStateException e) {
                        onFailure(response.request(), new IOException("JSON Failed"));
                        return;
                    } catch (JsonSyntaxException e) {
                        onFailure(response.request(), new IOException("JSON Failed"));
                        return;
                    }

                    if(result == null) {
                        callback.onFailure(new NetworkErrorException("No response"));
                    } else if(result == null || result.size() <= 0) {
                        callback.onFailure(new NetworkErrorException("No movies found"));
                    } else {
                        PelisMagnetResponse pelisMagmet = new PelisMagnetResponse(result);
                        ArrayList<Media> formattedData = pelisMagmet.formatForApp(currentList, filters.onlyHD);
                        callback.onSuccess(filters, formattedData, true);
                        return;
                    }
                }
                onFailure(response.request(), new IOException("Couldn't connect to PelisMagnet"));
            }
        });
    }

    @Override
    public Call getDetail(ArrayList<Media> currentList, Integer index, Callback callback) {
        ArrayList<Media> returnList = new ArrayList<>();
        returnList.add(currentList.get(index));
        callback.onSuccess(null, returnList, true);
        return null;
    }

    private class PelisMagnetResponse
    {
        public List<MovieMagnet> movies;

        PelisMagnetResponse(List<MovieMagnet> movies)
        {
            this.movies = movies;
        }

        /**
         * Format data for the application
         *
         * @param existingList List to be extended
         * @return List with items
         */
        public ArrayList<Media> formatForApp(ArrayList<Media> existingList, boolean onlyHD) {

            Context context = ButterApplication.getAppContext();

            for (MovieMagnet movieMagnet : movies)
            {
                if (movieMagnet.isInResults(existingList) == false
                        && (onlyHD == false || movieMagnet.isHDMovie()))
                {
                    Movie movie = movieMagnet.getMovie(context, sMediaProvider);
                    existingList.add(movie);
                }
            }
            return existingList;
        }
    }

    @Override
    public int getLoadingMessage() {
        return R.string.loading_movies;
    }

    @Override
    public List<NavInfo> getNavigation()
    {
        List<NavInfo> tabs = new ArrayList<>();
        tabs.add(new NavInfo(R.id.magnet_filter_release_date,Filters.Sort.HD, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.hd),R.drawable.magnet_filter_hd));
        tabs.add(new NavInfo(R.id.magnet_filter_release_date,Filters.Sort.DATE, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.release_date),R.drawable.magnet_filter_release_date));
        tabs.add(new NavInfo(R.id.magnet_filter_top_rated,Filters.Sort.RATING, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.top_rated),R.drawable.magnet_filter_top_rated));
        tabs.add(new NavInfo(R.id.magnet_filter_popular_now,Filters.Sort.POPULARITY, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.popular),R.drawable.magnet_filter_popular_now));
        return tabs;
    }

    @Override
    public List<Genre> getGenres() {
        return new ArrayList<>();
    }
}
