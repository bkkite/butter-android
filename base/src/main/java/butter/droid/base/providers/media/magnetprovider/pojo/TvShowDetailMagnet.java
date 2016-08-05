package butter.droid.base.providers.media.magnetprovider.pojo;

import android.content.Context;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butter.droid.base.providers.media.MediaProvider;
import butter.droid.base.providers.media.models.Episode;
import butter.droid.base.providers.media.models.Media;
import butter.droid.base.providers.media.models.Show;
import butter.droid.base.utils.FileUtils;

public class TvShowDetailMagnet {

    @SerializedName("info")
    @Expose
    public String info;
    @SerializedName("nom")
    @Expose
    public String nom;
    @SerializedName("categorias")
    @Expose
    public List<String> categorias = new ArrayList<String>();
    @SerializedName("backurl")
    @Expose
    public String backurl;
    @SerializedName("temporadas")
    @Expose
    public List<Temporada> temporadas = new ArrayList<Temporada>();
    @SerializedName("nombusqueda")
    @Expose
    public String nombusqueda;
    @SerializedName("posterurl")
    @Expose
    public String posterurl;
    @SerializedName("puntuacio")
    @Expose
    public String puntuacio;
    @SerializedName("popularitat")
    @Expose
    public String popularitat;
    @SerializedName("year")
    @Expose
    public Integer year;
    @SerializedName("data")
    @Expose
    public Data data;
    @SerializedName("id")
    @Expose
    public Integer id;

    /**
     * Test if there is an item that already exists
     *
     * @param results List with items
     * @return Return the index of the item in the results
     */
    public boolean isInResults(ArrayList<Media> results)
    {
        for (Media item : results) {
            if (item.videoId.equals(id)) return true;
        }
        return false;
    }

    public Show getShow(Context context, MediaProvider mediaProvider)
    {
        Show show = new Show(mediaProvider, null);

        show.title = this.nom;
        show.videoId = this.id.toString();
        show.imdbId = this.id.toString();
        show.tvdbId = this.id.toString();
        show.seasons = this.temporadas.size();
        show.year = this.year.toString();

        show.image = "http://image.tmdb.org/t/p/w342" + this.posterurl;
        show.headerImage = "http://image.tmdb.org/t/p/w342" + this.posterurl;
        show.fullImage = "http://image.tmdb.org/t/p/w1280" + this.backurl;

        show.status = Show.Status.CONTINUING;

        show.country = "country";
        show.network = "network";

        if (this.info != null)
            show.synopsis = this.info;
        else
            show.synopsis = "";

        {
            Date da = new Date(this.data.sec);
            show.runtime = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(da);
        }
        show.airDay = "";
        show.airTime = "";
        show.genre = this.categorias.toString();
        show.rating = this.puntuacio;

        for (Temporada temporada: this.temporadas)
        {
            for (Capitulo capitulo: temporada.capituls)
            {
                Episode episodeObject = new Episode(mediaProvider, null, null);


                if (capitulo.links != null) {

                    episodeObject.torrents.clear();

                    Media.Torrent torrent = new Media.Torrent();
                    torrent.url = capitulo.links.magnet;
                    torrent.hash = capitulo.links.hash;
                    torrent.seeds = torrent.peers = 0;
                    torrent.isDownloaded = FileUtils.getMagnetIsDownloaded(context, torrent.hash);
                    episodeObject.torrents.put(capitulo.links.calitat, torrent);
                }

                episodeObject.showName = this.nom;
                episodeObject.dateBased = true;
                episodeObject.aired = this.year;
                episodeObject.title = capitulo.nomcapitul;
                episodeObject.overview = capitulo.overviewcapitul;

                try {
                    episodeObject.season = Integer.parseInt(temporada.numerotemporada);
                }catch (Exception e)
                {
                    episodeObject.season = 1;
                }

                try {
                    episodeObject.episode = Integer.parseInt(capitulo.numerocapitul);
                }catch (Exception e)
                {
                    episodeObject.episode = 1;
                }

                episodeObject.videoId = show.videoId;
                episodeObject.imdbId = show.videoId;
                episodeObject.image = episodeObject.fullImage = episodeObject.headerImage = show.headerImage;

                show.episodes.add(episodeObject);
            }
        }

        return show;
    }
}