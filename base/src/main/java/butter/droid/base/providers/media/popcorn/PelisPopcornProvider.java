/**
 * Created by bkkite on 3/11/16.
 */

/*
 * This file is part of Popcorn Time.
 *
 * Popcorn Time is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Popcorn Time is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Popcorn Time. If not, see <http://www.gnu.org/licenses/>.
 */

package butter.droid.base.providers.media.popcorn;

import android.accounts.NetworkErrorException;

import com.google.gson.internal.LinkedTreeMap;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butter.droid.base.ButterApplication;
import butter.droid.base.R;
import butter.droid.base.providers.media.MediaProvider;
import butter.droid.base.providers.media.models.Genre;
import butter.droid.base.providers.media.models.Media;
import butter.droid.base.providers.media.models.Movie;
import butter.droid.base.providers.subs.SubsProvider;
import butter.droid.base.providers.subs.YSubsProvider;
import butter.droid.base.utils.StringUtils;
import timber.log.Timber;

public class PelisPopcornProvider extends MediaProvider {

    private static Integer CURRENT_API = 0;
    private static final String[] API_URLS = {"https://movies-v2.api-fetch.website/"};
    private static final PelisPopcornProvider sMediaProvider = new PelisPopcornProvider();
    private static final SubsProvider sSubsProvider = new YSubsProvider();

    @Override
    public Call getList(final ArrayList<Media> existingList, MediaProvider.Filters filters, final Callback callback) {
        final ArrayList<Media> currentList;
        if (existingList == null) {
            currentList = new ArrayList<>();
        } else {
            currentList = (ArrayList<Media>) existingList.clone();
        }

        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new NameValuePair("limit", "30"));

        if (filters == null) {
            filters = new Filters();
        }

        if (filters.keywords != null) {
            params.add(new NameValuePair("keywords", filters.keywords));
        }

        if (filters.genre != null) {
            params.add(new NameValuePair("genre", filters.genre));
        }

        if (filters.order == Filters.Order.ASC) {
            params.add(new NameValuePair("order", "1"));
        } else {
            params.add(new NameValuePair("order", "-1"));
        }

        if(filters.langCode != null) {
            params.add(new NameValuePair("lang", filters.langCode));
        }

        String sort;
        switch (filters.sort) {
            default:
            case POPULARITY:
                sort = "popularity";
                break;
            case YEAR:
                sort = "year";
                break;
            case DATE:
                sort = "last added";
                break;
            case RATING:
                sort = "rating";
                break;
            case ALPHABET:
                sort = "name";
                break;
            case TRENDING:
                sort = "trending";
                break;
        }

        params.add(new NameValuePair("sort", sort));

        String url = API_URLS[CURRENT_API] + "movies/";
        if (filters.page != null) {
            if (filters.page == 0)
                filters.page++;

            url += filters.page;
        } else {
            url += "1";
        }

        Request.Builder requestBuilder = new Request.Builder();
        String query = buildQuery(params);
        url = url + "?" + query;
        requestBuilder.url(url);
        requestBuilder.tag(MEDIA_CALL);

        Timber.d("PelisPopcornProvider", "Making request to: " + url);

        return fetchList(currentList, requestBuilder, filters, callback);
    }

    /**
     * Fetch the list of movies from API
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
                        CURRENT_API++;
                    } else {
                        url = url.replace(API_URLS[CURRENT_API - 1], API_URLS[CURRENT_API]);
                    }
                    requestBuilder.url(url);
                    fetchList(currentList, requestBuilder, filters, callback);
                }
            }

            @Override
            public void onResponse(Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        String responseStr = response.body().string();

                        ArrayList<LinkedTreeMap<String, Object>> list = null;
                        if (responseStr.isEmpty()) {
                            list = new ArrayList<>();
                        } else {
                            list = (ArrayList<LinkedTreeMap<String, Object>>) mGson.fromJson(responseStr, ArrayList.class);
                        }

                        MovieResponse result = new MovieResponse(list);
                        if (list == null) {
                            callback.onFailure(new NetworkErrorException("Empty response"));
                        } else {
                            ArrayList<Media> formattedData = result.formatListForPopcorn(currentList);
                            callback.onSuccess(filters, formattedData, list.size() > 0);
                            return;
                        }
                    }
                } catch (Exception e) {
                    callback.onFailure(e);
                }
                callback.onFailure(new NetworkErrorException("Couldn't connect to MovieAPI"));
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

    @Override
    public int getLoadingMessage() {
        return R.string.loading_movies;
    }

    @Override
    public List<NavInfo> getNavigation() {
        List<NavInfo> tabs = new ArrayList<>();
        tabs.add(new NavInfo(R.id.popcorn_filter_trending,Filters.Sort.TRENDING, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.trending),R.drawable.popcorn_filter_trending));
        tabs.add(new NavInfo(R.id.popcorn_filter_popular_now,Filters.Sort.POPULARITY, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.popular),R.drawable.popcorn_filter_popular_now));
        tabs.add(new NavInfo(R.id.popcorn_filter_top_rated,Filters.Sort.RATING, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.top_rated),R.drawable.popcorn_filter_top_rated));
        tabs.add(new NavInfo(R.id.popcorn_filter_release_date,Filters.Sort.DATE, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.release_date),R.drawable.popcorn_filter_release_date));
        tabs.add(new NavInfo(R.id.popcorn_filter_year,Filters.Sort.YEAR, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.year),R.drawable.popcorn_filter_year));
        tabs.add(new NavInfo(R.id.popcorn_filter_a_to_z,Filters.Sort.ALPHABET, Filters.Order.ASC, ButterApplication.getAppContext().getString(R.string.a_to_z),R.drawable.popcorn_filter_a_to_z));
        return tabs;
    }

    @Override
    public List<Genre> getGenres() {
        return new ArrayList<>();
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    private class MovieResponse {
        LinkedTreeMap<String, Object> movieData;
        ArrayList<LinkedTreeMap<String, Object>> moviesList;

        public MovieResponse(ArrayList<LinkedTreeMap<String, Object>> moviesList) {
            this.moviesList = moviesList;
        }

        public ArrayList<Media> formatListForPopcorn(ArrayList<Media> existingList) {
            for (LinkedTreeMap<String, Object> item : moviesList) {
                Movie movie = new Movie(sMediaProvider, sSubsProvider);

                movie.videoId = (String) item.get("imdb_id");
                movie.imdbId = movie.videoId;

                movie.title = (String) item.get("title");
                movie.year = (String) item.get("year");

                List<String> genres = (ArrayList<String>) item.get("genres");

                movie.genre = "";
                if (genres.size() > 0) {
                    StringBuilder stringBuilder = new StringBuilder();
                    for (String genre : genres) {
                        if (stringBuilder.length() > 0) {
                            stringBuilder.append(", ");
                        }
                        stringBuilder.append(StringUtils.capWords(genre));
                    }
                    movie.genre = stringBuilder.toString();
                }

                movie.rating = Double.toString(((LinkedTreeMap<String, Double>) item.get("rating")).get("percentage") / 10);
                movie.trailer = (String) item.get("trailer");
                movie.runtime = (String) item.get("runtime");
                movie.synopsis = (String) item.get("synopsis");
                movie.certification = (String) item.get("certification");

                LinkedTreeMap<String, String> images = (LinkedTreeMap<String, String>) item.get("images");
                if(!images.get("poster").contains("images/posterholder.png")) {
                    movie.image = images.get("poster").replace("/original/", "/medium/");
                    movie.fullImage = images.get("poster");
                }
                if(!images.get("poster").contains("images/posterholder.png"))
                    movie.headerImage = images.get("fanart").replace("/original/", "/medium/");

                LinkedTreeMap<String, LinkedTreeMap<String, LinkedTreeMap<String, Object>>> torrents = (LinkedTreeMap<String, LinkedTreeMap<String, LinkedTreeMap<String, Object>>>) item.get("torrents");
                if (torrents != null) {
                    for (Map.Entry<String, LinkedTreeMap<String, LinkedTreeMap<String, Object>>> langTorrentObj : torrents.entrySet()) {
                        String langCode = langTorrentObj.getKey();
                        Map<String, Media.Torrent> torrentMap = new HashMap<>();
                        for (Map.Entry<String, LinkedTreeMap<String, Object>> torrentEntry : langTorrentObj.getValue().entrySet()) {
                            LinkedTreeMap<String, Object> torrentObj = torrentEntry.getValue();
                            String quality = torrentEntry.getKey();
                            if (quality == null) continue;

                            Media.Torrent torrent = new Media.Torrent();

                            torrent.seeds = ((Double) torrentObj.get("seed")).intValue();
                            torrent.peers = ((Double) torrentObj.get("peer")).intValue();
                            torrent.url = (String) torrentObj.get("url");

                            torrentMap.put(quality, torrent);
                        }

                        movie.torrents = torrentMap;
                    }
                }

                existingList.add(movie);
            }
            return existingList;
        }
    }

}
