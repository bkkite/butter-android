package butter.droid.base.providers.media.magnet.pojo;

import android.content.Context;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butter.droid.base.providers.media.MediaProvider;
import butter.droid.base.providers.media.models.Media;
import butter.droid.base.providers.media.models.Movie;
import butter.droid.base.utils.FileUtils;

public class MovieMagnet {

    @SerializedName("info")
    @Expose
    public String info;
    @SerializedName("nombusqueda")
    @Expose
    public String nombusqueda;
    @SerializedName("categorias")
    @Expose
    public List<String> categorias = new ArrayList<String>();
    @SerializedName("backurl")
    @Expose
    public String backurl;
    @SerializedName("nom")
    @Expose
    public String nom;
    @SerializedName("data")
    @Expose
    public Data data;
    @SerializedName("posterurl")
    @Expose
    public String posterurl;
    @SerializedName("puntuacio")
    @Expose
    public Double puntuacio;
    @SerializedName("popularitat")
    @Expose
    public Double popularitat;
    @SerializedName("magnets")
    @Expose
    public Magnets magnets;
    @SerializedName("descargas")
    @Expose
    public Integer descargas;
    @SerializedName("id")
    @Expose
    public Integer id;
    @SerializedName("trailerurl")
    @Expose
    public String trailerurl;
    @SerializedName("year")
    @Expose
    public Integer year;

    /**
     * Test if there is an item that already exists
     *
     * @param results List with items
     * @return Return the index of the item in the results
     */
    public boolean isInResults(ArrayList<Media> results)
    {
        for (Media item : results) {
            if (item.videoId.equals(id.toString())) return true;
        }
        return false;
    }

    public boolean isDownloaded(Context context)
    {
        if (this.magnets.m1080.magnet != null){
            if (FileUtils.getMagnetIsDownloaded(context, this.magnets.m1080.hash))
                return true;
        }

        if (this.magnets.m720.magnet != null){
            if (FileUtils.getMagnetIsDownloaded(context, this.magnets.m720.hash))
                return true;
        }

        if (this.magnets.m3D.magnet != null){
            if (FileUtils.getMagnetIsDownloaded(context, this.magnets.m3D.hash))
                return true;
        }

        return false;
    }

    public boolean isHDMovie()
    {
        if (this.magnets.m1080.magnet != null){
                return true;
        }

        return false;
    }

    public Movie getMovie(Context context, MediaProvider mediaProvider)
    {
        Movie movie = new Movie(mediaProvider, null);

        movie.videoId = this.id.toString();

        movie.title = this.nom;
        movie.year = this.year.toString();
        movie.rating = this.puntuacio.toString();
        movie.genre = this.categorias.toString();
        movie.image = "http://image.tmdb.org/t/p/w342" + this.posterurl;
        movie.headerImage = "http://image.tmdb.org/t/p/w1280" + this.backurl;
        movie.trailer = this.trailerurl;
        {
            Date da = new Date(this.data.sec);
            movie.runtime = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(da);
        }
        movie.synopsis = this.info;
        movie.fullImage = "http://image.tmdb.org/t/p/w1280" + this.backurl;

        movie.torrents.clear();

        if (this.magnets.m1080.magnet != null)
        {
            Media.Torrent torrent = new Media.Torrent();
            torrent.url = this.magnets.m1080.magnet;
            torrent.peers = this.magnets.m1080.peers;
            torrent.seeds = this.magnets.m1080.peers;
            torrent.hash = this.magnets.m1080.hash;
            torrent.isDownloaded = FileUtils.getMagnetIsDownloaded(context, torrent.hash);

            movie.torrents.put(this.magnets.m1080.quality, torrent);
        }

        if (this.magnets.m720.magnet != null)
        {
            Media.Torrent torrent = new Media.Torrent();
            torrent.url = this.magnets.m720.magnet;
            torrent.peers = this.magnets.m720.peers;
            torrent.seeds = this.magnets.m720.peers;
            torrent.hash = this.magnets.m720.hash;
            torrent.isDownloaded = FileUtils.getMagnetIsDownloaded(context, torrent.hash);

            movie.torrents.put(this.magnets.m720.quality, torrent);
        }

        if (this.magnets.m3D.magnet != null)
        {
            Media.Torrent torrent = new Media.Torrent();
            torrent.url = this.magnets.m3D.magnet;
            torrent.peers = this.magnets.m3D.peers;
            torrent.seeds = this.magnets.m3D.peers;
            torrent.hash = this.magnets.m3D.hash;
            torrent.isDownloaded = FileUtils.getMagnetIsDownloaded(context, torrent.hash);

            movie.torrents.put(this.magnets.m3D.quality, torrent);
        }

        return movie;
    }
}