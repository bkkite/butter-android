package butter.droid.base.providers.media.MagnetProvider.MagnetPOJO;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class Temporada {
    @SerializedName("numerotemporada")
    @Expose
    public String numerotemporada;
    @SerializedName("nomtemporada")
    @Expose
    public String nomtemporada;
    @SerializedName("capituls")
    @Expose
    public List<Capitulo> capituls = new ArrayList<Capitulo>();
}
