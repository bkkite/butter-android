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

package butter.droid.base.providers.media;

import android.accounts.NetworkErrorException;

import com.squareup.okhttp.Call;

import java.util.ArrayList;
import java.util.List;

import butter.droid.base.ButterApplication;
import butter.droid.base.R;
import butter.droid.base.database.tables.Downloads;
import butter.droid.base.providers.media.models.Genre;
import butter.droid.base.providers.media.models.Media;

public class DownloadProvider extends MediaProvider {

    private static final DownloadProvider sMediaProvider = new DownloadProvider();

    private static Filters sFilters = new Filters();

    @Override
    public Call getList(final ArrayList<Media> existingList, Filters filters, final Callback callback) {
        String state;
        sFilters = filters;

        final ArrayList<Media> currentList;
        if (existingList == null) {
            currentList = new ArrayList<>();
        } else {
            currentList = (ArrayList<Media>) existingList.clone();
        }

        if (filters == null) {
            filters = new Filters();
        }

        switch (filters.sort) {
            default:
            case ALPHABET:
            case DATE:
            case HD:
            case RATING:
            case POPULARITY:
                state = null;
                break;

            case WATCHED:
                state = Downloads.WATCHED;
                break;

            case NOT_WATCHED:
                state = Downloads.NOT_WATCHED;
                break;
        }

        return fetchList(currentList, filters, state, callback);
    }

    /**
     * Fetch the list of movies from YTS
     *
     * @param currentList Current shown list to be extended
     * @param callback    Network callback
     * @return Call
     */

    private Call fetchList(final ArrayList<Media> currentList, final Filters filters, final String state, final Callback callback) {

        Downloads.getList(ButterApplication.getAppContext(), currentList, state, sMediaProvider);

        if (currentList == null) {
            callback.onFailure(new NetworkErrorException("No response"));
        } else {
            callback.onSuccess(filters, currentList, true);
        }

        return null;
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
        tabs.add(new NavInfo(R.id.downloaded_filter_all, Filters.Sort.POPULARITY, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.all), R.drawable.magnet_filter_hd));
        tabs.add(new NavInfo(R.id.downloaded_filter_not_watched, Filters.Sort.NOT_WATCHED, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.not_watched), R.drawable.ic_action_not_watched));
        tabs.add(new NavInfo(R.id.downloaded_filter_watched, Filters.Sort.WATCHED, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.watched), R.drawable.ic_action_watched));

        return tabs;
    }

    @Override
    public List<Genre> getGenres() {
        return new ArrayList<>();
    }

    @Override
    public boolean isLocal() {
        return true;
    }
}

