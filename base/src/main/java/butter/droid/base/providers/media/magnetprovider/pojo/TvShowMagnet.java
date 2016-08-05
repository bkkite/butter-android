package butter.droid.base.providers.media.magnetprovider.pojo;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butter.droid.base.providers.media.MediaProvider;
import butter.droid.base.providers.media.models.Media;
import butter.droid.base.providers.media.models.Show;

public class TvShowMagnet {

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
    @SerializedName("nombusqueda")
    @Expose
    public String nombusqueda;
    @SerializedName("posterurl")
    @Expose
    public String posterurl;
    @SerializedName("puntuacio")
    @Expose
    public Double puntuacio;
    @SerializedName("popularitat")
    @Expose
    public Double popularitat;
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

    public Show getShow(MediaProvider mediaProvider)
    {
        Show show = new Show(mediaProvider, null);
        show.isMovie = false;

        show.videoId = this.id.toString();

        show.title = this.nom;
        show.year = this.year.toString();
        show.rating = this.puntuacio.toString();
        show.genre = this.categorias.toString();
        show.image = "http://image.tmdb.org/t/p/w342" + this.posterurl;
        show.headerImage = "http://image.tmdb.org/t/p/w1280" + this.backurl;

        {
            Date da = new Date(this.data.sec);
            show.runtime = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(da);
        }
        show.synopsis = this.info;
        show.fullImage = "image.tmdb.org/t/p/w1280" + this.backurl;

        return show;
    }
}